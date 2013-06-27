package com.mostc.pftt.model;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.main.Config;
import com.mostc.pftt.model.core.EBuildBranch;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;

public interface SourceTestPack<A extends ActiveTestPack, T extends TestCase> {
	void cleanup(ConsoleManager cm);
	String getSourceDirectory();
	/** may be called more than once. if tests have been added to test-pack
	 * since last read should add only those new tests, otherwise subsequent reads should be ignored
	 * 
	 * @param config
	 * @param test_cases
	 * @param cm
	 * @param twriter
	 * @param build
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws Exception
	 */
	void read(Config config, List<T> test_cases, ConsoleManager cm, ITestResultReceiver twriter, PhpBuild build) throws FileNotFoundException, IOException, Exception;
	A installInPlace(ConsoleManager cm, AHost host) throws IOException, Exception;
	A installNamed(AHost host, String string, List<T> test_cases) throws IllegalStateException, IOException, Exception;
	A install(ConsoleManager cm, AHost host, String local_test_pack_dir, String remote_test_pack_dir) throws IllegalStateException, IOException, Exception;
	EBuildBranch getTestPackBranch();
	String getTestPackVersionRevision();
}
