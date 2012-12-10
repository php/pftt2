@ECHO off
REM script for running PFTT on Windows

REM set important env vars
CALL set_env
SET PFTT_LIB=%PFTT_HOME%\lib

SET CLASSPATH=%PFTT_HOME%\build;%PFTT_LIB%\winp-1.14.jar;%PFTT_LIB%\htmlcleaner-2.2.jar;%PFTT_LIB%\groovy-1.8.6.jar;%PFTT_LIB%\icu4j-49_1.jar;%PFTT_LIB%\icudata.jar;%PFTT_LIB%\icutzdata.jar;%PFTT_LIB%\j2ssh-common-0.2.9.jar;%PFTT_LIB%\j2ssh-core-0.2.9.jar;%PFTT_LIB%\jansi-1.7.jar;%PFTT_LIB%\jline-0.9.94.jar;%PFTT_LIB%\jzlib-1.0.7.jar;%PFTT_LIB%\selenium-server-standalone-2.19.0.jar;%PFTT_LIB%\xercesImpl.jar;%PFTT_LIB%\xmlpull-1.1.3.1.jar;%PFTT_LIB%\commons-net-3.1.jar;%PFTT_LIB%\commons-cli-1.2.jar;%PFTT_LIB%\antlr-2.7.7.jar;%PFTT_LIB%\asm-3.2.jar;%PFTT_LIB%\asm-analysis-3.2.jar;%PFTT_LIB%\asm-commons-3.2.jar;%PFTT_LIB%\asm-tree-3.2.jar;%PFTT_LIB%\asm-util-3.2.jar


REM if user added -uac or -auto or -windebug console options, run elevated in UAC
REM user will get at most 1 UAC popup dialog
REM UAC popups break automation because there is no way to automate clicking on them
REM having 1 UAC popup at start when -auto is used will hopefully get the user to realize
REM pftt must be run automatically with elevated privileges or automated testing will fail
REM
REM unfortunately, elevation will open a 2nd command processor window for the PFTT console
REM
REM search console options for -uac or -auto or -windebug

SET pftt_args="str %*"
SET pftt_temp=%pftt_args:uac=%
IF NOT %pftt_args% EQU %pftt_temp% ( GOTO set_elevator )
SET pftt_args="str %*"
SET pftt_temp=%pftt_args:windebug=%
IF NOT %pftt_args% EQU %pftt_temp% ( GOTO set_elevator )
SET pftt_args="str %*"
SET pftt_temp=%pftt_args:auto=%
IF NOT %pftt_args% EQU %pftt_temp% ( GOTO set_elevator )

SET ELEVATOR=
GOTO :run_it

:set_elevator
SET ELEVATOR=%PFTT_HOME%\bin\elevate.exe
ECHO see other new command prompt Window for PFTT output

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

REM finally execute PFTT
%ELEVATOR% %JAVA_EXE% -classpath %CLASSPATH% com.mostc.pftt.main.PfttMain %*
