package com.mostc.pftt.results

class PhptTallyFile {
	public String os_name
	public int pass, fail, crash, skip, xskip, xfail, xfail_works, unsupported, bork, exception
	  
	static PhptTallyFile open(File file) {
		def root = new XmlSlurper().parse(file);
		for ( def e : root.depthFirst().findAll { it.name() == 'tally' } ) {
			def tally = new PhptTallyFile()
			tally.os_name = e['@os_name']
			tally.pass = Integer.parseInt(e['@pass'].text())
			tally.fail = Integer.parseInt(e['@fail'].text())
			tally.crash = Integer.parseInt(e['@crash'].text())
			tally.skip = Integer.parseInt(e['@skip'].text())
			tally.xskip = Integer.parseInt(e['@xskip'].text())
			tally.xfail = Integer.parseInt(e['@xfail'].text())
			tally.xfail_works = Integer.parseInt(e['@xfail_works'].text())
			tally.unsupported = Integer.parseInt(e['@unsupported'].text())
			tally.bork = Integer.parseInt(e['@bork'].text())
			tally.exception = Integer.parseInt(e['@exception'].text())
			return tally
		}
		return null
	}
	
	static void write(PhptTallyFile tally, Writer w) {
		def xml = new groovy.xml.MarkupBuilder(w)
		xml.tally(
				'os_name': tally.os_name,
				'pass': tally.pass,
				'fail': tally.fail,
				'crash': tally.crash,
				'skip': tally.skip,
				'xskip': tally.xskip,
				'xfail': tally.xfail,
				'xfail_works': tally.xfail_works,
				'unsupported': tally.unsupported,
				'bork': tally.bork,
				'exception': tally.exception
			)
	}
}
