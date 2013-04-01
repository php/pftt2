package com.mostc.pftt.results;

import java.io.File;

import javax.annotation.concurrent.NotThreadSafe;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.PhpBuildInfo;
import com.mostc.pftt.model.ui.EUITestStatus;

@NotThreadSafe
public class UITestWriter extends AbstractUITestRW {
	protected final AHost host;
	
	public UITestWriter(AHost host, File dir, PhpBuildInfo build_info, String scenario_set_str, String test_pack_name_and_version) {
		super(dir, build_info, scenario_set_str, test_pack_name_and_version);
		this.host = host;
	}

	@Override
	public void addResult(String test_name, String comment, EUITestStatus status, String verified_html) {
		addResult(test_name, comment, status, verified_html, null, null);
	}
	
	public void addResult(String test_name, String comment, EUITestStatus status, String verified_html, String sapi_output, String sapi_config) {
		// make sure name is unique
		if (hasTestNamed(test_name)) {
			for ( int i=2 ; i < 100 ; i++ ) {
				if (!hasTestNamed(test_name+"-"+i)) {
					test_name = test_name + "-" + i;
					break;
				}
			}
		}
		//
		
		switch(status) {
		case FAIL:
		case FAIL_WITH_WARNING:
		case PASS_WITH_WARNING:
		case TEST_EXCEPTION:
		case SKIP:
			// TODO record html
			break;
		default:
			break;
		}
		// TODO write to xml file
		
		super.addResult(test_name, comment, status, verified_html);
	} // end public void addResult
	
	public String getHTMLURL(String test_name) {
		UITestResult result = results_by_name.get(test_name);
		return result == null ? null : result.verified_html;
	}

	@Override
	public String getOSName() {
		return host.getOSNameLong();
	}
	
} // end public class UITestWriter
