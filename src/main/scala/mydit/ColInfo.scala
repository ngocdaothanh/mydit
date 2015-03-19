package mydit

import java.sql.Connection
import java.sql.DriverManager

/** enumValues: Only used when column type is "enum". */
case class ColInfo(name: String, typeLowerCase: String, enumValues: Seq[String])

/**
 * mysql-binlog-connector-java doesn't provide column names of tables.
 * We need to use JDBC.
 *
 * Each DB uses a connection (even for a single server). The connections are
 * kept alive to avoid reconnection time.
 */
object ColInfo {
  Class.forName("com.mysql.jdbc.Driver")

  private var consByDBName = Map[String, Connection]()

  def get(
    host: String, port: Int, username: String, password: String, db: String,
    table: String
  ): Seq[ColInfo] = synchronized {
    val con = consByDBName.get(db) match {
      case None =>
        val url = "jdbc:mysql://" + host + ":" + port + "/" + db + "?autoReconnect=true"
        val con = DriverManager.getConnection(url, username, password)
        consByDBName = consByDBName.updated(db, con)
        con

      case Some(con) =>
        con
    }

    get(con, table)
  }

  def get(con: Connection, table: String): Seq[ColInfo] = {
    // http://www.java2s.com/Code/Java/Database-SQL-JDBC/GetColumnName.htm

    val meta = con.getMetaData
    val cols = meta.getColumns(null, null, table, null)
    var ret  = Seq[ColInfo]()

    while (cols.next()) {
      val name          = cols.getString("COLUMN_NAME")
      val typeLowerCase = cols.getString("TYPE_NAME").toLowerCase
      if (typeLowerCase.equals("enum")) {
        val enumValues = getEnumValues(con, table, name)
        ret = ret :+ ColInfo(name, typeLowerCase, enumValues)
      } else {
        ret = ret :+ ColInfo(name, typeLowerCase, Seq.empty)
      }
    }

    cols.close()
    ret
  }

  private def getEnumValues(con: Connection, table: String, enumCol: String): Seq[String] = {
    val stmt = con.createStatement()
    val sql  = "SHOW COLUMNS FROM " + table + " LIKE '" + enumCol + "'"
    val rs   = stmt.executeQuery(sql)
    if (!rs.next()) throw new Exception(sql + " returns empty result")

    val enm = rs.getString("Type")  // Ex: "enum('pending','verified')"
    if (!enm.startsWith("enum(")) throw new Exception(table + "." + enumCol + " is not an enum")

    var ret = Seq[String]()

    val valueString  = enm.substring("enum(".length(), enm.length() - 1)
    val quotedValues = valueString.split(",")
    for (quotedValue <- quotedValues) {
      val trimedQuotedValue = quotedValue.trim()
      val value             = trimedQuotedValue.substring(1, trimedQuotedValue.length() - 1)
      ret = ret :+ value
    }

    rs.close()
    stmt.close()
    ret
  }
}
