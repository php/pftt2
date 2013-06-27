package com.mostc.pftt.model.app;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;

/** represents a PhpUnit.xml.dist file
 * 
 * @author Matt Ficken
 *
 */

public class PhpUnitDist {
	protected final PhpUnitSourceTestPack src_test_pack;
	/** optional: a PHP file to load at the start of every PhpUnit test.
	 * 
	 * Often, this will be an autoloader that loads all the required include files so you don't have to specify them here.
	 * 
	 * copy this from the bootstrap= attribute in PhpUnit.xml.dist
	 * */
	protected final File bootstrap_file;
	/** required: the base directory for tests. this is scanned for *Test.php files unless subdirectories are provided.
	 * 
	 * if subdirectories are provided, they are assumed to be inside this directory.
	 * 
	 * copy this from the dir= attribute in PhpUnit.xml.dist
	 * 
	 * @see #addSubDir
	 */
	protected final File path;
	protected ArrayList<String> include_files;
	protected ArrayList<String> subdirs;
	protected Map<String,String> globals, constants;
	
	protected PhpUnitDist(PhpUnitSourceTestPack src_test_pack, String path, String bootstrap_file, String[] include_files) {
		this.src_test_pack = src_test_pack;
		this.path = new File(path);
		this.bootstrap_file = StringUtil.isEmpty(bootstrap_file) ? null : new File(bootstrap_file);
	}
	
	public Map<String,String> getConstants() {
		return constants;
	}
	
	public Map<String,String> getGlobals() {
		return globals;
	}
	
	public List<String> getSubDirs() {
		return subdirs;
	}
	
	public File getPath() {
		return path;
	}
	
	public File getBootstrapFile() {
		return bootstrap_file;
	}
	
	public PhpUnitSourceTestPack getSourceTestPack() {
		return src_test_pack;
	}
	
	String[] _include_files;
	public String[] getIncludeFiles() {
		if (_include_files!=null)
			return _include_files;
		else if (include_files==null||include_files.isEmpty())
			return src_test_pack.include_files.toArray(new String[src_test_pack.include_files.size()]);
		
		ArrayList<String> merged = new ArrayList<String>(include_files.size() + src_test_pack.include_files.size());
		merged.addAll(src_test_pack.include_files);
		merged.addAll(include_files);
		return _include_files = merged.toArray(new String[merged.size()]);
	}
	
	String _include_path;
	public String getIncludePath(AHost host) {
		if (_include_path!=null)
			return _include_path;
		
		String pear_path = host.joinIntoOnePath(host.getPfttDir(), "/cache/util/PEAR/pear");
		
		return _include_path = host.joinMultiplePaths(host.joinMultiplePaths(src_test_pack.include_dirs, pear_path), path.getAbsolutePath());
	}
	
	/** add an included file.
	 * 
	 * copy this from the &gt;file&lt; tags from the XML file.
	 * 
	 * these will be added to require_once statements in the PhpUnit test case.
	 * 
	 * @param file
	 * @return
	 */
	public PhpUnitDist addIncludeFile(String file) {
		if (include_files==null)
			include_files = new ArrayList<String>(5);
		
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
	public PhpUnitDist addWhitelist(String test_name) {
		src_test_pack.addWhitelist(test_name);
		
		return this;
	}
	
	/** adds tests to blacklist. no matching test will be run
	 * 
	 * copy this from the &gt;blacklist&lt; tag in PhpUnit.xml.dist
	 * 
	 * @param test_name
	 * @return
	 */
	public PhpUnitDist addBlacklist(String test_name) {
		src_test_pack.addBlacklist(test_name);
		
		return this;
	}
	
	/** provide a subdirectory. if any subdirectories are provided, those are the ONLY directories
	 * that are scanned for *Test.php files (which are in turn scanned for PhpUnit tests)
	 * 
	 * these directories must be inside the #path directory
	 * 
	 * copy this from the &gt;subdir&lt; tag in PhpUnit.xml.dist
	 * 
	 * @param dir
	 * @return
	 */
	public PhpUnitDist addSubDir(String dir) {
		if (subdirs==null)
			subdirs = new ArrayList<String>(5);
		
		subdirs.add(dir);
		
		return this;
	}
	
	/** adds a PHP constant to run with the tests (name and value to pass to PHP's define() function)
	 * 
	 * copy this from the &gt;const&lt; tag in PhpUnit.xml.dist
	 * 
	 * @param name
	 * @param value
	 */
	public PhpUnitDist addConstant(String name, String value) {
		if (constants==null)
			constants = new HashMap<String,String>(5);
		
		constants.put(name, value);
		
		return this;
	}
	
	/** adds a global variable to define in $GLOBALS in the PhpUnit test
	 * 
	 * copy this from the &gt;var&lt; tag in PhpUnit.xml.dist
	 * 
	 * @param name
	 * @param value
	 */
	public PhpUnitDist addVar(String name, String value) {
		if (globals==null)
			globals = new HashMap<String,String>(5);
		
		globals.put(name, value);
		
		return this;
	}
	
} // end public class PhpUnitDist
