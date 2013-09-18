clear
source `dirname $PWD/$BASH_SOURCE`/set_env
echo
echo
echo           PFTT Shell
echo
echo
echo Type `pftt <enter>` to get help with PFTT. <tab> for auto-completion. 
echo <UP arrow> for history. Control+C to interrupt. Right-click for copy/paste menu.
echo Type `examples <enter>` to get some examples for using PFTT.
echo
echo Command Aliases:
echo core_all      core_named    core_list  ca cn cl  caaa  caaaua
echo app_all       all_named     app_list   aa an al  caua  smoke  parse
echo ui_all        ui_named      ui_list    ua un ul  aua   run-test
echo
echo Setup:
echo lc            setup       info
echo 
echo Cleanup: 
echo tka_apache    tka_php     stop    php_sdk
echo
echo
export PFTT_SHELL=1
php_sdk
