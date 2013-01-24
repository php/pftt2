@ECHO off
REM script for running SSH Server on Windows

CALL set_env
SET PFTT_LIB=%PFTT_HOME%\lib

SET CLASSPATH=%PFTT_HOME%\build;%PFTT_LIB%\commons-cli-1.2.jar;%PFTT_LIB%\winp-1.14.jar;%PFTT_LIB%\commons-net-3.1.jar;%PFTT_LIB%\commons-codec-1.6.jar;%PFTT_LIB%\commons-lang-2.6.jar;%PFTT_LIB%\commons-logging-1.1.1.jar;%PFTT_LIB%\jzlib-1.1.1.jar;%PFTT_LIB%\mina-core-2.0.7.jar;%PFTT_LIB%\mina-statemachine-2.0.7.jar;%PFTT_LIB%\slf4j-api-1.7.2.jar;%PFTT_LIB%\slf4j-log4j12-1.7.2.jar


:run_it


REM find java.exe
IF [%JAVA_EXE%] == [] (
	IF EXIST "%JAVA_HOME%\lib\tools.jar" (
		SET JAVA_EXE="%JAVA_HOME%\bin\java.exe"
	) ELSE (
		IF [%JAVA_EXE%] == [] ( 
			IF EXIST "%ProgramFiles%\java\jre6\bin\java.exe" (
				SET JAVA_EXE="%ProgramFiles%\java\jre6\bin\java.exe"
				SET JAVA_HOME="%ProgramFiles%\java\jre6"
			) ELSE (
				IF EXIST "%ProgramFiles(x86)%\java\jre6\bin\java.exe" (
					SET JAVA_EXE="%ProgramFiles(x86)%\java\jre6\bin\java.exe"
					SET JAVA_HOME="%ProgramFiles(x86)%\java\jre6"
				) ELSE (
					IF EXIST "%ProgramFiles%\java\jre7\bin\java.exe" (
						SET JAVA_EXE="%ProgramFiles%\java\jre7\bin\java.exe"
						SET JAVA_HOME="%ProgramFiles%\java\jre7"
					) ELSE (
						IF EXIST "%ProgramFiles(x86)%\java\jre7\bin\java.exe" (
							SET JAVA_EXE="%ProgramFiles(x86)%\java\jre7\bin\java.exe"
							SET JAVA_HOME="%ProgramFiles(x86)%\java\jre7"
						)
					)
				)
			)
		)
	)
)
	
IF [%JAVA_EXE%] == [] (
	REM check PATH last. it might find java.exe in \Windows\System32\java
	REM which elevate.exe can't find/execute for some weird reason
	WHERE java > pftt_cmd.tmp
	
	IF %ERRORLEVEL% EQU 0 (
		REM found java.exe in PATH
		SET /p JAVA_EXE= < pftt_cmd.tmp
	) ELSE (
		REM can't find java jre
		ECHO java may not be installed. Must Install Sun Java JRE 6 or 7.
		ECHO user error set JAVA_HOME or add java to PATH and try again
		DEL /Q pftt_cmd.tmp
		EXIT /B 200
	) 
	
	DEL /Q pftt_cmd.tmp
)

REM finally execute
%ELEVATOR% %JAVA_EXE% -classpath %CLASSPATH% com.mostc.pftt.main.SSHServer %*
