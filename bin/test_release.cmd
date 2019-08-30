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

SET cpu[0]=x64
SET cpu[1]=x86
SET cpu[2]=x64
SET cpu[3]=x86
SET thread[0]=NTS
SET thread[1]=NTS
SET thread[2]=TS
SET thread[3]=TS
set test_pack=php-test-pack-%branch%-latest

SET PHP_BUILDS=%~d0\PHPBuilds

if %branch%==7.1 (
	set build=vc14
) else (
	set build=vc15
)

for /L %%j in (0,1,3) do (
	call %~dp0get_release.cmd %branch% !thread[%%j]! !cpu[%%j]!
	
	if !thread[%%j]!==NTS (
		set file_name=php-%branch%-!thread[%%j]!-win32-%build%-!cpu[%%j]!-latest
	) else (
		set file_name=php-%branch%-win32-%build%-!cpu[%%j]!-latest
	)
	
	call %~dp0pftt.cmd -results_only core_list %PHP_BUILDS%\!file_name! %PHP_BUILDS%\%test_pack% %PFTT_HOME%\tests-to-run.txt
	call %~dp0pftt.cmd -config opcache -results_only core_list %PHP_BUILDS%\!file_name! %PHP_BUILDS%\%test_pack% %PFTT_HOME%\tests-to-run.txt
)