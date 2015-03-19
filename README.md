Mydit: MySQL to MongoDB Replicator

You can use it to implement transaction feature for programs using MongoDB:
Write transaction data to MySQL, then read from MongoDB.

[Packaged directory](https://github.com/ngocdaothanh/mydit/releases)
looks like this:

```
config/
  application.conf
  logback.xml
lib/
  <.jar files>
script/
  start
  start.sh
```

Java 6+ is required. Run:

```
script/start
```

## How Mydit works

MySQL ---> Mydit ---> MongoDB

[mysql-binlog-connector-java](https://github.com/shyiko/mysql-binlog-connector-java)
is used to get a stream of row events from MySQL.

The events are then replicated to MongoDB, using
[WriteConcern](http://docs.mongodb.org/manual/core/write-concern/)
[ACKNOWLEDGED](http://api.mongodb.org/java/current/com/mongodb/WriteConcern.html).

Info about MySQL's binlog file name and replication position is saved in MongoDB.
When the replicator is restarted, it knows where to continue the replication.

Error handling:

* MySQL: When there's problem connecting to MySQL, the replicator will try to reconnect.
* MongoDB: When there's problem replicating to MongoDB, the replicator will queue the
  data and try to replicate it later. If the queue size exceeds the configured
  threshold, the replicator process will exit.

## MySQL prerequisites

See [How to Set Up Replication](http://dev.mysql.com/doc/refman/5.6/en/replication-howto.html).

MySQL's binlog_format must be set to ROW. Note that RDS MySQL's binlog_format
is set to MIXED by default. If you use RDS you need to create a parameter group
and set it to the MySQL instance.

The user to connect to the MySQL server must have privileges `REPLICATION SLAVE`
and `REPLICATION CLIENT`.

Don't forget to set `server-id`, otherwise slaves cannot connect.

## Things Mydit doesn't do

* If there are indexes in your MySQL databases, you have to manually create
  MongoDB indexes yourself.
* When you change MySQL database schema, you need to do apply the change at
  MongoDB side yourself.

## Config

To config MySQL and MongoDB, see `config/application.conf`.

If you want to set, for example, MySQL password from system environment variable,
modify `config/application.conf` like this:

```
...
password = "${MYSQL_PASSWORD}"
...
```

To config JVM memory, see `script/start` and `script/start.bat`.

## Log

When the program runs, log will be output to `log/mydit.log`. The log is
rotated daily by default. See `config/logback.xml`.
