#!/bin/bash

# user should do: `source pftt_shell.sh` or `source set_env.sh` once to add to their current shell

# user may set custom PHP_SDK instead of $HOME/php-sdk
if [ -e `dirname $PWD/$BASH_SOURCE`/internal/set_php_sdk.sh ]
then
	# this must set PHP_SDK
	source `dirname $PWD/$BASH_SOURCE`/internal/set_php_sdk.sh
	
	echo Custom PHP_SDK: $PHP_SDK
else
	export PHP_SDK=$HOME/php-sdk
fi

source `dirname $PWD/$BASH_SOURCE`/set_path.sh

function parse() {
	pftt -ignore_unknown_option $* parse $*
}
function app_list() {
	pftt -ignore_unknown_option $* app_list $*
}
function app_all() {
	pftt -ignore_unknown_option $* app_all $*
}
function app_named() {
	pftt -ignore_unknown_option $* app_named $*
}
function ui_list() {
	pftt -ignore_unknown_option $* ui_list $*
}
function ui_all() {
	pftt -ignore_unknown_option $* ui_all $*
}
function ui_named() {
	pftt -ignore_unknown_option $* ui_named $*
}
function core_list() {
	pftt -ignore_unknown_option $* core_list $*
}
function core_all() {
	pftt -ignore_unknown_option $* core_all $*
}
function core_named() {
	pftt -ignore_unknown_option $* core_named $*
}
function smoke() {
	pftt -ignore_unknown_option $* smoke $*
}
function info() {
	pftt -ignore_unknown_option $* info $*
}
function run_tests() {
	pftt -ignore_unknown_option $* run-test $*
}
alias run_test=run_tests
alias aa=app_all
alias al=app_list
alias an=app_named
alias ca=core_all
alias cl=core_list
alias cn=core_named
alias ua=ui_all
alias ul=ui_list
alias un=ui_named
alias uia=ui_all
alias uil=ui_list
alias uin=ui_named
function caaa() {
	pftt -ignore_unknown_option $* core_all,app_all $*
}
function caaaua() {
	pftt -ignore_unknown_option $* core_all,app_all,ui_all $*
}
function caua() {
	pftt -ignore_unknown_option $* core_all,ui_all $*
}
function list_config() {
	pftt -ignore_unknown_option $* list_config $*
}
alias lc=list_config
alias listconfig=list_config
# `setup` will conflict on redhat
function setup() {
	pftt -ignore_unknown_option $* setup $*
}
function stop() {
	pftt -ignore_unknown_option $* stop $*
}
function run-test() {
	pftt -ignore_unknown_option $* run-test $*
}
