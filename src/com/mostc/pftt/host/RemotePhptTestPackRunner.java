package com.mostc.pftt.host;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhptActiveTestPack;
import com.mostc.pftt.model.core.PhptSourceTestPack;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.results.LocalConsoleManager;
import com.mostc.pftt.results.PhpResultPackWriter;
import com.mostc.pftt.scenario.ScenarioSet;

/**
 * 
 * @see PSCAgentServer - server side
 *
 */

public class RemotePhptTestPackRunner extends AbstractRemoteTestPackRunner<PhptActiveTestPack, PhptSourceTestPack, PhptTestCase> {
	
	public RemotePhptTestPackRunner(PhpResultPackWriter tmgr, ScenarioSet scenario_set, PhpBuild build, LocalHost host, AHost remote_host) {
		super(tmgr, scenario_set, build, host, remote_host);
	}

	@Override
	protected void generateSimulation(PhptSourceTestPack test_pack) throws FileNotFoundException, IOException, Exception {
		final PrintStream orig_ps = System.out;
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		try {
			System.setOut(new PrintStream(out));
			
			runAllTests(null, test_pack);
		} finally {
			System.setOut(orig_ps);
		}
		
		System.out.println(StringUtil.toJava(out.toString()));
	}
	
	@Override
	protected void simulate() {
		String str = 
				"";
		System.setIn(new ByteArrayInputStream(str.getBytes()));
	}
	
	public static void main(String[] args) throws Exception {
		LocalHost host = new LocalHost();
		
		LocalConsoleManager cm = new LocalConsoleManager(null, null, false, false, false, false, true, false, true, false, false, false, 1, 1, true, 1, 1, 1, null, null, null, null, false, 0, 0, false, false, 0, 0, 0, false);
		
		PhpBuild build = new PhpBuild("C:\\php-sdk\\php-5.5-ts-windows-vc11-x64-re3aeb6c");
		build.open(cm, host);
		
		PhptSourceTestPack test_pack = new PhptSourceTestPack("C:\\php-sdk\\php-test-pack-5.5-ts-windows-vc11-x86-r0704e4b");
		test_pack.open(cm, host);
		
		PhpResultPackWriter tmgr = new PhpResultPackWriter(host, cm, new File(host.getPhpSdkDir()), build, test_pack, null);
		
		// TODO
		ScenarioSet scenario_set = ScenarioSet.getDefaultScenarioSets().get(0);
		
		RemotePhptTestPackRunner runner = new RemotePhptTestPackRunner(tmgr, scenario_set, build, host, host);
		if (args.length>0) {
			if (args[0].equals("simulate")) {
				//
				runner.simulate();
			} else if (args[0].equals("generate")) {
				//
				runner.generateSimulation(test_pack);
			}
		} else {
			runner.runAllTests(null, test_pack);
		}
	} // end public static void main
	
} // end public class RemotePhptTestPackRunner
