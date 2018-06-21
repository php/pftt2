package com.mostc.pftt.model.app;

/** a PhpUnitTestCase
 * 
 * @see EPhpUnitTestStatus
 * @author Matt Ficken
 *
 */

public class PhpUnitTestCase extends AppUnitTestCase {
	public static final int MAX_TEST_TIME_SECONDS = 60;
	protected final PhpUnitDist php_unit_dist;
	protected final String className, methodName, dependsMethodName, dataProviderMethodName;
	protected final int arg_count;
	protected final boolean exception_expected;
	
	protected PhpUnitTestCase(PhpUnitDist php_unit_dist, String abs_filename, String rel_filename, String className, String methodName, int arg_count, String dataProviderMethodName, String dependsMethodName, boolean exception_expected) {
		super(rel_filename, abs_filename);
		this.php_unit_dist = php_unit_dist;
		// don't need to normalize classname:
		// if it has \ thats ok b/c its legal PHP (namespaces) whereas it won't be / b/c that's illegal in PHP
		this.className = className;
		this.methodName = methodName;
		this.arg_count = arg_count;
		this.dataProviderMethodName = dataProviderMethodName;
		this.dependsMethodName = dependsMethodName;
		this.exception_expected = exception_expected;
	}
	
	public boolean isExceptionExpected() {
		return exception_expected;
	}
	
	public String getDataProviderMethodName() {
		return dataProviderMethodName;
	}
	
	public String getDependsMethodName() {
		return dependsMethodName;
	}
	
	public int getArgCount() {
		return arg_count;
	}
	
	public PhpUnitDist getPhpUnitDist() {
		return php_unit_dist;
	}
	
	/** search file for classes that extend PhpUnit_Framework_TestCase
	 * 
	 * @return
	 */
	public String getClassName() {
		return className;
	}
	
	/** search class for all methods that start with 'test'
	 * 
	 * @return
	 */
	public String getMethodName() {
		return methodName;
	}
	
	
	
	public boolean isFileName(String... file_names) {
		for (String file_name : file_names) {
			if (isFileName(file_name))
				return true;
		}
		return false;
	}
	
	public boolean equals(String file_name, String class_name) {
		return isFileName(file_name) && this.className.equals(class_name);
	}
	
	public boolean equals(String file_name, String class_name, String method_name) {
		return equals(file_name, class_name) && this.methodName.equals(method_name);
	}

	@Override
	public String getName() {
		return className + "::" + methodName + "(" + rel_filename + ")";
	}
	
} // end public class PhpUnitTestCase
