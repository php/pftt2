package com.mostc.pftt.model.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.github.mattficken.Overridable;
import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.main.Config;
import com.mostc.pftt.model.SourceTestPack;
import com.mostc.pftt.model.core.EBuildBranch;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.core.PhpParser;
import com.mostc.pftt.model.core.PhpParser.ClassDefinition;
import com.mostc.pftt.model.core.PhpParser.FunctionDefinition;
import com.mostc.pftt.model.core.PhpParser.PhpScript;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.results.PhpResultPackWriter;
import com.mostc.pftt.scenario.ScenarioSet;

/** Represents a pack of PhpUnitTestCases and the configuration information needed to run them.
 * 
 * To configure a PhpUnitSourceTestPack:
 * 1. provide PhpUnitSourceTestPack the path to the test pack
 *        -implement #getSourceRoot
 * 2. find all the phpunit.dist.xml files in the test-pack
 *        -implement #openAfterInstall
 * 3. (required) create corresponding PhpUnitDists by calling PhpUnitSourceTestPack#addPhpUnitDist
 *    and provide all the information from the phpunit.dist.xml file.
 *    the Javadoc on the PhpUnitDist methods explains which method matches which XML tag.
 * 4. provide files and directories to include to PhpUnitSourceTestPack (optional)
 * 5. check if any tests are non-thread-safe (NTS) and if so, add their file names or partial file names
 *    to the list returned by PhpUnitSourceTestPack#getNonThreadSafeTestFileNames
 *    
 *    To speed test running, making testing more convenient and thus done more frequently and thoroughly,
 *    test running is threaded, so multiple tests are run at the same time except for NTS tests.
 * 6. you may provide some additional info to PhpUnitSourceTestPack (optional; mainly, its just doing steps 3 and 4)
 * 7. optionally, add pre-bootstrap and post-bootstrap php code that will be run before or after the bootstrap file is loaded
 *      NOTE: what `phpunit` calls 'preamble' code, is post-bootstrap in PFTT.
 * 8. optionally, add globals to #prepareGlobals. optionally, add INI directives to #prepareINI
 * 
 * If your test-pack MUST be run with a database scenario (ex: mysql), you should extend RequiredDatabasePhpUnitSourceTestPack instead (it'll take care of making sure a database scenario is included in the configuration).
 * If your test-pack has some database tests that can be run, you should extend OptionalDatabasePhpUnitSourceTestPack.
 * 
 *  While your test-pack is in development, you should override #isDevelopment and have it return true. You'll get more stack traces and other
 *  information to help during the develop-test cycle you'll be in developing your test-pack.
 * 
 * @author Matt Ficken
 *
 */

public abstract class PhpUnitSourceTestPack implements SourceTestPack<PhpUnitActiveTestPack, PhpUnitTestCase> {
	protected String test_pack_root;
	protected final ArrayList<PhpUnitDist> php_unit_dists;
	protected final ArrayList<String> blacklist_test_names, whitelist_test_names, include_dirs, include_files;
	protected SoftReference<ArrayList<PhpUnitTestCase>> _ref_test_cases;
	
	public PhpUnitSourceTestPack() {
		blacklist_test_names = new ArrayList<String>(3);
		whitelist_test_names = new ArrayList<String>(3);
		php_unit_dists = new ArrayList<PhpUnitDist>(3);
		include_dirs = new ArrayList<String>(5);
		include_files = new ArrayList<String>(3);
		
		// add default entries to include_path
		addIncludeDirectory(".");
	}
	
	/** TRUE if test-pack is 'under development'. FALSE if its stable.
	 * 
	 * test runner will include extra info(stack traces, etc...) for test-packs that are under development
	 * 
	 * @return
	 */
	@Overridable
	public boolean isDevelopment() {
		return false;
	}
	
	@Override
	public EBuildBranch getTestPackBranch() {
		return null;
	}
	
	@Override
	public String getTestPackVersionRevision() {
		return getNameAndVersionString();
	}
	
	protected void resetDists() {
		for ( PhpUnitDist dist : php_unit_dists ) {
			dist._include_files = null;
			dist._include_path = null;
		}
	}
	
	/** add a PhpUnitDist... can base this off a PhpUnit.xml.dist
	 * 
	 * @param path
	 * @param bootstrap_file
	 * @return
	 */
	public PhpUnitDist addPhpUnitDist(String path, String bootstrap_file) {
		return addPhpUnitDist(path, bootstrap_file, null);
	}
	
