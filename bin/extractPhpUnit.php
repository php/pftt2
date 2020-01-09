<?php
if(isset($argv[1])) {
	$path = str_replace("\\", "/", $argv[1]);
	
	try {
		$phar = new Phar($path);
		
		$path_parts = explode("/", $path);
		$file = $path_parts[count($path_parts) - 1];
		$file_parts = explode(".phar", $file);
		$file_name = $file_parts[0];
		
		$phar->extractTo(dirname($path).'/'.$file_name, null, true); 
	} catch (Exception $e) {
		echo "There was an error.\n";
		echo "Make sure command is: php extractPhpUnit.php path/to/phpunit.phar\n";
	}
} else {
	echo "Please specify path to phpunit.phar file";
}