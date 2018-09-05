package com.mostc.pftt.main

import java.io.StringWriter;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.ui.EUITestStatus;
import com.mostc.pftt.results.AbstractReportGen;
import com.mostc.pftt.results.AbstractUITestRW;
import com.mostc.pftt.results.ConsoleManager;

class UITestReportGen extends AbstractReportGen {
	protected AbstractUITestRW base_telem, test_telem;
	
	public UITestReportGen(AbstractUITestRW base_telem, AbstractUITestRW test_telem) {
		this.base_telem = base_telem;
		this.test_telem = test_telem;
	}
	
	@Override
	public void run(ConsoleManager cm, boolean abbreviated, BuilderSupport html) {
		EUITestStatus base_status, test_status;
		def test_html_url;
		
		// make it easier to see the important test results (fails, warnings) - put them at the top of the report:
		// sort by status FAIL* PASS* TE SKIP NI
		def test_names = test_telem.getTestNames();
		/*test_names.sort(true) { a, b ->
				test_telem.getTestStatus(a).compareTo(test_telem.getTestStatus(b))
			}*/
		//
		
		def os_names = abbreviated?['Localhost']:['Win 2008r2 SP0 x64', 'Win 2008r2 SP1 x64', 'Win 2012 SP0 x64', 'Win 2008 SP1 x64', 'Win 2008 SP1 x86', 'Win 2008 SP2 x64', 'Win 2008 SP2 x86', 'Win 7 SP0 x64', 'Win 7 SP0 x86', 'Win 7 SP1 x64', 'Win 7 SP1 x86', 'Win 8 SP0 x64', 'Win Vista SP2 x64', 'Win Vista SP2 x86'];
		
		sw = new StringWriter()
html.html {
	body {
		h1('PFTT UI Report')
		
		def build_title, test_pack_title, scenario_set_title;
		if (base_telem.getBuildInfo().equals(test_telem.getBuildInfo()))
			build_title = test_telem.getBuildInfo().toString()
		else
			build_title =  "${base_telem.getBuildInfo().toString()} with ${test_telem.getBuildInfo().toString()}"
			
		if (base_telem.getTestPackNameAndVersionString().equals(test_telem.getTestPackNameAndVersionString()) && base_telem.getWebBrowserNameAndVersion().equals(test_telem.getWebBrowserNameAndVersion()))
			test_pack_title = "${test_telem.getTestPackNameAndVersionString()} with ${test_telem.getWebBrowserNameAndVersion()}"
		else
			test_pack_title = "${base_telem.getTestPackNameAndVersionString()} with ${base_telem.getWebBrowserNameAndVersion()} with ${test_telem.getTestPackNameAndVersionString()} with ${test_telem.getWebBrowserNameAndVersion()}"
		scenario_set_title = test_telem.getScenarioSetNameWithVersionInfo()
		if (scenario_set_title!=base_telem.getScenarioSetNameWithVersionInfo()) {
			// for some reason, comparing runs of 2 different SAPIs... make that clear in report.
			scenario_set_title = base_telem.getScenarioSetNameWithVersionInfo()+' (Base) with ' + test_telem.getScenarioSetNameWithVersionInfo()+' (Test)'
		}
		
		table('border':"1", 'cellspacing':"0", 'cellpadding':"8") {
			tr {
				td('colspan': 2) {
					b(build_title)
				}
			}
			tr {
				td('colspan': 2, test_pack_title)
			}
			tr {
				td('colspan': 2, scenario_set_title)
			}
			
			tr {
				td()
				td('Win2008r2sp1')
			}
		
			tr {
				td('Pass%') {
					test_telem.passRate()
				}
			}
			
			tr {
				td('Pass') {
					test_telem.count(EUITestStatus.PASS) + test_telem.count(EUITestStatus.PASS_WITH_WARNING)
				}
			}
			
			for ( String test_name : test_names ) {
				base_status = base_telem.getTestStatus(test_name)
				test_status = test_telem.getTestStatus(test_name)
				test_html_url = test_telem.getHTMLURL(test_name)
				
				tr {
					td(test_name)
					def style = styleForTestStatus(test_status)
					
					if (base_status==test_status) {
						td('style': style) {
							test_html_url?
								a(href: test_html_url, test_status):
								test_status
						}
					} else if (base_status==EUITestStatus.PASS) {
						td('style': style) {
							test_html_url?
								a(href: test_html_url, '-'+test_status):
								'-'+test_status
						}
					} else if (test_status==EUITestStatus.PASS) {
						td('style': style) {
							test_html_url?
								a(href: test_html_url, '+'+test_status):
								'+'+test_status
						}
					} else {
						td('style': style) {
							test_html_url?
								a(href: test_html_url, test_status):
								test_status
						}
					}
				} // end tr
			} // end for

			tr {
				td('Not Implemented') {
					test_telem.count(EUITestStatus.NOT_IMPLEMENTED)
				}
			}
			tr {
				td('Skip') {
					test_telem.count(EUITestStatus.SKIP)
				}
			}
		
		/* ----------------- begin footer ------------------ */
			tr {
				td('Result-Pack')
				td() {
					a(href:'http://131.107.220.66/PFTT-Results/'+base_telem.getBuildInfo().getBuildBranch(), 'Base') // TODO
					'&nbps;'
					a(href:'http://131.107.220.66/PFTT-Results/'+test_telem.getBuildInfo().getBuildBranch(), 'Test') // TODO
				}
			}
		} // end table
		/* ----------------- end footer ------------------ */
		
		p('* - FAIL_WITH_WARNING and PASS_WITH_WARNING indicate a PHP warning or error message or HTTP error was found in the HTTP response during the test')
		
		h2('Test-Pack Notes')
		String base_notes = base_telem.getNotes();
		String test_notes = test_telem.getNotes();
		
		// include notes from test-pack
		String test_pack_notes = base_notes==null||base_notes==test_notes?test_notes==null?'(No Notes Found)':test_notes:test_notes==null?base_notes:base_notes+"\n"+test_notes;
		for ( String line : StringUtil.splitLines(test_pack_notes))
			b(line)
		
		h2('Test Description/Comments')
		// list comments for each test-case that has a comment
		table('border':"1", 'cellspacing':"0", 'cellpadding':"8") {
			tr {
				td('colspan': 2) {
					b(build_title)
				}
			}
			tr {
				td('colspan': 2, test_pack_title)
			}
			tr {
				td('colspan': 2, scenario_set_title)
			}
			
			for ( String test_name : test_names ) {
				def comment = test_telem.getComment(test_name)
				
				if (!comment)
					continue;
					
				tr {
					td(test_name)
					td(comment)
				} // tr
			} // end for
		} // table
		
		if (!abbreviated) {
			// show screenshots here
			h2('Screenshots')
			def file_name;
			table('border':"1", 'cellspacing':"0", 'cellpadding':"8") {
				tr {
					td('colspan': 2) {
						b(build_title)
					}
				}
				tr {
					td('colspan': 2, test_pack_title)
				}
				tr {
					td('colspan': 2, scenario_set_title)
				}
				
				for ( String test_name : test_names ) {
					file_name = test_telem.getScreenshotFilename(test_name)
					test_status = test_telem.getTestStatus(test_name)
					tr {
						td(test_name)
						td('style': styleForTestStatus(test_status)) {
							img(src: 'file://'+AHost.toUnixPath(file_name), height: 640)
						}
					}
				}
			} // table
		}
		
	} // end body
} // end html
	} // end void run
	
	def styleForTestStatus(def test_status) {
		switch(test_status) {
		case EUITestStatus.FAIL:
		case EUITestStatus.FAIL_WITH_WARNING:
		case EUITestStatus.CRASH:
			return 'background:#ff0000';
		case EUITestStatus.PASS_WITH_WARNING:
			return 'background:yellow'
		case EUITestStatus.PASS:
			return 'background:#ccff66'
		default:
			return 'background:#ffc000'
		} // end switch
	}
	
} // end class UITestReportGen
