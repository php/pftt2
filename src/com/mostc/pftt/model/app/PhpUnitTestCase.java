package com.mostc.pftt.model.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.TestCase;

/** a PhpUnitTestCase
 * 
 * @see EPhpUnitTestStatus
 * @author Matt Ficken
 *
 */

public class PhpUnitTestCase extends TestCase {
	public final PhpUnitDist php_unit_dist;
	// name of file
	public final String filename;
	// search file for classes that extend PhpUnit_Framework_TestCase
	public final String className;
	// search class for all methods that start with 'test'
	public final String methodName;
	
	protected PhpUnitTestCase(PhpUnitDist php_unit_dist, String filename, String className, String methodName) {
		this.php_unit_dist = php_unit_dist;
		// don't need to call #normalizeFilename here usually. it's called in PhpUnitSourcetestPack#readDir...
		// calling it (again )here would be a performance hit
		this.filename = filename;
		// don't need to normalize classname:
		// if it has \ thats ok b/c its legal PHP (namespaces) whereas it won't be / b/c that's illegal in PHP
		this.className = className;
		this.methodName = methodName;
	}

	@Override
	public String getName() {
		return className + "#" + methodName + "(" + filename + ")";
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	public static String normalizeFileName(String test_name) {
		return Host.toUnixPath(test_name).toLowerCase();
	}

} // end public class PhpUnitTestCase
