@ECHO OFF
SETLOCAL
REM runs all tests on an RC build to make sure the binary is ready for production use

REM Usage:
REM rctest <ts build>;<nts build> <phpt test pack>
REM rctest <ts build x86>;<nts build x86>;<ts build x64>;<nts build x64> <phpt test pack>

REM release builds must pass these scenarios

REM core_all,app_all: 30 scenario sets (+6 for IIS-Standard, +6 for IIS express)
REM ui_all: 12 scenario sets (+3 for IIS-Standard, +3 for IIS express    *2 for Internet Explorer)

REM no spaces, separate with commas ,,,
SET PFTT_CONFIGS=apache,cli,builtin_web,smb,localfs,opcache,no_code_cache,symfony
REM 36 * 5 * 3 = 540 * 9
REM SET PFTT_CONFIGS=rchosts,apcu,wincacheu,no_user_cache,apache,cli,builtin_web,iis,iis_express,deduplication,dfs,smb,ca,localfs,opcache,no_code_cache,symfony,wordpress,joomla,mediawiki,drupal,xdebug,mysql,postgresql,imap,curl,ftp,soap,xmlrpc,EN_US,SHIFT_JIS,BIG5,EUC_JP,EUC_KR,GB18030,ISO_2022_CN,ISO_2022_JP,ISO_2022_KR

REM begin



IF "%PFTT_SHELL%"=="" (
ECHO rctest: rctest must be run within the PFTT Shell edition of the Windows Command Prompt
REM show directory in Windows Explorer
START %~dp0%
ECHO rctest:
ECHO rctest: Click the `PFTT Shell` shortcut in %~dp0%
ECHO.

) ELSE (



REM the real reason this can't be run outside of PFTT Shell/UAC is that pftt will split into a separate NON-BLOCKING command prompt window
REM which means this loop would cause several copies of PFTT to run at the same time (bad/a real mess)

REM parse %* ... can't look at just %~1 or %1 for the build paths. windows breaks ; into separate arguments!

IF "%1"=="" (
	@ECHO rctest: first argument must be PHPT test-pack to use
	@ECHO rctest: example: rctest php-test-pack-5.5.0beta4 php-5.5.0beta4-nts-Win32-VC11-x86;php-5.5.0beta4-Win32-VC11-x86
) ELSE (
	IF "%2"=="" (
		@ECHO rctest: second argument must be PHP builds to test
		@ECHO rctest: example: rctest php-test-pack-5.5.0beta4 php-5.5.0beta4-nts-Win32-VC11-x86;php-5.5.0beta4-Win32-VC11-x86
	) ELSE (

SET PHPT_TEST_PACK=%1

:start
IF "%1"=="" (goto :do_finished)
SET PHP_BUILD=%1

IF NOT "%PHP_BUILD%"=="" (
IF NOT "%PHPT_TEST_PACK%"=="" (

ECHO.
ECHO rctest: starting build: %PHP_BUILD%
ECHO rctest: PHPT Core test-pack: %PHPT_TEST_PACK%
ECHO.

REM finally run PFTT on this build
CALL pftt -skip_smoke_tests -c %PFTT_CONFIGS% -no_result_file_for_pass_xskip_skip -auto core_all %PHP_BUILD% %PHPT_TEST_PACK%
CALL pftt -skip_smoke_tests -c %PFTT_CONFIGS% -no_result_file_for_pass_xskip_skip -auto app_all %PHP_BUILD%
REM CALL pftt -skip_smoke_tests -c %PFTT_CONFIGS% -no_result_file_for_pass_xskip_skip -auto ui_all %PHP_BUILD%


ECHO. rctest: finished build: %PHP_BUILD%
ECHO.

)
)

REM get next build to test
SHIFT
GOTO :start

:do_finished
REM done testing all builds

ECHO PFTT: rctest: done
ECHO.

)

)

)
