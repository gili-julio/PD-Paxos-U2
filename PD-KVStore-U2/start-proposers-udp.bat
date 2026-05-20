@echo off
setlocal
set JAR=%~dp0target\pd-kvstore-1.0-SNAPSHOT.jar
set GW=udp://127.0.0.1:8088
start "Proposer P1 UDP 9001" java -jar "%JAR%" --role=proposer --protocol=udp --port=9001 --host=127.0.0.1 --gateway=%GW% --id=P1
start "Proposer P2 UDP 9002" java -jar "%JAR%" --role=proposer --protocol=udp --port=9002 --host=127.0.0.1 --gateway=%GW% --id=P2
endlocal
