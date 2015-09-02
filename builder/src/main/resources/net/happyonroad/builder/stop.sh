#! /bin/sh

if [ $1 ]; then
  APP_INDEX=$1
else
  APP_INDEX=1
fi
APP_HOME=$(cd `dirname $0`; cd ..; pwd)
APP_NAME=${app.name}
APP_NAME=$APP_NAME_$APP_INDEX
BOOTSTRAP_JAR=${app.boot}
APP_TARGET=${app.target}
OPTIONS=-Dlogback.configurationFile=config/logback.xml

cd $APP_HOME
java -Dapp.home=$APP_HOME \
     -Dapp.name=$APP_NAME \
     $OPTIONS             \
     -jar $BOOTSTRAP_JAR  \
     $APP_TARGET          \
     --stop
     --index $APP_INDEX

process=`ps aux | grep $APP_NAME | grep -v grep | grep -v check.sh | awk '{print $2}'`
if [ "$process" == "" ]; then
    echo "$APP_NAME is stopped already"
else
  echo "Force to stop $APP_NAME by pid = $process"
  kill -9 $process
fi
