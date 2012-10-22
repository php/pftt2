@echo off
REM script for running PFTT on Windows

set PFTT_HOME=C:\php-sdk\PFTT\current
set PFTT_LIB=%PFTT_HOME%\lib

set CLASSPATH=%PFTT_HOME%\build;%PFTT_LIB%\htmlcleaner-2.2.jar;%PFTT_LIB%\groovy-1.8.6.jar;%PFTT_LIB%\icu4j-49_1.jar;%PFTT_LIB%\icudata.jar;%PFTT_LIB%\icutzdata.jar;%PFTT_LIB%\j2ssh-common-0.2.9.jar;%PFTT_LIB%\j2ssh-core-0.2.9.jar;%PFTT_LIB%\jansi-1.7.jar;%PFTT_LIB%\jline-0.9.94.jar;%PFTT_LIB%\jzlib-1.0.7.jar;%PFTT_LIB%\selenium-server-standalone-2.19.0.jar;%PFTT_LIB%\xercesImpl.jar;%PFTT_LIB%\xmlpull-1.1.3.1.jar;%PFTT_LIB%\commons-net-3.1.jar;%PFTT_LIB%\commons-cli-1.2.jar;%PFTT_LIB%\antlr-2.7.7.jar;%PFTT_LIB%\asm-3.2.jar;%PFTT_LIB%\asm-analysis-3.2.jar;%PFTT_LIB%\asm-commons-3.2.jar;%PFTT_LIB%\asm-tree-3.2.jar;%PFTT_LIB%\asm-util-3.2.jar

REM find java and execute PfttMain
REM search %PATH% for java
WHERE java > NUL
if %ERRORLEVEL% EQU 0 (
	java -classpath %CLASSPATH% com.mostc.pftt.main.PfttMain %*
) ELSE (
	REM check for JAVA_HOME
	IF EXIST %JAVA_HOME%\lib\tools.jar (
		%JAVA_HOME%\bin\java -classpath %CLASSPATH% com.mostc.pftt.main.PfttMain %*
	) ELSE (
		ECHO user error set JAVA_HOME or add java to PATH and try again
	)
)
