<?php

class dummy {}

function errhandler() {
	unset($GLOBALS['arr1'][0]);
	return true;
}

$arr1 = array(new dummy, 1);
$oldhandler = set_error_handler("errhandler");

$x = new %{classname};
$x->%{methodname}(&$arr1, %{args2}$info);
restore_error_handler();
