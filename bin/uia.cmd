@ECHO OFF
REM windows doesn't do aliases, so have to make a batch script. see `pftt_shell` on *nix
pftt -ignore_unknown_option %* ui_all %*
