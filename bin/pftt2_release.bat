@ECHO OFF

REM set important env vars
IF DEFINED PFTT_SHELL GOTO :skip_set_env
CALL %~dp0set_env.cmd
:skip_set_env

SET PFTT_LIB=%PFTT_HOME%\lib
SET PFTT_BUILD=%PFTT_HOME%\build

REM Check if build directory exists
if exist %PFTT_BUILD% (
	cd ..
	REM Create pftt-release directory in main folder
	md pftt-release

	REM Copy contents of bin, conf and lib to respective folders
	cd pftt-release
	md bin
	md conf
	md lib

	xcopy /s /i "%PFTT_BIN%" "%PFTT_HOME%/pftt-release/bin"
	xcopy /s /i "%PFTT_CONF%" "%PFTT_HOME%/pftt-release/conf"
	xcopy /s /i "%PFTT_LIB%" "%PFTT_HOME%/pftt-release/lib"

	REM Create pftt2.jar in lib of the package
	cd %PFTT_HOME%/pftt-release/lib
	jar cf pftt2.jar -C %PFTT_BUILD% com %PFTT_BUILD%/org/columba %PFTT_BUILD%/org/kxml2 %PFTT_BUILD%/org/incava

	REM Create zip file of pftt-release folder
	%PFTT_BIN%/7za.exe a -tzip %PFTT_HOME%/pftt-release.zip %PFTT_HOME%/pftt-release

	REM Delete temp files/folders
	cd %PFTT_HOME%
	rd /s /q pftt-release

	cd %PFTT_BIN%
) else (
	ECHO Build folder does not exist
)