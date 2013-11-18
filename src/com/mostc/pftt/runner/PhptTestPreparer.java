package com.mostc.pftt.runner;

import java.io.IOException;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.TestCase;
import com.mostc.pftt.model.app.PhpUnitTemplate;
import com.mostc.pftt.model.core.EPhptSection;
import com.mostc.pftt.model.core.PhptActiveTestPack;
import com.mostc.pftt.model.core.PhptSourceTestPack;
import com.mostc.pftt.model.core.PhptTestCase;

public class PhptTestPreparer {
	protected final boolean xdebug;
	
	public PhptTestPreparer(boolean xdebug) {
		this.xdebug = xdebug;
	}

	public PreparedPhptTestCase prepare(PhptTestCase test_case, AHost host, PhptActiveTestPack active_test_pack) throws IllegalStateException, IOException {
		PreparedPhptTestCase prep = new PreparedPhptTestCase(test_case);
		
		prep.test_dir = host.joinIntoOnePath(active_test_pack.getStorageDirectory(), AHost.dirname(test_case.getName()));
		
		// CRITICAL
		// TODO must be done for -auto - should be more efficient
		host.mkdirs(prep.test_dir);
	
		// some intl tests have + in their name... sending this to the builtin web server breaks it (HTTP 404)
		prep.base_file_name = AHost.basename(test_case.getBaseName()).replace("+", "");
		
		//
		if (test_case.containsSection(EPhptSection.SKIPIF)) {
			prep.skipif_file = host.joinIntoOnePath(prep.test_dir, prep.base_file_name + ".skip.php");
				
			host.saveTextFile(prep.skipif_file, test_case.get(EPhptSection.SKIPIF));
		} else {
			// clearly flag that skipif isn't to be executed
			prep.skipif_file = null;
		}
		// @see AbstractSAPIScenario#willSkip - skips certain tests before even getting here to #prepare
		//
	
		prep.test_file = host.joinIntoOnePath(prep.test_dir, prep.base_file_name + ".php");
		
		if (test_case.containsSection(EPhptSection.CLEAN)) {
			prep.test_clean = host.joinIntoOnePath(prep.test_dir, prep.base_file_name + ".clean.php");
			
			host.saveTextFile(prep.test_clean, test_case.get(EPhptSection.CLEAN));
		} // else test_clean = null;
		
		return prep;
	}
	
	protected String preparePhptTestCode(String php_code) {
		if (xdebug)
			return PhpUnitTemplate.renderXDebugPhptTemplate(php_code);
		else
			return php_code;
	}
	
	public class PreparedPhptTestCase extends TestCase {
		public final PhptTestCase test_case;
		public String base_file_name, skipif_file, test_dir, test_file, test_clean;
		
		public PreparedPhptTestCase(PhptTestCase test_case) {
			this.test_case = test_case;
		}

		@Override
		public String getName() {
			return test_case.getName();
		}

		public void prepareTest(PhptSourceTestPack src_test_pack, AHost host) throws Exception {
			if (test_case.containsSection(EPhptSection.FILE_EXTERNAL)) {
				// open external file and copy to test_file (binary, no char conversion - which could break it - often this is a PHAR file - which will be broken if charset coversion is done)
				
				// @see run-test.php:1281
				String src_file = host.joinIntoOnePath(
						src_test_pack.getSourceDirectory(), 
						AHost.dirname(test_case.getName()), 
						test_case.getTrim(EPhptSection.FILE_EXTERNAL).replaceAll("\\.\\.", "")
					);
				host.copy(src_file, test_file);
				
			} else {
				host.saveTextFile(test_file, preparePhptTestCode(test_case.get(EPhptSection.FILE)), test_case.getCommonCharsetEncoder());
			}
		}
	}
	
}
