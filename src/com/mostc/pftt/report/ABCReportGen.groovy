package com.mostc.pftt.report

class ABCReportGen extends AbstractReportGen {
	final String os_name;
	final File tmp_file;
	
	public ABCReportGen(File tmp_file, String os_name) {
		this.tmp_file = tmp_file;
		this.os_name = os_name;
	}

	@Override
	void run() {
				
		def oses = [os_name]
		
		def root = new XmlSlurper().parse(tmp_file);
		def testcases = root.depthFirst().findAll { it.name() == 'testcase' }
		
		def sw = new StringWriter()
		def html = new groovy.xml.MarkupBuilder(sw)	
		html.html {
			body {
				p {
					span(style: 'font-size:14.0pt;line-height:115%') {
						span("AUT Report")
						b("Joomla Platform")
					}
				}
	
				p {
					b("Joomla Platform")
					span(" PHPUnit Test-Pack comparing ")
					b("PHP_5_4-r328a3d9")
					span(" with ")
					b("PHP_5_4-r8cdd6bc")
					span(" (using ")
					b("Joomla Platform")
					span(" PHPUnit Test-Pack from ")
					b("Joomla_Platform-12.1")
					span(")")
				}
	
				p {
					span(style: 'font-size:14.0pt;line-height:115%', 'Summary')
				}
	
				table(border: 1, cellspacing: 0, cellpadding: 0, style: 'border-collapse:collapse;border:none;') {
					tr() {
						td(valign:'top', style: 'width:1.45in;border:solid windowtext 1.0pt; background:#A9CAED;padding:0in 5.4pt 0in 5.4pt',
							"OS"
						)
						td(valign: 'top', style: 'width:49.5pt;border:solid windowtext 1.0pt; border-left:none;background:#A9CAED;padding:0in 5.4pt 0in 5.4pt',
							"Tests"
						)
						td(valign: 'top', style: 'width:49.5pt;border:solid windowtext 1.0pt; border-left:none;background:#A9CAED;padding:0in 5.4pt 0in 5.4pt')
						td(valign: 'top', style: 'width:49.5pt;border:solid windowtext 1.0pt; border-left:none;background:#A9CAED;padding:0in 5.4pt 0in 5.4pt',
							"Failures"
						)
						td(valign: 'top', style: 'width:49.5pt;border:solid windowtext 1.0pt; border-left:none;background:#A9CAED;padding:0in 5.4pt 0in 5.4pt')
						td(valign: 'top', style: 'width:49.5pt;border:solid windowtext 1.0pt; border-left:none;background:#A9CAED;padding:0in 5.4pt 0in 5.4pt',
							"Errors"
						)
						td(valign: 'top', style: 'width:49.5pt;border:solid windowtext 1.0pt; border-left:none;background:#A9CAED;padding:0in 5.4pt 0in 5.4pt')
						td(valign: 'top', style: 'width:49.5pt;border:solid windowtext 1.0pt; border-left:none;background:#A9CAED;padding:0in 5.4pt 0in 5.4pt',
							"Success Rate"
						)
						td(valign: 'top', style: 'width:49.5pt;border:solid windowtext 1.0pt; border-left:none;background:#A9CAED;padding:0in 5.4pt 0in 5.4pt')
						td(valign: 'top', style: 'width:49.5pt;border:solid windowtext 1.0pt; border-left:none;background:#A9CAED;padding:0in 5.4pt 0in 5.4pt',
							"Time"
						)
					}
					
					oses.each{ os_name ->
					tr(style: 'height:26.05pt') {
						td(valign: 'top', style: 'width:1.45in;border:solid windowtext 1.0pt; border-top:none; background:#ECEEE1;padding:0in 5.4pt 0in 5.4pt;height:26.05pt',
							os_name
						)
						td(valign: "top", style: 'width:49.5pt;border-top:none;border-left:none; border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;  background:#ECEEE1;padding:0in 5.4pt 0in 5.4pt; height:26.05pt',
							testcases.size()
						)
						td(valign: 'top', style: 'width:27.0pt;border-top:none;border-left:none; border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;  background:#ECEEE1;padding:0in 5.4pt 0in 5.4pt; height:26.05pt',
							"+0"
						)
						td(valign: 'top', style: 'width:.75in;border-top:none;border-left:none; border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;  background:#ECEEE1;padding:0in 5.4pt 0in 5.4pt; height:26.05pt',
							"34"
						)
						td(valign: 'top', style: 'width:22.5pt;border-top:none;border-left:none; border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;  background:#ECEEE1;padding:0in 5.4pt 0in 5.4pt; height:26.05pt',
							"+0"
						)
						td(valign: 'top', style: 'width:45.0pt;border-top:none;border-left:none; border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;  background:#ECEEE1;padding:0in 5.4pt 0in 5.4pt; height:26.05pt',
							"158"
						)
						td(valign: 'top', style: 'width:22.5pt;border-top:none;border-left:none; border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;  background:#ECEEE1;padding:0in 5.4pt 0in 5.4pt; height:26.05pt',
							"+0"
						)
						td(valign: 'top', style: 'width:76.5pt;border-top:none;border-left: none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;  background:#ECEEE1;padding:0in 5.4pt 0in 5.4pt; height:26.05pt',
							"92%"
						)
						td(valign: 'top', style: 'width:22.5pt;border-top:none;border-left:none; border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;  background:#ECEEE1;padding:0in 5.4pt 0in 5.4pt; height:26.05pt',
							"+0"
						)
						td(valign: 'top', style: 'width:45.9pt;border-top:none;border-left:none; border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;  background:#ECEEE1;padding:0in 5.4pt 0in 5.4pt; height:26.05pt',
							"0:08:00"
						)
					}	
					}
				} // end table
	 
				p {
				   span(style: 'font-size:14.0pt;line-height:115%',
				   		"New Failures and Errors"
				   )
				}
	
				table(border: 1, cellspacing: 0, cellpadding: 0, style: 'border-collapse:collapse;border:none;') {
					oses.each { os_name ->
				   tr() {
						  td(valign: 'top', style: 'width:113.4pt;border:solid windowtext 1.0pt; background:#ECEEE1;padding:0in 5.4pt 0in 5.4pt',
							  os_name
						  )
						  td(valign: 'top', style: 'width:113.4pt;border:solid windowtext 1.0pt; background:#ECEEE1;padding:0in 5.4pt 0in 5.4pt') {
							  testcases.each {
			if ( it.getAt("failure").size() > 0 || it.getAt("error").size() > 0 ) {
				def test_name = it['@name']
				
				p(test_name)
				
			}
		}
						  }
				   }
				  	}
				} // end table
	
				p {
					span(style: 'font-size:14.0pt;line-height:115%',
						"List of Failing and Error Test Cases"
					)
				}
	
				table(border: 1, cellspacing: 0, cellpadding: 0, style: 'border-collapse:collapse;border:none;') {
					   tr() {
						   td(valign: 'top', style: 'width:113.4pt;border:solid windowtext 1.0pt; background:#A9CAED;padding:0in 5.4pt 0in 5.4pt')
						   td(valign: 'top', style: 'width:153.0pt;border:solid windowtext 1.0pt; border-left:none;background:#A9CAED;padding:0in 5.4pt 0in 5.4pt',
							   "Base"
						   )
						   td(valign: 'top', style: 'width:2.95in;border:solid windowtext 1.0pt; border-left:none;background:#A9CAED;padding:0in 5.4pt 0in 5.4pt',
							   "Test"
						   )
					   }
					   
					   
					   
					
					oses.each { os_name ->   
				  	tr() {
						  td(valign: 'top', style: 'width:113.4pt;border:solid windowtext 1.0pt; border-top:none; background:#ECEEE1;padding:0in 5.4pt 0in 5.4pt',
							  os_name
						  )
						  td(valign: 'top', style: 'width:113.4pt;border:solid windowtext 1.0pt; border-top:none; background:#ECEEE1;padding:0in 5.4pt 0in 5.4pt') {
							   testcases.each {
			if ( it.getAt("failure").size() > 0 || it.getAt("error").size() > 0 ) {
				def test_name = it['@name']
				
				p(test_name)
			}
		}
						  }
						  td(valign: 'top', style: 'width:113.4pt;border:solid windowtext 1.0pt; border-top:none; background:#ECEEE1;padding:0in 5.4pt 0in 5.4pt') {
							    testcases.each {
			if ( ( it['failure'] || it['error'] ) && it.text() != null && it.text().length() > 0 ) {
				def test_name = it['@name']
				
				p(test_name)
			}
		}
						  }
						  }
				  	}
				} // end table
								
			} // end body
		} // end html
	
		def f = new File("index.html")
		f.write(sw.toString())
		
		  
	} // end void run

	@Override
	public String getHTMLString() {
		return sw.toString();
	}

} // end class AUTReportGen
