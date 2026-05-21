@echo off
setlocal
set JAR=%~dp0target\pd-kvstore-1.0-SNAPSHOT.jar
start "Gateway UDP 8088" java -jar "%JAR%" --role=gateway --protocol=udp --port=8088 --host=localhost
endlocal
