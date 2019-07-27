@ECHO OFF

REM set important env vars
IF DEFINED PFTT_SHELL GOTO :skip_set_env
CALL %~dp0set_env.cmd
:skip_set_env

SET PFTT_LIB=%PFTT_HOME%\lib
SET PFTT_BUILD=%PFTT_HOME%\build
SET PFTT_RELEASE=%PFTT_HOME%\pftt2

REM Check if build directory exists
if exist %PFTT_BUILD% (
	REM Create pftt_release directory in main folder
	md %PFTT_HOME%\pftt2

	REM Copy contents of bin, conf and lib to respective folders
	md %PFTT_RELEASE%\bin
	md %PFTT_RELEASE%\conf
	md %PFTT_RELEASE%\lib

	xcopy /s /i "%PFTT_BIN%" "%PFTT_RELEASE%\bin"
	xcopy /s /i "%PFTT_CONF%" "%PFTT_RELEASE%\conf"
	xcopy /s /i "%PFTT_LIB%" "%PFTT_RELEASE%\lib"

	REM Create pftt2.jar in lib of the package
	jar cf pftt2.jar -C %PFTT_BUILD% com %PFTT_BUILD%\org\columba %PFTT_BUILD%\org\kxml2 %PFTT_BUILD%\org\incava
	MOVE pftt2.jar %PFTT_RELEASE%\lib

	REM Create zip file of pftt_release folder
	%PFTT_BIN%\7za.exe a -tzip %PFTT_HOME%\pftt_release.zip %PFTT_HOME%\pftt2

	REM Delete temp files/folders
	rd /s /q %PFTT_HOME%\pftt2
) else (
	ECHO Build folder does not exist
)
