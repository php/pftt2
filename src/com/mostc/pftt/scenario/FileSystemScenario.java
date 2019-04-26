package com.mostc.pftt.scenario;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import com.github.mattficken.io.ByLineReader;
import com.github.mattficken.io.CharsetDeciderDecoder;
import com.github.mattficken.io.IOUtil;
import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.host.SSHHost;
import com.mostc.pftt.model.ActiveTestPack;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

/** 
 * 
 * @author Matt Ficken
 *
 */

public abstract class FileSystemScenario extends AbstractSerialScenario {
	
	/** deletes all files in directory with extension
	 * 
	 * deleteFileExtension(".", ".tmp"); => deletes all .tmp files
	 * 
	 * @param dir
	 * @param ext
	 */
	public boolean deleteFileExtension(String dir, final String ext) {
		return deleteChosenFiles(dir, new IFileChooser() {
				@Override
				public boolean choose(String dir, String name, boolean isdir) {
					return StringUtil.endsWithIC(name, ext);
				}	
			});
	}
	
	public interface IFileChooser {
		public boolean choose(String dir, String name, boolean isdir);
	}
	
	
	/** removes the file extension from file.
	 * 
	 * for filenames like A.B returns A
	 * for all others (if . not found), path
	 * 
	 * if path is a relative or absolute path (if path has directory|ies), the
	 * directories are not removed just .BBB
	 * 
	 * @param path
	 * @return
	 */
	public static String removeFileExt(String path) {
		int i = path.lastIndexOf('.');
		return i == -1 ? path : path.substring(0, i);
	}
	
	/** returns the directory portion of a file path
	 * 
	 * @param path
	 * @return
	 */
	public static String dirname(String path) {
		int i = path.lastIndexOf('/');
		int j = path.lastIndexOf('\\');
		if (i==-1) {
			if (j==-1)
				return path;
			else
				return path.substring(0, j);
		} else if (j==-1) {
			if (i==-1)
				return path;
			else
				return path.substring(0, i);
		} else if (i>j) {
			return path.substring(j, i);
		} else {
			// j>i
			return path.substring(i, j);
		}		
	}
	
	/** returns the filename from a directory path
	 * 
	 * @param path
	 * @return
	 */
	public static String basename(String path) {
		return new File(path).getName();
	}
	
	/** splits path using either Windows or Unix path separator
	 * 
	 * @param path
	 * @return
	 */
	public static String[] splitPath(String path) {
		return path.split("[/|\\\\]");
	} 
	
	public abstract boolean isDirectory(String string);

	public abstract boolean exists(String string);
	public abstract boolean createDirs(String path) throws IllegalStateException, IOException;
	
	public String mktempname(String ctx_str) {
		return mktempname(ctx_str, (String) null);
	}
	/** returns Host's preferred directory for storing temporary files
	 * 
	 * @return
	 */
	public abstract String getTempDir();
	static final Random rand = new Random();
	/** generates the name of a temporary file that is not in use
	 * 
	 * @param ctx_str - part of PFTT that needs this temporary filename
	 * @param suffix - string that is appended to end of file name (ex: .php file extension)
	 * @return
	 */
	public String mktempname(String ctx_str, String suffix) {
		return mktempname(getTempDir(), ctx_str, suffix);
	}
	/** generates the name of a temporary file in a custom directory
	 * 
	 * @param temp_dir
	 * @param ctx_str
	 * @param suffix
	 * @return
	 */
	public String mktempname(String temp_dir, String ctx_str, String suffix) {
		StringBuilder sb = new StringBuilder(50);
		String str = null;
		
		// generate random filename until one found that isn't in use
		int j;
		for ( int i=0 ; i < 65535 ; i++ ) {
			sb.append(temp_dir);
			//sb.append(dirSeparator()); // getTempDir() returns path ending with / or \
			sb.append("PFTT-");
			if (StringUtil.isNotEmpty(ctx_str)) {
				sb.append(ctx_str);
				sb.append('-');
			}
			for (j=0 ; j < 10 ; j++ )
				sb.append((char)( rand.nextInt(26) + 65 ));
			if (StringUtil.isNotEmpty(suffix))
				sb.append(suffix);
			str = sb.toString();
			
			//
			if (exists(str)) {
				sb.setLength(0); // important
				continue;
			} else {
				break;
			}
		}
		
		return str;
	} // end public String mktempname
	public static String toContext(Class<?> clazz) {
		return clazz == null ? null : clazz.getSimpleName();
	}
	public static String toContext(Class<?> clazz, String method_name) {
		return clazz == null ? method_name : clazz.getSimpleName()+"#"+method_name;
	}
	public String mktempname(Class<?> clazz, String suffix) {
		return mktempname(toContext(clazz), suffix);
	}
	public String mktempname(Class<?> clazz) {
		return mktempname(toContext(clazz));
	}
	public String mktempname(String temp_dir, Class<?> clazz, String suffix) {
		return mktempname(temp_dir, toContext(clazz), suffix);
	}
	public String mktempname(String temp_dir, Class<?> clazz) {
		return mktempname(temp_dir, toContext(clazz), null);
	}
	
