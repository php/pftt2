package com.mostc.pftt.main

import groovy.lang.GroovyObject;
import groovy.xml.MarkupBuilder;

import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.host.AHost;

class CmpReport2G {
	public void run(BuilderSupport html, CmpReport2 cmp, ConsoleManager cm) {
		html.html {
			html.body {
				
		for (AHost host : cmp.hosts) {
			// TODO detect multiple hosts and make separate reports for each host
			for (String test_pack_name : cmp.phpt_test_packs) {
				PHPTSingleHostMultiBuildMultiScenarioSetReportGen phpt_report = new PHPTSingleHostMultiBuildMultiScenarioSetReportGen();
				phpt_report.run(host, test_pack_name, html, cmp, cmp.result_packs.keySet(), cm, false);
				html.br();
			}
		
			for (String test_pack_name : cmp.phpunit_test_packs) {
				PhpUnitSingleHostMultiBuildMultiScenarioSetReportGen phpunit_report = new PhpUnitSingleHostMultiBuildMultiScenarioSetReportGen();
				phpunit_report.run(host, test_pack_name, html, cmp, cmp.result_packs.keySet(), cm, false);
				html.br()
			}
		}
			} // body
		} // html
		
	}
}
