<?php

class dummy {
	public function __toString() {
		unset($GLOBALS['arr1'][0]);
		return '';
	}
}

$arr1 = array(new dummy, '1234');
%{classname}::%{methodname}(%{args2}&$arr1);