	/** add a PhpUnitDist... can base this off a PhpUnit.xml.dist
	 * 
	 * @param path - directory where tests are stored
	 * @param bootstrap_file - php file to load with each test
	 * @param include_files - (optional) php files to include
	 * @return
	 */
	public PhpUnitDist addPhpUnitDist(String path, String bootstrap_file, String[] include_files) {
		PhpUnitDist dist = new PhpUnitDist(this, path, bootstrap_file, include_files);
		
		php_unit_dists.add(dist);
		
		addIncludeDirectory(path);
		
		return dist;
	}
	
	/** directory to add to the PHP include path.
	 * 
	 * all the classes all the tests need must be found either:
	 * 1. in the include path
	 * 2. loaded by the bootstrap
	 * 3. loaded as an include file
	 * 4. loaded by Pre-bootstrap or Post-bootstrap code
	 * 
	 * @param dir
	 * @return
	 */
	public PhpUnitSourceTestPack addIncludeDirectory(String dir) {
		resetDists();
		
		include_dirs.add(dir);
		
		return this;
	}
	
	/** file to load at the start of every test case using 'require_once'
	 * 
	 * @param file
	 * @return
	 */
	public PhpUnitSourceTestPack addIncludeFile(String file) {
		resetDists();
		
		include_files.add(file);
		
		return this;
	}
	
	/** adds tests to whitelist. if whitelist is not empty, only matching tests will be run
	 * 
	 * copy this from the &gt;whitelist&lt; tag in PhpUnit.xml.dist
	 * 
	 * @param test_name
	 * @return
	 */
	public PhpUnitSourceTestPack addWhitelist(String test_name) {
		whitelist_test_names.add(PhpUnitTestCase.normalizeFileName(test_name).toLowerCase());
		
		return this;
	}
	
	/** adds tests to blacklist. no matching test will be run
	 * 
	 * copy this from the &gt;blacklist&lt; tag in PhpUnit.xml.dist
	 * 
	 * @param test_name
	 * @return
	 */
	public PhpUnitSourceTestPack addBlacklist(String test_name) {
		blacklist_test_names.add(PhpUnitTestCase.normalizeFileName(test_name).toLowerCase());
		
		return this;
	}
	
	@Override
	public void read(Config config, List<PhpUnitTestCase> test_cases, List<String> names, ConsoleManager cm, PhpResultPackWriter twriter, PhpBuild build) throws FileNotFoundException, IOException, Exception {
		read(config, test_cases, names, cm, twriter, build, false);
	}
	
	@Override
	public void read(Config config, List<PhpUnitTestCase> test_cases, List<String> names, ConsoleManager cm, PhpResultPackWriter twriter, PhpBuild build, boolean ignore_missing) throws FileNotFoundException, IOException, Exception {
		doRead(config, cm, test_cases, names);
	}
	
	protected static void copyTestsNoDuplicates(List<PhpUnitTestCase> src, List<PhpUnitTestCase> dst) {
		// multiple PhpUnitDist files may reference the same tests, just run(load) each one time
		final HashMap<String,PhpUnitTestCase> by_name = new HashMap<String,PhpUnitTestCase>();
		for ( PhpUnitTestCase test_case : src )
			by_name.put(test_case.getName(), test_case);
		dst.clear(); // TODO temp? prevents enumerating each test twice
		dst.addAll(by_name.values());
	}
	
	/** reads all the PhpUnitTestCases from this test-pack
	 * 
	 * @param config
	 * @param test_cases
	 * @throws IOException
	 * @throws Exception
	 */
	public void read(Config config, ConsoleManager cm, List<PhpUnitTestCase> test_cases) throws IOException, Exception {
		// TODO if subdir used, only search within that
		
		//
		// read from cache
		ArrayList<PhpUnitTestCase> _test_cases;
		if (_ref_test_cases!=null) {
			_test_cases = _ref_test_cases.get();
			if (_test_cases!=null) {
				copyTestsNoDuplicates(_test_cases, test_cases);
				return;
			}
		}
		//
		
		_test_cases = new ArrayList<PhpUnitTestCase>(3500);
		doRead(config, cm, _test_cases, null);
		
		// cache for future use
		copyTestsNoDuplicates(_test_cases, test_cases);
		_ref_test_cases = new SoftReference<ArrayList<PhpUnitTestCase>>(_test_cases);
		//
	}
	
