#! /bin/sh

APP_HOME=$(cd `dirname $0`; cd ..; pwd)
APP_NAME=${app.name}

BOOTSTRAP_JAR=${app.boot}
APP_TARGET=${app.target}
OPTIONS=-Dlogback.configurationFile=config/logback.xml

cd $APP_HOME
java -Dapp.home=$APP_HOME \
     -Dapp.name=$APP_NAME \
     $OPTIONS             \
     ${jvm.options}       \
     -jar $BOOTSTRAP_JAR  \
     $APP_TARGET
