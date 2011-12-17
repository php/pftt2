<?php

class dummy {
	public function __toString() {
		unset($GLOBALS['arr1'][0]);
		$GLOBALS['test1'] .= str_repeat('B', 34);
		$GLOBALS['test2'] .= str_repeat('C', 34);
		return '';
	}
}

$test1=0;
$test2=4;
$arr1 = array(new dummy, '343434');
%{funcname}(%{args2}"/555555/", &$arr1);
