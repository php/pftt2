package com.mostc.pftt.util;

public class DllVersion {
	protected String path;
	protected String version;
	
	public DllVersion(String path, String version) {
		this.path = path;
		this.version = version;
	}
	
	public String getVersion() {
		return version;
	}
	
	public String getPath() {
		return path;
	}
}
