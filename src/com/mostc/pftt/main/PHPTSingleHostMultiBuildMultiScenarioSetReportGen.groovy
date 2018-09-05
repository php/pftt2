package com.mostc.pftt.main

import java.util.List;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.EPhptTestStatus;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.AbstractPhptRW;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ScenarioSet;

class PHPTSingleHostMultiBuildMultiScenarioSetReportGen extends Report2 {
	
	@Override
	def run(AHost host, String test_pack_name_and_version, GroovyObject body, CmpReport2 cmp, def builds, ConsoleManager cm, boolean abbreviated) {
		int row = 1;
		body.table(border: '1', style:'background:#F2F2F2', 'cellspacing': '0', 'cellpadding': '10') {
			tr {
				td('colspan': 2+(builds.size()*4)) {
					h1('PFTT PHPT Results Overview')
				}
			}
			tr {
				td('colspan': 2+(builds.size()*4)) {
					h1(test_pack_name_and_version)
				}
			}
			tr {
				td('colspan': 2, 'Scenario Set')
				builds.each { build ->
					td('colspan': 4, 'style': 'background:#CCFF66') {
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
				td('colspan': 2)
				def first_col = true;
				builds.each { build ->
					td('style': 'background:#CCFF66', 'Pass%')
					td('style': 'background:yellow', first_col?'Fail':'F')
					td('style': 'background:#CCFF66', first_col?'Timeout':'T')
					td('style': 'background:yellow', first_col?'Crash':'C')
					first_col = false;
				}
			}
			cmp.getPhptScenarioSets(test_pack_name_and_version).each { scenario_set ->
				tr {
					td(row++)
					td(scenario_set.toString())
					builds.each { build ->
						AbstractPhptRW r = cmp.getPhpt(host, build, scenario_set, test_pack_name_and_version);
						if (r==null) {
							td('colspan': 4, 'n/a')
						} else {
							td('style': 'background:#CCFF66', r.passRate()+"%")
							int fail = r.count(EPhptTestStatus.FAIL)
							td('style': fail>0?'background:#ff0000':'background:yellow', fail)
							td('style': 'background:#CCFF66', r.count(EPhptTestStatus.TIMEOUT))
							int crash = r.count(EPhptTestStatus.CRASH)
							td('style': crash>0?'background:#ff0000':'background:yellow', crash)
						}
					}
				}
			}
		} // end table
		body.br()
		body.table(border: '1', style:'background:#F2F2F2') {
			tr {
				td('colspan': 2) {
					h1('Result-Packs')
				}
			}
			builds.each { build ->
				def branch = build.getBuildBranch()
				def rev = build.getVersionRevision();
				def url = "http://windows.php.net/downloads/snaps/ostc/pftt/$branch/$rev";
				tr {
					td(build.toString())
					td() {
						a('href': url, url)
					}
				}
			}
			tr {
				td('colspan': 2) {
					span('Download PFTT for Windows from: http://windows.php.net/downloads/snaps/ostc/pftt/')
				}
			}
		}
		body.br()
		body.table(border: '1', style:'background:#F2F2F2') {
			tr {
				td('colspan': 2) {
					h1('Unique Fails and Crashes')
				}
			}
			tr {
				td('colspan': 2)
				builds.each { build ->
					td('colspan': 2, 'style': 'background:#CCFF66') {
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
				td('colspan': 2)
				builds.each { build ->
					td('style': 'background:yellow', 'Fails')
					td('style': 'background:yellow', 'Crash')
				}
			}
			cmp.getPhptScenarioSets(test_pack_name_and_version).each { scenario_set ->
				tr {
					td(row++)
					td(scenario_set.toString())
					builds.each { build ->
						td('style': 'background:yellow', cmp.getUniquePhptTestNames(build, test_pack_name_and_version, scenario_set, EPhptTestStatus.FAIL)+"")
						td('style': 'background:yellow', cmp.getUniquePhptTestNames(build, test_pack_name_and_version, scenario_set, EPhptTestStatus.CRASH)+"")
					}
				}
			}
		} // end table
		body.br()
		body.table(border: '1', style:'background:#F2F2F2', 'cellspacing': '0', 'cellpadding': '10') {
			tr {
				td('colspan': 2+(builds.size()*4)) {
					h1('Fails')
				}
			}
			tr {
				td('colspan': 2+(builds.size()*1)) {
					h1(test_pack_name_and_version)
				}
			}
			tr {
				td('colspan': 2)
				builds.each { build ->
					td() {
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
			cmp.getPhptTestNames(test_pack_name_and_version, EPhptTestStatus.FAIL).each { test_name ->
				tr {
					td(row++)
					td(test_name)
					builds.each { build ->
						td('style': 'background:yellow') {
							cmp.getPhptScenarioSets(build, test_pack_name_and_version, test_name, EPhptTestStatus.FAIL).each { scenario_set_str ->
								p('style': 'background:'+cmp.getColor(scenario_set_str), scenario_set_str)
							}
						}
					}
				}
			}
		} // end table
		body.br()
		body.table(border: '1', style:'background:#F2F2F2') {
			tr {
				td('colspan': 2+(builds.size()*4)) {
					h1('Crashes')
				}
			}
			tr {
				td('colspan': 2+(builds.size()*1)) {
					h1(test_pack_name_and_version)
				}
			}
			tr {
				td('colspan': 2)
				builds.each { build ->
					td() {
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
			cmp.getPhptTestNames(test_pack_name_and_version, EPhptTestStatus.CRASH).each { test_name ->
				tr {
					td(row++)
					td(test_name)
					builds.each { build ->
						td('style': 'background:yellow') {
							cmp.getPhptScenarioSets(build, test_pack_name_and_version, test_name, EPhptTestStatus.CRASH).each { scenario_set_str ->
								p('style': 'background:'+cmp.getColor(scenario_set_str), scenario_set_str)
							}
						}
					}
				}
			}
		} // end table
		body.br()
	} // end void run
	
}
