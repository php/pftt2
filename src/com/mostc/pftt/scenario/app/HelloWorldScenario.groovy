package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ApplicationScenario;
import com.mostc.pftt.scenario.ScenarioSet;

/** Simple PHP script that just prints 'Hello World'
 * 
 * @author Matt Ficken
 *
 */

public class HelloWorldScenario extends ApplicationScenario {

	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		def php_code = """
<?php

echo "Hello World";

?>
"""
		
		host.saveTextFile("helloworld.php", php_code)
		
		return true;
	}

	@Override
	public String getName() {
		return "HelloWorld";
	}

	@Override
	public boolean isImplemented() {
		return true;
	}

}
