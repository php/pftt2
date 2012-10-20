package com.mostc.pftt.report

import java.io.StringWriter;

import org.apache.commons.collections.ListUtils;

import com.mostc.pftt.model.phpt.EPhptTestStatus;
import com.mostc.pftt.model.phpt.EBuildBranch;
import com.mostc.pftt.telemetry.PhptTestResult;
import com.mostc.pftt.telemetry.PhptTelemetry;

class FBCReportGen extends AbstractReportGen {
	final PhptTelemetry base_telem, test_telem;
	int row = 1;
	StringWriter sw;
	
	public FBCReportGen(PhptTelemetry base_telem, PhptTelemetry test_telem) {
		this.base_telem = base_telem;
		this.test_telem = test_telem;
		
		sw = new StringWriter();
	}
	
	private static String bav(EBuildBranch branch, String version) {
		if (version==null)
			return 'unknown';
		else if (!version.startsWith('r'))
			version = 'r' + version;
		return branch+' '+version;
	} 

	@Override
	public void run() {
		def sapi_title = base_telem.getSAPIScenarioName()
		if (sapi_title!=test_telem.getSAPIScenarioName()) {
			// for some reason, comparing runs of 2 different SAPIs... make that clear in report.
			sapi_title = sapi_title+' (Base) ' + test_telem.getSAPIScenarioName()+' (Test)'
		}
		String base_build_branch_and_version = bav(base_telem.getBuildBranch(), base_telem.getBuildVersion())
		String base_test_pack_branch_and_version = bav(base_telem.getTestPackBranch(), base_telem.getTestPackVersion())
		String test_build_branch_and_version = bav(test_telem.getBuildBranch(), test_telem.getBuildVersion())
		String test_test_pack_branch_and_version = bav(test_telem.getTestPackBranch(), test_telem.getTestPackVersion())
		
def html = new groovy.xml.MarkupBuilder(sw)
html.html {
	body {
		h1("Functional Build Comparison(FBC) Report")
		p {
			span {
				b(base_build_branch_and_version+" (Base)")
				span("with")
				b(test_build_branch_and_version+" (Test)")
				
				// check that the build and test pack versions all match
				if (test_test_pack_branch_and_version==test_build_branch_and_version && base_test_pack_branch_and_version==base_build_branch_and_version) {
					span("(using test-packs included with each build)")
				} else {
					// if not, make it clear which versions of which were used
					span("(using ${base_test_pack_branch_and_version} with {$base_build_branch_and_version} and ${test_test_pack_branch_and_version} with {$test_build_branch_and_version})")
				}
			} //span
		} // p
		
		if (test_telem.getOSName() != base_telem.getOSName()) {
			// for some reason, comparing runs from two different OSes, make it clear in report.
			p("Warning: Base OS: ${base_telem.getOSName()} Test OS: ${test_telem.getOSName()}")
		}

		table(border: 0, cellspacing:0, cellpadding:0) {
			/* --------------- begin headers -------------- */
			tr {
				td {
					p(sapi_title)
				}
			}
			tr {
				td(colspan:2) { p(align: 'center', 'OS') }
				td(colspan:6, style:'background:#CCFF66') {}
				td(colspan:4, style:'background:yellow') {
					p(align:'center', 'Skip')
				}
				td(colspan:4, style:'background:#FFC000') {
					p(align:'center', 'Test Bug')
				}
				td(colspan:2, style:'background:#F28020') {
					p(align:'center', 'PFTT Bug')
				}
				td(colspan:2) {
					p(align:'center', 'Smoke Checks')
				}
			} // tr
			tr {
				td(colspan:2) {}
				td(style:'background:#CCFF66') {
					p('Pass Rate(%)')
				}
				td(style:'background:#CCFF66') {
					p('Pass')
				}
				td(colspan:2, style:'background:#CCFF66') {
					p('Fail')
				}
				td(colspan:2, style:'background:#CCFF66') {
					p('XFail')
				}
				td(colspan:2, style:'background:yellow') {
					p('Skip')
				}
				td(colspan:2, style:'background:yellow') {
					p('XSkip*')
				}
				td(colspan:2, style:'background:#FFC000') {
					p('XFail (Work)')
				}
				td(colspan:2, style:'background:#FFC000') {
					p('Bork**')
				}
				td(colspan:2, style:'background:#F28020') {
					p('Unsupported***')
				}
				td() {
					p(align:'center', 'Required Extensions')
				}
				td() {
					p(align:'center', 'Required Features')
				}
			} // tr
			/* --------------- end headers -------------- */
			
			tr {
				td(row++)
				td(test_telem.getOSName())
				td(test_telem.passRate())
				td(test_telem.count(EPhptTestStatus.PASS))
				td(add(test_telem.count(EPhptTestStatus.PASS) - base_telem.count(EPhptTestStatus.PASS)))
				td(test_telem.count(EPhptTestStatus.FAIL))
				td(add(test_telem.count(EPhptTestStatus.FAIL) - base_telem.count(EPhptTestStatus.FAIL)))
				td(test_telem.count(EPhptTestStatus.SKIP))
				td(add(test_telem.count(EPhptTestStatus.SKIP) - base_telem.count(EPhptTestStatus.SKIP)))
				td(test_telem.count(EPhptTestStatus.XSKIP))
				td(add(test_telem.count(EPhptTestStatus.XSKIP) - base_telem.count(EPhptTestStatus.XSKIP)))
				td(test_telem.count(EPhptTestStatus.XFAIL_WORKS))
				td(add(test_telem.count(EPhptTestStatus.XFAIL_WORKS) - base_telem.count(EPhptTestStatus.XFAIL_WORKS)))
				td(test_telem.count(EPhptTestStatus.BORK))
				td(add(test_telem.count(EPhptTestStatus.BORK) - base_telem.count(EPhptTestStatus.BORK)))
				td(test_telem.count(EPhptTestStatus.UNSUPPORTED))
				td(add(test_telem.count(EPhptTestStatus.UNSUPPORTED) - base_telem.count(EPhptTestStatus.UNSUPPORTED)))
				td('Pass')
				td('Pass')
			}

			
			/* ----------------- begin footer ------------------ */
			tr {
				td('Telemetry')
				td() {
					a(href:'http://131.107.220.66/PFTT-Results/PHPT/PHP_5_4/', 'Base')
				}
				td() {
					a(href:'http://131.107.220.66/PFTT-Results/PHPT/PHP_5_4/', 'Test')
				}
			}
			/* TODO tr {
				td('Scenarios:')
				test_telem.scenario_set
			} // tr*/
			/* ----------------- end footer ------------------ */
			
		} // table

		p("* - tests skipped because they can't be run on that Operating System (ok)")
		p("** - tests missing required sections (test is not runnable)")
		p("*** - tests with unsupported sections (test is not runnable)")


		// smoke test failures
		table(border:0, cellspacing:0, cellpadding:0, style:'background:#F2F2F2') {
			/* ---------------- begin header ------------------- */
			tr {
				td(colspan:4) {
					p(style:'text-align:center', sapi_title)
				}
			}
			tr {
				td(colspan:2, '')
				td('Required Extensions')
				td('Required Features')
			}
			/* ---------------- end header ------------------- */
			
			tr {
				td(row++)
				td('OS')
				td('Yes')
				td('Yes')
			} // tr
		} // table

		// new PHPT failures
		table(border:0, cellspacing:0, cellpadding:0, style:'background:#F2F2F2') {
			/* --------------- begin headers -------------- */
			tr {				
				td(colspan:3, 'New Failures')
			}
			/* --------------- end headers -------------- */
			tr {
				td(row++)
				td(test_telem.getOSName())
				td() {
					for ( String test_name : PhptTestResult.listANotInB(test_telem.getTestNames(EPhptTestStatus.FAIL), base_telem.getTestNames(EPhptTestStatus.FAIL)) ) {
						br(test_name)
					}
				}
			}
		}
		
		// all PHPT failures
		table(border:0, cellspacing:0, cellpadding:0, style:'background:#FFC000') {
			/* --------------- begin headers -------------- */
			tr {
				td(colspan:4, sapi_title)
			}
			tr {
				td(colspan:2, 'Failures (Base)')
				td(colspan:2, 'Failures (Test)')
			}
			/* --------------- end headers -------------- */
			tr {
				td(row++)
				td(test_telem.getOSName())
				td() {
					for ( String test_name : base_telem.getTestNames(EPhptTestStatus.FAIL)) {
						br(test_name)
					}
				}
				td() {
					for ( String test_name : test_telem.getTestNames(EPhptTestStatus.FAIL)) {
						br(test_name)
					}
				}
			}
		} // table
		
	} // body
} // html

	} // end void run
	
	protected static String add(int n) {
		return n >= 0 ? "+"+n : "-" + n;
	}

	@Override
	public String getHTMLString() {
		run();
		return sw.toString();
	}

} // end class FBCReportGen
