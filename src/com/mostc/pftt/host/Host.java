package com.mostc.pftt.host;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;


import com.github.mattficken.io.ByLineReader;
import com.github.mattficken.io.CharsetDeciderDecoder;
import com.mostc.pftt.runner.AbstractTestPackRunner.TestPackRunnerThread;
import com.mostc.pftt.util.StringUtil;

/** Abstracts host management so client code doesn't need to care if host is local or remote(ssh).
 * 
 * @see #exec
 * @see #cmd
 * @see Host#DEV
 * @see #isRemote
 * @see #readFile
 * @see #saveText
 * @see #getSystemDrive
 * @see #isWindows
 * @see #join
 * @see #dirSeparator
 * @author Matt Ficken
 *
 */

public abstract class Host {
	/** for development, set DEV > 0 (ex: DEV=1) and it will use PFTT/Dev-N/ instead of PFTT/Current/
	 * and php-sdk/Dev-N instead of php-sdk
	 * 
	 * this allows for running development versions of PFTT on same host as production(Current) version
	 */
	public static final int DEV = 0;
	public static final int NO_TIMEOUT = 0;
	public static final int ONE_HOUR = 3600;
	public static final int ONE_MINUTE = 60;
	
	/** removes the file extension from file.
	 * 
	 * for filenames like A.B returns a
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
		String p = new File(path).getParent();
		return p == null ? "" : p;
	}
	
	/** returns the filename from a directory path
	 * 
	 * @param path
	 * @return
	 */
	public static String basename(String path) {
		return new File(path).getName();
	}
	
	/** closes any connections to Host and frees up resources
	 * 
	 */
	public void close() {
		
	}
	
	/** returns the name of the Host operating system.
	 * 
	 * cleans up the name and returns it in a short standard form with CPU architecture and service pack,
	 * for example: Win 7 SP1 x64
	 * 
	 * @return
	 */
	public String getOSName() {
		String os_name = getOSNameLong();
		
		os_name.replaceAll("Windows", "Win");
		
		return os_name;
	}
	
	/** returns the OS name with whatever platform specific info in the platform specific format
	 * 
	 * @see #getOSName
	 * @return
	 */
	public abstract String getOSNameLong();
	
	/** returns true if this Host refers to a remote host (ex: ssh).
	 * 	 returns false for localhost
	 * 
	 * @return
	 */
	public abstract boolean isRemote();
	
