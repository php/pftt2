package com.mostc.pftt.results

import groovy.xml.MarkupBuilder;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.model.app.EPhpUnitTestStatus;
import com.mostc.pftt.model.core.EBuildBranch;

class PhpUnitReportGen extends AbstractReportGen {
	StringWriter sw;
	AbstractPhpUnitRW base_telem, test_telem;
	int row;

	public PhpUnitReportGen(AbstractPhpUnitRW base_telem, AbstractPhpUnitRW test_telem) {
		this.base_telem = base_telem;
		this.test_telem = test_telem;
	}
	
	@Override
	void run(ConsoleManager cm, boolean abbreviated) {
		def scenario_set_title = test_telem.getScenarioSetNameWithVersionInfo()
		def test_scenario_set_title = scenario_set_title + ' (Test)'
		if (scenario_set_title!=base_telem.getScenarioSetNameWithVersionInfo()) {
			// for some reason, comparing runs of 2 different SAPIs... make that clear in report.
			scenario_set_title = base_telem.getScenarioSetNameWithVersionInfo()+' (Base) with ' + test_telem.getScenarioSetNameWithVersionInfo()+' (Test)'
		}
		String base_build_branch_and_version = base_telem.getBuildInfo().toString();
		String test_build_branch_and_version = test_telem.getBuildInfo().toString();

		def os_names = abbreviated?['Localhost']:base_telem.getBuildInfo().isX64()?['Win 2008r2 SP0 x64', 'Win 2008r2 SP1 x64', 'Win 2012 SP0 x64', 'Win 2008 SP1 x64', 'Win 2008 SP2 x64', 'Win 7 SP0 x64', 'Win 7 SP1 x64', 'Win 8 SP0 x64', 'Win Vista SP2 x64']:['Win 2008r2 SP0 x64', 'Win 2008r2 SP1 x64', 'Win 2012 SP0 x64', 'Win 2008 SP1 x64', 'Win 2008 SP1 x86', 'Win 2008 SP2 x64', 'Win 2008 SP2 x86', 'Win 7 SP0 x64', 'Win 7 SP0 x86', 'Win 7 SP1 x64', 'Win 7 SP1 x86', 'Win 8 SP0 x64', 'Win Vista SP2 x64', 'Win Vista SP2 x86'];
		
		row = 1;
		sw = new StringWriter()
		def html = new groovy.xml.MarkupBuilder(sw)
		html.html {
			body {
				h1("PFTT PhpUnit Report")
				
				// warnings
				if (cm.isSkipSmokeTests()) {
					p('Warning: Smoke tests were not run on build. Smoke tests were disabled with -skip_smoke_tests console option.')
				}
				if (!cm.isThreadSafety()) {
					p('Warning: Some errors or failures may be due to race conditions. Thread safety was disabled with -no_thread_safety console option.')
				}
				if (test_telem.getOSName() != base_telem.getOSName()) {
					// for some reason, comparing runs from two different OSes, make it clear in report.
					p("Warning: Base OS: ${base_telem.getOSName()} Test OS: ${test_telem.getOSName()}")
				}
				//
				
				p {
					span(style: 'font-size:14.0pt;line-height:115%', 'Summary')
				}

				table(border: 1, cellspacing: 0, cellpadding: 0, style: 'border-collapse:collapse;border:none;') {
					tr {
						td(colspan:11) {
							b(base_build_branch_and_version)
							span("using")
							b(base_telem.getTestPackNameAndVersionString()+" (Base)")
							span("with")
							b(test_build_branch_and_version)
							span("using")
							b(test_telem.getTestPackNameAndVersionString()+" (Test)")
						} // td
					} // tr
					tr {
						td(colspan:11, scenario_set_title)
					}
					tr() {
						td() // row number
						td(valign:'top', style: 'border:solid windowtext 1.0pt; background:#A9CAED;padding:0in 5.4pt 0in 5.4pt',
								"OS"
								)
						td(valign: 'top', style: 'border:solid windowtext 1.0pt; border-left:none;background:#A9CAED;padding:0in 5.4pt 0in 5.4pt',
								"Tests"
								)
						td(colspan:1, valign: 'top', style: 'border:solid windowtext 1.0pt; border-left:none;background:#A9CAED;padding:0in 5.4pt 0in 5.4pt',
							"Pass"
							)
						td(colspan:2, valign: 'top', style: 'border:solid windowtext 1.0pt; border-left:none;background:#A9CAED;padding:0in 5.4pt 0in 5.4pt',
							"Errors"
							)
						td(colspan: 2, valign: 'top', style: 'border:solid windowtext 1.0pt; border-left:none;background:#A9CAED;padding:0in 5.4pt 0in 5.4pt',
								"Failures"
								)
						td(colspan: 2, valign: 'top', style: 'border:solid windowtext 1.0pt; border-left:none;background:#A9CAED;padding:0in 5.4pt 0in 5.4pt',
							"Crashes"
							)
						td(colspan: 1, valign: 'top', style: 'border:solid windowtext 1.0pt; border-left:none;background:#A9CAED;padding:0in 5.4pt 0in 5.4pt',
								"Success Rate"
								)
					}
					
					os_names.each { os_name ->
					tr(style: 'height:26.05pt') {
						td(row++, style: 'border:solid windowtext 1.0pt; border-top:none; background:#ECEEE1;padding:0in 5.4pt 0in 5.4pt;height:26.05pt')
						td(valign: 'top', style: 'border:solid windowtext 1.0pt; border-top:none; background:#ECEEE1;padding:0in 5.4pt 0in 5.4pt;height:26.05pt',
								os_name
								)
						td(valign: "top", style: 'border-top:none;border-left:none; border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;  background:#ECEEE1;padding:0in 5.4pt 0in 5.4pt; height:26.05pt',
								test_telem.getTestCount()
								)
						td(valign: 'top', style: 'border-top:none;border-left:none; border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;  background:#ECEEE1;padding:0in 5.4pt 0in 5.4pt; height:26.05pt',
							test_telem.count(EPhpUnitTestStatus.PASS)
							)
						int error = test_telem.count(EPhpUnitTestStatus.ERROR);
						int cmp_error = error - base_telem.count(EPhpUnitTestStatus.ERROR);
						td(valign: 'top', style: 'border-top:none;border-left:none; border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;  background:#ECEEE1;padding:0in 5.4pt 0in 5.4pt; height:26.05pt',
								error
							)
						td(valign: 'top', style: 'background:'+(cmp_error>0?'#FF0000':cmp_error<0?'#96DC28':'#ECEEE1')+'; border-top:none;border-left:none; border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;  padding:0in 5.4pt 0in 5.4pt; height:26.05pt',
								cmp_error > 0 ? "+" + cmp_error : ""+cmp_error
							)
						int failure = test_telem.count(EPhpUnitTestStatus.FAILURE);
						int cmp_failure = failure - base_telem.count(EPhpUnitTestStatus.FAILURE);
						td(valign: 'top', style: 'border-top:none;border-left:none; border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;  background:#ECEEE1;padding:0in 5.4pt 0in 5.4pt; height:26.05pt',
								failure
							)
						td(valign: 'top', style: 'background:'+(cmp_failure>0?'#FF0000':cmp_failure<0?'#96DC28':'#ECEEE1')+'; border-top:none;border-left:none; border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt; padding:0in 5.4pt 0in 5.4pt; height:26.05pt',
								cmp_failure > 0 ? "+" + cmp_failure : ""+cmp_failure
							)
						int crash = test_telem.count(EPhpUnitTestStatus.CRASH);
						int cmp_crash = crash - base_telem.count(EPhpUnitTestStatus.CRASH);
						td(valign: 'top', style: 'background:'+(crash>0?'#ff0000':'#ECEEE1')+'; border-top:none;border-left:none; border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt; height:26.05pt',
								crash
							)
						td(valign: 'top', style: 'background:'+(cmp_crash>0?'#FF0000':cmp_crash<0?'#96DC28':'#ECEEE1')+'; border-top:none;border-left:none; border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt; padding:0in 5.4pt 0in 5.4pt; height:26.05pt',
								cmp_crash > 0 ? "+" + cmp_crash : ""+cmp_crash
							)
						td(valign: 'top', style: 'border-top:none;border-left: none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;  background:#ECEEE1;padding:0in 5.4pt 0in 5.4pt; height:26.05pt',
								PhpResultPack.round1(test_telem.passRate())
								)
					}
					
					} // os
				} // end table
				
				/* ----------------- begin footer ------------------ */
				table {
					tr {
						td('Result-Pack')
						td(colspan: 2) {
							a(href:'http://131.107.220.66/PFTT-Results/', 'Base') // TODO
						}
						td(colspan: 2) {
							a(href:'http://131.107.220.66/PFTT-Results/', 'Test') // TODO
						}
						td(colspan: 7, '')
					}
				}
				/* ----------------- end footer ------------------ */
				
				p {
					span(style: 'font-size:14.0pt;line-height:115%',
							"Php INI for this Report"
							)
				}
				
				table(border: 1, cellspacing: 0, cellpadding: 0, style: 'border-collapse:collapse;border:solid windowtext 1.0pt; background:#ECEEE1;padding:0in 5.4pt 0in 5.4pt') {
					tr {
						td(colspan:4) {
							b(base_build_branch_and_version)
							span("using")
							b(base_telem.getTestPackNameAndVersionString()+" (Base)")
							span("with")
							b(test_build_branch_and_version)
							span("using")
							b(test_telem.getTestPackNameAndVersionString()+" (Test)")
						} // td
					} // tr
					tr {
						td(colspan:4, scenario_set_title)
					}
					tr {
						td()
						td()
						td('Base')
						td('Test')
					}
					tr {
						td(row++)
						td('Windows (All)')
						td(){nl2Br(html, base_telem.getPhpIni().toString())}
						td(){nl2Br(html, test_telem.getPhpIni().toString())}
					}
				} // end table

				p {
					span(style: 'font-size:14.0pt;line-height:115%',
							"NEW Errors and Crashes and Failures"
							)
				}
				
				table(border: 1, cellspacing: 0, cellpadding: 0, style: 'border-collapse:collapse;border:none;') {
					tr {
						td(colspan:3) {
							b(test_build_branch_and_version)
							span("using")
							b(test_telem.getTestPackNameAndVersionString()+" (Test)")
						} // td
					} // tr
					tr {
						td(colspan:3, test_scenario_set_title)
					}
					
					os_names.each { os_name ->
						tr() {
							td(row++, style: 'border:solid windowtext 1.0pt; background:#ECEEE1;padding:0in 5.4pt 0in 5.4pt')
							td(valign: 'top', style: 'border:solid windowtext 1.0pt; background:#ECEEE1;padding:0in 5.4pt 0in 5.4pt',
									os_name
									)
							td(valign: 'top', style: 'border:solid windowtext 1.0pt; background:#ECEEE1;padding:0in 5.4pt 0in 5.4pt') {
								if (!abbreviated || sw.getBuffer().length()<ABBREVIATED_MAX_LENGTH) {
									PhptTestResult.listANotInB(test_telem.getTestNames(EPhpUnitTestStatus.CRASH), base_telem.getTestNames(EPhpUnitTestStatus.CRASH)).each { test_name ->
										p(test_name)
										String a = test_telem.getFailureOutput(test_name);
										if (a!=null) {
											if (a.length()>150)
												a = a.substring(0,150);
											p { b(a) }
										}
									}
								}
								if (!abbreviated || sw.getBuffer().length()<ABBREVIATED_MAX_LENGTH) {
									PhptTestResult.listANotInB(test_telem.getTestNames(EPhpUnitTestStatus.ERROR), base_telem.getTestNames(EPhpUnitTestStatus.ERROR)).each { test_name ->
										p(test_name)
										String a = test_telem.getFailureOutput(test_name);
										if (a!=null) {
											if (a.length()>150)
												a = a.substring(0,150);
											p { b(a) }
										}
									}
								}
								if (!abbreviated || sw.getBuffer().length()<ABBREVIATED_MAX_LENGTH) {
									PhptTestResult.listANotInB(test_telem.getTestNames(EPhpUnitTestStatus.FAILURE), base_telem.getTestNames(EPhpUnitTestStatus.FAILURE)).each { test_name ->
										p(test_name)
										String a = test_telem.getFailureOutput(test_name);
										if (a!=null) {
											if (a.length()>150)
												a = a.substring(0,150);
											p { b(a) }
										}
									}
								}
							}
						}
					} // end os
				} // end table

				p {
					span(style: 'font-size:14.0pt;line-height:115%',
							"List of Errors and Crashes and Failures"
							)
				}

				table(border: 1, cellspacing: 0, cellpadding: 0, style: 'border-collapse:collapse;border:none;') {
					tr {
						td(colspan:4) {
							b(base_build_branch_and_version)
							span("using")
							b(base_telem.getTestPackNameAndVersionString()+" (Base)")
							span("with")
							b(test_build_branch_and_version)
							span("using")
							b(test_telem.getTestPackNameAndVersionString()+" (Test)")
						} // td
					} // tr
					tr {
						td(colspan:4, scenario_set_title)
					}
					tr() {
						td() // row
						td(valign: 'top', style: 'border:solid windowtext 1.0pt; background:#A9CAED;padding:0in 5.4pt 0in 5.4pt')
						td(valign: 'top', style: 'border:solid windowtext 1.0pt; border-left:none;background:#A9CAED;padding:0in 5.4pt 0in 5.4pt',
								"Base"
								)
						td(valign: 'top', style: 'border:solid windowtext 1.0pt; border-left:none;background:#A9CAED;padding:0in 5.4pt 0in 5.4pt',
								"Test"
								)
					}

					os_names.each { os_name ->
					tr() {
						td(row++, style: 'border:solid windowtext 1.0pt; border-top:none; background:#ECEEE1;padding:0in 5.4pt 0in 5.4pt')
						td(valign: 'top', style: 'border:solid windowtext 1.0pt; border-top:none; background:#ECEEE1;padding:0in 5.4pt 0in 5.4pt',
								os_name
								)
						td(valign: 'top', style: 'border:solid windowtext 1.0pt; border-top:none; background:#ECEEE1;padding:0in 5.4pt 0in 5.4pt') {
							// don't let the length get too long (for email message, etc...)
							if (!abbreviated || sw.getBuffer().length()<ABBREVIATED_MAX_LENGTH) {
								base_telem.getTestNames(EPhpUnitTestStatus.CRASH).each { test_name ->
									p(test_name)
								}
							}
							if (!abbreviated || sw.getBuffer().length()<ABBREVIATED_MAX_LENGTH) { 
								base_telem.getTestNames(EPhpUnitTestStatus.ERROR).each { test_name ->
									p(test_name)
								}
							}
							if (!abbreviated || sw.getBuffer().length()<ABBREVIATED_MAX_LENGTH) { 
								base_telem.getTestNames(EPhpUnitTestStatus.FAILURE).each { test_name ->
									p(test_name)
								}
							}
						}
						td(valign: 'top', style: 'border:solid windowtext 1.0pt; border-top:none; background:#ECEEE1;padding:0in 5.4pt 0in 5.4pt') {
							if (!abbreviated || sw.getBuffer().length()<ABBREVIATED_MAX_LENGTH) {
								test_telem.getTestNames(EPhpUnitTestStatus.CRASH).each { test_name ->
									p(test_name)
								}
							}
							if (!abbreviated || sw.getBuffer().length()<ABBREVIATED_MAX_LENGTH) {
								test_telem.getTestNames(EPhpUnitTestStatus.ERROR).each { test_name ->
									p(test_name)
								}
							}
							if (!abbreviated || sw.getBuffer().length()<ABBREVIATED_MAX_LENGTH) {
								test_telem.getTestNames(EPhpUnitTestStatus.FAILURE).each { test_name ->
									p(test_name)
								}
							}
						}
					}
					} // end os
				} // end table

			} // end body
		} // end html
	} // end void run
	
	static void nl2Br(MarkupBuilder html, String str) {
		for ( String line : StringUtil.splitLines(str) ) {
			html.invokeMethod("br", line);
		}
	}

	@Override
	public String getHTMLString(ConsoleManager cm, boolean abbreviated) {
		run(cm, abbreviated);
		return sw.toString();
	}

} // end class AUTReportGen
