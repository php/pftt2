
def describe() {
	"Load Joomla-CMS and Joomla-Platform"
}

def scenarios() {
	// install Joomla CMS
	new JoomlaScenario()
}

def getUITestPack() {
	return null; // TODO
}

/** Joomla-Platform != Joomla-CMS
 *
 * @see https://github.com/joomla/joomla-platform
 * 
 */

class JoomlaPlatformPhpUnitTestPack extends PhpUnitSourceTestPack { 
	
	@Override
	public String getNameAndVersionString() {
		return "Joomla-Platform-12.3";
	}
	
	@Override
	protected String getSourceRoot(AHost host) {
		return host.getPfttDir()+"/cache/working/joomla-platform";
	}

	@Override
	protected boolean openAfterInstall(ConsoleManager cm, AHost host) throws Exception {
		addPhpUnitDist(getRoot()+"/tests/suites/database", getRoot()+"/tests/bootstrap.php");
		addPhpUnitDist(getRoot()+"/tests/suites/unit", getRoot()+"/tests/bootstrap.php");
		addPhpUnitDist(getRoot()+"/tests/suites/legacy", getRoot()+"/tests/bootstrap.legacy.php");
		return true;
	} // end public boolean open
	
	@Override
	public String getPostBootstrapCode(ConsoleManager cm, AHost host, ScenarioSet scenario_set, PhpBuild build) {
		// run this code including joomla's bootstrap or bootstrap.legacy
		//     for some reason, joomla's bootstrap doesn't seem to include them
		//
		// this could be replaced by an include file, but the jimport calls can't be
		def root = getRoot()
		return """jimport('joomla.filesystem.file');
jimport('joomla.filesystem.path');
jimport('joomla.base.adapter');
require_once '$root/tests/suites/unit/joomla/application/stubs/JApplicationWebInspector.php';
"""
	}
	
	@Override
	public String[][] getNonThreadSafeTestFileNames() {
		return [
				["joomla/filesystem/"],
				["joomla/language/"],
				["joomla/cache/"],
				["joomla/application/"],
				["joomla/user/"],
				["controller/"],
				//["joomla/database/"],
				["joomla/form/"],
				//["joomla/github/"],
				//["joomla/google/"],
				["joomla/grid/"],
				["joomla/html/html/"],
				["joomla/image/"],
				["joomla/input/"]
			]
	}
	
} // end class JoomlaPlatformPhpUnitTestPack

def getPhpUnitSourceTestPack() {
	// test Joomla Platform
	return new JoomlaPlatformPhpUnitTestPack();
}
