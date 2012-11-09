package com.mostc.pftt.model.phpt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.model.SourceTestPack;
import com.mostc.pftt.telemetry.PhptTelemetryWriter;

/** manages a test-pack of PHPT tests
 * 
 * @author Matt Ficken
 *
 */

public class PhptSourceTestPack extends SourceTestPack {
	// CRITICAL: on Windows, must use \\ not /
	//    -some tests fail b/c the path to php will have / in it, which it can't execute via `shell_exec`
	protected String test_pack;
	protected File test_pack_file;
	protected Host host;
	
	public PhptSourceTestPack(String test_pack) {
		this.test_pack = test_pack;
	}
	
	@Override
	public String toString() {
		return getSourceDirectory();
	}
	
	public boolean open(Host host) {
		this.host = host;
		this.test_pack = host.fixPath(test_pack);
		return host.exists(this.test_pack);
	}
	
	public String getSourceDirectory() {
		return test_pack;
	}
	
	/** cleans up this test-pack from previous runs of PFTT or run-test.php that were interrupted
	 * 
	 */
	public void cleanup() {
		// these are symlinks(junctions) which may cause an infinite loop
		//
		// normally, they are deleted, but if certain tests were interrupted, they may still be there
		host.deleteIfExists("ext/standard/tests/file/windows_links/mklink_junction");
		host.deleteIfExists("ext/standard/tests/file/windows_links/directory");
		host.deleteIfExists("ext/standard/tests/file/windows_links/mounted_volume");
		host.deleteIfExists("ext/standard/tests/file/windows_links/mnt");
	}
	
	public void add_named_tests(List<PhptTestCase> test_files, List<String> names, PhptTelemetryWriter twriter, PhpBuild build) throws FileNotFoundException, IOException, Exception {
		add_named_tests(test_files, names, twriter, build, false);
	}
	
	// TODO rename this - always call before (only once) before using PhptTestPack
	public void add_named_tests(List<PhptTestCase> test_files, List<String> names, PhptTelemetryWriter twriter, PhpBuild build, boolean ignore_missing) throws FileNotFoundException, IOException, Exception {
		test_pack_file = new File(test_pack);
		test_pack = test_pack_file.getAbsolutePath(); // normalize path
		
		LinkedList<PhptTestCase> redirect_targets = new LinkedList<PhptTestCase>();
		
		Iterator<String> name_it = names.iterator();
		String name;
		File file;
		PhptTestCase test_case;
		while (name_it.hasNext()) {
			name = name_it.next();
			
			if (name.endsWith(PhptTestCase.PHPT_FILE_EXTENSION)) {
				file = new File(test_pack_file, host.fixPath(name));
				if (file.exists()) {
					// String is exact name of test
					
					test_case = PhptTestCase.load(host, this, name, twriter);
					
					add_test_case(test_case, test_files, names, twriter, build, null, redirect_targets);
					
					// don't need to search for it
					name_it.remove();
				}
			}
		}
		
		if (names.size() > 0) {
			// assume any remaining names are name fragments and search for tests with matching names
			
			add_test_files(test_pack_file.listFiles(), test_files, names, twriter, build, null, redirect_targets);
		}
		
		if (!ignore_missing && names.size() > 0) {
			// one or more test names not matched to an actual test
			throw new FileNotFoundException(names.toString());
		}

		// sort alphabetically
		Collections.sort(test_files, new Comparator<PhptTestCase>() {
				@Override
				public int compare(PhptTestCase o1, PhptTestCase o2) {
					return o2.getName().compareTo(o2.getName());
				}
			});
		
		twriter.setTotalCount(test_files.size());
	}

	public void add_test_files(List<PhptTestCase> test_files, PhptTelemetryWriter twriter, PhpBuild build) throws FileNotFoundException, IOException, Exception {
		test_pack_file = new File(test_pack);
		test_pack = test_pack_file.getAbsolutePath(); // normalize path
		add_test_files(test_pack_file.listFiles(), test_files, null, twriter, build, null, new LinkedList<PhptTestCase>());
		twriter.setTotalCount(test_files.size());
	}
	
