@ECHO OFF 
setlocal enabledelayedexpansion

REM set important env vars
IF DEFINED PFTT_SHELL GOTO :skip_set_env
CALL %~dp0set_env.cmd
:skip_set_env

if not exist %PHP_BUILDS% (
	md %PHP_BUILDS%
)

SET branch[0]=7.1
SET branch[1]=7.2
SET branch[2]=7.3
SET branch[3]=7.4
SET cpu[0]=x64
SET cpu[1]=x86
SET cpu[2]=x64
SET cpu[3]=x86
SET thread[0]=NTS
SET thread[1]=NTS
SET thread[2]=TS
SET thread[3]=TS
SET vc[0]=vc14
SET vc[1]=vc15
SET vc[2]=vc15
SET vc[3]=vc15

REM Check php-7.1 for new snap builds
for /L %%i in (0,1,3) do (
	SET branch=!branch[%%i]!
	SET revision=latest
	CALL %~dp0get_latest_revision.cmd
	
	REM Only run next part if revision is not null, otherwise skip
	if not [!revision!]==[] (
		for /L %%j in (0,1,3) do (
			SET build[%%j]=php-!branch!-!thread[%%j]!-windows-!vc[%%i]!-!cpu[%%j]!-!revision!
			SET test_pack[%%j]=php-test-pack-!branch!-!thread[%%j]!-windows-!vc[%%i]!-!cpu[%%j]!-!revision!
			
			REM If the build does not exist, fetch and test it
			if not exist %PHP_BUILDS%\!build[%%j]! (
				call %~dp0get_snapshot.cmd !branch! !thread[%%j]! !cpu[%%j]! !revision!
				REM call %~dp0pftt.cmd -results_only core_list %PHP_BUILDS%\!build[%%j]! %PHP_BUILDS%\!test_pack[%%j]! %PFTT_HOME%\tests-to-run.txt
			) else (
				echo Build already exists: !build[%%j]!
			)
		)
	) else (
		echo No new build available.
	)
)