package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ApplicationScenario;
import com.mostc.pftt.scenario.EScenarioSetPermutationLayer;
import com.mostc.pftt.scenario.IScenarioSetup;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.scenario.FileSystemScenario;

/** Simple PHP script that just prints 'Hello World'
 * 
 * @author Matt Ficken
 *
 */

public class HelloWorldScenario extends ApplicationScenario {

	@Override
	public IScenarioSetup setup(ConsoleManager cm, FileSystemScenario fs, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) {
		def php_code = """
<?php

echo "Hello World";

?>
"""
		
		host.saveTextFile("helloworld.php", php_code)
		
		return SETUP_SUCCESS;
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
