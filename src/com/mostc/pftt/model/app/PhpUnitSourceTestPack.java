package com.mostc.pftt.model.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.parser.QuercusParseException;
import com.caucho.quercus.parser.QuercusParser;
import com.caucho.quercus.program.InterpretedClassDef;
import com.caucho.quercus.program.QuercusProgram;
import com.caucho.vfs.FilePath;
import com.caucho.vfs.FileReadStream;
import com.caucho.vfs.ReadStream;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.SourceTestPack;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;

/** Represents a pack of PhpUnitTestCases and the configuration information needed to run them.
 * 
 * To configure a PhpUnitSourceTestPack:
 * 1. provide PhpUnitSourceTestPack the path to the test pack
 * 2. find all the phpunit.dist.xml files in the test-pack
 * 3. (required) create corresponding PhpUnitDists by calling PhpUnitSourceTestPack#addPhpUnitDist
 *    and provide all the information from the phpunit.dist.xml file.
 *    the Javadoc on the PhpUnitDist methods explains which method matches which XML tag.
 * 4. provide files and directories to include to PhpUnitSourceTestPack (optional)
 * 5. you may provide some additional info to PhpUnitSourceTestPack (optional; mainly, its just doing steps 3 and 4) 
 * 
 * @author Matt Ficken
 *
 */

public abstract class PhpUnitSourceTestPack implements SourceTestPack<PhpUnitActiveTestPack, PhpUnitTestCase> {
	/** required: file path to test-pack */
	protected String test_pack_root;
	/** optional: PHP code to run before every test case. this is meant to do additional initialization
	 * that a bootstrap file doesn't do (without having to modify that bootstrap file)
	 * 
	 * @see PhpUnitDist#bootstrap
	 * */
	protected String preamble_code;
	protected final ArrayList<PhpUnitDist> php_unit_dists;
	protected final ArrayList<String> blacklist_test_names, whitelist_test_names, include_dirs, include_files;
	protected final QuercusContext qctx;
	
