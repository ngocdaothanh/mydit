package mydit

import scala.collection.JavaConverters._
import com.typesafe.config.ConfigFactory

case class Config(
  myHost: String, myPort: Int, myUsername: String, myPassword: String, myOnly: Seq[String],
  mongoUri: String, mongoBinlogDb: String, mongoBinlogColl: String,
  enumToString: Boolean, maxFailedEventQueueSize: Int
)

object Config {
  def load(): Config = {
    val conf  = ConfigFactory.load()
    val mydit = conf.getConfig("mydit")

    val myHost     = mydit.getString("mysql.host")
    val myPort     = mydit.getInt   ("mysql.port")
    val myUsername = mydit.getString("mysql.username")
    val myPassword = mydit.getString("mysql.password")
    val myOnly     = mydit.getStringList("mysql.only").asScala

    val mongoUri        = mydit.getString("mongodb.uri")
    val mongoBinlogDb   = mydit.getString("mongodb.binlog.db")
    val mongoBinlogColl = mydit.getString("mongodb.binlog.collection")

    val enumToString            = mydit.getBoolean("enumToString")
    val maxFailedEventQueueSize = mydit.getInt("maxFailedEventQueueSize")

    Config(
      myHost, myPort, myUsername, myPassword, myOnly,
      mongoUri, mongoBinlogDb, mongoBinlogColl,
      enumToString, maxFailedEventQueueSize
    )
  }
}
