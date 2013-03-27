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

SET PFTT_HOME=%PHP_SDK%\PFTT\current

SET PATH=%PFTT_HOME%\BIN;%PATH%

