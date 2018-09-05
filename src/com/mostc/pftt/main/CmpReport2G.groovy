package com.mostc.pftt.main

import groovy.lang.GroovyObject;
import groovy.xml.MarkupBuilder;

import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.host.AHost;

class CmpReport2G {
	public void run(String url, BuilderSupport html, CmpReport2 cmp, ConsoleManager cm) {
		html.html {
			html.body {
				if (url!=null&&url.length()>0) {
					// important: put ' ' after URL or some HTML-to-text converters will
					//            merge the URL with any formatting or other characters
					p('For HTML Version, see: '+url+'  ')
				}
				def os_names;
				
				for (AHost host : cmp.hosts) {
					// TODO detect multiple hosts and make separate reports for each host
					for (String test_pack_name : cmp.phpt_test_packs) {
						PHPTSingleHostMultiBuildMultiScenarioSetReportGen phpt_report = new PHPTSingleHostMultiBuildMultiScenarioSetReportGen();
						phpt_report.run(host, test_pack_name, html, cmp, cmp.result_packs.keySet(), cm, false);
						html.br();
						
						os_names = cmp.result_packs.values().iterator().next().getBuildInfo().isX64()?
							test_pack_name.contains("MSSQL")?
								['Win 2008r2 SP0 x64', 'Win 2008r2 SP1 x64', 'Win 2012 SP0 x64', 'Win 2012r2', 'Win 7 SP0 x64', 'Win 7 SP1 x64', 'Win 8 SP0 x64']:
								['Win 2008r2 SP0 x64', 'Win 2008r2 SP1 x64', 'Win 2012 SP0 x64', 'Win 2012r2', 'Win 2008 SP1 x64', 'Win 2008 SP2 x64', 'Win 7 SP0 x64', 'Win 7 SP1 x64', 'Win 8 SP0 x64', 'Win Vista SP2 x64']:
							test_pack_name.contains("MSSQL")?
								['Win 2008r2 SP0 x64', 'Win 2008r2 SP1 x64', 'Win 2012 SP0 x64', 'Win 2012r2', 'Win 7 SP0 x64', 'Win 7 SP0 x86', 'Win 7 SP1 x64', 'Win 7 SP1 x86', 'Win 8 SP0 x64']:
								['Win 2008r2 SP0 x64', 'Win 2008r2 SP1 x64', 'Win 2012 SP0 x64', 'Win 2012r2', 'Win 2008 SP1 x64', 'Win 2008 SP1 x86', 'Win 2008 SP2 x64', 'Win 2008 SP2 x86', 'Win 7 SP0 x64', 'Win 7 SP0 x86', 'Win 7 SP1 x64', 'Win 7 SP1 x86', 'Win 8 SP0 x64', 'Win Vista SP2 x64', 'Win Vista SP2 x86']
						break;
					}
					
					html.table(border: '1', style:'background:#F2F2F2', 'cellspacing': '0', 'cellpadding': '10') {
						html.tr() {
							html.td('Operating Systems')
						}
						os_names.each { os_name ->
							html.tr() {
								html.td(os_name)
							}
						}
					} // end table
					
					html.br()
					for (String test_pack_name : cmp.phpunit_test_packs) {
						PhpUnitSingleHostMultiBuildMultiScenarioSetReportGen phpunit_report = new PhpUnitSingleHostMultiBuildMultiScenarioSetReportGen();
						phpunit_report.run(host, test_pack_name, html, cmp, cmp.result_packs.keySet(), cm, false);
						html.br()
					}
					break;
				}
			
				
				
				
				
		
			} // body
		} // html
		
	}
}