	/** saves text in given file
	 * 
	 * @param filename
	 * @param text
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public abstract boolean saveTextFile(String path, String string) throws IllegalStateException, IOException;
	
	public abstract boolean saveTextFile(String filename, String text, CharsetEncoder ce) throws IllegalStateException, IOException;

	public abstract boolean delete(String file) throws IllegalStateException, IOException;
	public abstract boolean deleteElevated(String file) throws IllegalStateException, IOException;
	
	public boolean deleteIfExists(String path) {
		try {
			return delete(path);
		} catch ( Exception ex ) {
			
		}
		return false;
	}
	public boolean deleteIfExistsElevated(String path) {
		try {
			return deleteElevated(path);
		} catch ( Exception ex ) {
			
		}
		return false;
	}
	/** copies file/directory from source to destination on host
	 * 
	 * @see #download - to copy file from remote host to local
	 * @param src
	 * @param dst
	 * @throws IllegalStateException
	 * @throws Exception
	 */
	public abstract boolean copy(String src, String dst) throws IllegalStateException, Exception ;
	public abstract boolean copyElevated(String src, String dst) throws IllegalStateException, Exception ;
	
	/** moves file/directory
	 * 
	 * @param src
	 * @param dst
	 * @return
	 * @throws IllegalStateException
	 * @throws Exception
	 */
	public abstract boolean move(String src, String dst) throws IllegalStateException, Exception;
	public abstract boolean moveElevated(String src, String dst) throws IllegalStateException, Exception ;

	/** returns the character to separate directories within one path
	 * 
	 * On Windows this is \\
	 * On Linux this is /
	 * Note: on Windows, if #exec or #cmd not being used, then / can still be used
	 * 
	 * @return
	 */
	public abstract String dirSeparator();
	
	
	public abstract String pathsSeparator();
	
	
	public String readFileAsString(String path) throws IllegalStateException, FileNotFoundException, IOException {
		return IOUtil.toString(readFile(path), IOUtil.ONE_MEGABYTE);
	}
	
	final public String readFileAsStringEx(String path) {
		try {
			return readFileAsString(path);
		} catch ( Exception ex ) {
			return null;
		}
	}
	
	public abstract boolean deleteChosenFiles(String dir, IFileChooser chr);

	public abstract boolean saveFile(String stdin_file, byte[] stdin_post) throws IllegalStateException, IOException;
	
	public abstract ByLineReader readFile(String file) throws FileNotFoundException, IOException;
	
	public abstract ByLineReader readFile(String file, Charset cs) throws IllegalStateException, FileNotFoundException, IOException;
	
	public abstract ByLineReader readFileDetectCharset(String file, CharsetDeciderDecoder cdd) throws FileNotFoundException, IOException;
	
	public abstract String getContents(String file) throws IOException;
	
	public abstract String getContentsDetectCharset(String file, CharsetDeciderDecoder cdd) throws IOException;
	
	public abstract boolean dirContainsExact(String path, String name);

	public abstract boolean dirContainsFragment(String path, String name_fragment);

	public abstract String[] list(String path);
	
	public abstract long getSize(String file);
	
	public abstract long getMTime(String file);

	public abstract boolean isOpen();
	
	public abstract String joinIntoOnePath(String ...parts);
	public abstract String joinIntoOnePath(List<String> parts);
	public abstract String joinMultiplePaths(String ...paths);
	public abstract String joinMultiplePaths(List<String> paths, String ...paths2);
	
	

	static final Pattern PAT_fs = Pattern.compile("[/]+");
	static final Pattern PAT_bs = Pattern.compile("[\\\\]+");
	
