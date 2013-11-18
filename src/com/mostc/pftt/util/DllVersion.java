package com.mostc.pftt.util;

public class DllVersion {
	protected final String base, dll_name, pdb_name, version;
	
	public DllVersion(String base, String dll_name, String pdb_name, String version) {
		this.base = base;
		this.dll_name = dll_name;
		this.pdb_name = pdb_name;
		this.version = version;
	}
	
	public DllVersion(String base, String dll_name, String version) {
		this(base, dll_name, dll_name.replace(".dll", ".pdb"), version);
	}
	
	public String getVersion() {
		return version;
	}
	
	public String getPath() {
		return base+"/"+dll_name;
	}
	
	public String getDebugPath() {
		return base+"/"+pdb_name;
	}
}
