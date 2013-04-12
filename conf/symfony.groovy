
/**
 *
 * @see http://symfony.com/download
 *
 */
// use symfony-standard instead of just symfony
// seems that composer.json from symfony-standard installs a bunch of extra modules that composer.json from symfony does not
class SymfonyPhpUnitTestPack extends PhpUnitSourceTestPack {
	
	@Override
	public String getNameAndVersionString() {
		return "Symfony-Standard-2.1.8";
	}
	
	@Override
	protected String getSourceRoot(AHost host) {
		return host.getPfttDir()+"/cache/working/symfony-standard";
	}
	@Override
	protected Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		return super.clone();
	}
	public String[][] getNonThreadSafeTestFileNames() {
		return [
			["Symfony/Component/HttpFoundation"],
			["Symfony/Component/HttpKernel"],
			["Symfony/Component/Security/Tests/Acl/"],
			["Symfony/Component/Form/Tests/"]
		]
	}
 
	@Override
	protected boolean openAfterInstall(ConsoleManager cm, AHost host) throws Exception {
		// 1. do initial install (if not done already)
		if (!host.exists(getRoot()+"/vendor")) {
			if (host.isWindows()) {
				// composer needs git
				host.exec(host.getPfttDir()+"\\cache\\dep\\git.exe /VERYSILENT /SP-", AHost.ONE_MINUTE*4)
				
				// make sure composer is up to date (won't run otherwise)
				host.exec("php.exe "+host.getPfttDir()+"\\cache\\util\\composer.phar self-update", AHost.ONE_HOUR)
				
				// now run composer to install symfony
				java.util.HashMap<String,String> env = new java.util.HashMap<String,String>();
				// must put git in PATH so composer can find it
				env.put(AHost.PATH, host.getSystemDrive()+"\\Program Files (x86)\\Git\\Bin")
				for (int i=0 ; i < 3 ; i++) {
					// make sure composer downloads everything (sometimes have http problem)
					
					
					host.exec(cm, "SymfonyPhpUnitTestPack", 
							"php.exe "+host.getPfttDir()+"\\cache\\util\\composer.phar install",
							AHost.HALF_HOUR,
							env,
							// where cwd is symfony-standard
							getSourceRoot(host)
						);
				}
			} else {
				// XXX installer for Linux
			}
		}
		
		// 2. don't run these, they're really broken
		addBlacklist("vendor/kriswallsmith/assetic/tests/assetic/test/filter/sass/sassfiltertest.php");
		addBlacklist("vendor/sensio/generator-bundle/sensio/bundle/generatorbundle/resources/skeleton/bundle/defaultcontrollertest.php");
		addBlacklist("vendor/symfony/symfony/vendor/kriswallsmith/assetic/tests/assetic/test/filter/sass/sassfiltertest.php");
		addBlacklist("symfony/component/yaml/tests/parsertest.php");
		addBlacklist("vendor/symfony/symfony/vendor/sensio/generator-bundle/sensio/bundle/generatorbundle/resources/skeleton/bundle/defaultcontrollertest.php");
		addBlacklist("vendor/symfony/symfony/vendor/twig/twig/test/twig/tests/integrationtest.php");
		addBlacklist("vendor/twig/twig/test/twig/tests/integrationtest.php");
		
		// 3.
		addPhpUnitDist(getRoot()+"/vendor/symfony/symfony/src", getRoot()+"/vendor/symfony/symfony/autoload.php.dist");
		addPhpUnitDist(getRoot()+"/vendor/doctrine/common/tests", getRoot()+"/vendor/doctrine/common/tests/Doctrine/Tests/TestInit.php");
		addIncludeDirectory(getRoot()+"/vendor/symfony/symfony/src");
		
		// 4.
		if (!host.exists(getRoot()+"/vendor/symfony/symfony/vendor")) {
			// copy Vendors, which is part of the Symfony install process
			//
			//
			String tmp_dir = host.mktempname(getClass());
			// have to move to a temp directory because it'll cause a loop otherwise
			host.copy(getRoot()+"/vendor", tmp_dir+"/vendor");
			
			host.move(tmp_dir+"/vendor", getRoot()+"/vendor/symfony/symfony/vendor");
			
			host.deleteIfExists(tmp_dir);
		}
		
		return true;
	} // end public boolean openAfterInstall
	
	@Override
	public void prepareINI(ConsoleManager cm, AHost host, ScenarioSet scenario_set, PhpBuild build, PhpIni ini) {
		if (scenario_set.contains(OpcacheScenario.class)) {
			// when using OpcacheScenario
			//
			// Doctrine(Symfony) requires classes have annotations like @ORM\Entity. Annotations are
			// comments. Comments have to be loaded and saved or doctrine won't see those annotations.
			ini.putSingle("opcache.save_comments", 1);
			ini.putSingle("opcache.load_comments", 1);
		}
	}
	
} // end class SymfonyPhpUnitTestPack
getBinding().setVariable("SymfonyPhpUnitTestPack", SymfonyPhpUnitTestPack);
def getPhpUnitSourceTestPack() {
	// test symfony
	return new SymfonyPhpUnitTestPack();
}

// symfony is a framework only, not an application scenario to install

