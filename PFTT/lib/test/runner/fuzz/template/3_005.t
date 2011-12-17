<?php

class dummy extends %{classname} {
	public function __construct() {
		parent::__construct(%{args});
	}
	public function __call($a,$b) { }
}

$x = new dummy;
$x[0] = 1;
$x->b();
