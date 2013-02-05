package com.mostc.pftt.model.app;

import com.mostc.pftt.host.LocalHost;

/**
 * 
 * @see http://symfony.com/download
 *
 */

public class Symfony {
	
	public String getVersionString() {
		return "Symfony-2.1.7";
	}

	public void setup(PhpUnitSourceTestPack test_pack) throws Exception {
		// 1.
		test_pack.blacklist_test_names.add("vendor/kriswallsmith/assetic/tests/assetic/test/filter/sass/sassfiltertest.php");
		test_pack.blacklist_test_names.add("vendor/sensio/generator-bundle/sensio/bundle/generatorbundle/resources/skeleton/bundle/defaultcontrollertest.php");
		test_pack.blacklist_test_names.add("vendor/symfony/symfony/vendor/kriswallsmith/assetic/tests/assetic/test/filter/sass/sassfiltertest.php");
		test_pack.blacklist_test_names.add("vendor/symfony/symfony/vendor/sensio/generator-bundle/sensio/bundle/generatorbundle/resources/skeleton/bundle/defaultcontrollertest.php");
		test_pack.blacklist_test_names.add("vendor/symfony/symfony/vendor/twig/twig/test/twig/tests/integrationtest.php");
		test_pack.blacklist_test_names.add("vendor/twig/twig/test/twig/tests/integrationtest.php");
		
		// 2.
		test_pack.test_pack_root = "C:\\php-sdk\\PFTT\\current\\cache\\working\\Symfony";
		test_pack.addPhpUnitDist(test_pack.test_pack_root+"/vendor/symfony/symfony/src", test_pack.test_pack_root+"/vendor/symfony/symfony/autoload.php.dist");
		test_pack.addPhpUnitDist(test_pack.test_pack_root+"/vendor/doctrine/common/tests", test_pack.test_pack_root+"/vendor/doctrine/common/tests/Doctrine/Tests/TestInit.php");
		
		// 3.
		LocalHost host = new LocalHost();
		if (!host.exists(test_pack.test_pack_root+"/vendor/symfony/symfony/vendor")) {
			// copy Vendors, which is part of the Symfony install process
			//
			//
			String tmp_dir = host.mktempname(getClass());
			// have to move to a temp directory because it'll cause a loop otherwise
			host.copy(test_pack.test_pack_root+"/vendor", tmp_dir+"/vendor");
			
			host.move(tmp_dir+"/vendor", test_pack.test_pack_root+"/vendor/symfony/symfony/vendor");
			
			host.delete(tmp_dir);
		}
	} // end public void setup
	
}