	protected void doRead(Config config, ConsoleManager cm, List<PhpUnitTestCase> test_cases, List<String> test_names) throws IOException {
		final int max_read_count = cm.getMaxTestReadCount();
		for (PhpUnitDist php_unit_dist : php_unit_dists) {
			readDir(config, max_read_count, test_cases, php_unit_dist, php_unit_dist.path, test_names);
		}
		
		// alphabetize
		Collections.sort(test_cases, new Comparator<PhpUnitTestCase>() {
				@Override
				public int compare(PhpUnitTestCase a, PhpUnitTestCase b) {
					return a.getName().compareTo(b.getName());
				}
			});
	}
	
	/** Many test-packs store their phpunit tests only in files that end with `Test.php`,
	 * but some don't.
	 * 
	 * This checks for that. you can override that for custom checks.
	 * 
	 * @param file_name
	 * @return
	 */
	@Overridable
	public boolean isFileNameATest(String file_name) {
		return file_name.endsWith("Test.php");
	}
	
	/** PhpUnit tests are supposed to be stored functions with names beginning with `test`.
	 * 
	 * This allows for overridding that behavior.
	 * 
	 * @param function_name
	 * @return
	 */
	@Overridable
	public boolean isFunctionATest(String function_name) {
		return function_name.startsWith("test");
	}
	
	/** scans for *Test.php files and reads PhpUnitTestCase(s) from them
	 * 
	 * @param config
	 * @param max_read_count - max number of test cases to read (0=unlimited)
	 * @param test_cases
	 * @param php_unit_dist
	 * @param dir
	 * @throws IOException
	 */
	protected void readDir(Config config, final int max_read_count, List<PhpUnitTestCase> test_cases, PhpUnitDist php_unit_dist, File dir, List<String> test_names) throws IOException {
		if (max_read_count > 0 && test_cases.size() >= max_read_count)
			return;
		
		File[] list_files = dir.listFiles();
		if (list_files==null)
			return;
		
		for ( File file : list_files ) {
			if (file.isDirectory()) {
				if (max_read_count > 0 && test_cases.size() >= max_read_count)
					return;
				readDir(config, max_read_count, test_cases, php_unit_dist, file, test_names);
				if (max_read_count > 0 && test_cases.size() >= max_read_count)
					return;
			} else if (isFileNameATest(file.getName())) {
				String rel_file_name = PhpUnitTestCase.normalizeFileName(Host.pathFrom(php_unit_dist.path.getAbsolutePath(), file.getAbsolutePath()));
				
				String abs_file_name = PhpUnitTestCase.normalizeFileName(file.getAbsolutePath());
				
				String lc_test_file_name = rel_file_name.toLowerCase();
				if (test_names!=null) {
					boolean skip = true;
					for ( String test_name : test_names ) {
						if (lc_test_file_name.contains(test_name)) {
							skip = false;
							break;
						}
					}
					if (skip)
						continue;
				}
				if (blacklist_test_names.contains(lc_test_file_name))
					continue;
				else if (!whitelist_test_names.isEmpty() && !whitelist_test_names.contains(lc_test_file_name))
					continue;
				
				readTestFile(config, max_read_count, rel_file_name, abs_file_name, php_unit_dist, test_cases, file);
				
				if (max_read_count > 0 && test_cases.size() >= max_read_count)
					return;
			}
		}
		
	} // end protected void readDir
	
