package com.mostc.pftt.results;

import java.io.IOException;

import com.mostc.pftt.model.core.PhpBuildInfo;

public abstract class AbstractTestResultRW {
	public abstract String getOSName();
	public abstract String getScenarioSetNameWithVersionInfo();
	public abstract PhpBuildInfo getBuildInfo();
	public abstract void close() throws IOException;
	public abstract float passRate();
	public abstract String getPath();
}
