<?php

class dummy {
	public function __toString() {
		unset($GLOBALS['arr1'][0]);
		return '';
	}
}

$arr1 = array(new dummy, '1234');

$x = new %{classname};
$x->%{methodname}(%{args2}&$arr1);
