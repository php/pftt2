import java.util.Map;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ScenarioSet;


def scenarios() {
	new PhpBB3Scenario()
}

def describe() {
	"Load PhpBB3 application"
}

class PhpBB3PhpUnitTestPack extends RequiredDatabasePhpUnitSourceTestPack {
	
	@Override
	public String getNameAndVersionString() {
		return "PhpBB3";
	}
	
	@Override
	protected String getSourceRoot(AHost host) {
		return host.getPfttDir()+"/cache/working/phpbb3";
	}
	
	@Override
	public boolean isDevelopment() {
		return true;
	}
	
	@Override
	public boolean isFileNameATest(String file_name) {
		return file_name.endsWith(".php");
	}
	
	@Override
	protected boolean openAfterInstall(ConsoleManager cm, AHost host) throws Exception {
		
		addIncludeDirectory(getRoot()+"/phpBB/");
		addIncludeDirectory(getRoot()+"/phpBB/includes");
		addIncludeDirectory(getRoot()+"/phpBB/libraries");
		addPhpUnitDist(getRoot()+"/tests/", getRoot()+"/tests/bootstrap.php");
		
		return true;
	}
	
	@Override
	public void prepareGlobals(ConsoleManager cm, AHost runner_host, ScenarioSet scenario_set, PhpBuild build, Map<String, String> globals) {
		// @see /phpBB/includes/startup.php
		globals.put("PHPBB_NO_COMPOSER_AUTOLOAD", "1");
	}
	
} // end class PhpBB3PhpUnitTestPack

def getPhpUnitSourceTestPack() {
	new PhpBB3PhpUnitTestPack();
}
