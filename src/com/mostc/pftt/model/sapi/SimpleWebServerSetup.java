package com.mostc.pftt.model.sapi;

import com.mostc.pftt.scenario.SimpleScenarioSetup;

public abstract class SimpleWebServerSetup extends SimpleScenarioSetup implements IWebServerSetup {

	@Override
	public String getRootURL() {
		return "http://"+getHostname()+":"+getPort()+"/";
	}
	
}
