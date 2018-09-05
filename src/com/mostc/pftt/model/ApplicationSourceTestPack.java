package com.mostc.pftt.model;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nullable;

import com.github.mattficken.Overridable;
import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.model.core.EBuildBranch;
import com.mostc.pftt.results.ConsoleManager;
//TODO import com.mostc.pftt.scenario.AzureWebsitesScenario;
import com.mostc.pftt.scenario.FileSystemScenario;
import com.mostc.pftt.scenario.SAPIScenario;

public abstract class ApplicationSourceTestPack<A extends ActiveTestPack, T extends TestCase> extends SourceTestPack<A,T> {
	protected String test_pack_root;
	
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
	
	/** the base directory within the PFTT directory to find the test case files and required php files
	 * 
	 * Typically, test-packs will call #ensureAppDecompressed
	 * 
	 * @param cm
	 * @param host - determine the absolute path on this host
	 * @see AHost#getPfttDir
	 * @return
	 */
	protected abstract String getSourceRoot(ConsoleManager cm, AHost host);
	
	protected void doInstallInPlace(ConsoleManager cm, AHost host) throws IOException, Exception {
		final String src_root = getSourceRoot(cm, LocalHost.getInstance());
		if (!new File(src_root).isDirectory()) {
			throw new IOException("source-test-pack not found: "+src_root);
		}
		setRoot(src_root);
		
		openAfterInstall(cm, host);
	}
	
	protected void doInstallNamed(ConsoleManager cm, AHost host) throws IOException, Exception {
		final String src_root = getSourceRoot(cm, LocalHost.getInstance());
		if (!new File(src_root).isDirectory()) {
			throw new IOException("source-test-pack not found: "+src_root);
		}
		setRoot(src_root);
		
		openAfterInstall(cm, host);
	}
	
	protected void doInstall(SAPIScenario sapi_scenario, ConsoleManager cm, AHost host, String local_test_pack_dir, String remote_test_pack_dir) throws IOException, Exception {
		LocalHost local_host = LocalHost.getInstance();
		final String src_root = getSourceRoot(cm, local_host);
		if (!new File(src_root).isDirectory()) {
			throw new IOException("source-test-pack not found: "+src_root);
		}
		
		// using #uploadCompressWith7Zip instead of just #upload makes a huge difference
		// for PhpUnit test-packs because of the large number of small files that have to be uploaded
		// TODO temp azure host.uploadCompressWith7Zip(cm, getClass(), src_root, local_host, remote_test_pack_dir);
		System.out.println("71 "+local_test_pack_dir);
		//System.exit(0);
		
		if (false /* TODO AzureWebsitesScenario.check(sapi_scenario) */) {
			setRoot("D:\\HOME\\SITE\\WWWROOT\\"+FileSystemScenario.basename(getName()).replace("-12.3", "").replace("-1.20.2", ""));//MEDIAWIKI");//local_test_pack_dir);
		} else {
			setRoot(src_root);// TODO temp azure ?? local_test_pack_dir);
		}
		
		openAfterInstall(cm, local_host);
	}
	
	private boolean decompressed = false;
	protected void ensureAppDecompressed(ConsoleManager cm, AHost host, String zip7_file) throws IllegalStateException, IOException, Exception {
		if (decompressed)
			return;
		decompressed = true;
		if (!StringUtil.endsWithIC(zip7_file, ".7z"))
			zip7_file += ".7z";
		
		// TODO temp azure host.decompress(cm, host, host.getPfttDir()+"/app/"+zip7_file, host.getPfttDir()+"/cache/working/");
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
	
	public String getName() {
		return getNameAndVersionString();
	}
	
	/** file path to test-pack */
	public void setRoot(String test_pack_root) {
		this.test_pack_root = test_pack_root;
	}
	@Override
	public String getSourceDirectory() {
		return getRoot();
	}
	/** file path to test-pack */
	public String getRoot() {
		return this.test_pack_root;
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
	
	@Override
	public String toString() {
		return getName();
	}
	
} // end public abstract class ApplicationSourceTestPack
 