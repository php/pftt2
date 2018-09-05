package com.mostc.pftt.main

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.app.EPhpUnitTestStatus;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.AbstractPhpUnitRW;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.results.ConsoleManager;

class PhpUnitSingleHostMultiBuildMultiScenarioSetReportGen extends Report2 {

	@Override
	def run(AHost host, String test_pack_name_and_version, GroovyObject body, CmpReport2 cmp, def builds, ConsoleManager cm, boolean abbreviated) {
		body.table('border': 1, 'cellpadding': 5, 'style': 'background:#ECEEE1') {
			tr {
				td('colspan': 1+(5*builds.size())) {
					h1('PFTT PhpUnit Results Overview')
				}
			}
			tr {
				td('colspan': 1+(5*builds.size())) {
					h1(test_pack_name_and_version)
				}
			}
			tr {
				td()
				builds.each { build ->
					td('colspan': 5, 'style': 'background:#A9CAED') {
						b(build.build_branch)
						span('-')
						span(build.version_revision)
						span('-')
						b(build.build_type)
						span('-')
						span(build.cpu_arch)
					}
				}
			}
			tr {
				td()
				def first_col = true;
				builds.each { build ->
					td('style': 'background:#A9CAED', 'Pass%')
					td(first_col?'Failure':'F')
					td(first_col?'Error':'E')
					td(first_col?'Timeout':'T')
					td(first_col?'Crash':'C')
					first_col = false;
				}
			}
			cmp.getPhpUnitScenarioSets(test_pack_name_and_version).each { scenario_set ->
				tr {
					td(scenario_set.toString())
					builds.each { build ->
						AbstractPhpUnitRW r = cmp.getPhpUnit(host, build, scenario_set, test_pack_name_and_version)
						if (r==null) {
							td('colspan': 5, '-')
						} else {
							td('style': 'background:#A9CAED', r.passRate()+"%")
							td(r.count(EPhpUnitTestStatus.FAILURE))
							td(r.count(EPhpUnitTestStatus.ERROR))
							td(r.count(EPhpUnitTestStatus.TIMEOUT))
							td(r.count(EPhpUnitTestStatus.CRASH))
						}
					}
				}
			}
		} // end table
		
		// also: don't include ERRORs as its a bigger list (too much data)
		//       and the list of FAILURES is more useful
		body.br()
		body.table('border': 1, 'style': 'background:#ECEEE1') {
			tr {
				td('colspan': 1+(2*builds.size())) {
					h1('Unique Failures and Crashes')
				}
			}
			tr {
				td('colspan': 1+(2*builds.size())) {
					h1(test_pack_name_and_version)
				}
			}
			tr {
				td('style': 'width:15%;max-width:15%')
				builds.each { build ->
					td('colspan': 2, 'style': 'background:#A9CAED') {
						b(build.build_branch)
						span('-')
						span(build.version_revision)
						span('-')
						b(build.build_type)
						span('-')
						span(build.cpu_arch)
					}
				}
			}
			tr {
				td()
				builds.each { build ->
					td('Failures', 'style': 'background:#A9CAED')
					td('Crashes', 'style': 'background:#A9CAED')
				}
			}
			cmp.getPhpUnitScenarioSets(test_pack_name_and_version).each { scenario_set ->
				tr {
					td(scenario_set.toString())
					builds.each { build ->
						td {
								cmp.getUniquePhpUnitTestNames(build, test_pack_name_and_version, scenario_set, EPhpUnitTestStatus.FAILURE).each {
									p(it)
								}
							}
						td {
								cmp.getUniquePhpUnitTestNames(build, test_pack_name_and_version, scenario_set, EPhpUnitTestStatus.CRASH).each {
									p(it)
								}
							}
					}
				}
			}
		} // end table
		/*
		 * Don't list Errors and Failures separately (user will just have the unique lists to look at)
		 * because its too much data
		
		body.br()
		
		// Errors are worse than Failures 
		// @see http://phpunit.de/manual/3.7/en/textui.html
		body.table('border': 1, 'style': 'background:#ECEEE1') {
			tr {
				td('colspan': 1+(1*builds.size())) {
					h1('Errors')
				}
			}
			tr {
				td('colspan': 1+(1*builds.size())) {
					h1(test_pack_name_and_version)
				}
			}
			tr {
				td('style': 'width:35%;max-width:35%')
				builds.each { build ->
					td('style': 'background:#A9CAED') {
						b(build.build_branch)
						span('-')
						span(build.version_revision)
						span('-')
						b(build.build_type)
						span('-')
						span(build.cpu_arch)
					}
				}
			}
			cmp.getPhpUnitTestNames(test_pack_name_and_version, EPhpUnitTestStatus.ERROR).each { test_name ->
				tr {
					td(test_name)
					builds.each { build ->
						td {
							cmp.getPhpUnitScenarioSets(build, test_pack_name_and_version, test_name, EPhpUnitTestStatus.ERROR).each { scenario_set_str ->
								p('style': 'background:'+cmp.getColor(scenario_set_str), scenario_set_str)
							}
						}
					}
				}
			}
		} // end table
		body.br()
		
		body.table('border': 1, 'style': 'background:#ECEEE1') {
			tr {
				td('colspan': 1+(1*builds.size())) {
					h1('Failures')
				}
			}
			tr {
				td('colspan': 1+(1*builds.size())) {
					h1(test_pack_name_and_version)
				}
			}
			tr {
				td('style': 'width:35%;max-width:35%')
				builds.each { build ->
					td('style': 'background:#A9CAED') {
						b(build.build_branch)
						span('-')
						span(build.version_revision)
						span('-')
						b(build.build_type)
						span('-')
						span(build.cpu_arch)
					}
				}
			}
			cmp.getPhpUnitTestNames(test_pack_name_and_version, EPhpUnitTestStatus.FAILURE).each { test_name ->
				tr {
					td(test_name)
					builds.each { build ->
						td {
							cmp.getPhpUnitScenarioSets(build, test_pack_name_and_version, test_name, EPhpUnitTestStatus.FAILURE).each { scenario_set_str ->
								p('style': 'background:'+cmp.getColor(scenario_set_str), scenario_set_str)
							}
						}
					}
				}
			}
		} // end table
		*/
		body.br()
		
		body.table('border': 1, 'style': 'background:#ECEEE1') {
			tr {
				td('colspan': 1+(1*builds.size())) {
					h1('Crashes')
				}
			}
			tr {
				td('colspan': 1+(1*builds.size())) {
					h1(test_pack_name_and_version)
				}
			}
			tr {
				td('style': 'width:35%;max-width:35%')
				builds.each { build ->
					td('style': 'background:#A9CAED') {
						b(build.build_branch)
						span('-')
						span(build.version_revision)
						span('-')
						b(build.build_type)
						span('-')
						span(build.cpu_arch)
					}
				}
			}
			cmp.getPhpUnitTestNames(test_pack_name_and_version, EPhpUnitTestStatus.CRASH).each { test_name ->
				tr {
					td(test_name)
					builds.each { build ->
						td {
							cmp.getPhpUnitScenarioSets(build, test_pack_name_and_version, test_name, EPhpUnitTestStatus.CRASH).each { scenario_set_str ->
								p('style': 'background:'+cmp.getColor(scenario_set_str), scenario_set_str)
							}
						}
					}
				}
			}
		} // end table
	}
	
}
