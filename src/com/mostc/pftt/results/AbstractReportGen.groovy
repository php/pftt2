package com.mostc.pftt.results

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.EBuildBranch;

abstract class AbstractReportGen {
	static int ABBREVIATED_MAX_LENGTH = 512*1024;

	abstract void run(ConsoleManager cm, boolean abbreviated);
	
	abstract String getHTMLString(ConsoleManager cm, boolean abbreviated);
	
	String createHTMLTempFile(AHost host) {
		String html_str = getHTMLString();
		
		String html_file = host.mktempname("Report", ".html")
		
		System.out.println(html_file);
		System.out.println(html_str);
		host.saveTextFile(html_file, html_str);
		
		return html_file;
	}
}
