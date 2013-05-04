@ECHO OFF

REM system drive might not be C:, in such situations,
REM php-sdk might be on C: or it could be both
REM assume the one where PFTT is installed is the one to use (in such cases)
REM
IF EXIST %SYSTEMDRIVE%\php-sdk\PFTT\current (
	SET PHP_DRIVE=%SYSTEMDRIVE%
) ELSE (
	SET PHP_DRIVE=C:
)

SET PHP_SDK=%PHP_DRIVE%\php-sdk

SET PFTT_ROOT_DIR=%PHP_SDK%\PFTT
SET PFTT_HOME=%PHP_SDK%\PFTT\current
SET PFTT_BIN=%PFTT_HOME%\bin
SET PFTT_BIN_INTERNAL=%PFTT_BIN%\internal
SET PFTT_CONF=%PFTT_HOME%\conf
SET PFTT_CONF_INTERNAL=%PFTT_CONF%\internal
SET PFTT_CONF_APP=%PFTT_CONF%\app
SET PFTT_DEV_CONF=%PFTT_CONF%\dev
SET PFTT_DEV_CONF_APP=%PFTT_DEV_CONF%\app
SET PFTT_CACHE=%PFTT_HOME%\CACHE
SET PFTT_CACHE_WORKING=%PFTT_CACHE%\working
SET PFTT_CACHE_DEP=%PFTT_CACHE%\working
SET PFTT_DEP=%PFTT_CACHE_DEP%
SET PFTT_WORKING=%PFTT_CACHE_WORKING%

REM add PFTT to path. and GIT and Notepad++ if present
SET PATH=%PFTT_BIN%;%PFTT_BIN_INTERNAL%;%PATH%;"%ProgramFiles(x86)%\Git\Bin";"%ProgramFiles(x86)%\Git\Notepad++";"%ProgramFiles%\Git\Bin";"%ProgramFiles%\Git\Notepad++"

