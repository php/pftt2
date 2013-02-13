
/**
 *
 * @see http://symfony.com/download
 *
 */
class SymfonyPhpUnitTestPack extends PhpUnitSourceTestPack {
	
	@Override
	public String getVersionString() {
		return "Symfony-2.1.7";
	}

	@Override
	public boolean open(ConsoleManager cm, AHost host) throws Exception {
		// 1.
		addBlacklist("vendor/kriswallsmith/assetic/tests/assetic/test/filter/sass/sassfiltertest.php");
		addBlacklist("vendor/sensio/generator-bundle/sensio/bundle/generatorbundle/resources/skeleton/bundle/defaultcontrollertest.php");
		addBlacklist("vendor/symfony/symfony/vendor/kriswallsmith/assetic/tests/assetic/test/filter/sass/sassfiltertest.php");
		addBlacklist("vendor/symfony/symfony/vendor/sensio/generator-bundle/sensio/bundle/generatorbundle/resources/skeleton/bundle/defaultcontrollertest.php");
		addBlacklist("vendor/symfony/symfony/vendor/twig/twig/test/twig/tests/integrationtest.php");
		addBlacklist("vendor/twig/twig/test/twig/tests/integrationtest.php");
		
		// 2.
		setRoot("C:\\php-sdk\\PFTT\\current\\cache\\working\\Symfony");
		addPhpUnitDist(getRoot()+"/vendor/symfony/symfony/src", getRoot()+"/vendor/symfony/symfony/autoload.php.dist");
		addPhpUnitDist(getRoot()+"/vendor/doctrine/common/tests", getRoot()+"/vendor/doctrine/common/tests/Doctrine/Tests/TestInit.php");
		addIncludeDirectory(getRoot()+"/vendor/symfony/symfony/src");
		
		// 3.
		if (!host.exists(getRoot()+"/vendor/symfony/symfony/vendor")) {
			// copy Vendors, which is part of the Symfony install process
			//
			//
			String tmp_dir = host.mktempname(getClass());
			// have to move to a temp directory because it'll cause a loop otherwise
			host.copy(getRoot()+"/vendor", tmp_dir+"/vendor");
			
			host.move(tmp_dir+"/vendor", getRoot()+"/vendor/symfony/symfony/vendor");
			
			host.delete(tmp_dir);
		}
		
		return true;
	} // end public boolean open
	
} // end class SymfonyPhpUnitTestPack

def getPhpUnitSourceTestPack() {
	// test symfony
	return new SymfonyPhpUnitTestPack();
}

// symfony is a framework only, not an application scenario to install