	/** reads PhpUnitTestCase(s) from given PHP file
	 * 
	 * @param config 
	 * @param max_read_count
	 * @param rel_test_file_name
	 * @param abs_test_file_name
	 * @param php_unit_dist
	 * @param test_cases
	 * @param file
	 */
	protected void readTestFile(Config config, final int max_read_count, String rel_test_file_name, String abs_test_file_name, PhpUnitDist php_unit_dist, List<PhpUnitTestCase> test_cases, File file) {
		PhpScript script = PhpParser.parseScript(file);
		String dataProviderMethodName, dependsMethodName;
		
		for ( ClassDefinition clazz : script.getClasses() ) {
			if (clazz.isAbstract()||clazz.isInterface())
				continue;
			
			for ( FunctionDefinition func : clazz.getFunctions() ) {
				// search class for functions that start with 'test'
				if (isFunctionATest(func.getName())) {
					// this is a test case
					//
					//
					// some tests use these annotations to provide the name of a function (in same class)
					// to call to get the arguments for this test case method
					//
					// @see http://phpunit.de/manual/3.7/en/appendixes.annotations.html#appendixes.annotations.dataProvider
					dataProviderMethodName = cleanFunctionName(func.getAnnotationValue("dataProvider"));
					// @see http://phpunit.de/manual/3.7/en/appendixes.annotations.html#appendixes.annotations.depends
					dependsMethodName = cleanFunctionName(func.getAnnotationValue("depends"));
					
					PhpUnitTestCase test_case = new PhpUnitTestCase(
							php_unit_dist,
							abs_test_file_name,
							rel_test_file_name,
							// some PhpUnits use the namespace keyword and/or \\ in the class name (namespaces)
							// InterpretedclassDef#getName will provide the absolute class name (including namespace)
							// in such cases, so nothing special needs to be done here for them
							clazz.getName(),
							// name of method within the class
							func.getName(),
							func.getArgumentCount(),
							dataProviderMethodName,
							dependsMethodName
						);
					config.processPhpUnit(test_case);
					test_cases.add(test_case);
					
					if (max_read_count > 0 && test_cases.size() >= max_read_count)
						return;
				}
			}
		}
	} // end protected void readTestFile
	
	protected static String cleanFunctionName(String name) {
		if (StringUtil.isEmpty(name))
			return null;
		name = name.trim();
		int i = name.indexOf("::");
		if (i!=-1)
			name = name.substring(i+"::".length());
		i = name.indexOf('(');
		if (i!=-1)
			name = name.substring(0, i);
		return StringUtil.isEmpty(name) ? null : name;
	}

	@Override
	public void cleanup(ConsoleManager cm) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getSourceDirectory() {
		return getRoot();
	}

	@Override
	public void read(Config config, List<PhpUnitTestCase> test_cases,
			ConsoleManager cm, ITestResultReceiver twriter, PhpBuild build)
			throws FileNotFoundException, IOException, Exception {
		config.processPhpUnitTestPack(this, twriter, build);
		read(config, cm, test_cases);
	}

	@Override
	public PhpUnitActiveTestPack installInPlace(ConsoleManager cm, AHost host) throws Exception {
		final String src_root = getSourceRoot(cm, LocalHost.getInstance());
		addIncludeDirectory(src_root);
		if (!new File(src_root).isDirectory()) {
			throw new IOException("source-test-pack not found: "+src_root);
		}
		setRoot(src_root);
		
		openAfterInstall(cm, host);
		
		return new PhpUnitActiveTestPack(src_root, src_root);
	}

	@Override
	public PhpUnitActiveTestPack installNamed(ConsoleManager cm, AHost host, String string, List<PhpUnitTestCase> test_cases) throws IllegalStateException, IOException, Exception {
		final String src_root = getSourceRoot(cm, LocalHost.getInstance());
		addIncludeDirectory(src_root);
		if (!new File(src_root).isDirectory()) {
			throw new IOException("source-test-pack not found: "+src_root);
		}
		setRoot(src_root);
		
		openAfterInstall(cm, host);
		
		return new PhpUnitActiveTestPack(src_root, src_root);
	}

	@Override
	public PhpUnitActiveTestPack install(ConsoleManager cm, AHost host,
			String local_test_pack_dir, String remote_test_pack_dir)
			throws IllegalStateException, IOException, Exception {
		LocalHost local_host = LocalHost.getInstance();
		final String src_root = getSourceRoot(cm, local_host);
		addIncludeDirectory(src_root);
		if (!new File(src_root).isDirectory()) {
			throw new IOException("source-test-pack not found: "+src_root);
		}
		
		// using #uploadCompressWith7Zip instead of just #upload makes a huge difference
		// for PhpUnit test-packs because of the large number of small files that have to be uploaded
		host.uploadCompressWith7Zip(cm, getClass(), src_root, local_host, remote_test_pack_dir);
		
		setRoot(local_test_pack_dir);
		
		openAfterInstall(cm, local_host);
		
		return new PhpUnitActiveTestPack(local_test_pack_dir, remote_test_pack_dir);
	}
	
	private boolean decompressed = false;
	protected void ensureAppDecompressed(ConsoleManager cm, AHost host, String zip7_file) throws IllegalStateException, IOException, Exception {
		if (decompressed)
			return;
		decompressed = true;
		if (!StringUtil.endsWithIC(zip7_file, ".7z"))
			zip7_file += ".7z";
		
		host.decompress(cm, host, host.getPfttDir()+"/app/"+zip7_file, host.getPfttDir()+"/cache/working/");
	}
	
