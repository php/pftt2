package com.mostc.pftt.main

import groovy.lang.GroovyObject;

import java.util.List;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.results.ConsoleManager;

abstract class Report2 {

	abstract def run(AHost host, String test_pack_name_and_version, GroovyObject body, CmpReport2 cmp, def builds, ConsoleManager cm, boolean abbreviated);
	
}
