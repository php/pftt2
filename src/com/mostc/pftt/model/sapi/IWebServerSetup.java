package com.mostc.pftt.model.sapi;

import com.mostc.pftt.scenario.IScenarioSetup;

public interface IWebServerSetup extends IScenarioSetup {
	public String getHostname();
	public int getPort();
	public String getRootURL();
}
