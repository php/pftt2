package com.mostc.pftt.results

class PhptTallyFile {
	public String sapi_scenario_name, build_branch, test_pack_branch, build_revision, test_pack_revision, os_name, os_name_long
	public int pass, fail, skip, xskip, xfail, xfail_works, unsupported, bork, exception
	
	static PhptTallyFile open(File file) {
		def root = new XmlSlurper().parse(file);
		for ( def e : root.depthFirst().findAll { it.name() == 'tally' } ) {
			def tally = new PhptTallyFile()
			tally.sapi_scenario_name = e['@sapi_scenario_name']
			tally.build_branch = e['@build_branch']
			tally.test_pack_branch = e['@test_pack_branch']
			tally.build_revision = e['@build_revision']
			tally.test_pack_revision = e['@test_pack_revision']
			tally.os_name = e['@os_name']
			tally.os_name_long = e['@os_name_long']
			tally.pass = Integer.parseInt(e['@pass'].text())
			tally.fail = Integer.parseInt(e['@fail'].text())
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
				'sapi_scenario_name': tally.sapi_scenario_name,
				'build_branch': tally.build_branch,
				'test_pack_branch': tally.test_pack_branch,
				'build_revision': tally.build_revision,
				'test_pack_revision': tally.test_pack_revision,
				'os_name': tally.os_name,
				'os_name_long': tally.os_name_long,
				'pass': tally.pass,
				'fail': tally.fail,
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
