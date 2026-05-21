@echo off
setlocal
set JAR=%~dp0target\pd-kvstore-1.0-SNAPSHOT.jar
start "Gateway TCP 8088" java -jar "%JAR%" --role=gateway --protocol=tcp --port=8088 --host=localhost
endlocal
