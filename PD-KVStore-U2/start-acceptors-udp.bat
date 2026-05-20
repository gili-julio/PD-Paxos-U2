@echo off
setlocal
set JAR=%~dp0target\pd-kvstore-1.0-SNAPSHOT.jar
set GW=udp://127.0.0.1:8088
start "Acceptor A1 UDP 10001" java -jar "%JAR%" --role=acceptor --protocol=udp --port=10001 --host=127.0.0.1 --gateway=%GW% --id=A1
start "Acceptor A2 UDP 10002" java -jar "%JAR%" --role=acceptor --protocol=udp --port=10002 --host=127.0.0.1 --gateway=%GW% --id=A2
start "Acceptor A3 UDP 10003" java -jar "%JAR%" --role=acceptor --protocol=udp --port=10003 --host=127.0.0.1 --gateway=%GW% --id=A3
endlocal
