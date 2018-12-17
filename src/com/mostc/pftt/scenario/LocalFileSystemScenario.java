package com.mostc.pftt.scenario;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.List;

import com.github.mattficken.io.ByLineReader;
import com.github.mattficken.io.CharsetDeciderDecoder;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.model.ActiveTestPack;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;

/** Placeholder scenario for storing a PHP build and its test pack on the local 
 * file system as opposed to remotely somewhere.
 * 
 * @author Matt Ficken
 *
 */

public class LocalFileSystemScenario extends FileSystemScenario {
	protected ITestPackStorageDir LOCAL_DIR = new AbstractTestPackStorageDir() {
			@Override
			public boolean notifyTestPackInstalled(ConsoleManager cm, AHost host) {
				return true;
			}
			@Override
			public boolean isRunning() {
				return true;
			}
			@Override
			public String getLocalPath(AHost host) {
				return host.getJobWorkDir();
			}
			@Override
			public String getRemotePath(AHost host) {
				return host.getJobWorkDir();
			}
			@Override
			public boolean closeIfEmpty(ConsoleManager cm, AHost local_host, ActiveTestPack active_test_pack) {
				try {
					return active_test_pack==null?false:local_host.mDeleteElevated(active_test_pack.getStorageDirectory());
				} catch (Exception ex) {
					cm.addGlobalException(EPrintType.CLUE, LocalFileSystemScenario.class, "disposeIfEmpty", ex, "can't delete active test pack");
					return false;
				}
			}
			@Override
			public boolean closeForce(ConsoleManager cm, AHost local_host, ActiveTestPack active_test_pack) {
				return true; // TODO temp azure active_test_pack==null?false:local_host.mDeleteIfExistsElevated(active_test_pack.getStorageDirectory());
			}
			@Override
			public String getNameWithVersionInfo() {
				return getName();
			}
			@Override
			public String getName() {
				return LocalFileSystemScenario.this.getName();
			}
		};
	//
	protected final AHost host;
		
	public LocalFileSystemScenario(AHost host) {
		this.host = host;
	}
	
	@Override
	public boolean setupRequired(EScenarioSetPermutationLayer layer) {
		// not a placeholder, but no setup is needed
		return false;
	}
	
	@Override
	public String getName() {
		return "Local-FileSystem";
	}
	
	@Override
	public boolean isImplemented() {
		return true;
	}
	
	@Override
	public boolean isRemote() {
		return false;
	}
	
	@Override
	public ITestPackStorageDir setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		try {
			host.mCreateDirs(host.getJobWorkDir());
			return LOCAL_DIR;
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, LocalFileSystemScenario.class, "createStorageDir", ex, "");
			return null;
		}
	}

	@Override
	public boolean isDirectory(String path) {
		return host.mIsDirectory(path);
	}

	@Override
	public boolean exists(String path) {
		return host.mExists(path);
	}

	@Override
	public boolean createDirs(String path) throws IllegalStateException, IOException {
		return host.mCreateDirs(path);
	}

	@Override
	public String getTempDir() {
		return host.getTempDir();
	}

	@Override
	public boolean saveTextFile(String path, String string) throws IllegalStateException, IOException {
		return host.mSaveTextFile(path, string);
	}

	@Override
	public boolean saveTextFile(String filename, String text, CharsetEncoder ce) throws IllegalStateException, IOException {
		return host.mSaveTextFile(filename, text, ce);
	}

	@Override
	public boolean delete(String file) throws IllegalStateException, IOException {
		return host.mDelete(file);
	}

	@Override
	public boolean deleteElevated(String file) throws IllegalStateException, IOException {
		return host.mDeleteElevated(file);
	}

	@Override
	public boolean copy(String src, String dst) throws IllegalStateException, Exception {
		return host.mCopy(src, dst);
	}

	@Override
	public boolean copyElevated(String src, String dst) throws IllegalStateException, Exception {
		return host.mCopyElevated(src, dst);
	}

	@Override
	public boolean move(String src, String dst) throws IllegalStateException, Exception {
		return host.mMove(src, dst);
	}

	@Override
	public boolean moveElevated(String src, String dst) throws IllegalStateException, Exception {
		return host.mMoveElevated(src, dst);
	}

	@Override
	public String dirSeparator() {
		return host.mDirSeparator();
	}
	
	@Override
	public String pathsSeparator() {
		return host.mPathsSeparator();
	}

	@Override
	public boolean deleteChosenFiles(String dir, IFileChooser chr) {
		return host.mDeleteChosenFiles(dir, chr);
	}

	@Override
	public boolean saveFile(String stdin_file, byte[] stdin_post) throws IllegalStateException, IOException {
		return host.mSaveFile(stdin_file, stdin_post);
	}

	@Override
	public ByLineReader readFile(String file) throws FileNotFoundException, IOException {
		return host.mReadFile(file);
	}

	@Override
	public ByLineReader readFile(String file, Charset cs) throws IllegalStateException, FileNotFoundException, IOException {
		return host.mReadFile(file, cs);
	}

	@Override
	public ByLineReader readFileDetectCharset(String file, CharsetDeciderDecoder cdd) throws FileNotFoundException, IOException {
		return host.mReadFileDetectCharset(file, cdd);
	}

	@Override
	public String getContents(String file) throws IOException {
		return host.mGetContents(file);
	}

	@Override
	public String getContentsDetectCharset(String file, CharsetDeciderDecoder cdd) throws IOException {
		return host.mGetContentsDetectCharset(file, cdd);
	}

	@Override
	public boolean dirContainsExact(String path, String name) {
		return host.mDirContainsExact(path, name);
	}

	@Override
	public boolean dirContainsFragment(String path, String name_fragment) {
		return host.mDirContainsFragment(path, name_fragment);
	}

	@Override
	public String[] list(String path) {
		return host.mList(path);
	}

	@Override
	public long getSize(String file) {
		return host.mSize(file);
	}

	@Override
	public long getMTime(String file) {
		return host.mMTime(file);
	}

	@Override
	public boolean isOpen() {
		return host.isOpen();
	}

	@Override
	public String joinMultiplePaths(String... paths) {
		return host.joinIntoMultiplePath(paths);
	}

	@Override
	public String joinMultiplePaths(List<String> paths, String... paths2) {
		return host.joinIntoMultiplePath(paths, paths2);
	}

	@Override
	public String fixPath(String path) {
		return host.fixPath(path);
	}
	
	@Override
	public String joinIntoOnePath(String... parts) {
		return host.joinIntoOnePath(parts);
	}

	@Override
	public String joinIntoOnePath(List<String> parts) {
		return host.joinIntoOnePath(parts);
	}

	public static LocalFileSystemScenario getInstance() {
		return getInstance(LocalHost.getInstance());
	}

	public static LocalFileSystemScenario getInstance(LocalHost host) {
		return new LocalFileSystemScenario(host);
	}

} // end public class LocalFileSystemScenario
