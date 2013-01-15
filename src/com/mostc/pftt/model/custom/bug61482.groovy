
// runs 'nmake snap' to test building php
package com.mostc.pftt.model.custom;

// this catches bugs like #61482 which prevent php from being built due to a change in php
// (build process uses php being built for later stages of build process. broke in #61482)

class Bug61482 {
	def skip(host) {
		//
		// not for daily use... for QA(rc, beta) and final releases  (it takes a while)
		//
		host.env_value('RUN_SLOW_TESTS')
	}
	def runTimeSeconds() {
		// not building using PGO or lots of extensions
		//
		// just doing a 'quick build' which should only take 10-20 minutes
		3600
	}
	def execute(test_runner) {
		host.exec('nmake snap', runTimeSecond())
	}
}
