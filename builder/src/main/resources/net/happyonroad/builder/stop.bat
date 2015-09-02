

@ECHO OFF

SET APP_HOME=%~dp0..
SET APP_NAME=${app.name}

SET BOOTSTRAP_JAR=${app.boot}
SET APP_TARGET=${app.target}
SET OPTIONS=-Dlogback.configurationFile=config/logback.xml
if ""%1""==""""
  SET APP_INDEX=1
else
  SET APP_INDEX=%1

@ECHO ON
CD %APP_HOME%
java -Dapp.home=%APP_HOME% ^
     -Dapp.name=%APP_NAME% ^
     %OPTIONS%             ^
     -jar %BOOTSTRAP_JAR%  ^
     %APP_TARGET%          ^
     --stop                ^
     --index %APP_INDEX%