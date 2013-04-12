@ECHO OFF
REM sleep <seconds>
ping 192.168.1.1 -n %1 -w 1000 > nul