	/** the base directory within the PFTT directory to find the phpunit and required php files
	 * 
	 * Typically, test-packs will call #ensureAppDecompressed
	 * 
	 * @param cm
	 * @param host - determine the absolute path on this host
	 * @see AHost#getPfttDir
	 * @return
	 */
	protected abstract String getSourceRoot(ConsoleManager cm, AHost host);

	/** installs the tests after they have been copied to storage (if needed)
	 * 
	 * @see #getRoot() returns the location the tests and their php files have been copied to (if they were
	 * copied, if not copied, returns location they are stored at)
	 * 
	 * @param cm
	 * @param host
	 * @return
	 * @throws Exception
	 */
	protected abstract boolean openAfterInstall(ConsoleManager cm, AHost host) throws Exception;

	/** file path to test-pack */
	public void setRoot(String test_pack_root) {
		this.test_pack_root = test_pack_root;
	}

	/** file path to test-pack */
	public String getRoot() {
		return this.test_pack_root;
	}
	
	/** (Optional) return PHP code to run BEFORE loading the bootstrap file
	 * 
	 * @param cm
	 * @param host
	 * @param scenario_set
	 * @param build
	 * @return
	 */
	@Overridable
	public String getPreBootstrapCode(ConsoleManager cm, AHost host, ScenarioSet scenario_set, PhpBuild build) {
		return null;
	}
	
	/** (Optional) return PHP code to run AFTER loading the bootstrap file
	 * 
	 * @param cm
	 * @param host
	 * @param scenario_set
	 * @param build
	 * @return
	 */
	@Overridable
	public String getPostBootstrapCode(ConsoleManager cm, AHost host, ScenarioSet scenario_set, PhpBuild build) {
		return null;
	}
	
	@Overridable
	public int getThreadCount(AHost host, ScenarioSet scenario_set, int default_thread_count) {
		return default_thread_count;
	}
	
	public abstract String getNameAndVersionString();
	
	public String getName() {
		return getNameAndVersionString();
	}
	
	@Override
	public String toString() {
		return getName();
	}

	/** Sometimes there are multiple tests that share a common resource (such as a file directory
	 * or database) and can not be run at the same time. Such tests are non-thread-safe (known as NTS tests).
	 * 
	 * Return the full or partial filenames of NTS tests here. The returned array is processed in
	 * order. If any string from the same string array matches, all tests matching that array will
	 * be run in the same thread.
	 * 
	 * @return
	 */
	@Nullable
	public String[][] getNonThreadSafeTestFileNames() {
		return null;
	}
	
	/** allows a test-pack to create custom edits of PhpInis
	 * 
	 * @param cm
	 * @param host
	 * @param scenario_set
	 * @param build
	 * @param ini
	 */
	public void prepareINI(ConsoleManager cm, AHost host, ScenarioSet scenario_set, PhpBuild build, PhpIni ini) {
		
	}
	
	/** called when an individual test case is run
	 * 
	 * @param cm
	 * @param runner_host
	 * @param scenario_set
	 * @param build
	 * @param test_case
	 * @return TRUE to run the test
	 */
	@Overridable
	public boolean startTest(ConsoleManager cm, AHost runner_host, ScenarioSet scenario_set, PhpBuild build, PhpUnitTestCase test_case) {
		return true;
	}

	/** called just before test-run starts
	 * 
	 * @param cm
	 * @param runner_host
	 * @param scenario_set
	 * @param build
	 * @return FALSE to not run
	 */
	@Overridable
	public boolean startRun(ConsoleManager cm, AHost runner_host, ScenarioSet scenario_set, PhpBuild build) {
		return true;
	}
	
	/** called just after test-run stops
	 * 
	 * @param cm
	 * @param runner_host
	 * @param scenario_set
	 * @param build
	 */
	@Overridable
	public void stopRun(ConsoleManager cm, AHost runner_host, ScenarioSet scenario_set, PhpBuild build) {
	}

	/** add to PhpUnit's globals
	 * 
	 * @param cm
	 * @param runner_host
	 * @param scenario_set
	 * @param build
	 * @param globals
	 */
	@Overridable
	public void prepareGlobals(ConsoleManager cm, AHost runner_host, ScenarioSet scenario_set, PhpBuild build, Map<String, String> globals) {
		
	}
	
} // end public abstract class PhpUnitSourceTestPack
