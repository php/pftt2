
def scenarios() {
	// install Joomla CMS
	new JoomlaScenario()
}

/** Joomla-Platform != Joomla-CMS
 *
 * @see https://github.com/joomla/joomla-platform
 *
 */
class JoomlaPlatformPhpUnitTestPack extends SymfonyPhpUnitTestPack {
	
	@Override
	public String getVersionString() {
		return "Joomla-Platform-12.3";
	}

	@Override
	public boolean open(ConsoleManager cm, AHost host) throws Exception {
		// 1. dependency on SymfonyPhpUnitTestPack (Joomla-Platform depends on Symfony)
		super.open(cm, host);
		
		// 2.
		setRoot("C:\\php-sdk\\PFTT\\current\\cache\\working\\joomla-platform");
		addPhpUnitDist(getRoot()+"/tests/suites/database", getRoot()+"/tests/bootstrap.php");
		addPhpUnitDist(getRoot()+"/tests/suites/unit", getRoot()+"/tests/bootstrap.php");
		addPhpUnitDist(getRoot()+"/tests/suites/legacy", getRoot()+"/tests/bootstrap.legacy.php");
		
		// 3. run this code including joomla's bootstrap or bootstrap.legacy
		//     for some reason, joomla's bootstrap doesn't seem to include them
		setPreambleCode(
				"jimport('joomla.filesystem.file');\n" +
				"jimport('joomla.filesystem.path');\n" +
				"jimport('joomla.base.adapter');\n" +
				// this could be replaced by an include file, but the jimport calls can't be
				"require_once '"+getRoot()+"/tests/suites/unit/joomla/application/stubs/JApplicationWebInspector.php';"
			);

		return true;
	} // end public boolean open
	
} // end class JoomlaPlatformPhpUnitTestPack

def getPhpUnitSourceTestPack() {
	// test Joomla Platform
	return new JoomlaPlatformPhpUnitTestPack();
}
