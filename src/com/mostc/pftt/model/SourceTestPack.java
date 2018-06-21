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
import com.mostc.pftt.results.PhpResultPackWriter;
import com.mostc.pftt.scenario.SAPIScenario;

public abstract class SourceTestPack<A extends ActiveTestPack, T extends TestCase> {
	public abstract void cleanup(ConsoleManager cm);
	public abstract String getSourceDirectory();
	public abstract String getNameAndVersionString();
	/** may be called more than once. if tests have been added to test-pack
	 * since last read should add only those new tests, otherwise subsequent reads should be ignored
	 * 
	 * @param config
	 * @param test_cases
	 * @param cm
	 * @param twriter
	 * @param build
	 * @param sapi_scenario TODO
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws Exception
	 */
	public abstract void read(Config config, List<T> test_cases, ConsoleManager cm, ITestResultReceiver twriter, PhpBuild build, SAPIScenario sapi_scenario) throws FileNotFoundException, IOException, Exception;
	/** only reads tests with names matching the given name fragments
	 * 
	 * @param config
	 * @param test_cases
	 * @param names - fragments of names to read
	 * @param cm
	 * @param twriter
	 * @param build
	 * @param ignore_missing
	 * @param sapi_scenario TODO
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws Exception
	 */
	public abstract void read(Config config, List<T> test_cases, List<String> names, ConsoleManager cm, PhpResultPackWriter twriter, PhpBuild build, boolean ignore_missing, SAPIScenario sapi_scenario) throws FileNotFoundException, IOException, Exception;
	public abstract void read(Config config, List<T> test_cases, List<String> names, ConsoleManager cm, PhpResultPackWriter twriter, PhpBuild build, SAPIScenario sapi_scenario) throws FileNotFoundException, IOException, Exception;
	public abstract A installInPlace(ConsoleManager cm, AHost host) throws IOException, Exception;
	public abstract A installNamed(ConsoleManager cm, AHost host, String string, List<T> test_cases) throws IllegalStateException, IOException, Exception;
	public abstract A install(ConsoleManager cm, AHost host, String local_test_pack_dir, String remote_test_pack_dir, SAPIScenario sapi_scenario) throws IllegalStateException, IOException, Exception;
	public abstract EBuildBranch getTestPackBranch();
	public abstract String getTestPackVersionRevision();
}
