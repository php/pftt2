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
		this.filename = filename;
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

	public static String normalizeName(String test_name) {
		return Host.toUnixPath(test_name).toLowerCase();
	}

} // end public class PhpUnitTestCase
