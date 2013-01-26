package com.mostc.pftt.model.phpt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.model.SourceTestPack;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.IPhptTestResultReceiver;
import com.mostc.pftt.results.PhptResultPackWriter;
import com.mostc.pftt.util.StringUtil;

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
	protected LinkedList<File> non_phpt_files;
	protected HashMap<String,PhptTestCase> test_cases_by_name;
	
	public PhptSourceTestPack(String test_pack) {
		this.test_pack = test_pack;
		
		test_cases_by_name = new HashMap<String,PhptTestCase>();
		non_phpt_files = new LinkedList<File>();
	}
	
	@Override
	public String toString() {
		return getSourceDirectory();
	}
	
	public boolean open(ConsoleManager cm, Host host) {
		if (StringUtil.endsWithIC(this.test_pack, ".zip")) {
			// automatically decompress build
			String zip_file = test_pack;
			this.test_pack = host.uniqueNameFromBase(Host.removeFileExt(test_pack));
				
			if (!host.unzip(cm, zip_file, test_pack))
				return false;
		}
		
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
		host.deleteIfExists(test_pack+"/ext/standard/tests/file/windows_links/mklink_junction");
		host.deleteIfExists(test_pack+"/ext/standard/tests/file/windows_links/directory");
		host.deleteIfExists(test_pack+"/ext/standard/tests/file/windows_links/mounted_volume");
		host.deleteIfExists(test_pack+"/ext/standard/tests/file/windows_links/mnt");
		host.deleteIfExists(test_pack+"/tests/security/globtest1");
		host.deleteIfExists(test_pack+"/tests/security/globtest2");
	}
	
	public void read(List<PhptTestCase> test_cases, List<String> names, ConsoleManager cm, PhptResultPackWriter twriter, PhpBuild build) throws FileNotFoundException, IOException, Exception {
		read(test_cases, names, cm, twriter, build, false);
	}
	
	public void read(List<PhptTestCase> test_cases, List<String> names, ConsoleManager cm, PhptResultPackWriter twriter, PhpBuild build, boolean ignore_missing) throws FileNotFoundException, IOException, Exception {
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
					
					add_test_case(test_case, test_cases, names, cm, twriter, build, null, redirect_targets);
					
					// don't need to search for it
					name_it.remove();
				}
			}
		}
		
		if (names.size() > 0) {
			// assume any remaining names are name fragments and search for tests with matching names
			
			add_test_files(test_pack_file.listFiles(), test_cases, names, cm, twriter, build, null, redirect_targets);
		}
		
		if (!ignore_missing && names.size() > 0) {
			// one or more test names not matched to an actual test
			throw new FileNotFoundException(names.toString());
		}

		// sort alphabetically
		Collections.sort(test_cases, new Comparator<PhptTestCase>() {
				@Override
				public int compare(PhptTestCase o1, PhptTestCase o2) {
					return o2.getName().compareTo(o2.getName());
				}
			});
		
		twriter.setTotalCount(test_cases.size());
	}

	public void read(List<PhptTestCase> test_files, ConsoleManager cm, IPhptTestResultReceiver twriter, PhpBuild build) throws FileNotFoundException, IOException, Exception {
		test_pack_file = new File(test_pack);
		test_pack = test_pack_file.getAbsolutePath(); // normalize path
		add_test_files(test_pack_file.listFiles(), test_files, null, cm, twriter, build, null, new LinkedList<PhptTestCase>());
		twriter.setTotalCount(test_files.size());
	}
	
	private void add_test_files(File[] files, List<PhptTestCase> test_files, List<String> names, ConsoleManager cm, IPhptTestResultReceiver twriter, PhpBuild build, PhptTestCase redirect_parent, List<PhptTestCase> redirect_targets) throws FileNotFoundException, IOException, Exception {
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
				
				PhptTestCase test_case = PhptTestCase.load(host, this, false, test_name, twriter, redirect_parent);
				
				add_test_case(test_case, test_files, names, cm, twriter, build, redirect_parent, redirect_targets);
			} else if (f.isFile()) {
				String n = f.getName().toLowerCase();
				if (!(n.endsWith(".sh") && n.endsWith(".php") && n.endsWith(".diff") && n.endsWith(".out") && n.endsWith(".exp") && n.endsWith(".cmd") && n.endsWith(".stdin"))) {
					// ignore these files. they may be left over if the user ran run-test.php or PFTT and aren't actually used for testing
					// 
					// test files we need are usually .inc but may also be .db... may be others (especially in future)
					// have to copy them all just in case they are needed
					if (!non_phpt_files.contains(f))
						non_phpt_files.add(f);
				}
			}
			add_test_files(f.listFiles(), test_files, names, cm, twriter, build, redirect_parent, redirect_targets);
		}
	}
	
	private void add_test_case(PhptTestCase test_case, List<PhptTestCase> test_files, List<String> names, ConsoleManager cm, IPhptTestResultReceiver twriter, PhpBuild build, PhptTestCase redirect_parent, List<PhptTestCase> redirect_targets) throws FileNotFoundException, IOException, Exception {
		test_cases_by_name.put(test_case.getName(), test_case);
		
		if (test_case.containsSection(EPhptSection.REDIRECTTEST)) {
			if (build==null || redirect_parent!=null) {
				// ignore the test
			} else {
				// execute php code in the REDIRECTTEST section to get the test(s) to load
				for ( String target_test_name : test_case.readRedirectTestNames(cm, host, build) ) {
					
					// test may actually be a directory => load all the PHPT tests from that directory
					File dir = new File(test_pack+host.dirSeparator()+target_test_name);
					if (dir.isDirectory()) {
						// add all PHPTs in directory 
						add_test_files(dir.listFiles(), test_files, names, cm, twriter, build, redirect_parent, redirect_targets);
						
					} else {
						// test refers to a specific test, load it
						test_case = PhptTestCase.load(host, this, false, target_test_name, twriter, redirect_parent);
						
						if (redirect_targets.contains(test_case))
							// can only have 1 level of redirection
							return;
						test_cases_by_name.put(test_case.getName(), test_case);
						
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
	
	/** returns the contents of the named file from this test pack
	 * 
	 * @param host
	 * @param name
	 * @return
	 * @throws IOException
	 */
	public String getContents(Host host, String name) throws IOException {
		return host.getContentsDetectCharset(new File(test_pack_file, name).getAbsolutePath(), PhptTestCase.newCharsetDeciderDecoder());
	}
	
	/** installs the test-pack in its source location (so its not copied or uploaded or downloaded anywhere)
	 * 
	 * @return
	 */
	public PhptActiveTestPack installInPlace() {
		return new PhptActiveTestPack(this.getSourceDirectory());
	}

	/** installs this test pack into to the given location to actively run it from.
	 * 
	 * location can be local or remote.
	 * 
	 * @see #read - must have already been called
	 * @param cm
	 * @param host
	 * @param test_pack_dir
	 * @return
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws Exception
	 */
	public PhptActiveTestPack install(ConsoleManager cm, Host host, String test_pack_dir) throws IllegalStateException, IOException, Exception {
		if (!this.host.isRemote() || this.host.equals(host)) {
			// installing from local host to remote host OR from remote host to itself
			host.uploadCompressWith7Zip(cm, getClass(), test_pack, host, test_pack_dir);
		} else if (!host.isRemote()) {
			// installing from remote host to local host
			host.download7ZipAndDecompress(cm, getClass(), test_pack, this.host, test_pack_dir);
		} else {
			// installing from 1 remote host(src) to a different remote host (dst)
			LocalHost local_host = new LocalHost();
			
			// decide file names
			String local_7zip_file = local_host.mktempname(getClass(), ".7z");
			String src_7zip_file = this.host.mktempname(getClass(), ".7z");
			String dst_7zip_file = host.mktempname(getClass(), ".7z");
			
			// compress and download/upload and decompress
			this.host.compress(cm, host, test_pack, src_7zip_file);
			this.host.download(src_7zip_file, local_7zip_file);
			host.upload(local_7zip_file, dst_7zip_file);
			host.decompress(cm, this.host, dst_7zip_file, test_pack_dir);
			
			// cleanup
			this.host.delete(dst_7zip_file);
			host.delete(src_7zip_file);
			local_host.delete(local_7zip_file);
		}
		return new PhptActiveTestPack(test_pack_dir);
	} // end public PhptActiveTestPack install
	
	/** installs only the named test cases from test pack into to the given location to actively run it from.
	 * 
	 * @see #read - must have already been called
	 * @param host
	 * @param test_pack_dir
	 * @param test_cases
	 * @return
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws Exception
	 */
	public PhptActiveTestPack installNamed(Host host, String test_pack_dir, List<PhptTestCase> test_cases) throws IllegalStateException, IOException, Exception {
		if (!this.host.isRemote() || this.host.equals(host)) {
			// installing from local host to remote host OR from remote host to itself
			uploadNonTestCaseFiles(host, test_pack, test_pack_dir);
			for ( PhptTestCase test_case : test_cases )
				host.upload(test_pack+"/"+test_case.getName(), test_pack_dir+"/"+test_case.getName());
		} else if (!host.isRemote()) {
			// installing from remote host to local host
			downloadNonTestCaseFiles(host, test_pack, test_pack_dir);
			for ( PhptTestCase test_case : test_cases )
				host.download(test_pack+"/"+test_case.getName(), test_pack_dir+"/"+test_case.getName());
		} else {
			// installing from 1 remote host to a different remote host
			LocalHost local_host = new LocalHost();
			String local_dir = local_host.mktempname(getClass());
			downloadNonTestCaseFiles(this.host, test_pack, test_pack_dir);
			for ( PhptTestCase test_case : test_cases )
				this.host.download(test_pack+"/"+test_case.getName(), local_dir+"/"+test_case.getName());
			uploadNonTestCaseFiles(host, local_dir, test_pack_dir);
			for ( PhptTestCase test_case : test_cases )
				host.upload(local_dir+"/"+test_case.getName(), test_pack_dir+"/"+test_case.getName());
			local_host.delete(local_dir);
		}
		return new PhptActiveTestPack(test_pack_dir);
	}

	protected void uploadNonTestCaseFiles(Host dst_host, String src_dir, String dst_dir) throws IllegalStateException, IOException, Exception {
		String remote_name;
		for ( File f : non_phpt_files ) {
			remote_name = Host.pathFrom(test_pack, f.getAbsolutePath());
			
			dst_host.upload(f.getAbsolutePath(), dst_dir+"/"+remote_name);
		}
	}

	protected void downloadNonTestCaseFiles(Host dst_host, String src_dir, String dst_dir) throws IllegalStateException, IOException, Exception {
		String remote_name;
		for ( File f : non_phpt_files ) {
			remote_name = Host.pathFrom(test_pack, f.getAbsolutePath());
			
			dst_host.download(src_dir+"/"+remote_name, dst_dir+"/"+remote_name);
		}
	}

	/** gets the branch of this test-pack
	 * 
	 * @return
	 */
	public EBuildBranch getVersionBranch() {
		String dir = Host.basename(test_pack);
		if (dir.contains("5.4")||dir.contains("5-4")||dir.contains("5_4")||dir.contains("54"))
			return EBuildBranch.PHP_5_4;
		else if (dir.contains("5.3")||dir.contains("5-3")||dir.contains("5_3")||dir.contains("53"))
			return EBuildBranch.PHP_5_3;
		else if (dir.contains("5.5")||dir.contains("5-5")||dir.contains("5_5")||dir.contains("55"))
			return EBuildBranch.PHP_5_5;
		else if (dir.contains("5.6")||dir.contains("5-6")||dir.contains("5_6")||dir.contains("56"))
			return EBuildBranch.PHP_5_6;
		else if (dir.toLowerCase().contains("master"))
			return EBuildBranch.MASTER;
		else
			return null;
	}

	/** gets the revision number of this test-pack
	 * 
	 * @return
	 */
	public String getVersion() {
		String[] split = Host.basename(test_pack).split("[\\.|\\-]");
		return split.length==0?null:split[split.length-1];
	}

	/** returns test case by name
	 * 
	 * @see #read - must have already been called
	 * @param name
	 * @return
	 */
	public PhptTestCase getByName(String name) {
		PhptTestCase test_case = test_cases_by_name.get(name);
		if (test_case!=null)
			return test_case;
		name = name.toLowerCase();
		test_case = test_cases_by_name.get(name);
		if (test_case!=null)
			return test_case;
		return test_cases_by_name.get(name+".phpt");
	}
	
} // end public class PhptSourceTestPack