	/** fixes path so it uses the appropriate / or \\ for the Host
	 * 
	 * @param test_dir
	 * @return
	 */
	public abstract String fixPath(String path);
	
	public static String toWindowsPath(String path) {
		return StringUtil.replaceAll(PAT_bs, "\\\\", StringUtil.replaceAll(PAT_fs, "\\\\", path));
	} 
	
	/** converts file path to Unix format (using / instead of Windows \)
	 * 
	 * @param name
	 * @return
	 */
	public static String toUnixPath(String name) {
		// \ is a legal file char on unix so it must get removed or it'll be part of file/dir name
		return StringUtil.replaceAll(PAT_fs, "/", StringUtil.replaceAll(PAT_bs, "/", name));
	}
	
	
	public static FileSystemScenario getFileSystemScenario(ScenarioSet scenario_set) {
		return scenario_set.getScenario(FileSystemScenario.class, DEFAULT_FILESYSTEM_SCENARIO);
	}
	
	@Override
	public Class<?> getSerialKey(EScenarioSetPermutationLayer layer) {
		return FileSystemScenario.class;
	}
	
	public interface ITestPackStorageDir extends IScenarioSetup {
		/** called once test pack is copied/installed to storage dir.
		 * 
		 * returns FALSE if shouldn't proceed with using test-pack on storage dir
		 * 
		 * @param cm
		 * @param host
		 * @return
		 */
		boolean notifyTestPackInstalled(ConsoleManager cm, AHost local_host);
		
		/** returns local path to access storage directory.
		 * 
		 * this will be a drive that the storage dir is mounted on (if remote) or the actual location of the storage dir (if local)
		 * 
		 * @param local_host
		 * @return
		 */
		String getLocalPath(AHost local_host);
		
		/** returns remote path to access storage directory on the remote host.
		 * 
		 * for local file systems this will be the same as #getLocalPath.
		 * 
		 * for remote file systems, this is the path on the remote host (which will be different
		 * than local path #getLocalPath)
		 * 
		 * @param local_host
		 * @return
		 */
		String getRemotePath(AHost local_host);
		
		/** disposes of this StorageDirectory if its empty (if it has no files or folders)
		 * 
		 * 
		 * @see #disposeForce
		 * @param cm
		 * @param host
		 * @param active_test_pack
		 * @return
		 */
		boolean closeIfEmpty(ConsoleManager cm, AHost local_host, ActiveTestPack active_test_pack);
		
		/** disposes of the directory, deleting all files, shares, namespaces, etc... that
		 * back this StorageDirectory
		 * 
		 * @see #disposeIfEmpty
		 * @param cm
		 * @param local_host
		 * @param active_test_pack 
		 * @return
		 */
		boolean closeForce(ConsoleManager cm, AHost local_host, ActiveTestPack active_test_pack);
	} // end public interface ITestPackStorageDir
	
	public static abstract class AbstractTestPackStorageDir extends SimpleScenarioSetup implements ITestPackStorageDir {
		@Override
		public void close(ConsoleManager cm) {
			
		}
		@Override
		public abstract boolean isRunning();
	}
	
	public abstract ITestPackStorageDir setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set);
	
	/** checks if -phpt-in-place console option can be ignored or not
	 * 
	 * @return FALSE - ignore -phpt-in-place, TRUE to follow it (if present)
	 */
	public boolean allowPhptInPlace() {
		// always make sure test-pack is installed onto SMB Share
		// otherwise, there wouldn't be a point in testing on SMB
		return !isRemote();
	}

	public abstract boolean isRemote();
	
	/** TODO
	 * 
	 * @param scenario_set
	 * @param host
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static FileSystemScenario getFS(ScenarioSet scenario_set, Host host) throws IllegalArgumentException {
		FileSystemScenario fs = scenario_set.getScenario(FileSystemScenario.class);
		if (fs!=null)
			return fs;
		else if (host instanceof SSHHost)
			return new SSHFileSystemScenario((SSHHost)host);
		else if (!host.isRemote())
			return LOCALFILESYSTEM_SCENARIO;
		else if (host instanceof AHost)
			return new LocalFileSystemScenario((AHost)host);
		else
			throw new IllegalArgumentException();
	}
	
} // end public abstract class AbstractFileSystemScenario
