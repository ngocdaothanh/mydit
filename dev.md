You need [SBT](http://www.scala-sbt.org/) to build.

## Run in development mode

```
sbt run
```

To create Eclipse or IntelliJ project:

```
sbt eclipse
sbt gen-idea
```

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
