You need [SBT](http://www.scala-sbt.org/) to build.

## Run in development mode

```
sbt run
```

To create Eclipse project:

```
sbt eclipse
```

For IntelliJ, simply install its Scala plugin and open the project.

## Run in production mode

```
sbt xitrum-package
```

Directory `target/xitrum` will be created:

```
  target/xitrum/
    config/
      application.conf
      logback.xml
    lib/
      <.jar files>
    script/
      start
      start.sh
```

Just copy it to the server where you want to run (Java 6+ is required).
