for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
  set JAVA_VERSION=%%g
)

set JAVA_VERSION=%JAVA_VERSION:"=%

for /f "delims=. tokens=1-3" %%v in ("%JAVA_VERSION%") do (
  set MAJOR=%%v
  set MINOR=%%w
  set BUILD=%%x

  set META_SIZE=-XX:MaxMetaspaceSize=128m
  if "!MINOR!" LSS "8" (
    set META_SIZE=-XX:MaxPermSize=128m
  )

  set MEM_OPTS=!META_SIZE!
)

# You may need to customize memory config below to optimize for your environment.
# To display time when the application is stopped for GC:
# -XX:+PrintGCTimeStamps -XX:+PrintGCApplicationStoppedTime
set JAVA_OPTS=-Xmx256m -Xms128m %MEM_OPTS% -XX:+HeapDumpOnOutOfMemoryError -XX:+AggressiveOpts -XX:+OptimizeStringConcat -XX:+UseFastAccessorMethods -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+CMSClassUnloadingEnabled -XX:SurvivorRatio=8 -XX:MaxTenuringThreshold=1 -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly -Djava.awt.headless=true -server -Dmydit.mode=production

set ROOT_DIR=%~dp0..
cd "%$ROOT_DIR%"

# Include ROOT_DIR to find this pid easier later, when
# starting multiple processes from different directories
set CLASS_PATH="%ROOT_DIR%\lib\*;config"

java %JAVA_OPTS% -cp %CLASS_PATH% %* mydit.Main
