<?php

class dummy extends %{classname} {
	public function __construct() { }
	public function __call($a,$b) { }
}

$x = new dummy;
$x[0] = 1;
$x->b();
