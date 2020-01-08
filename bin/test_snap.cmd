@ECHO OFF 
setlocal enabledelayedexpansion

set branch=%1

REM Check if parameters are set
if %branch%.==. (
	GOTO args_error
) else (
	GOTO set_env
)


:args_error
echo User error: must specify branch
echo test_release "<branch>"
echo Branch can be any of: 7.1, 7.2, 7.3, 7.4
exit /b

:set_env
REM set important env vars
IF DEFINED PFTT_SHELL GOTO :skip_set_env
CALL %~dp0set_env.cmd
:skip_set_env

if not exist %PHP_BUILDS% (
	md %PHP_BUILDS%
)

SET cpu[0]=x64
SET cpu[1]=x86
SET cpu[2]=x64
SET cpu[3]=x86
SET thread[0]=NTS
SET thread[1]=NTS
SET thread[2]=TS
SET thread[3]=TS
set test_pack=php-test-pack-%branch%-latest

if %branch%==7.1 (
	set build=vc14
) else (
	set build=vc15
)

SET revision=latest
CALL %~dp0get_latest_revision.cmd

REM Only run next part if revision is not null, otherwise skip
if [!revision!]==[] (
	echo No new build available.
	exit /b
)

for /L %%j in (0,1,3) do (
	SET package[%%j]=php-!branch!-!thread[%%j]!-windows-!build!-!cpu[%%j]!-!revision!
	SET test_pack[%%j]=php-test-pack-!branch!-!thread[%%j]!-windows-!build!-!cpu[%%j]!-!revision!
	
	call %~dp0get_snapshot.cmd !branch! !thread[%%j]! !cpu[%%j]! !revision!
	call %~dp0pftt.cmd -results_only core_list %PHP_BUILDS%\!package[%%j]! %PHP_BUILDS%\!test_pack[%%j]! %PFTT_HOME%\tests-to-run.txt
)
