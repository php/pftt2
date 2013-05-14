@ECHO OFF
REM script for running PFTT on Windows

REM set important env vars
CALL set_env
SET PFTT_LIB=%PFTT_HOME%\lib

SET CLASSPATH=%PFTT_HOME%\build;%PFTT_LIB%\apache-mime4j-0.6.jar;%PFTT_LIB%\commons-exec-1.1.jar;%PFTT_LIB%\cssparser-0.9.8.jar;%PFTT_LIB%\guava-14.0.jar;%PFTT_LIB%\hamcrest-core-1.3.jar;%PFTT_LIB%\httpclient-4.2.1.jar;%PFTT_LIB%\httpcore-4.2.1.jar;%PFTT_LIB%\httpmime-4.2.1.jar;%PFTT_LIB%\jna-3.4.0.jar;%PFTT_LIB%\jna-platform-3.4.0.jar;%PFTT_LIB%\json-20080701.jar;%PFTT_LIB%\jzlib-1.1.1.jar;%PFTT_LIB%\nekohtml-1.9.17.jar;%PFTT_LIB%\phantomjsdriver-1.0.1.jar;%PFTT_LIB%\selenium-java-2.31.0.jar;%PFTT_LIB%\htmlcleaner-2.2.jar;%PFTT_LIB%\groovy-1.8.6.jar;%PFTT_LIB%\icu4j-49_1.jar;%PFTT_LIB%\icudata.jar;%PFTT_LIB%\icutzdata.jar;%PFTT_LIB%\jansi-1.7.jar;%PFTT_LIB%\jline-0.9.94.jar;%PFTT_LIB%\xercesImpl.jar;%PFTT_LIB%\xmlpull-1.1.3.1.jar;%PFTT_LIB%\commons-cli-1.2.jar;%PFTT_LIB%\antlr-2.7.7.jar;%PFTT_LIB%\asm-3.2.jar;%PFTT_LIB%\asm-analysis-3.2.jar;%PFTT_LIB%\asm-commons-3.2.jar;%PFTT_LIB%\asm-tree-3.2.jar;%PFTT_LIB%\asm-util-3.2.jar;%PFTT_LIB%\winp-1.14.jar;%PFTT_LIB%\commons-net-3.1.jar;%PFTT_LIB%\commons-codec-1.6.jar;%PFTT_LIB%\commons-lang-2.6.jar;%PFTT_LIB%\commons-logging-1.1.1.jar;%PFTT_LIB%\jzlib-1.1.1.jar;%PFTT_LIB%\mina-core-2.0.7.jar;%PFTT_LIB%\mina-statemachine-2.0.7.jar;%PFTT_LIB%\slf4j-api-1.7.2.jar;%PFTT_LIB%\slf4j-log4j12-1.7.2.jar;%PFTT_LIB%\php_parser.jar;%PFTT_LIB%\log4j-1.2.17.jar


REM if user added -uac -auto -debug* console options or setup or stop commands, run elevated in UAC
REM user will get at most 1 UAC popup dialog
REM UAC popups break unattended automation because there is no way to automate clicking on them
REM having 1 UAC popup at start when -auto is used will hopefully get the user to realize
REM pftt must be run automatically with elevated privileges or automated testing will fail
REM
REM unfortunately, elevation will open a 2nd command processor window for the PFTT console
REM
REM search console options for -uac or -auto or -debug_all or -debug_list or stop or setup

REM if running in PFTT shell, can assume already running under UAC, so don't run with elevate (stay in pftt shell)
IF DEFINED PFTT_SHELL GOTO :run_it

SET pftt_args="str %*"
SET pftt_temp=%pftt_args:uac=%
IF NOT %pftt_args% EQU %pftt_temp% ( GOTO set_elevator )
SET pftt_args="str %*"
SET pftt_temp=%pftt_args:debug=%
IF NOT %pftt_args% EQU %pftt_temp% ( GOTO set_elevator )
SET pftt_args="str %*"
SET pftt_temp=%pftt_args:auto=%
IF NOT %pftt_args% EQU %pftt_temp% ( GOTO set_elevator )
SET pftt_args="str %*"
SET pftt_temp=%pftt_args:setup=%
IF NOT %pftt_args% EQU %pftt_temp% ( GOTO set_elevator )
SET pftt_args="str %*"
SET pftt_temp=%pftt_args:stop=%
IF NOT %pftt_args% EQU %pftt_temp% ( GOTO set_elevator )


REM not using elevate, clear these vars!
SET ELEVATOR=
SET ELEVATOR_OPTS=
GOTO :run_it

:set_elevator
SET ELEVATOR=%PFTT_HOME%\bin\elevate.exe
REM pass -pause to pftt (IMPORTANT: trailing space must be present or this will break everything)
SET ELEVATOR_OPTS="-pause " 
ECHO see other new command prompt Window for PFTT output

:run_it

REM find java.exe
IF EXIST %JAVA_HOME%\lib\tools.jar (
	SET JAVA_EXE=%JAVA_HOME%\bin\java.exe
) ELSE (
	REM prefer the JRE bundled with PFTT
	IF EXIST "%PFTT_HOME%\jre\bin\java.exe" (
		SET JAVA_EXE="%PFTT_HOME%\jre\bin\java.exe"
		SET JAVA_HOME="%PFTT_HOME%\jre"
	) ELSE ( 
		IF EXIST "%ProgramFiles%\java\jre6\bin\java.exe" (
			SET JAVA_EXE="%ProgramFiles%\java\jre6\bin\java.exe"
			SET JAVA_HOME="%ProgramFiles%\java\jre6"
		) ELSE (
			IF EXIST "%ProgramW6432%\java\jre6\bin\java.exe" (
				SET JAVA_EXE="%ProgramW6432%\java\jre6\bin\java.exe"
				SET JAVA_HOME="%ProgramW6432%\java\jre6"
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
	WHERE java > pftt_cmd.tmp 2> NUL
	
	IF %ERRORLEVEL% EQU 0 (
		REM found java.exe in PATH
		SET /p JAVA_EXE= < pftt_cmd.tmp
	) ELSE (
		REM can't find java jre
		ECHO java may not be installed. Must Install Sun Java JRE 6 or 7.
		ECHO user error set JAVA_HOME or add java to PATH and try again
		ECHO searched "%ProgramFiles%" "%Programfiles(x86)%" "%ProgramW6432%"
		DEL /Q pftt_cmd.tmp
		EXIT /B 200
	) 
	DEL /Q pftt_cmd.tmp
)
REM finally execute PFTT
SET pftt_args="str %*"
SET pftt_temp=%pftt_args:pftt-profile=%
IF NOT %pftt_args% EQU %pftt_temp% ( 
	%ELEVATOR% %JAVA_EXE% -agentpath:"C:\Program Files (x86)\YourKit Java Profiler 12.0.5\bin\win64\yjpagent.dll" -classpath %CLASSPATH% com.mostc.pftt.main.PfttMain %ELEVATOR_OPTS%%*
) ELSE (
	%ELEVATOR% %JAVA_EXE% -classpath %CLASSPATH% com.mostc.pftt.main.PfttMain %ELEVATOR_OPTS%%*
)
