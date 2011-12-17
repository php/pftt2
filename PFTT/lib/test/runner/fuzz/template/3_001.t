<?php

try {
	$x = new %{classname};

	$x->foo = 1;
	$x->bar[0] = 2;
} catch (Exception $e) {
	$x->foo = 1;
	$x->bar[0] = 2;
}

unserialize(serialize($x)) = new %{classname};


