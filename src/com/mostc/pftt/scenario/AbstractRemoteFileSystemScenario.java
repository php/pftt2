package com.mostc.pftt.scenario;

import com.github.mattficken.io.Trie;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.EPhptTestStatus;
import com.mostc.pftt.model.core.ESAPIType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.results.PhptTestResult;

public abstract class AbstractRemoteFileSystemScenario extends AbstractFileSystemScenario {

	@Override
	public boolean isUACRequiredForStart() {
		return true;
	}
	
	@Override
	public boolean isUACRequiredForSetup() {
		return true;
	}

	public abstract AHost getRemoteHost();


	public static final Trie LOCAL_FS_ONLY_TESTS = PhptTestCase.createNamed(
				// if you try running these tests on a remote filesystem (ex: DFS)
				// they fail with a message that specifically states that they can only be run on a local NTFS filesystem
				"ext/standard/tests/file/windows_links/bug48746.phpt",
				"ext/standard/tests/file/windows_links/bug48746_1.phpt",
				"ext/standard/tests/file/windows_links/bug48746_2.phpt",
				"ext/standard/tests/file/windows_links/bug48746_3.phpt"
			);
	@Override
	public boolean willSkip(ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSetSetup setup, ESAPIType type, PhpBuild build, PhptTestCase test_case) throws Exception {
		if (test_case.isNamed(LOCAL_FS_ONLY_TESTS)) {
			twriter.addResult(host, setup, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "Not supported on Remote File Systems", null, null, null, null, null, null, null, null, null, null, null));
			
			return true;
		}
		return false;
	}
	
} // end public abstract class AbstractRemoteFileSystemScenario
