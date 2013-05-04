package com.mostc.pftt.results

import java.io.StringWriter;

import com.mostc.pftt.model.core.EPhptTestStatus;
import com.mostc.pftt.model.core.EBuildBranch;

class PHPTReportGen extends AbstractReportGen {
	protected AbstractPhptRW base_telem, test_telem;
	protected int row;
	protected StringWriter sw;
	
	public PHPTReportGen(AbstractPhptRW base_telem, AbstractPhptRW test_telem) {
		this.base_telem = base_telem;
		this.test_telem = test_telem;
		
		sw = new StringWriter();
	}
	
	protected static String bav(EBuildBranch branch, String version) {
		if (version==null)
			return 'unknown'; // fallback
		else if (!version.startsWith('r'))
			version = 'r' + version;
		return branch.toString()+' '+version;
	}
	
	@Override
	public void run(ConsoleManager cm, boolean abbreviated) {
		def scenario_set_title = test_telem.getScenarioSetNameWithVersionInfo()
		def test_scenario_set_title = scenario_set_title + ' (Test)'
		if (scenario_set_title!=base_telem.getScenarioSetNameWithVersionInfo()) {
			// for some reason, comparing runs of 2 different SAPIs... make that clear in report.
			scenario_set_title = base_telem.getScenarioSetNameWithVersionInfo()+' (Base) with ' + test_telem.getScenarioSetNameWithVersionInfo()+' (Test)'
		}
		String base_build_branch_and_version = base_telem.getBuildInfo().toString();
		String base_test_pack_branch_and_version = bav(base_telem.getTestPackBranch(), base_telem.getTestPackVersion())
		String test_build_branch_and_version = test_telem.getBuildInfo().toString();
		String test_test_pack_branch_and_version = bav(test_telem.getTestPackBranch(), test_telem.getTestPackVersion())
		
		def os_names = abbreviated?['Localhost']:base_telem.getBuildInfo().isX64()?['Win 2008r2 SP0 x64', 'Win 2008r2 SP1 x64', 'Win 2012 SP0 x64', 'Win 2008 SP1 x64', 'Win 2008 SP2 x64', 'Win 7 SP0 x64', 'Win 7 SP1 x64', 'Win 8 SP0 x64', 'Win Vista SP2 x64']:['Win 2008r2 SP0 x64', 'Win 2008r2 SP1 x64', 'Win 2012 SP0 x64', 'Win 2008 SP1 x64', 'Win 2008 SP1 x86', 'Win 2008 SP2 x64', 'Win 2008 SP2 x86', 'Win 7 SP0 x64', 'Win 7 SP0 x86', 'Win 7 SP1 x64', 'Win 7 SP1 x86', 'Win 8 SP0 x64', 'Win Vista SP2 x64', 'Win Vista SP2 x86'];
		
		row=1
def html = new groovy.xml.MarkupBuilder(sw)
html.html {
	body {
		h1("PFTT Core PHPT Report")
		
		// warnings
		if (cm.isSkipSmokeTests()) {
			p('Warning: Smoke tests were not run on build. Smoke tests were disabled with -skip_smoke_tests console option.')
		}
		if (!cm.isThreadSafety()) {
			p('Warning: Some failures may be due to race conditions. Thread safety was disabled with -no_thread_safety console option.')
		}
		if (test_telem.getOSName() != base_telem.getOSName()) {
			// for some reason, comparing runs from two different OSes, make it clear in report.
			p("Warning: Base OS: ${base_telem.getOSName()} Test OS: ${test_telem.getOSName()}")
		}
		//

		table(border: 1, cellspacing:0, cellpadding:8) {
			/* --------------- begin headers -------------- */
			tr {
				td(colspan:15) {
					b(base_build_branch_and_version+" (Base)")
					span("with")
					b(test_build_branch_and_version+" (Test)")
					
					// check that the build and test pack versions all match
					if (
						base_telem.getTestPackBranch()==base_telem.getBuildInfo().getBuildBranch() &&
						test_telem.getTestPackBranch()==test_telem.getBuildInfo().getBuildBranch() &&
						(
							base_telem.getTestPackVersion()==base_telem.getBuildInfo().getVersionRevision()
							||base_telem.getBuildInfo().getVersionRevision().contains(base_telem.getTestPackVersion()) 
						) && (
							test_telem.getTestPackVersion()==test_telem.getBuildInfo().getVersionRevision()
							||test_telem.getBuildInfo().getVersionRevision().contains(test_telem.getTestPackVersion())
						)) {
						//
						span("(using test-packs included with each build)")
					} else {
						// if not, make it clear which versions of which were used
						span("(using $base_test_pack_branch_and_version with $base_build_branch_and_version and $test_test_pack_branch_and_version with $test_build_branch_and_version)")
					}
				}
			} // tr
			tr {
				td(colspan:15, scenario_set_title)
			}
			tr {
				td(colspan:2) { p(align: 'center', 'OS') }
				td(colspan:7, style:'background:#CCFF66') {}
				td(colspan:2, style:'background:yellow') {
					p(align:'center', 'Skip')
				}
				td(colspan:2, style:'background:#FFC000') {
					p(align:'center', 'Test Bug')
				}
				td(colspan:2, style:'background:#F28020') {
					p(align:'center', 'PFTT Bug')
				}
			} // tr
			tr {
				td(colspan:2) {}
				td(style:'background:#CCFF66') {
					p('Pass Rate(%)')
				}
				td(colspan:1, style:'background:#CCFF66') {
					p('Pass')
				}
				td(colspan:2, style:'background:#CCFF66') {
					p('Fail')
				}
				td(colspan:2, style:'background:#CCFF66') {
					p('Crash')
				}
				td(colspan:1, style:'background:#CCFF66') {
					p('XFail')
				}
				td(colspan:1, style:'background:yellow') {
					p('Skip')
				}
				td(colspan:1, style:'background:yellow') {
					p('XSkip*')
				}
				td(colspan:1, style:'background:#FFC000') {
					p('XFail (Work)')
				}
				td(colspan:1, style:'background:#FFC000') {
					p('Bork**')
				}
				td(colspan:1, style:'background:#F28020') {
					p('Unsupported***')
				}
				td(colspan:1, style:'background:#F28020') {
					p('Exceptions')
				}
			} // tr
			/* --------------- end headers -------------- */
			
			os_names.each { os_name ->
			tr {
				td(row++)
				td(os_name)
				td(style:'background:#CCFF66', PhpResultPack.round1(test_telem.passRate()))
				td(style:'background:#CCFF66', test_telem.count(EPhptTestStatus.PASS))
				td(style:'background:#CCFF66', test_telem.count(EPhptTestStatus.FAIL))
				int cmp_fail = test_telem.count(EPhptTestStatus.FAIL) - base_telem.count(EPhptTestStatus.FAIL);
				td(style:'background:'+(cmp_fail>0?'#FF0000':cmp_fail<0?'#96DC28':'#CCFF66'), cmp_symbol(cmp_fail))
				// highlight crash count if > 0
				int crash_count = test_telem.count(EPhptTestStatus.CRASH);
				td(style:crash_count>0?'background: #ff0000':'background:#CCFF66', crash_count)
				int cmp_crash = test_telem.count(EPhptTestStatus.CRASH) - base_telem.count(EPhptTestStatus.CRASH);
				td(style:'background:'+(cmp_crash>0?'#FF0000':cmp_crash<0?'#96DC28':'#CCFF66'), cmp_symbol(cmp_crash))
				//
				td(style:'background:#CCFF66', test_telem.count(EPhptTestStatus.XFAIL))
				td(style:'background:yellow', test_telem.count(EPhptTestStatus.SKIP))
				td(style:'background:yellow', test_telem.count(EPhptTestStatus.XSKIP))
				td(style:'background:#FFC000', test_telem.count(EPhptTestStatus.XFAIL_WORKS))
				td(style:'background:#FFC000', test_telem.count(EPhptTestStatus.BORK))
				td(style:'background:#F28020', test_telem.count(EPhptTestStatus.UNSUPPORTED))
				td(style:'background:#F28020', test_telem.count(EPhptTestStatus.TEST_EXCEPTION))
			}
			} // end os
			
			/* ----------------- begin footer ------------------ */
			tr {
				td(colspan: 2, 'Result-Pack')
				td(colspan: 2) {
					a(href:'http://131.107.220.66/PFTT-Results/'+base_telem.getBuildInfo().getBuildBranch(), 'Base') // TODO
				}
				td(colspan: 2) {
					a(href:'http://131.107.220.66/PFTT-Results/'+test_telem.getBuildInfo().getBuildBranch(), 'Test') // TODO
				}
				td(colspan: 9, '')
			}
			/* ----------------- end footer ------------------ */
			
		} // table

		p("* - tests skipped because they can't be run on that Operating System (ok)")
		p("** - tests missing required sections (test is not runnable)")
		p("*** - tests with unsupported sections (test is not runnable)")

		// all PHPTs marked as CRASH
		table(border:1, cellspacing:0, cellpadding:8, style:'background:#F2F2F2') {
			/* --------------- begin headers -------------- */
			tr {
				td(colspan:3) {
					b(test_build_branch_and_version)
					
					// check that the build and test pack versions all match
					if (
						base_telem.getTestPackBranch()==base_telem.getBuildInfo().getBuildBranch() &&
						test_telem.getTestPackBranch()==test_telem.getBuildInfo().getBuildBranch() &&
						(
							base_telem.getTestPackVersion()==base_telem.getBuildInfo().getVersionRevision()
							||base_telem.getBuildInfo().getVersionRevision().contains(base_telem.getTestPackVersion()) 
						) && (
							test_telem.getTestPackVersion()==test_telem.getBuildInfo().getVersionRevision()
							||test_telem.getBuildInfo().getVersionRevision().contains(test_telem.getTestPackVersion())
						)) {
						span("(using test-pack included with test build)")
					} else {
						// if not, make it clear which versions of which were used
						span("(using $test_test_pack_branch_and_version with $test_build_branch_and_version)")
					}
				}
			} // tr
			tr {
				td(colspan:3, test_scenario_set_title)
			}
			tr {
				td()
				td()
				td('Crashes (Test)')
			}
			/* --------------- end headers -------------- */
			os_names.each { os_name ->
			tr {
				td(row++)
				td(os_name)
				td() {
					if (!abbreviated || sw.getBuffer().length()<ABBREVIATED_MAX_LENGTH) {
						for ( String test_name : test_telem.getTestNames(EPhptTestStatus.CRASH)) {
							br(test_name)
						}
					}
				}
			}
			} // end os
		} // table
		
		br()

		// new PHPT failures
		table(border:1, cellspacing:0, cellpadding:8, style:'background:#F2F2F2') {
			/* --------------- begin headers -------------- */
			tr {
				td(colspan:3) {
					b(base_build_branch_and_version+" (Base)")
					span("with")
					b(test_build_branch_and_version+" (Test)")
					
					// check that the build and test pack versions all match
					if (
						base_telem.getTestPackBranch()==base_telem.getBuildInfo().getBuildBranch() &&
						test_telem.getTestPackBranch()==test_telem.getBuildInfo().getBuildBranch() &&
						(
							base_telem.getTestPackVersion()==base_telem.getBuildInfo().getVersionRevision()
							||base_telem.getBuildInfo().getVersionRevision().contains(base_telem.getTestPackVersion()) 
						) && (
							test_telem.getTestPackVersion()==test_telem.getBuildInfo().getVersionRevision()
							||test_telem.getBuildInfo().getVersionRevision().contains(test_telem.getTestPackVersion())
						)) {
						span("(using test-pack included with test build)")
					} else {
						// if not, make it clear which versions of which were used
						span("(using $test_test_pack_branch_and_version with $test_build_branch_and_version)")
					}
				}
			} // tr
			tr {
				td(colspan:3, test_scenario_set_title)
			}
			tr {				
				td(colspan:3, 'New Failures')
			}
			/* --------------- end headers -------------- */
			os_names.each { os_name ->
			tr {
				td(row++)
				td(os_name)
				td() {
					if (!abbreviated || sw.getBuffer().length()<ABBREVIATED_MAX_LENGTH) {
						for ( String test_name : PhptTestResult.listANotInB(test_telem.getTestNames(EPhptTestStatus.FAIL), base_telem.getTestNames(EPhptTestStatus.FAIL)) ) {
							br(test_name)
						}
					}
				}
			}
			} // end os
		}
		
		br()
		
		// all PHPTs marked as FAIL
		table(border:1, cellspacing:0, cellpadding:8, style:'background:#FFC000') {
			/* --------------- begin headers -------------- */
			tr {
				td(colspan:4) {
					b(base_build_branch_and_version+" (Base)")
					span("with")
					b(test_build_branch_and_version+" (Test)")
					
					// check that the build and test pack versions all match
					if (
						base_telem.getTestPackBranch()==base_telem.getBuildInfo().getBuildBranch() &&
						test_telem.getTestPackBranch()==test_telem.getBuildInfo().getBuildBranch() &&
						(
							base_telem.getTestPackVersion()==base_telem.getBuildInfo().getVersionRevision()
							||base_telem.getBuildInfo().getVersionRevision().contains(base_telem.getTestPackVersion()) 
						) && (
							test_telem.getTestPackVersion()==test_telem.getBuildInfo().getVersionRevision()
							||test_telem.getBuildInfo().getVersionRevision().contains(test_telem.getTestPackVersion())
						)) {
						span("(using test-packs included with each build)")
					} else {
						// if not, make it clear which versions of which were used
						span("(using $base_test_pack_branch_and_version with $base_build_branch_and_version and $test_test_pack_branch_and_version with $test_build_branch_and_version)")
					}
				}
			} // tr
			tr {
				td(colspan:4, scenario_set_title)
			}
			tr {
				td()
				td()
				td('Failures (Base)')
				td('Failures (Test)')
			}
			/* --------------- end headers -------------- */
			os_names.each { os_name ->
			tr {
				td(row++)
				td(os_name)
				td() {
					if (!abbreviated || sw.getBuffer().length()<ABBREVIATED_MAX_LENGTH) {
						for ( String test_name : base_telem.getTestNames(EPhptTestStatus.FAIL)) {
							br(test_name)
						}
					}
				}
				td() {
					if (!abbreviated || sw.getBuffer().length()<ABBREVIATED_MAX_LENGTH) {
						for ( String test_name : test_telem.getTestNames(EPhptTestStatus.FAIL)) {
							br(test_name)
						}
					}
				}
			}
			} // end os
		} // table
		
	} // body
} // html

	} // end void run
	
	protected static String cmp_symbol(int n) {
		return n >= 0 ? "+"+n : ""+ n;
	}

	@Override
	public String getHTMLString(ConsoleManager cm, boolean abbreviated) {
		run(cm, abbreviated);
		return sw.toString();
	}

} // end class FBCReportGen
