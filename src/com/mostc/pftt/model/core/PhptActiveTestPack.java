package com.mostc.pftt.model.core;

import com.mostc.pftt.model.ActiveTestPack;

public class PhptActiveTestPack implements ActiveTestPack {
	protected final String running_dir, storage_dir;

	public PhptActiveTestPack(String running_dir, String storage_dir) {
		this.running_dir = running_dir;
		this.storage_dir = storage_dir;
	}
	
	@Override
	public String getRunningDirectory() {
		return running_dir;
	}
	
	@Override
	public String getStorageDirectory() { 
		return storage_dir;
	}
	
	@Override
	public String toString() {
		return "[running="+getRunningDirectory() + " ; storage="+getStorageDirectory()+"]";
	}
	
}
