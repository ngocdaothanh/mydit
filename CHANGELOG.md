1.6:

* [#13](https://github.com/ngocdaothanh/mydit/issues/13)
  MySQLExtractor: Reconnect after some time on connection failure

1.5:

* [#12](https://github.com/ngocdaothanh/mydit/issues/12)
  Exit if binlog filename/position is too old

1.4:

* [#9](https://github.com/ngocdaothanh/mydit/issues/9)
  Fix ArrayIndexOutOfBoundsException when enum value is null
* [#10](https://github.com/ngocdaothanh/mydit/issues/10)
  Update mysql-binlog-connector-java from 0.1.2 to 0.1.3

1.3:

* [#4](https://github.com/ngocdaothanh/mydit/issues/4)
  Optimize: Don't get column info again when not needed
* [#5](https://github.com/ngocdaothanh/mydit/issues/5)
  Convert MySQL text to MongoDB UTF-8 String
* [#8](https://github.com/ngocdaothanh/mydit/issues/8)
  Fix bug: Cannot exit when the number of errors exceeds the configured threshold
* [#6](https://github.com/ngocdaothanh/mydit/issues/6)
  Update Logback from 1.1.2 to 1.1.3
* [#7](https://github.com/ngocdaothanh/mydit/issues/7)
  Update mysql-connector-java from 5.1.34 to 5.1.35

1.2:

* [#2](https://github.com/ngocdaothanh/mydit/issues/2)
  Insert: Stop replication on DuplicateKeyException only when the doc to be
  inserted is different from the doc in DB
* [#3](https://github.com/ngocdaothanh/mydit/issues/3)
  Log INFO when failed replication event queue changes from nonempty to empty

1.1:

* [#1](https://github.com/ngocdaothanh/mydit/issues/1)
  Improve performance by running event processing in a separate thread to
  avoid blocking the MySQL binlog
  [event reader thread](https://github.com/shyiko/mysql-binlog-connector-java/issues/32)

1.0: First public release
