package com.mostc.pftt.model.app;

import com.mostc.pftt.model.ActiveTestPack;

public class PhpUnitActiveTestPack implements ActiveTestPack {
	protected final String local_test_pack_dir, remote_test_pack_dir;

	public PhpUnitActiveTestPack(String local_test_pack_dir, String remote_test_pack_dir) {
		this.local_test_pack_dir = local_test_pack_dir;
		this.remote_test_pack_dir = remote_test_pack_dir;
	}

	@Override
	public String getRunningDirectory() {
		return remote_test_pack_dir;
	}

	@Override
	public String getStorageDirectory() {
		return local_test_pack_dir;
	}
	
	
}