	private void add_test_files(File[] files, List<PhptTestCase> test_files, List<String> names, PhptTelemetryWriter twriter, PhpBuild build, PhptTestCase redirect_parent, List<PhptTestCase> redirect_targets) throws FileNotFoundException, IOException, Exception {
		if (files==null)
			return;
		main_loop:
		for ( File f : files ) {
			if (f.getName().toLowerCase().endsWith(PhptTestCase.PHPT_FILE_EXTENSION)) {
				if (names!=null) {
					for(String name: names) {
						if (f.getName().toLowerCase().contains(name))
							break;
					}
					// test doesn't match any name, ignore it
					continue main_loop;
				}
					
				String test_name = f.getAbsolutePath().substring(test_pack.length());
				if (test_name.startsWith("/") || test_name.startsWith("\\"))
					test_name = test_name.substring(1);
				
//				if (test_name.contains("a_dir"))
//					continue; // TODO
				
				PhptTestCase test_case = PhptTestCase.load(host, this, false, test_name, twriter, redirect_parent);
				
				add_test_case(test_case, test_files, names, twriter, build, redirect_parent, redirect_targets);
			}
			add_test_files(f.listFiles(), test_files, names, twriter, build, redirect_parent, redirect_targets);
		}
	}
	
	private void add_test_case(PhptTestCase test_case, List<PhptTestCase> test_files, List<String> names, PhptTelemetryWriter twriter, PhpBuild build, PhptTestCase redirect_parent, List<PhptTestCase> redirect_targets) throws FileNotFoundException, IOException, Exception {
		if (test_case.containsSection(EPhptSection.REDIRECTTEST)) {
			if (build==null || redirect_parent!=null) {
				// ignore the test
			} else {
				// execute php code in the REDIRECTTEST section to get the test(s) to load
				for ( String target_test_name : test_case.readRedirectTestNames(twriter.getConsoleManager(), host, build) ) {
					
					// test may actually be a directory => load all the PHPT tests from that directory
					File dir = new File(test_pack+host.dirSeparator()+target_test_name);
					if (dir.isDirectory()) {
						// add all PHPTs in directory 
						add_test_files(dir.listFiles(), test_files, names, twriter, build, redirect_parent, redirect_targets);
						
					} else {
						// test refers to a specific test, try to load it
						//try {
							test_case = PhptTestCase.load(host, this, false, target_test_name, twriter, redirect_parent);
						/*} catch ( Exception ex ) {
							ex.printStackTrace(); // TODO temp
						}*/
						
						if (redirect_targets.contains(test_case))
							// can only have 1 level of redirection
							return;
						redirect_targets.add(test_case);
						
						test_files.add(test_case);
					}
				}
			}
		} else {
			if (redirect_parent!=null) {
				if (redirect_targets.contains(test_case))
					return;
				// can only have 1 level of redirection
				redirect_targets.add(test_case);
			}
			
			test_files.add(test_case);
		}
	}
	
	public String getContents(Host host, String name) throws IOException {
		return host.getContentsDetectCharset(new File(test_pack_file, name).getAbsolutePath(), PhptTestCase.newCharsetDeciderDecoder());
	}

	public PhptActiveTestPack install(Host host, String test_pack_dir) throws IllegalStateException, IOException, Exception {
		if (!this.host.isRemote() || this.host.equals(host)) {
			// installing from local host to remote host OR from remote host to itself
			host.upload(test_pack, test_pack_dir);
		} else if (!host.isRemote()) {
			// installing from remote host to local host
			host.download(test_pack, test_pack_dir);
		} else {
			// installing from 1 remote host to a different remote host
			LocalHost local_host = new LocalHost();
			String local_dir = local_host.mktempname("PhptTestPack");
			this.host.download(test_pack, local_dir);
			host.upload(local_dir, test_pack_dir);
			local_host.delete(local_dir);
		}
		return new PhptActiveTestPack(test_pack_dir);
	}
	
} // end public class PhptSourceTestPack
