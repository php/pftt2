package com.mostc.pftt.model.app;

/** Joomla-Platform != Joomla-CMS
 * 
 * @see https://github.com/joomla/joomla-platform
 *
 */

public class JoomlaPlatform {
	
	public String getVersionString() {
		return "Joomla-Platform-12.3";
	}

	public void setup(PhpUnitSourceTestPack test_pack) throws Exception {
		// 1. TODO dependency on SymfonyTestPack
		test_pack.addIncludeDirectory("C:\\php-sdk\\PFTT\\current\\cache\\working\\Symfony\\vendor\\symfony\\symfony\\src");
		
		// 2.
		test_pack.test_pack_root = "C:\\php-sdk\\PFTT\\current\\cache\\working\\joomla-platform";
		test_pack.addPhpUnitDist(test_pack.test_pack_root+"/tests/suites/database", test_pack.test_pack_root+"/tests/bootstrap.php");
		test_pack.addPhpUnitDist(test_pack.test_pack_root+"/tests/suites/unit", test_pack.test_pack_root+"/tests/bootstrap.php");
		test_pack.addPhpUnitDist(test_pack.test_pack_root+"/tests/suites/legacy", test_pack.test_pack_root+"/tests/bootstrap.legacy.php");
		
		// 3. run this code including joomla's bootstrap or bootstrap.legacy
		//     for some reason, joomla's bootstrap doesn't seem to include them
		test_pack.preamble_code = 
				"jimport('joomla.filesystem.file');\n" +
				"jimport('joomla.filesystem.path');\n" +
				"jimport('joomla.base.adapter');\n" +
				// this could be replaced by an include file, but the jimport calls can't be
				"require_once '"+test_pack.test_pack_root+"/tests/suites/unit/joomla/application/stubs/JApplicationWebInspector.php';"
			;

	} // end public void setup
	
}
