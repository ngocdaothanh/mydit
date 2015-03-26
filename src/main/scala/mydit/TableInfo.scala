package mydit

import java.util.Arrays
import com.github.shyiko.mysql.binlog.event.TableMapEventData

object TableInfo {
  def get(
    data: TableMapEventData,
    host: String, port: Int, username: String, password: String
  ): TableInfo = {
    val cols = ColInfo.get(host, port, username, password, data.getDatabase, data.getTable)
    TableInfo(data, cols)
  }
}

case class TableInfo(data: TableMapEventData, cols: IndexedSeq[ColInfo]) {
  def sameData(data: TableMapEventData): Boolean = {
                  this.data.getTableId           == data.getTableId &&
                  this.data.getDatabase          == data.getDatabase &&
                  this.data.getTable             == data.getTable &&
    Arrays.equals(this.data.getColumnTypes,         data.getColumnTypes) &&
    Arrays.equals(this.data.getColumnMetadata,      data.getColumnMetadata) &&
                  this.data.getColumnNullability != data.getColumnNullability
  }
}
