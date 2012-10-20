package com.mostc.pftt.report

import com.mostc.pftt.host.Host;

abstract class AbstractReportGen implements Runnable {

	abstract String getHTMLString();
	
	String createHTMLTempFile(Host host) {
		String html_str = getHTMLString();
		
		String html_file = host.mktempname(".html")
		
		System.out.println(html_file);
		System.out.println(html_str);
		host.saveText(html_file, html_str);
		
		return html_file;
	}
}