	/** returns true if #close not yet called.
	 * 
	 * if #close called though, most methods will throw an IllegalStateException
	 * 
	 * @return
	 */
	public boolean isOpen() {
		return true;
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	@Override
	public int hashCode() {
		return getName().hashCode();
	}
	
	@Override
	public abstract boolean equals(Object o);
	
	public abstract String getName();
	@Nullable
	public abstract String getHostname();
	/** saves text in given file
	 * 
	 * @param filename
	 * @param text
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public abstract void saveText(String filename, String text) throws IllegalStateException, IOException;
	public abstract void saveText(String filename, String text, Charset charset) throws IllegalStateException, IOException;
	public abstract void delete(String path) throws IllegalStateException, IOException;
	public void deleteIfExists(String path) {
		try {
			delete(path);
		} catch ( Exception ex ) {
			
		}
	}
	public abstract boolean exists(String string);
	public abstract void mkdirs(String path) throws IllegalStateException, IOException;
	/** copies file/directory from source to destination on host
	 * 
	 * @see #download - to copy file from remote host to local
	 * @param src
	 * @param dst
	 * @throws IllegalStateException
	 * @throws Exception
	 */
	public abstract void copyFile(String src, String dst) throws IllegalStateException, Exception ;
	/** returns the character to separate several different paths on Host
	 * 
	 * On Windows this is ; 
	 * On Linux this is :
	 * 
	 * @return
	 */
	public abstract String pathsSeparator();
	/** returns the character to separate directories within one path
	 * 
	 * On Windows this is \\
	 * On Linux this is /
	 * Note: on Windows, if #exec or #cmd not being used, then / can still be used
	 * 
	 * @return
	 */
	public abstract String dirSeparator();
	/** gets the contents of the file as a single string
	 * 
	 * @param file
	 * @return
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public abstract String getContents(String file) throws IllegalStateException, IOException;
	/** gets the contents of the file as single unicode string, automatically detecting the character set and converting
	 * the bytes to a string (supports exotic charsets like EUC-CN or SHIFT_JS).
	 * 
	 *  Note: this takes extra time, often most files can be read with just #getContents because they don't use exotic charsets 
	 * 
	 * @param file
	 * @param cdd
	 * @return
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public abstract String getContentsDetectCharset(String file, CharsetDeciderDecoder cdd) throws IllegalStateException, IOException;
	/** opens a file to be read line by line
	 * 
	 * @param file
	 * @return
	 * @throws IllegalStateException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public abstract ByLineReader readFile(String file) throws IllegalStateException, FileNotFoundException, IOException;
	/** opens a file to be read line by line, automatically detecting charset
	 * 
	 * @param file
	 * @param cdd
	 * @return
	 * @throws IllegalStateException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public abstract ByLineReader readFileDetectCharset(String file, CharsetDeciderDecoder cdd) throws IllegalStateException, FileNotFoundException, IOException;
	/** executes the given program
	 * 
	 * @see #NO_TIMEOUT
	 * @param cmd - program to run
	 * @param timeout_sec - maximum number of seconds program is allowed to run - terminated if it goes over
	 * @param env - map of environment variables to use
	 * @param stdin_post - bytes to pass to program's STDIN
	 * @param charset - character set to assume program's output is - otherwise it'll autodetect it, which is slower 
	 * @param current_dir - current directory for the program
	 * @return
	 * @throws IllegalStateException
	 * @throws Exception
	 */
	public abstract ExecOutput exec(String cmd, int timeout_sec, Map<String,String> object, byte[] stdin_post, Charset charset, String current_dir) throws IllegalStateException, Exception;
	/** executes the given program. if thread_slow_sec time is exceeded, calls TestPackRunnerThread#slowTest so that the TestPackRunner
	 * can compensate
	 * 
	 * @param commandline
	 * @param timeout
	 * @param env
	 * @param stdin
	 * @param charset
	 * @param chdir
	 * @param thread
	 * @param thread_slow_sec
	 * @return
	 * @throws Exception
	 */
	public abstract ExecOutput exec(String commandline, int timeout, Map<String,String> env, byte[] stdin, Charset charset, String chdir, TestPackRunnerThread thread, int thread_slow_sec) throws Exception;
	public ExecOutput exec(String commandline, int timeout, String chdir) throws Exception {
		return exec(commandline, timeout, null, null, null, chdir);
	}	
	public ExecOutput exec(String commandline, int timeout, Map<String,String> env, Charset charset, String chdir) throws Exception {
		return exec(commandline, timeout, env, null, charset, chdir);
	}
	public abstract String getUsername();
	/** gets value of environment variable
	 * 
	 * @param name
	 * @return
	 */
	@Nullable
	public abstract String getEnvValue(String name);
	/** compresses file using 7zip on remote host, then downloads the file and decompresses it locally.
	 * 
	 * This will improve performance for a few large files or a lots of small files.
	 * 
	 * @param src
	 * @param dst
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws Exception
	 */
	public abstract void downloadCompressWith7Zip(String src, String dst) throws IllegalStateException, IOException, Exception;
	/** downloads file from remote source to local destination
	 * 
	 * @param src
	 * @param dst
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws Exception
	 */
	public abstract void download(String src, String dst) throws IllegalStateException, IOException, Exception;
	/** Compresses local source into a 7zip file and uploads it and then decompresses it on the remote host
	 * 
	 * This will improve performance for a few large files or a lots of small files.
	 * 
	 * @param src
	 * @param dst
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws Exception
	 */
	public abstract void uploadCompressWith7Zip(String src, String dst) throws IllegalStateException, IOException, Exception;
	/** Uploads file from local source to remote destination
	 * 
	 * @param src
	 * @param dst
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws Exception
	 */
	public abstract void upload(String src, String dst) throws IllegalStateException, IOException, Exception;

	protected String tmp_dir, system_drive, home_dir, php_sdk_dir;
	/** returns if this is Windows Vista or 2008, but not 7, 2008r2, 8, 2012
	 * 
	 * @return
	 */
	public boolean isLonghornExact() {
		return false; // TODO
	}
	/** returns TRUE if host is Windows
	 * 
	 * this is a fast method which caches its values, so you don't have to do that in your code
	 * 
	 * generally if !isWindows == isPosix
	 * 
	 * @return
	 */
	public abstract boolean isWindows();
	public String getTempDir() {
		if (tmp_dir!=null)
			return tmp_dir;
		if (isWindows()) {
			tmp_dir = getEnvValue("TEMP");
			if (tmp_dir!=null)
				tmp_dir = getHomeDir() + "\\AppData\\Local\\Temp\\";
			return tmp_dir;			
		} else {
			return "/tmp";
		}
	}
	/** returns the Host's SystemDrive.
	 * 
	 * On Windows this is usually C:\\ but not always.
	 * 
	 * On Linux, it is always /.
	 * 
	 * @return
	 */
	public String getSystemDrive() {
		if (system_drive!=null)
			return system_drive;
		else if (isWindows())
			return system_drive = getEnvValue("SYSTEMDRIVE");
		else
			return system_drive = "/";
	}
	@SuppressWarnings("unused")
	public String getPfttDir() {
		if (DEV > 0) {
			return isWindows() ? getPhpSdkDir() + "\\PFTT\\Dev-"+DEV+"\\" : getPhpSdkDir() + "/PFTT/dev-"+DEV+"/";
		} else {
			return isWindows() ? getPhpSdkDir() + "\\PFTT\\Current\\" : getPhpSdkDir() + "/PFTT/current/";
		}
	}
	public String getPhpSdkDir() {
		if (php_sdk_dir!=null)
			return php_sdk_dir;
		else if(isWindows())
			return php_sdk_dir = getSystemDrive() + "\\php-sdk\\";
		else
			return php_sdk_dir = getHomeDir() + "/php-sdk/";
	}
	public String getHomeDir() {
		if (home_dir!=null)
			return home_dir;
		else if (isWindows())
			return home_dir = getEnvValue("USERPROFILE");
		else
			return home_dir = getEnvValue("HOME");
	}
	public ExecOutput exec(String cmd, int timeout_sec, Map<String, String> env, Charset charset) throws IllegalStateException, Exception {
		return exec(cmd, timeout_sec, env, charset, null);
	}
	public ExecOutput exec(String cmd, int timeout) throws Exception {
		return exec(cmd, timeout, (String)null);
	}
	/** same as #exec, but prompts the local user to elevate the program to Administrator privileges (using Windows UAC)
	 * 
	 * @param cmd
	 * @param timeout
	 * @return
	 * @throws Exception
	 */
	public ExecOutput execElevated(String cmd, int timeout_sec) throws Exception {
		return execElevated(cmd, timeout_sec, null, null, null, null, null, NO_TIMEOUT);
	}
	public ExecOutput execElevated(String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset) throws Exception {
		return execElevated(cmd, timeout_sec, env, stdin_data, charset, null, null, NO_TIMEOUT);
	}
	public ExecOutput execElevated(String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset, String chdir) throws Exception {
		return execElevated(cmd, timeout_sec, env, stdin_data, charset, chdir, null, NO_TIMEOUT);
	}
	public ExecOutput execElevated(String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset, String chdir, TestPackRunnerThread test_thread, int slow_timeout_sec) throws Exception {
		if (isWindows())
			// execute command with this utility that will elevate the program using Windows UAC
			cmd = "c:\\php-sdk\\pftt\\current\\bin\\elevate "+cmd;
		
		return exec(cmd, timeout_sec, env, stdin_data, charset, chdir, test_thread, slow_timeout_sec);
	}
	public ExecOutput execElevated(String cmd, int timeout_sec, Map<String, String> env, Charset charset) throws Exception {
		return execElevated(cmd, timeout_sec, env, null, charset, null, null, NO_TIMEOUT);
	}
	public ExecOutput execElevated(String cmd, int timeout_sec, Map<String, String> env, Charset charset, String chdir) throws Exception {
		return execElevated(cmd, timeout_sec, env, null, charset, chdir, null, NO_TIMEOUT);
	}
	public ExecOutput execElevated(String cmd, int timeout_sec, String chdir) throws Exception {
		return execElevated(cmd, timeout_sec, null, null, null, chdir, null, NO_TIMEOUT);
	}
	@ThreadSafe
	public static abstract class ExecHandle {
		public abstract void close();
		public abstract boolean isRunning();
		public abstract boolean isCrashed();
		public abstract String getOutput();
		public abstract int getExitCode();
	}
	public ExecHandle execThread(String commandline) throws Exception {
		return execThread(commandline, null, null, null);
	}
	public ExecHandle execThread(String commandline, byte[] stdin_data) throws Exception {
		return execThread(commandline, null, null, stdin_data);
	}
	public ExecHandle execThread(String commandline, String chdir) throws Exception {
		return execThread(commandline, null, chdir, null);
	}
	public ExecHandle execThread(String commandline, String chdir, byte[] stdin_data) throws Exception {
		return execThread(commandline, null, chdir, stdin_data);
	}
	public ExecHandle execThread(String commandline, Map<String,String> env, byte[] stdin_data) throws Exception {
		return execThread(commandline, env, null, stdin_data);
	}
	public ExecHandle execThread(String commandline, Map<String,String> env, String chdir) throws Exception {
		return execThread(commandline, env, chdir, null);
	}
	/** executes command, returning immediately before it ends, returning a handle to asynchronously monitor
	 * the process.
	 * 
	 * @param commandline
	 * @param env
	 * @param chdir
	 * @param stdin_data
	 * @return
	 * @throws Exception
	 */
	public abstract ExecHandle execThread(String commandline, Map<String,String> env, String chdir, byte[] stdin_data) throws Exception;
	public ExecOutput exec(String cmd, int timeout_sec, Map<String,String> object, byte[] stdin_post, Charset charset) throws IllegalStateException, Exception {
		return exec(cmd, timeout_sec, object, stdin_post, charset, (String)null);
	}
	/** on Windows, certain command line commands aren't actually programs, but rather commands to CMD.exe
	 * 
	 * Also, on Windows, if you want to execute a group of programs (ex: "A.exe && B.exe") you must use #cmd
	 * 
	 * If this is windows, this will automatically execute the command with CMD.
	 * If this is linux, it just passes the command to #exec
	 * 
	 * @see #exec
	 * @param cmd
	 * @param timeout_sec
	 * @param env
	 * @param stdin_data
	 * @param charset
	 * @return
	 * @throws IllegalStateException
	 * @throws Exception
	 */
	public ExecOutput cmd(String cmd, int timeout_sec) throws Exception {
		return exec(toCmd(cmd), timeout_sec);
	}
	public ExecOutput cmd(String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset) throws IllegalStateException, Exception {
		return exec(toCmd(cmd), timeout_sec, env, stdin_data, charset);
	}
	public ExecOutput cmd(String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset, String current_dir) throws IllegalStateException, Exception {
		return exec(toCmd(cmd), timeout_sec, env, stdin_data, charset, current_dir);
	}
	public ExecOutput cmd(String cmd, int timeout_sec, Map<String, String> env, Charset charset) throws IllegalStateException, Exception {
		return exec(toCmd(cmd), timeout_sec, env, charset);
	}
	public ExecOutput cmd(String cmd, int timeout_sec, Map<String, String> env, Charset charset, String current_dir) throws IllegalStateException, Exception {
		return exec(toCmd(cmd), timeout_sec, env, charset, current_dir);
	}
	public ExecOutput cmd(String cmd, int timeout_sec, String current_dir) throws IllegalStateException, Exception {
		return exec(toCmd(cmd), timeout_sec, current_dir);
	}
	public String toCmd(String cmd) {
		return isWindows() ? "cmd /C "+cmd : cmd;
	}
		
	public String mktempname() {
		return mktempname(null);
	}
	static final Random rand = new Random();
	/** generates the name of a temporary file that is not in use
	 * 
	 * @param suffix - string that is appended to end of file name (ex: .php file extension)
	 * @return
	 */
	public String mktempname(String suffix) {
		StringBuilder sb = new StringBuilder(20);
		String str;
		
		// generate random filename until one found that isn't in use
		do {
			sb.append(getTempDir());
			sb.append(dirSeparator());
			for (int i=0 ; i < 5 ; i++ )
				sb.append((char)( rand.nextInt(26) + 65 ));
			if (StringUtil.isNotEmpty(suffix))
				sb.append(suffix);
			str = sb.toString();
		} while (exists(str));
		
		return str;
	}
	
	static final Pattern PAT_fs = Pattern.compile("/");
	static final Pattern PAT_bs = Pattern.compile("\\\\");
	static final Pattern PAT_double_fs = Pattern.compile("[/]+");
	/** fixes path so it uses the appropriate / or \\ for the Host
	 * 
	 * @param test_dir
	 * @return
	 */
	public String fixPath(String path) {
		return isWindows() ? StringUtil.replaceAll(PAT_fs, "\\\\", path) : toUnixPath(path);
	}
	
	public String join(String... path_parts) {
		return StringUtil.join(dirSeparator(), path_parts);
	}
	
	/** converts file path to Unix format (using / instead of Windows \\)
	 * 
	 * @param name
	 * @return
	 */
	public static String toUnixPath(String name) {
		// TODO temp return StringUtil.replaceAll(PAT_double_fs, "/", StringUtil.replaceAll(PAT_bs, "\\", name));
		//return name.replaceAll("\\", "\\");//StringUtil.replaceAll(PAT_bs, "\\", name);
		return name.replace('\\', '/');
	}
	
	/** uploads a 7zip file from local source to remote destination and decompresses it.
	 * 
	 * Assumes local source is already a 7zip file, while #uploadCompressWith7Zip does the compression itself
	 * 
	 * @param src
	 * @param dst
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws Exception
	 */
	public void upload7ZipAndDecompress(String src, String dst) throws IllegalStateException, IOException, Exception {
		// TODO
	}
	
	/** Downloads remote 7Zip file to local destination and decompresses it.
	 * 
	 * Assumes remote source is already a 7zip file.
	 * 
	 * @param src
	 * @param dst
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws Exception
	 */
	public void download7ZipAndDecompress(String src, String dst) throws IllegalStateException, IOException, Exception {
		// TODO 
	}
	
	/** gets a string of info about the host
	 * 
	 * @return
	 * @throws Exception
	 */
	public String getSystemInfo() throws Exception {
		if (isWindows())
			return exec("systeminfo", ONE_MINUTE).output;
		else
			return exec("uname -a", ONE_MINUTE).output;
	}
	
	/** finds path to program on host
	 * 
	 * @param cmd
	 * @return
	 * @throws Exception
	 */
	public String where(String cmd) throws Exception {
		return isWindows() ? cmd("WHERE "+cmd, ONE_MINUTE).output : exec("WHICH "+cmd, ONE_MINUTE).output;
	}
	
	/** returns true if command or program is found
	 * 
	 * if false, passing this command to #exec will result in error because program can't be found
	 * 
	 * @param cmd
	 * @return
	 */
	public boolean hasCmd(String cmd) {
		try {
			return StringUtil.isNotEmpty(where(cmd));
		} catch ( Exception ex ) {
			return false;
		}
	}
	
	/** returns an address representing this host on one of the networks its on
	 * 
	 * returns null if host is not on any network at all
	 * 
	 * @return
	 */
	@Nullable
	public abstract String getAddress();

	/** servers running on Windows won't accept sockets from localhost to 127.0.0.1|localhost.
	 * 
	 * instead have to use the ip address of one of its network interfaces (Windows doesn't have a Loopback(LO) network device)
	 * 
	 * @return
	 */
	public String getLocalhostServableAddress() {
		return isWindows() ? getAddress() : "127.0.0.1";
	}
	
	/** returns number of CPUs on this host
	 * 
	 * if exception, returns 1, always returns at least 1
	 * 
	 * @return
	 */
	public int getCPUCount() {
		try {
			if (isWindows())
				return Integer.parseInt(getEnvValue("NUMBER_OF_PROCESSORS"));
		
			int proc = 0;
			ByLineReader r = readFile("/proc/cpuinfo");
			String line;
			while (r.hasMoreLines()) {
				line = r.readLine();
				if (line==null)
					break;
				
				if (line.startsWith("processor"))
					proc++;
			}
			return Math.max(1, proc);
		} catch ( Exception ex ) {
			ex.printStackTrace();
			
			return 1;
		}
	}
	
	/* TODO
	public long getTotalPhysicalMemory() {
		if (isWindows()) {
			// could use `wmic` but that won't work on winxp, 2003, 2003r2
			this.getSystemInfo();
			// look for line like: `Total Physical Memory:     4,096 MB`
		} else {
			this.exec("free", NO_TIMEOUT);
		}
	}
	*/

} // end public abstract class Host
