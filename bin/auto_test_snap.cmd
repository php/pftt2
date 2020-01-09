SET branches=7.2 7.3 7.4

for %%i in (%branches%) do (
	call %~dp0test_snap.cmd %%i
)