@echo off
echo.
echo    PFTT SDK for Windows
echo.
echo.
echo  These Environment Variables may be useful: 
echo  PHPT_BRANCH  = %PHPT_BRANCH%
echo  PHPS         = %PHPS%
echo  PHP_DEPS     = %PHP_DEPS%
echo  PFTT_RESULTS = %PFTT_RESULTS%

if DEFINED NET_PFTT_SERVER (
echo.
echo  NET_PHPT = shared location with latest PHPTs
echo  NET_PHPS = shared location with PHP Builds
echo  NET_PHPS54_TS  = %%NET_PHPS%%\php-5.4-ts-windows-vc9-x86-
echo  NET_PHPS54_NTS = %%NET_PHPS%%\php-5.4-nts-windows-vc9-x86-
echo.
echo  ex: pftt func_full --phpt-dir=%%NET_PHPT%%\PHP_5_4 --php-dir=%%NET_PHP54_TS%%-r318303
echo.
)

echo.
echo  For everything, run 'pftt'
echo  Run 'last_pftt' to show output from last pftt command
echo.
echo.