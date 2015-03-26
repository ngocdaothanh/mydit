package mydit

import java.sql.Connection
import java.sql.DriverManager
import scala.collection.mutable.ArrayBuffer

/** enumValues: Only used when column type is "enum". */
case class ColInfo(name: String, typeLowerCase: String, enumValues: IndexedSeq[String])

/**
 * mysql-binlog-connector-java doesn't provide column names of tables.
 * We need to use JDBC.
 *
 * Each DB uses a connection (even for a single server). The connections are
 * kept alive to avoid reconnection time.
 */
object ColInfo {
  Class.forName("com.mysql.jdbc.Driver")

  def get(
    host: String, port: Int, username: String, password: String, db: String,
    table: String
  ): IndexedSeq[ColInfo] = {
    // http://www.java2s.com/Code/Java/Database-SQL-JDBC/GetColumnName.htm
    // http://docs.oracle.com/javase/6/docs/api/java/sql/DatabaseMetaData.html#getColumns(java.lang.String, java.lang.String, java.lang.String, java.lang.String)

    val url  = "jdbc:mysql://" + host + ":" + port + "/" + db
    val con  = DriverManager.getConnection(url, username, password)
    val meta = con.getMetaData
    val cols = meta.getColumns(null, null, table, null)
    val buf  = ArrayBuffer[ColInfo]()

    while (cols.next()) {
      val name          = cols.getString("COLUMN_NAME")
      val typeLowerCase = cols.getString("TYPE_NAME").toLowerCase
      if (typeLowerCase.equals("enum")) {
        val enumValues = getEnumValues(con, table, name)
        buf.append(ColInfo(name, typeLowerCase, enumValues))
      } else {
        buf.append(ColInfo(name, typeLowerCase, IndexedSeq.empty))
      }
    }

    val ret = buf.toIndexedSeq

    cols.close()
    con.close()

    Log.trace("{}.{}: {}", db, table, ret)
    ret
  }

  private def getEnumValues(con: Connection, table: String, enumCol: String): IndexedSeq[String] = {
    val stmt = con.createStatement()
    val sql  = "SHOW COLUMNS FROM " + table + " LIKE '" + enumCol + "'"
    val rs   = stmt.executeQuery(sql)
    if (!rs.next()) throw new Exception(sql + " returns empty result")

    val enm = rs.getString("Type")  // Ex: "enum('pending','verified')"
    if (!enm.startsWith("enum(")) throw new Exception(table + "." + enumCol + " is not an enum")

    val valueString  = enm.substring("enum(".length(), enm.length() - 1)
    val quotedValues = valueString.split(",")
    val ret          = new Array[String](quotedValues.size)
    for (i <- 0 until quotedValues.size) {
      val quotedValue       = quotedValues(i)
      val trimedQuotedValue = quotedValue.trim()
      val value             = trimedQuotedValue.substring(1, trimedQuotedValue.length() - 1)
      ret(i)                = value
    }

    rs.close()
    stmt.close()

    ret
  }
}
