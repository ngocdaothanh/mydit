package mydit

import org.slf4j.bridge.SLF4JBridgeHandler

object Main {
  def main(args: Array[String]) {
    // mysql-binlog-connector-java logs to JUL
    // http://www.slf4j.org/legacy.html#jul-to-slf4j
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()

    val config = Config.load()
    new Rep(config)
  }
}
