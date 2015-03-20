package mydit

import java.io.Serializable
import java.math.BigDecimal
import java.util.BitSet

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

import com.github.shyiko.mysql.binlog.event.DeleteRowsEventData
import com.github.shyiko.mysql.binlog.event.UpdateRowsEventData
import com.github.shyiko.mysql.binlog.event.WriteRowsEventData

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.mongodb.DuplicateKeyException
import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.ReadPreference
import com.mongodb.WriteConcern

/**
 * All writes are done with WriteConcern.ACKNOWLEDGED.
 *
 * Binlog position will be written as:
 * <code>{filename: "mysql-bin-changelog.002301", position: NumberLong(4)}</code>
 */
class MongoDBApplier(uri: String, binlogDb: String, binlogCollName: String, enumToString: Boolean) {
  private val client     = new MongoClient(new MongoClientURI(uri))
  private val binlogColl = client.getDB(binlogDb).getCollection(binlogCollName)

  //--------------------------------------------------------------------------

  /** @return Option(filename, position) */
  def binlogGetPosition: Option[(String, Long)] = {
    Option(binlogColl.findOne).map { obj =>
      (obj.get("filename").asInstanceOf[String], obj.get("position").asInstanceOf[Long])
    }
  }

  def binlogRotate(filename: String, position: Long) {
    val fp = new BasicDBObject("filename", filename).append("position", position)
    binlogColl.update(new BasicDBObject(), fp, true, false, WriteConcern.ACKNOWLEDGED)
  }

  def binlogNextPosition(position: Long) {
    val p    = new BasicDBObject("position", position)
    val setP = new BasicDBObject("$set", p)
    binlogColl.update(new BasicDBObject(), setP, false, false, WriteConcern.ACKNOWLEDGED)
  }

  //--------------------------------------------------------------------------

  def insert(nextPosition: Long, dbName: String, collName: String, cols: Seq[ColInfo], data: WriteRowsEventData) {
    val db   = client.getDB(dbName)
    val coll = db.getCollection(collName)
    for (mySQLValues <- data.getRows.asScala) {
      val obj = mySQLRowToMongoDBObject(cols, data.getIncludedColumns, mySQLValues)
      try {
        coll.insert(obj, WriteConcern.ACKNOWLEDGED)
      } catch {
        case dup: DuplicateKeyException =>
          // Stop replication only when the doc to be inserted is different from
          // the doc in DB
          // https://github.com/ngocdaothanh/mydit/issues/2
          val fields = new BasicDBObject("_id", 1)
          if (coll.findOne(obj, fields, ReadPreference.primary) == null) throw dup

        case NonFatal(e) =>
          throw e
      }
    }
    binlogNextPosition(nextPosition)
  }

  def update(nextPosition: Long, dbName: String, collName: String, cols: Seq[ColInfo], data: UpdateRowsEventData) {
    val db   = client.getDB(dbName)
    val coll = db.getCollection(collName)
    for (entry <- data.getRows.asScala) {
      val before = mySQLRowToMongoDBObject(cols, data.getIncludedColumnsBeforeUpdate, entry.getKey)
      val after  = mySQLRowToMongoDBObject(cols, data.getIncludedColumns,             entry.getValue)
      coll.update(before, new BasicDBObject("$set", after), true, false, WriteConcern.ACKNOWLEDGED)  // Upsert
    }
    binlogNextPosition(nextPosition)
  }

  def remove(nextPosition: Long, dbName: String, collName: String, cols: Seq[ColInfo], data: DeleteRowsEventData) {
    val db   = client.getDB(dbName)
    val coll = db.getCollection(collName)
    for (mySQLValues <- data.getRows.asScala) {
      val obj = mySQLRowToMongoDBObject(cols, data.getIncludedColumns, mySQLValues)
      coll.remove(obj, WriteConcern.ACKNOWLEDGED)
    }
    binlogNextPosition(nextPosition)
  }

  //--------------------------------------------------------------------------

  private def mySQLRowToMongoDBObject(cols: Seq[ColInfo], includedColumns: BitSet, mySQLValues: Array[Serializable]): DBObject = {
    val ret = new BasicDBObject
    for (i <- 0 until cols.size) {
      if (includedColumns.get(i)) {
        val ci = cols(i)
        val mongoDBValue = mySQLValueToMongoDBValue(ci, mySQLValues(i))
        ret.put(ci.name, mongoDBValue)
      }
    }
    ret
  }

  private def mySQLValueToMongoDBValue(ci: ColInfo, value: Serializable): Serializable = {
    // Convert MySQL enum to MongoDB String
    if (enumToString && ci.typeLowerCase.equals("enum")) {
      val id = value.asInstanceOf[Int]  // Starts from 1
      return ci.enumValues(id - 1)
    }

    // MongoDB doesn't support BigDecimal
    if (value.isInstanceOf[BigDecimal]) {
      val b = value.asInstanceOf[BigDecimal]
      return b.doubleValue
    }

    return value
  }
}
