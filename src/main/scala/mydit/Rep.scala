package mydit

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import scala.collection.mutable.Queue
import scala.util.control.NonFatal

class Rep(config: Config) extends RepEvent.Listener {
  private val mo = new MongoDBApplier(
    config.mongoUri, config.mongoBinlogDb, config.mongoBinlogColl,
    config.enumToString
  )

  private val failedEventQ  = Queue[RepEvent.Event]()
  private val singleThreadE = Executors.newSingleThreadExecutor()

  val my = new MySQLExtractor(
    config.myHost, config.myPort, config.myUsername, config.myPassword, config.myOnly,
    mo.binlogGetPosition
  )
  my.addListener(this)
  my.start()

  //--------------------------------------------------------------------------

  override def onEvent(event: RepEvent.Event) {
    // Queue the event to be processed one by one because the replication
    // must be in order. For better performance, we run in a separate thread
    // to avoid blocking the MySQL binlog event reader thread.
    singleThreadE.execute(new Runnable {
      override def run() {
        processEvent(event)
      }
    })
  }

  private def processEvent(event: RepEvent.Event) {
    // Replicate things in the queue first
    var ok = true
    while (ok && failedEventQ.nonEmpty) {
      val failedEvent = failedEventQ.front
      ok = replicate(failedEvent)
      if (ok) failedEventQ.dequeue()
    }

    // Replicate this latest event if the above are replicated ok
    if (ok) ok = replicate(event)

    if (!ok) failedEventQ.enqueue(event)

    val qSize = failedEventQ.size
    if (qSize > 0) {
      Log.info("Failed replication event queue size: {}", qSize)
      if (qSize > config.maxFailedEventQueueSize) {
        Log.error(
          "Replicator program now exits because the failed replication event queue size exceeds {} (see config/application.conf)",
          config.maxFailedEventQueueSize
        )
        System.exit(-1)
      }
    }
  }

  /** @return false on any Exception */
  private def replicate(event: RepEvent.Event): Boolean = {
    try {
      event match {
        case e: RepEvent.BinlogRotate =>
          mo.binlogRotate(e.filename, e.position)

        case e: RepEvent.BinlogNextPosition =>
          mo.binlogNextPosition(e.position)

        case e: RepEvent.Insert =>
          mo.insert(e.nextPosition, e.ti.db, e.ti.table, e.ti.cols, e.data)

        case e: RepEvent.Update =>
          mo.update(e.nextPosition, e.ti.db, e.ti.table, e.ti.cols, e.data)

        case e: RepEvent.Remove =>
          mo.remove(e.nextPosition, e.ti.db, e.ti.table, e.ti.cols, e.data)
      }
      true
    } catch {
      case NonFatal(e) =>
        Log.warn("Could not replicate to MongoDB", e)
        false
    }
  }
}
