package com.mostc.pftt.results

import com.github.mattficken.io.IOUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.EBuildBranch;

abstract class AbstractReportGen { 
	// don't send large email messages
	// keep in mind: you're probably sending both an HTML and a text/plain copy,
	// so the actual size of the mail message may be as much as double the size here
	static int ABBREVIATED_MAX_LENGTH = IOUtil.HALF_MEGABYTE;
 
	abstract void run(ConsoleManager cm, boolean abbreviated, BuilderSupport html);
	
	protected StringWriter sw;
	public String getHTMLString(ConsoleManager cm, boolean abbreviated) {
		sw = new StringWriter();
		run(cm, abbreviated, new groovy.xml.MarkupBuilder(sw));
		return sw.toString();
	}
	
	public String getPlainTextString(ConsoleManager cm, boolean abbreviated) {
		sw = new StringWriter();
		run(cm, abbreviated, new TextBuilder(sw));
		return sw.toString();
	}
	
	String createHTMLTempFile(AHost host) {
		String html_str = getHTMLString();
		
		String html_file = host.mktempname("Report", ".html")
		
		System.out.println(html_file);
		System.out.println(html_str);
		host.saveTextFile(html_file, html_str);
		
		return html_file;
	}
}
