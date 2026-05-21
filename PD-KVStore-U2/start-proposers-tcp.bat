@echo off
setlocal
set JAR=%~dp0target\pd-kvstore-1.0-SNAPSHOT.jar
set GW=tcp://localhost:8088
start "Proposer P1 TCP 9001" java -jar "%JAR%" --role=proposer --protocol=tcp --port=9001 --host=localhost --gateway=%GW% --id=P1
start "Proposer P2 TCP 9002" java -jar "%JAR%" --role=proposer --protocol=tcp --port=9002 --host=localhost --gateway=%GW% --id=P2
endlocal
