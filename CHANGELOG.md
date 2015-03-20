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
