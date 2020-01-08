@ECHO OFF 
setlocal enabledelayedexpansion

SET branch[0]=7.2
SET branch[1]=7.3
SET branch[2]=7.4

for /L %%i in (0,1,2) do (
	SET branch=!branch[%%i]!
	call %~dp0test_snap.cmd !branch!
)