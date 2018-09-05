package com.mostc.pftt.model.app

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.scenario.ScenarioSetSetup;

public class SimpleTestTemplate {
	
	// based on _simpletest_batch_operation() in simpletest.module
	public static String renderTemplate(AHost host, ScenarioSet scenario_set, SimpleTestCase test_case, String test_case_run_id) {
		"""<?php
echo "12"; echo PHP_EOL;
set_include_path('C:/php-sdk/PFTT/current/cache/working/drupal-7/;C:/php-sdk/PFTT/current/cache/working/drupal-7/includes;C:/php-sdk/PFTT/current/cache/working/drupal-7/modules/simpletest');
echo "14"; echo PHP_EOL;
require_once 'simpletest.module';
echo "16"; echo PHP_EOL;
simpletest_classloader_register();
echo "18 {$test_case.class_name}"; echo PHP_EOL;
\$test = new ${test_case.class_name}(${test_case_run_id});
echo "20"; echo PHP_EOL;
\$test->run();
echo "22"; echo PHP_EOL;
var_dump(\$test->getInfo());
echo "24"; echo PHP_EOL;
var_dump(\$test->results);
echo "26"; echo PHP_EOL;
?>"""
	}

}
