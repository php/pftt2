@ECHO OFF
setlocal enabledelayedexpansion

set branch=%1
set build=%2
set cpu=%3
set revision=%4

REM Check if parameters are set
if %branch%.==. GOTO args_error
if %build%.==. GOTO args_error
if %cpu%.==. GOTO args_error
if %revision%.==. (
	GOTO args_error
) else (
	GOTO set_env
)

:args_error
echo User error: must specify branch, build type, CPU arch and revision code
echo get_release "<branch> <build> <cpu> <revision code | latest>"
echo Branch can be any of: 7.1, 7.2, 7.3, 7.4
echo Build can be any of: NTS, TS
echo CPU can be any of: X64, X86
echo Revision code starts with r (i.e. rxxxxxx)
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

REM Set file_name and test_pack based on parameters
if %branch%==7.1 (
	set file_name=php-%branch%-%build%-windows-vc14-%cpu%
	set test_pack=php-test-pack-%branch%-%build%-windows-vc14-%cpu%
) else if %branch%==7.2 (
	set file_name=php-%branch%-%build%-windows-vc15-%cpu%
	set test_pack=php-test-pack-%branch%-%build%-windows-vc15-%cpu%
) else if %branch%==7.3 (
	set file_name=php-%branch%-%build%-windows-vc15-%cpu%
	set test_pack=php-test-pack-%branch%-%build%-windows-vc15-%cpu%
) else if %branch%==7.4 (
	set file_name=php-%branch%-%build%-windows-vs16-%cpu%
	set test_pack=php-test-pack-%branch%-%build%-windows-vs16-%cpu%
)

if /i %revision%==latest call get_latest_revision.cmd

set file_name=%file_name%-%revision%
set test_pack=%test_pack%-%revision%

REM Download the build if it is not available
if not exist %PHP_BUILDS%\%file_name% (
	set build_link=https://windows.php.net/downloads/snaps/php-%branch%/%revision%/%file_name%.zip

	bitsadmin /transfer DownloadingSnap-%branch%-%build%-%cpu%-Build /download /priority high !build_link! %PFTT_CACHE%\%file_name%.zip
	7za.exe x %PFTT_CACHE%\%file_name%.zip -o%PHP_BUILDS%\*
	del %PFTT_CACHE%\%file_name%.zip
) else (
	echo Snap build already exists. Remove or move file in %PHP_BUILDS% if you want new copy.
)

REM Also download test-pack if it is not available
if not exist %PHP_BUILDS%\%test_pack% (
	set test_pack_link=https://windows.php.net/downloads/snaps/php-%branch%/%revision%/%test_pack%.zip

	bitsadmin /transfer DownloadingSnap-%branch%-%build%-%cpu%-TestPack /download /priority high !test_pack_link! %PFTT_CACHE%\%test_pack%.zip
	7za.exe x %PFTT_CACHE%\%test_pack%.zip -o%PHP_BUILDS%\*
	del %PFTT_CACHE%\%test_pack%.zip
) else (
	echo Test pack already exists. Remove or move file in %PHP_BUILDS% if you want new copy.
)