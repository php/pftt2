package com.mostc.pftt.model.app;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.model.TestCase;
import com.mostc.pftt.scenario.FileSystemScenario;

public abstract class AppUnitTestCase extends TestCase {
	protected final String rel_filename, abs_filename;
	
	public AppUnitTestCase(String rel_filename, String abs_filename) {
		this.rel_filename = rel_filename;
		// don't need to call #normalizeFilename here usually. it's called in PhpUnitSourcetestPack#readDir...
		// calling it (again )here would be a performance hit
		this.abs_filename = abs_filename;
	}

	public boolean fileNameStartsWithAny(String[] ext_names) {
		return StringUtil.startsWithAnyIC(getFileName(), ext_names);
	}
	
	/** name of file.
	 * 
	 * file is case preserved to avoid breaking posix.
	 * 
	 * windows slashes \\ are standardized to / posix.
	 * 
	 * for case-insensitive matching @see #isFileName or @see #fileNameStartsWithAny
	 * 
	 * @return
	 */
	public String getFileName() {
		return rel_filename;
	}
	
	public String getAbsoluteFileName() {
		return abs_filename;
	}
	
	public boolean isFileName(String file_name) {
		return this.rel_filename.toLowerCase().equals(file_name.toLowerCase()) 
				|| this.abs_filename.toLowerCase().equals(file_name.toLowerCase());
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o==this)
			return true;
		else if (o instanceof AppUnitTestCase) 
			return o.toString().equals(this.toString());
		else
			return false;
	}
	
	@Override
	public int hashCode() {
		return getName().hashCode();
	}
	
	/** fixes slashes to Posix forward slash /.
	 * 
	 * this is case-preserving (to avoid breaking on posix). case is not changed.
	 * 
	 * to do case-insenstive matching @see #isFileName
	 * 
	 * @param test_name
	 * @return
	 */
	public static String normalizeFileName(String test_name) {
		return FileSystemScenario.toUnixPath(test_name);
	}
	
} // end public abstract class AppUnitTestCase
