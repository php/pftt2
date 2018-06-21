package com.mostc.pftt.model;

public abstract class ActiveTestPack {
	protected final String running_dir, storage_dir;

	public ActiveTestPack(String running_dir, String storage_dir) {
		this.running_dir = running_dir;
		this.storage_dir = storage_dir;
	}
	
	public String getRunningDirectory() {
		return running_dir;
	}
	
	public String getStorageDirectory() { 
		return storage_dir;
	}
	
	@Override
	public String toString() {
		return "[running="+getRunningDirectory() + " ; storage="+getStorageDirectory()+"]";
	}
}
