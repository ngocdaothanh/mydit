Mydit: MySQL to MongoDB data replicator

You can use it to implement transaction feature for programs using MongoDB:
Write transaction data to MySQL, then read from MongoDB.

[The download](https://github.com/ngocdaothanh/mydit/releases)
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

**MySQL ---> Mydit ---> MongoDB**

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

MySQL's `binlog_format` must be set to `ROW`. Note that RDS MySQL's `binlog_format`
is set to `MIXED` by default. If you use RDS you need to create a parameter group
and set it to the MySQL instance. You may need to restart the instance for the
setting in the parameter group to take effect.

The user to connect to the MySQL server must have privileges `REPLICATION SLAVE`
and `REPLICATION CLIENT`.

Don't forget to set `server-id`, otherwise slaves cannot connect.

Typically, my.cnf should look like this:

```
[mysqld]
server-id        = 1
binlog_format    = row
log_bin          = mysql-bin.log
expire_logs_days = 10
max_binlog_size  = 100M
```

## Things Mydit doesn't do

* If there are indexes in your MySQL databases, you have to manually create
  MongoDB indexes yourself.
* When you change MySQL database schema, you need to do apply the change at
  MongoDB side yourself.

## Config

See `config/application.conf`.

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

## Start with Supervisor

[Supervisor](http://supervisord.org/) is a very easy to use tool to start
program at OS boot and restart program automatically when it stops abnormally.

See [Ubuntu tutorial](https://serversforhackers.com/monitoring-processes-with-supervisord).

`/etc/supervisor/conf.d/mydit.conf` example:

```
[program:mydit]
command=/home/ubuntu/opt/mydit-1.0/script/start
directory=/home/ubuntu/opt/mydit-1.0
autostart=true
autorestart=true
startretries=3
stderr_logfile=/home/ubuntu/opt/mydit-1.1/supervisord.err.log
stdout_logfile=/home/ubuntu/opt/mydit-1.1/supervisord.out.log
user=ubuntu
```

Command examples:

```
sudo supervisorctl reread
sudo supervisorctl update

sudo supervisorctl status mydit
sudo supervisorctl stop mydit
sudo supervisorctl start mydit
```

## Bootstrap or rebootstrap MongoDB from existing MySQL data

There are two scenarios:

* MongoDB is empty and you want to populate it with existing MySQL data.
* Replication binlog position is too old and MySQL has deleted the old binlog file.

To make MongoDB data to be updated to the latest MySQL data, you can do like this:

1. Alter MySQL tables to include a dummy additional field.
2. Update MySQL tables to set value to that field. This will force all rows in
   the tables to be replicated to MongoDB.
3. Delete the field in both MySQL and MongoDB.

## Similar tools

* [Tungsten Replicator](https://code.google.com/p/tungsten-replicator/)
* [Bottled Water for PostgreSQL](https://github.com/confluentinc/bottledwater-pg)
