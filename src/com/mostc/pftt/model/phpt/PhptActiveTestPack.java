package com.mostc.pftt.model.phpt;

import com.mostc.pftt.model.ActiveTestPack;

public class PhptActiveTestPack extends ActiveTestPack {
	protected final String test_pack_dir;

	public PhptActiveTestPack(String test_pack_dir) {
		this.test_pack_dir = test_pack_dir;
	}

	public String getDirectory() {
		return test_pack_dir;
	}
	
	@Override
	public String toString() {
		return getDirectory();
	}
	
}
