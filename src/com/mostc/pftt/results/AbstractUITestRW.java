package com.mostc.pftt.results;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;

import com.mostc.pftt.model.core.PhpBuildInfo;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.ui.EUITestStatus;

public abstract class AbstractUITestRW extends AbstractTestResultRW {
	protected final File dir;
	protected final PhpBuildInfo build_info;
	protected String scenario_set_str, test_pack_name_and_version, os_name, web_browser_name_and_version, notes;
	protected final HashMap<String,UITestResult> results_by_name;
	protected final HashMap<EUITestStatus,LinkedList<UITestResult>> results_by_status;
	
	public AbstractUITestRW(File dir, PhpBuildInfo build_info) {
		this.dir = dir;
		this.build_info = build_info;
		
		//
		results_by_name = new HashMap<String,UITestResult>();
		results_by_status = new HashMap<EUITestStatus,LinkedList<UITestResult>>();
		for (EUITestStatus status:EUITestStatus.values())
			results_by_status.put(status, new LinkedList<UITestResult>());
	}
		
	public String getWebBrowserNameAndVersion() {
		return web_browser_name_and_version;
	}
	
	public boolean hasTestNamed(String test_name) {
		return results_by_name.containsKey(test_name);
	}

	public void addResult(String test_name, String comment, EUITestStatus status, String verified_html) {
		UITestResult result = new UITestResult(test_name, comment, status, verified_html);
		results_by_name.put(test_name, result);
		results_by_status.get(status).add(result);
	}
	
	protected static class UITestResult {
		final String test_name, comment, verified_html;
		final EUITestStatus status;
		
		public UITestResult(String test_name, String comment, EUITestStatus status, String verified_html) {
			this.test_name = test_name;
			this.comment = comment;
			this.status = status;
			this.verified_html = verified_html;
		}
	}

	public EUITestStatus getTestStatus(String test_name) {
		UITestResult result = results_by_name.get(test_name);
		return result == null ? null : result.status;
	}

	public abstract String getHTMLURL(String test_name);
	
	public String getScreenshotFilename(String test_name) {
		return dir+File.separator+test_name+".png";
	}

	public String getComment(String test_name) {
		UITestResult result = results_by_name.get(test_name);
		return result == null ? null : result.comment;
	}

	public String getTestPackNameAndVersionString() {
		return test_pack_name_and_version;
	}

	public int count(EUITestStatus status) {
		return results_by_status.get(status).size();
	}

	public List<String> getTestNames(EUITestStatus status) {
		LinkedList<UITestResult> results = results_by_status.get(status);
		ArrayList<String> names = new ArrayList<String>(results.size());
		for ( UITestResult result : results )
			names.add(result.test_name);
		return names;
	}
	
	public Collection<String> getTestNames() {
		return results_by_name.keySet();
	}

	@Override
	public String getScenarioSetNameWithVersionInfo() {
		return scenario_set_str;
	}

	@Override
	public PhpBuildInfo getBuildInfo() {
		return build_info;
	}
	
	public abstract String getNotes();

	@Override
	public void close() throws IOException {
		
	}
	public int getTestCount() {
		return count(EUITestStatus.PASS)
				+ count(EUITestStatus.PASS_WITH_WARNING)
				+ count(EUITestStatus.FAIL)
				+ count(EUITestStatus.FAIL_WITH_WARNING)
				+ count(EUITestStatus.XFAIL)
				+ count(EUITestStatus.CRASH)
				+ count(EUITestStatus.TEST_EXCEPTION);
	}
	@Override
	public float passRate() {
		return 100.0f * ((float)count(EUITestStatus.XFAIL)+count(EUITestStatus.PASS)+count(EUITestStatus.PASS_WITH_WARNING))/((float)getTestCount());
	}
	
	@Nonnull
	public abstract PhpIni getPhpIni();
	
} // end public abstract class AbstractUITestRW
