@echo off
setlocal
echo == Building middleware JAR ==
pushd "%~dp0..\PD-Middleware-U2"
call mvn -q -DskipTests clean install
if errorlevel 1 ( popd & exit /b 1 )
popd

echo == Building KV Store app ==
pushd "%~dp0"
call mvn -q -DskipTests clean package
popd
endlocal
