package com.mostc.pftt.report

import com.mostc.pftt.host.AHost;

abstract class AbstractReportGen implements Runnable {

	abstract String getHTMLString();
	
	String createHTMLTempFile(AHost host) {
		String html_str = getHTMLString();
		
		String html_file = host.mktempname("Report", ".html")
		
		System.out.println(html_file);
		System.out.println(html_str);
		host.saveTextFile(html_file, html_str);
		
		return html_file;
	}
}
