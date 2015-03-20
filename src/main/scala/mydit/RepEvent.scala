package mydit

import java.io.Serializable

import com.github.shyiko.mysql.binlog.event.DeleteRowsEventData
import com.github.shyiko.mysql.binlog.event.UpdateRowsEventData
import com.github.shyiko.mysql.binlog.event.WriteRowsEventData

/** The events are serializable and may be played back later. */
object RepEvent {
  sealed trait Event extends Serializable
  case class BinlogRotate(filename: String, position: Long)                       extends Event
  case class BinlogNextPosition(position: Long)                                   extends Event
  case class Insert(nextPosition: Long, ti: TableInfo, data: WriteRowsEventData)  extends Event
  case class Update(nextPosition: Long, ti: TableInfo, data: UpdateRowsEventData) extends Event
  case class Remove(nextPosition: Long, ti: TableInfo, data: DeleteRowsEventData) extends Event

  trait Listener {
    /**
     * This method is run on the MySQL binlog event reader thread. For
     * better performance, it should avoid blocking the reader thread by
     * processing the event on a separate thread, if necessary.
     *
     * How EventListener#onEvent is executed:
     * https://github.com/shyiko/mysql-binlog-connector-java/issues/32
     */
    def onEvent(event: Event)
  }
}
