package com.mostc.pftt.results;

import java.io.File;

import com.mostc.pftt.model.core.PhpBuildInfo;

public class UITestReader extends AbstractUITestRW {
	protected final String os_name;

	public UITestReader(File dir, PhpBuildInfo build_info, String scenario_set_str, String test_pack_name_and_version, String os_name) {
		super(dir, build_info, scenario_set_str, test_pack_name_and_version);
		this.os_name = os_name;
	}

	@Override
	public String getHTMLURL(String test_name) {
		return null;
	}

	@Override
	public String getOSName() {
		return os_name;
	}

}
