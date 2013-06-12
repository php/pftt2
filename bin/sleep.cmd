@ECHO OFF
REM sleep <seconds>

REM ping is the common solution
REM its not very accurate
REM 
REM can use PFTT(java) which is accurate, a memory hog AND takes a few seconds to startup
if %1 GEQ 1 (
	if %1 LEQ 7 (
		REM ping is accurate for small time intervals, but inaccurate for longer intervals
		ping 192.168.1.1 -n %1 -w 1000 > nul
	) ELSE (
		REM will take a few seconds before sleep starts so don't use `pftt sleep` for short durations
		pftt sleep %1
	)
)
