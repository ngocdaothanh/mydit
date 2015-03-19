package mydit

import scala.collection.mutable.Queue
import scala.util.control.NonFatal

class Rep(config: Config) extends RepEvent.Listener {
  private val mo = new MongoDBApplier(
    config.mongoUri, config.mongoBinlogDb, config.mongoBinlogColl,
    config.enumToString
  )

  private val failedEventQ = Queue[RepEvent.Event]()

  val my = new MySQLExtractor(
    config.myHost, config.myPort, config.myUsername, config.myPassword, config.myOnly,
    mo.binlogGetPosition
  )
  my.addListener(this)
  my.start()

  //--------------------------------------------------------------------------

  /** This method is synchronized because the replication must be in order. */
  override def onEvent(event: RepEvent.Event): Unit = synchronized {
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

  /** @return false on failure */
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
