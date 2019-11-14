@ECHO OFF
setlocal enabledelayedexpansion

set branch=%1
set build=%2
set cpu=%3

REM Check if parameters are set
if %branch%.==. GOTO args_error
if %build%.==. GOTO args_error
if %cpu%.==. (
	GOTO args_error
) else (
	GOTO set_env
)

:args_error
echo User error: must specify branch, build type, CPU arch and revision code
echo get_qa "<branch> <build> <cpu>"
echo Branch can be any of: 7.2.XRCX, 7.3.XRCX, 7.4.XRCX
echo Build can be any of: NTS, TS
echo CPU can be any of: X64, X86
exit /b

:set_env
REM set important env vars
IF DEFINED PFTT_SHELL GOTO :skip_set_env
CALL %~dp0set_env.cmd
:skip_set_env

SET PHP_BUILDS=%~d0\PHPBuilds

REM Create cache folder if it does not exist
if not exist %PHP_BUILDS% (
	md %~d0\PHPBuilds
)

if not exist %PFTT_CACHE% (
	md %PFTT_HOME%\cache
)

set file_name=php-%branch%
set test_pack=php-test-pack-%branch%

REM Add nts to file_name if needed
if /I %build%==nts (
	set file_name=%file_name%-nts-win32-vc15-%cpu%
) else (
	set file_name=%file_name%-win32-vc15-%cpu%
)

REM Download the build if it is not available
if not exist %PHP_BUILDS%\%file_name% (
	set build_link=https://windows.php.net/downloads/qa/%file_name%.zip

	powershell download_files.ps1 !build_link! %PFTT_CACHE% %file_name%.zip
	7za.exe x %PFTT_CACHE%\%file_name%.zip -o%PHP_BUILDS%\*
	del %PFTT_CACHE%\%file_name%.zip
) else (
	echo QA build already exists. Remove or move file in %PHP_BUILDS% if you want new copy.
)

REM Also download test-pack if it is not available
if not exist %PHP_BUILDS%\%test_pack% (
	set test_pack_link=https://windows.php.net/downloads/qa/%test_pack%.zip

	powershell download_files.ps1 !test_pack_link! %PFTT_CACHE% %test_pack%.zip
	7za.exe x %PFTT_CACHE%\%test_pack%.zip -o%PHP_BUILDS%\*
	del %PFTT_CACHE%\%test_pack%.zip
) else (
	echo Test pack already exists. Remove or move file in %PHP_BUILDS% if you want new copy.
)