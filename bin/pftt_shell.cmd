@ECHO OFF
CLS
COLOR b0
TITLE PFTT Shell
CALL ^\php-sdk^\PFTT^\current^\bin^\set_env
@ECHO.
@ECHO.                                                +--------------------------------+
@ECHO. PPPPPP    FFFFFFFF  TTTTTTTTTT  TTTTTTTTTT     ^|                            ^< ^| ^|
@ECHO. PP    PP  FF            TT          TT     +-- ^\/^\/^\/^\/^\/^\                  ^<_^| ^|
@ECHO. PP    PP  FF            TT          TT    /               ^\/^\/^\/^\/^\/^\           ^|
@ECHO. PP    PP  FF            TT          TT   /                           ^\/^\/^\/^\/^\/^\/
@ECHO. PP    PP  FF            TT          TT   ^|             PPPP  H   H  PPPP
@ECHO. PP    PP  FF            TT          TT   ^|             P   P H   H  P   P
@ECHO. PPPPPP    FFFFF         TT          TT   /             P   P H   H  P   P
@ECHO. PP        FF            TT          TT --              PPPP  HHHHH  PPPP
@ECHO. PP        FF            TT          TT   ^\             P     H   H  P
@ECHO. PP        FF            TT          TT   ^|             P     H   H  P
@ECHO. PP        FF            TT          TT   ^|             P     H   H  P
@ECHO. PP        FF            TT          TT   ^\
@ECHO. PP        FF            TT          TT    ^\     ,.   (   .     )       .      "
@ECHO.                                            +-- ("     )  )'    ,'       )  . (`    '`
@ECHO.                                                .; )  ' (( (" )   ;(,    ((  ( ;) "  )"
@ECHO.                      /)                       _"., ,._'_.,)_(..,(. )_ _' )_') (._..( '..
@ECHO.                     // 
@ECHO.            .-------^| ^|--------------------------------------------.__
@ECHO.            ^|WMWMWMW^| ^|^>^>^>^>^>^>^>^>^>^>^>^>^>^> Php Full Test Tool ^>^>^>^>^>^>^>^>^>^>^>^>^>:^>
@ECHO.            `-------^| ^|--------------------------------------------'^^
@ECHO.                     ^\^\
@ECHO.                      ^\)    PFTT Shell - for testing the PHP Ecosystem
@ECHO.
ECHO  Type `pftt ^<enter^>` to get help with PFTT. ^<tab^> for auto-completion. 
ECHO  ^<F7^> for history. Control+C to interrupt. Right-click for copy/paste menu.
ECHO  Type `examples ^<enter^>` to get some examples for using PFTT.
@ECHO.
ECHO  Command Aliases:
ECHO  core_all      core_named    core_list  ca cn cl  caaa  caaaua
ECHO  app_all       all_named     app_list   aa an al  caua  open
ECHO  ui_all        ui_named      ui_list    ua un ul  aaua  smoke
ECHO  release_get   release_list  rl    rg   rgn   rgp parse run-test
@ECHO.
ECHO  Setup:
ECHO  list_config            setup
@ECHO. 
ECHO  Cleanup: 
ECHO  tka_apache    tka_php     tka_windbg     `net use`    stop
@ECHO.
ECHO  Useful:
ECHO  ls            `start .`   windbg   npp   clear  taskmgr
ECHO  php_sdk       sleep       pftt     cat   info
@ECHO.
@prompt $t %COMPUTERNAME% $M$p$g
REM signal pftt.cmd to not run pftt with elevate ever
REM pftt_shell should only be run from the PFTT shortcut which always runs pftt shell under UAC already
SET PFTT_SHELL=1
CALL php_sdk
