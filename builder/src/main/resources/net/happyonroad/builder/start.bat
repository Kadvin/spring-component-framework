@ECHO OFF

SET APP_HOME=%~dp0..
SET APP_NAME=${app.name}

SET BOOTSTRAP_JAR=${app.boot}
SET APP_TARGET=${app.target}
SET OPTIONS=-Dlogback.configurationFile=config/logback.xml

@ECHO ON
cd %APP_HOME%
java -Dapp.home=%APP_HOME% ^
     -Dapp.name=%APP_NAME% ^
     %OPTIONS%             ^
     ${jvm.options}        ^
     -jar %BOOTSTRAP_JAR%  ^
     %APP_TARGET%
