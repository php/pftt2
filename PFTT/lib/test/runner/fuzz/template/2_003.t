<?php

try {
	$x = new %{classname}(%{args});
	$x->%{methodname}(%{args});
} catch (Exception $e) {
	$x->%{methodname}(%{args});
}