	public PhpUnitSourceTestPack() {
		blacklist_test_names = new ArrayList<String>(3);
		whitelist_test_names = new ArrayList<String>(3);
		php_unit_dists = new ArrayList<PhpUnitDist>(3);
		include_dirs = new ArrayList<String>(5);
		include_files = new ArrayList<String>(3);
		
		qctx = new QuercusContext();
		
		// add default entries to include_path
		addIncludeDirectory(".");
		addIncludeDirectory("C:\\php-sdk\\PFTT\\current\\cache\\util\\PEAR\\pear");
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
	 * @param path
	 * @param bootstrap_file
	 * @param include_files
	 * @return
	 */
	public PhpUnitDist addPhpUnitDist(String path, String bootstrap_file, String[] include_files) {
		PhpUnitDist dist = new PhpUnitDist(this, path, bootstrap_file, include_files);
		
		php_unit_dists.add(dist);
		
		return dist;
	}
	
	/** directory to add to the PHP include path.
	 * 
	 * all the classes all the tests need must be found either:
	 * 1. in the include path
	 * 2. loaded by the bootstrap
	 * 3. loaded as an include file
	 * 4. loaded by #preamble_code
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
		whitelist_test_names.add(test_name);
		
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
		blacklist_test_names.add(test_name);
		
		return this;
	}
	
	/** reads all the PhpUnitTestCases from this test-pack
	 * 
	 * @param test_cases
	 * @throws IOException
	 * @throws Exception
	 */
	public void read(List<PhpUnitTestCase> test_cases) throws IOException, Exception {
		// TODO if subdir used, only search within that
		for (PhpUnitDist php_unit_dist : php_unit_dists) {
			readDir(test_cases, php_unit_dist, php_unit_dist.path);
		}
		
		// alphabetize
		Collections.sort(test_cases, new Comparator<PhpUnitTestCase>() {
				@Override
				public int compare(PhpUnitTestCase a, PhpUnitTestCase b) {
					return a.getName().compareTo(b.getName());
				}
			});
	}
	
	/** scans for *Test.php files and reads PhpUnitTestCase(s) from them
	 * 
	 * @param test_cases
	 * @param php_unit_dist
	 * @param dir
	 * @throws IOException
	 */
	protected void readDir(List<PhpUnitTestCase> test_cases, PhpUnitDist php_unit_dist, File dir) throws IOException {
		File[] list_files = dir.listFiles();
		if (list_files==null)
			return;
		
		for ( File file : list_files ) {
			if (file.isDirectory()) {
				readDir(test_cases, php_unit_dist, file);
			} else if (file.getName().endsWith("Test.php")) {
				String file_name = Host.pathFrom(php_unit_dist.path.getAbsolutePath(), file.getAbsolutePath());
				
				String test_name = PhpUnitTestCase.normalizeName(file_name);
				
				if (blacklist_test_names.contains(test_name))
					continue;
				else if (!whitelist_test_names.isEmpty() && !whitelist_test_names.contains(test_name))
					continue;
				
				try {
					readTestFile(php_unit_dist, test_cases, file);
				} catch ( QuercusParseException ex ) {
					ex.printStackTrace();
				}
			}
		}
		
	} // end protected void readDir
	
	/** reads PhpUnitTestCase(s) from given PHP file
	 * 
	 * @param php_unit_dist
	 * @param test_cases
	 * @param file
	 * @throws IOException
	 */
	protected void readTestFile(PhpUnitDist php_unit_dist, List<PhpUnitTestCase> test_cases, File file) throws IOException {
		FileInputStream fin = new FileInputStream(file);
		QuercusParser p = new QuercusParser(qctx, new FilePath(file.getAbsolutePath()), new ReadStream(new FileReadStream(fin)));
		QuercusProgram prog = p.parse();
		
		// Expr that may be useful
		// FunIncludeOnceExpr => PHP include_once require_once
		// FunIncludeExpr => PHP include require
		// ImportExpr => PHP import
		
		// search file for classes
		for (InterpretedClassDef clazz : prog.getClasses() ) {
			for (Map.Entry<String, AbstractFunction> e : clazz.functionSet()) {
				// search class for functions that start with 'test'
				if (e.getValue().getName().startsWith("test")) {
					// this is a test case
					test_cases.add(new PhpUnitTestCase(
							php_unit_dist,
							file.getAbsolutePath(),
							// some PhpUnits use the namespace keyword and/or \\ in the class name (namespaces)
							// InterpretedclassDef#getName will provide the absolute class name (including namespace)
							// in such cases, so nothing special needs to be done here for them
							clazz.getName(),
							// name of method within the class
							e.getValue().getName()
						));
				}
			}
		}
		
		fin.close();
	} // end protected void readTestFile

	@Override
	public void cleanup(ConsoleManager cm) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getSourceDirectory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void read(List<PhpUnitTestCase> test_cases, ConsoleManager cm,
			ITestResultReceiver twriter, PhpBuild build)
			throws FileNotFoundException, IOException, Exception {
		// TODO Auto-generated method stub
		
		read(test_cases);
	}

	@Override
	public PhpUnitActiveTestPack installInPlace() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PhpUnitActiveTestPack installNamed(AHost host, String string,
			List<PhpUnitTestCase> test_cases) throws IllegalStateException,
			IOException, Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PhpUnitActiveTestPack install(ConsoleManager cm, AHost host,
			String local_test_pack_dir, String remote_test_pack_dir)
			throws IllegalStateException, IOException, Exception {
		// TODO Auto-generated method stub
		return null;
	}

	public void setRoot(String test_pack_root) {
		this.test_pack_root = test_pack_root;
	}

	public String getRoot() {
		return this.test_pack_root;
	}

	public void setPreambleCode(String preamble_code) {
		this.preamble_code = preamble_code;
	}
	
	public String getPreambleCode() {
		return preamble_code;
	}
	
	public abstract String getVersionString();
	public abstract boolean open(ConsoleManager cm, AHost host) throws Exception;
	
} // end public abstract class PhpUnitSourceTestPack
