@ECHO off
REM script for running PFTT on Windows

SET PFTT_HOME=C:\php-sdk\PFTT\current
SET PFTT_LIB=%PFTT_HOME%\lib

SET CLASSPATH=%PFTT_HOME%\build;%PFTT_LIB%\winp-1.14.jar;%PFTT_LIB%\htmlcleaner-2.2.jar;%PFTT_LIB%\groovy-1.8.6.jar;%PFTT_LIB%\icu4j-49_1.jar;%PFTT_LIB%\icudata.jar;%PFTT_LIB%\icutzdata.jar;%PFTT_LIB%\j2ssh-common-0.2.9.jar;%PFTT_LIB%\j2ssh-core-0.2.9.jar;%PFTT_LIB%\jansi-1.7.jar;%PFTT_LIB%\jline-0.9.94.jar;%PFTT_LIB%\jzlib-1.0.7.jar;%PFTT_LIB%\selenium-server-standalone-2.19.0.jar;%PFTT_LIB%\xercesImpl.jar;%PFTT_LIB%\xmlpull-1.1.3.1.jar;%PFTT_LIB%\commons-net-3.1.jar;%PFTT_LIB%\commons-cli-1.2.jar;%PFTT_LIB%\antlr-2.7.7.jar;%PFTT_LIB%\asm-3.2.jar;%PFTT_LIB%\asm-analysis-3.2.jar;%PFTT_LIB%\asm-commons-3.2.jar;%PFTT_LIB%\asm-tree-3.2.jar;%PFTT_LIB%\asm-util-3.2.jar


REM if user added -uac or -auto console options, run elevated in UAC
REM user will get at most 1 UAC popup dialog
REM UAC popups break automation because there is no way to automate clicking on them
REM having 1 UAC popup at start when -auto is used will hopefully get the user to realize
REM pftt must be run automatically with elevated privileges or automated testing will fail
REM
REM unfortunately, elevation will open a 2nd command processor window for the PFTT console
REM
REM search console options for -uac or -auto 
SET "_pftt_args=%*"
SET "_pftt_args2=%*"
SET _pftt_temp=%_pftt_args:uac=%
SET _pftt_temp2=%_pftt_args2:auto=%
SET "ELEVATOR="
IF NOT "%_pftt_temp%" == "%_pftt_args%" (
	SET ELEVATOR=%PFTT_HOME%\bin\elevate.exe
) ELSE (
	IF NOT "%_pftt_temp2%" == "%_pftt_args2%" (
		SET ELEVATOR=%PFTT_HOME%\bin\elevate.exe
	) ELSE (
		SET "ELEVATOR="
	)
)
REM 

REM find java and execute PfttMain
REM search %PATH% for java
WHERE java > NUL
IF %ERRORLEVEL% EQU 0 (
	%ELEVATOR% "c:\program files\java\jdk1.6.0_35\bin\java.exe" -classpath %CLASSPATH% com.mostc.pftt.main.PfttMain %*
) ELSE (
	REM check for JAVA_HOME
	IF EXIST "%JAVA_HOME%\lib\tools.jar" (
		%ELEVATOR% %JAVA_HOME%\bin\java -classpath %CLASSPATH% com.mostc.pftt.main.PfttMain %*
	) ELSE ( 
		ECHO user error set JAVA_HOME or add java to PATH and try again
	)
)
