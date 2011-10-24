@echo off


set PHP_CMD_SHELL=1
set PHP_SDK=%SYSTEMDRIVE%\php-sdk
set PHPT_BRANCH=%PHP_SDK%\svn\branches
set PHP_BIN=%PHP_SDK%\bin
set PHPS=%PHP_SDK%\builds
set PFTT_HOME=%PHP_SDK%\PFTT2\PFTT
REM TODO TEMP use regular PFTT_HOME
set PFTT_HOME=%PHP_SDK%\0\PFTT2\PFTT
set PFTT_RESULTS=%PHP_SDK%\PFTT-Results
set PFTT_SCRIPTS=%PHP_SDK%\PFTT-Scripts
set PFTT_PHPS=%PHP_SDK%\PFTT-PHPs
set PHP_DEPS=%PHP_SDK%\deps
set PHP_DEP_LIBS=%PHP_DEPS\libs
set PHP_DEP_INCLUDES=%PHP_DEPS\includes


set PHP54_TS=%PHPS%\php-5.4-ts-windows-vc9-x86-
set PHP54_NTS=%PHPS%\php-5.4-nts-windows-vc9-x86-
set PHP55_TS=%PHPS%\php-5.5-ts-windows-vc9-x86-
set PHP55_NTS=%PHPS%\php-5.5-nts-windows-vc9-x86-

REM set vars for shared network resources (if client is using a PFTT server)
CALL %PFTT_HOME%\scripts\SDK4Win\net_config.cmd

REM configure git (for pftt devs/encourge pftt users to become devs)
CALL %PFTT_HOME%\scripts\SDK4Win\git_config.cmd

IF NOT EXIST %PHP_SDK% MKDIR %PHP_SDK%
IF NOT EXIST %PHPT_BRANCH% MKDIR %PHPT_BRANCH%
IF NOT EXIST %PHPS% MKDIR %PHPS%
IF NOT EXIST %PFTT_RESULTS% MKDIR %PFTT_RESULTS%
IF NOT EXIST %PFTT_SCRIPTS% MKDIR %PFTT_SCRIPTS%
IF NOT EXIST %PFTT_PHPS% MKDIR %PFTT_PHPS%

set PATH=%PFTT_HOME%;%PFTT_HOME%\Scripts\SDK4Win\;%PATH%

REM "%ProgramFiles%\Microsoft SDKs\Windows\v7.0\Bin\SetEnv.cmd" /xp /x86 /release

REM %PHP_BIN%\phpsdk_setvars.bat

cd %PFTT_HOME%


welcome
