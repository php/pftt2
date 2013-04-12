@ECHO OFF
CLS
TITLE PFTT Shell
CALL \php-sdk\PFTT\current\bin\set_env
@ECHO.
@ECHO.
@ECHO           PFTT Shell
@ECHO.
@ECHO.
ECHO Type `pftt ^<enter^>` to get help with PFTT. ^<tab^> for auto-completion. 
ECHO ^<F7^> for history. Control+C to interrupt. Right-click for copy/paste menu.
ECHO Type `examples ^<enter^>` to get some examples for using PFTT
@ECHO.
ECHO Command Aliases:
ECHO core_all      core_named    core_list  ca cn cl
ECHO app_all       all_named     app_list   aa an al
ECHO ui_all        ui_named      ui_list    ua un ul
ECHO release_get   release_list  rl    rg   rgn   rgp rctest
@ECHO.
ECHO Cleanup: 
ECHO tka_apache    tka_php     tka_windbg     `net use`
@ECHO.
ECHO Useful:
ECHO ls            `start .`   windbg   npp   cls
ECHO php_sdk       sleep       pftt
@ECHO.
@ECHO.
@prompt $t %COMPUTERNAME% $M$p$g
CALL php_sdk
