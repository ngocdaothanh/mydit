package mydit

import com.github.shyiko.mysql.binlog.BinaryLogClient
import com.github.shyiko.mysql.binlog.event.DeleteRowsEventData
import com.github.shyiko.mysql.binlog.event.Event
import com.github.shyiko.mysql.binlog.event.EventData
import com.github.shyiko.mysql.binlog.event.EventHeaderV4
import com.github.shyiko.mysql.binlog.event.FormatDescriptionEventData
import com.github.shyiko.mysql.binlog.event.RotateEventData
import com.github.shyiko.mysql.binlog.event.TableMapEventData
import com.github.shyiko.mysql.binlog.event.UpdateRowsEventData
import com.github.shyiko.mysql.binlog.event.WriteRowsEventData

// https://github.com/shyiko/rook/blob/master/rook-source-mysql/src/main/java/com/github/shyiko/rook/source/mysql/MySQLReplicationStream.java

/** @param only empty means all DBs should be replicated */
class MySQLExtractor(
  host: String, port: Int, username: String, password: String, only: Seq[String],
  binlogFilename_Position: Option[(String, Long)]
) {
  private val client = new BinaryLogClient(host, port, username, password)

  binlogFilename_Position.foreach { case (f, p) =>
    client.setBinlogFilename(f)
    client.setBinlogPosition(p)
  }

  client.registerEventListener(new BinaryLogClient.EventListener {
    override def onEvent(event: Event) {
      Log.trace(event.toString)

      event.getData.asInstanceOf[EventData] match {
        case data: FormatDescriptionEventData =>
          onFormatDescription(data)

        case data: RotateEventData =>
          onRotate(data)

        case data: TableMapEventData =>
          onTableMap(data)

        case data: WriteRowsEventData =>
          onInsert(event.getHeader.asInstanceOf[EventHeaderV4], data)

        case data: UpdateRowsEventData =>
          onUpdate(event.getHeader.asInstanceOf[EventHeaderV4], data)

        case data: DeleteRowsEventData =>
          onRemove(event.getHeader.asInstanceOf[EventHeaderV4], data)

        case _ =>
      }
    }
  })

  // No need to sync tablesById because it's only modified within "onEvent"
  // which should not be called concurrently
  private var tablesById = Map[Long, TableInfo]()
  private var listeners  = Seq[RepEvent.Listener]()

  def addListener(listener: RepEvent.Listener) {
    synchronized {
      listeners = listeners :+ listener
    }
  }

  def start() {
    client.connect()
  }

  //--------------------------------------------------------------------------

  private def onFormatDescription(data: FormatDescriptionEventData) {
    Log.info(
      "{}:{} server version: {}, binlog version: {}",
      host, port.toString,
      data.getServerVersion, data.getBinlogVersion.toString
    )
  }

  private def onRotate(data: RotateEventData) {
    val filename = data.getBinlogFilename
    val position = data.getBinlogPosition
    Log.info("Rotated filename: {}, position: {}", filename, position)

    synchronized {
      for (listener <- listeners) {
        listener.onEvent(new RepEvent.BinlogRotate(filename, position))
      }
    }
  }

  private def onTableMap(data: TableMapEventData) {
    val db = data.getDatabase
    if (only != null && !only.contains(db)) return

    val table = data.getTable
    val cols = ColInfo.get(host, port, username, password, db, table)
    tablesById = tablesById.updated(data.getTableId, TableInfo(db, table, cols))
  }

  //--------------------------------------------------------------------------

  private def onInsert(header: EventHeaderV4, data: WriteRowsEventData) {
    val np = header.getNextPosition
    val id = data.getTableId
    val ti = tablesById.get(id)

    if (doesDbNeedRep(ti, np)) synchronized {
      for (listener <- listeners) {
        listener.onEvent(new RepEvent.Insert(np, ti.get, data))
      }
    }
  }

  private def onUpdate(header: EventHeaderV4, data: UpdateRowsEventData) {
    val np = header.getNextPosition
    val id = data.getTableId
    val ti = tablesById.get(id)

    if (doesDbNeedRep(ti, np)) synchronized {
      for (listener <- listeners) {
        listener.onEvent(new RepEvent.Update(np, ti.get, data))
      }
    }
  }

  private def onRemove(header: EventHeaderV4, data: DeleteRowsEventData) {
    val np = header.getNextPosition
    val id = data.getTableId
    val ti = tablesById.get(id)

    if (doesDbNeedRep(ti, np)) synchronized {
      for (listener <- listeners) {
        listener.onEvent(new RepEvent.Remove(np, ti.get, data))
      }
    }
  }

  /** Calls onBinlogNextPosition of the listeners if the DB doesn't need replication. */
  private def doesDbNeedRep(ti: Option[TableInfo], nextPosition: Long): Boolean = {
    if (ti.isDefined && (only.isEmpty || only.contains(ti.get.db))) return true

    synchronized {
      for (listener <- listeners) {
        listener.onEvent(new RepEvent.BinlogNextPosition(nextPosition))
      }
    }
    false
  }
}
