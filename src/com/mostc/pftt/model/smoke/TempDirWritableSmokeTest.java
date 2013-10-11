package com.mostc.pftt.model.smoke;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.ESAPIType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.results.PhpResultPackWriter;

/** Smoke test that ensures that the Temporary directory is writable.
 * 
 * Many features must fail if it is not. This detects/avoids those false failures.
 * 
 * @author Matt Ficken
 *
 */

public class TempDirWritableSmokeTest extends SmokeTest {
	public ESmokeTestStatus test(PhpBuild build, ConsoleManager cm, AHost host, ESAPIType type, PhpResultPackWriter tmgr) {
		try {
			String tmp_name = host.mktempname(getClass(), ".txt");
			host.saveTextFile(tmp_name, "test_string");
			host.delete(tmp_name);
			cm.println(EPrintType.CLUE, "TempDir", "Can write to TEMP dir: "+host.getTempDir());
			return ESmokeTestStatus.PASS;
		} catch ( Exception ex ) {
			cm.println(EPrintType.CLUE, "TempDir", "Could NOT write to TEMP dir: "+host.getTempDir());
			return ESmokeTestStatus.FAIL;
		}
	}

	@Override
	public String getName() {
		return "Temp-Dir-Writable";
	}
	
} // end public class TempDirWritableSmokeTest
