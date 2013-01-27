package com.mostc.pftt.host;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import com.github.mattficken.io.ByLineReader;
import com.github.mattficken.io.CharsetDeciderDecoder;
import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ConsoleManager.EPrintType;
import com.mostc.pftt.runner.AbstractTestPackRunner.TestPackRunnerThread;

/** Abstracts host management so client code doesn't need to care if host is local or remote(ssh).
 * 
 * @see #exec
 * @see #test_cmd
 * @see AHost#DEV
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

public abstract class AHost extends Host {
	protected String sys_info, os_name, tmp_dir, system_drive, home_dir, php_sdk_dir;
	
	@Override
	public boolean isClosed() {
		return false;
	}
	
	/** returns the name of the Host operating system.
	 * 
	 * cleans up the name and returns it in a short standard form with CPU architecture and service pack,
	 * for example: Win 7 SP1 x64
	 * 
	 * @return
	 */
	public String getOSName() {
		if (os_name!=null)
			return os_name;
		
		os_name = shortenOSName(getOSNameLong());
		
		return os_name;
	}
	
	public static String shortenOSName(String os_name) {
		os_name = os_name.replace("Microsoft", "");
		os_name = os_name.replace("Server", "");
		os_name = os_name.replace("Datacenter", "");
		os_name = os_name.replace("Enterprise", "");
		os_name = os_name.replace("Standard", "");
		os_name = os_name.replace("Professional", "");
		os_name = os_name.replace("Home", "");
		os_name = os_name.replace("Basic", "");
		//
		os_name = os_name.replace("  ", " ");
		os_name = os_name.replace("  ", " ");
		os_name = os_name.replace("  ", " ");
		os_name = os_name.replace("  ", " ");
		return os_name.trim();
	}
	
	/** returns the OS name with whatever platform specific info in the platform specific format
	 * 
	 * @see #getOSName
	 * @return
	 */
	public abstract String getOSNameLong();
	
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
	public abstract ByLineReader readFile(String file, Charset cs) throws IllegalStateException, FileNotFoundException, IOException;
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
	 * @see #FOUR_HOURS
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
	public abstract ExecOutput execOut(String cmd, int timeout_sec, Map<String,String> object, byte[] stdin_post, Charset charset, String current_dir) throws IllegalStateException, Exception;
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
	public abstract ExecOutput execOut(String commandline, int timeout, Map<String,String> env, byte[] stdin, Charset charset, String chdir, TestPackRunnerThread thread, int thread_slow_sec) throws Exception;
	public ExecOutput execOut(String commandline, int timeout, String chdir) throws Exception {
		return execOut(commandline, timeout, null, null, null, chdir);
	}	
	public ExecOutput execOut(String commandline, int timeout, Map<String,String> env, Charset charset, String chdir) throws Exception {
		return execOut(commandline, timeout, env, null, charset, chdir);
	}
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
	 * @param cm
	 * @param ctx_str
	 * @param src_host
	 * @param src
	 * @param dst
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws Exception
	 */
	public abstract void downloadCompressWith7Zip(ConsoleManager cm, String ctx_str, AHost src_host, String src, String dst) throws IllegalStateException, IOException, Exception;
	
	public void downloadCompressWith7Zip(ConsoleManager cm, Class<?> clazz, AHost src_host, String src, String dst) throws IllegalStateException, IOException, Exception {
		downloadCompressWith7Zip(cm, toContext(clazz), src_host, src, dst);
	}
	
	/** downloads file from remote source to local destination
	 * 
	 * @param src
	 * @param dst
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws Exception
	 */
	public abstract boolean download(String src, String dst) throws IllegalStateException, IOException, Exception;
	/** Compresses local source into a 7zip file and uploads it and then decompresses it on the remote host
	 * 
	 * This will improve performance for a few large files or a lots of small files.
	 * 
	 * @param cm
	 * @param ctx
	 * @param src
	 * @param dst_host
	 * @param dst
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws Exception
	 */
	public abstract void uploadCompressWith7Zip(ConsoleManager cm, String ctx_str, String src, AHost dst_host, String dst) throws IllegalStateException, IOException, Exception;
	
	public void uploadCompressWith7Zip(ConsoleManager cm, Class<?> clazz, String src, AHost dst_host, String dst) throws IllegalStateException, IOException, Exception {
		uploadCompressWith7Zip(cm, toContext(clazz), src, dst_host, dst);
	}
	
	@Override
	public String getTempDir() {
		if (tmp_dir!=null)
			return tmp_dir;
		if (isWindows()) {
			tmp_dir = getEnvValue("TEMP");
			if (tmp_dir!=null)
				tmp_dir = getHomeDir() + "\\AppData\\Local\\Temp\\";
			else if (tmp_dir.endsWith("\\"))
				tmp_dir += "\\";
			return tmp_dir;			
		} else {
			return tmp_dir = "/tmp/";
		}
	}
	@Override
	public String getSystemDrive() {
		if (system_drive!=null)
			return system_drive;
		else if (isWindows())
			return system_drive = getEnvValue("SYSTEMDRIVE");
		else
			return system_drive = "/";
	}
	@SuppressWarnings("unused")
	@Override
	public String getPfttDir() {
		if (DEV > 0) {
			return isWindows() ? getPhpSdkDir() + "\\PFTT\\Dev-"+DEV+"\\" : getPhpSdkDir() + "/PFTT/dev-"+DEV+"/";
		} else {
			return isWindows() ? getPhpSdkDir() + "\\PFTT\\Current\\" : getPhpSdkDir() + "/PFTT/current/";
		}
	}
	@Override
	public String getPhpSdkDir() {
		if (php_sdk_dir!=null)
			return php_sdk_dir;
		else if(isWindows())
			return php_sdk_dir = getSystemDrive() + "\\php-sdk\\";
		else
			return php_sdk_dir = getHomeDir() + "/php-sdk/";
	}
	@Override
	public String getHomeDir() {
		if (home_dir!=null) {
			return home_dir;
		} else if (isWindows()) {
			home_dir = getEnvValue("USERPROFILE"); // Windows
			if (StringUtil.isEmpty(home_dir))
				home_dir = getSystemDrive() + "\\Users\\" + getUsername(); // fallback ; this shouldn't happen
			return home_dir;
		} else {
			return home_dir = getEnvValue("HOME"); // Linux
		}
	}
	public ExecOutput execOut(String cmd, int timeout_sec, Map<String, String> env, Charset charset) throws IllegalStateException, Exception {
		return execOut(cmd, timeout_sec, env, charset, null);
	}
	public ExecOutput execOut(String cmd, int timeout) throws Exception {
		return execOut(cmd, timeout, (String)null);
	}
	/** same as #exec, but prompts the local user to elevate the program to Administrator privileges (using Windows UAC)
	 * 
	 * @param cmd
	 * @param timeout
	 * @return
	 * @throws Exception
	 */
	public ExecOutput execElevatedOut(String cmd, int timeout_sec) throws Exception {
		return execElevatedOut(cmd, timeout_sec, null, null, null, null, null, FOUR_HOURS);
	}
	public ExecOutput execElevatedOut(String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset) throws Exception {
		return execElevatedOut(cmd, timeout_sec, env, stdin_data, charset, null, null, FOUR_HOURS);
	}
	public ExecOutput execElevatedOut(String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset, String chdir) throws Exception {
		return execElevatedOut(cmd, timeout_sec, env, stdin_data, charset, chdir, null, FOUR_HOURS);
	}
	private boolean checked_elevate, found_elevate;
	public ExecOutput execElevatedOut(String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset, String chdir, TestPackRunnerThread test_thread, int slow_timeout_sec) throws Exception {
		if (isWindows()) {
			if (!checked_elevate) {
				found_elevate = exists(getPfttDir()+"\\bin\\elevate.exe");
				
				checked_elevate = true;
			}
			if (found_elevate) {
				// execute command with this utility that will elevate the program using Windows UAC
				cmd = getPfttDir() + "\\bin\\elevate "+cmd;
			}
		}
		
		return execOut(cmd, timeout_sec, env, stdin_data, charset, chdir, test_thread, slow_timeout_sec);
	}
	public ExecOutput execElevatedOut(String cmd, int timeout_sec, Map<String, String> env, Charset charset) throws Exception {
		return execElevatedOut(cmd, timeout_sec, env, null, charset, null, null, FOUR_HOURS);
	}
	public ExecOutput execElevatedOut(String cmd, int timeout_sec, Map<String, String> env, Charset charset, String chdir) throws Exception {
		return execElevatedOut(cmd, timeout_sec, env, null, charset, chdir, null, FOUR_HOURS);
	}
	public ExecOutput execElevatedOut(String cmd, int timeout_sec, String chdir) throws Exception {
		return execElevatedOut(cmd, timeout_sec, null, null, null, chdir, null, FOUR_HOURS);
	}
	@ThreadSafe
	public static abstract class ExecHandle {
		public abstract InputStream getSTDOUT();
		public abstract OutputStream getSTDIN();
		/** KILLs process
		 * 
		 * @param force - on Windows, if the process crashed and Windows Error Reporting(WER) is enabled (default=enabled),
		 *                then a WER Popup dialog will block the process from being killed. set force=true
		 *                if the process needs to be killed even in this situation (normally process should be left running/crashed
		 *                so user can debug it)
		 */
		public abstract void close(boolean force);
		/** KILLs process.
		 * 
		 * Note: on Windows, if process crashed and Windows Error Reporting(WER) is enabled (default=enabled)
		 * a WER Popup dialog box will appear and block the process from exiting AND block the process from
		 * being killed
		 * 
		 * @see #close(boolean)
		 */
		public void close() {
			close(false);
		}
		public abstract boolean isRunning();
		public boolean isCrashed() {
			return getExitCode() != 0;
		}
		/** immediately returns the output the process has returned (if process is still running, it may
		 * return more output after this call)
		 * 
		 * @return
		 */
		public abstract String getOutput();
		/** returns the process's exit code
		 * 
		 * @see #isRunning - don't call this if the process is still running (call #isRunning first to check)
		 * @return
		 */
		public abstract int getExitCode();
	} // end public static abstract class ExecHandle
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
	public ExecOutput execOut(String cmd, int timeout_sec, Map<String,String> object, byte[] stdin_post, Charset charset) throws IllegalStateException, Exception {
		return execOut(cmd, timeout_sec, object, stdin_post, charset, (String)null);
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
	public ExecOutput cmdOut(String cmd, int timeout_sec) throws Exception {
		return execOut(toCmd(cmd), timeout_sec);
	}
	public ExecOutput cmdOut(String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset) throws IllegalStateException, Exception {
		return execOut(toCmd(cmd), timeout_sec, env, stdin_data, charset);
	}
	public ExecOutput cmdOut(String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset, String current_dir) throws IllegalStateException, Exception {
		return execOut(toCmd(cmd), timeout_sec, env, stdin_data, charset, current_dir);
	}
	public ExecOutput cmdOut(String cmd, int timeout_sec, Map<String, String> env, Charset charset) throws IllegalStateException, Exception {
		return execOut(toCmd(cmd), timeout_sec, env, charset);
	}
	public ExecOutput cmdOut(String cmd, int timeout_sec, Map<String, String> env, Charset charset, String current_dir) throws IllegalStateException, Exception {
		return execOut(toCmd(cmd), timeout_sec, env, charset, current_dir);
	}
	public ExecOutput cmdOut(String cmd, int timeout_sec, String current_dir) throws IllegalStateException, Exception {
		return execOut(toCmd(cmd), timeout_sec, current_dir);
	}
	@Override
	public boolean cmd(String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset, String current_dir) throws IllegalStateException, Exception {
		return exec(null, (String)null, toCmd(cmd), timeout_sec, env, stdin_data, charset, current_dir);
	}
	/** adds to command to run it with Host's shell.
	 * 
	 *  some Host commands aren't actually programs, but are commands to its shell and must pass through this instead.
	 * 
	 * @see #silenceCmd
	 * @param cmd
	 * @return
	 */
	public String toCmd(String cmd) {
		return isWindows() ? "cmd /C "+cmd : "bash -c "+cmd;
	}
		
	public void upload7ZipAndDecompress(ConsoleManager cm, Class<?> clazz, AHost src_host, String src, String dst) throws IllegalStateException, IOException, Exception {
		upload7ZipAndDecompress(cm, toContext(clazz), src_host, src, dst);
	}
	
	public void download7ZipAndDecompress(ConsoleManager cm, Class<?> clazz, String src, AHost dst_host, String dst) throws IllegalStateException, IOException, Exception {
		download7ZipAndDecompress(cm, toContext(clazz), src, dst_host, dst);
	}
	
	/** uploads a 7zip file from local source to remote destination and decompresses it.
	 * 
	 * Assumes local source is already a 7zip file, while #uploadCompressWith7Zip does the compression itself
	 * 
	 * @param cm
	 * @param ctx_str
	 * @param src_host
	 * @param src
	 * @param dst
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws Exception
	 */
	public void upload7ZipAndDecompress(ConsoleManager cm, String ctx_str, AHost src_host, String src, String dst) throws IllegalStateException, IOException, Exception {
		ensure7Zip(cm, src_host);
		
		String dst_7zip_file = mktempname(ctx_str, ".7z");
		
		upload(src, dst_7zip_file);
		
		decompress(cm, src_host, dst_7zip_file, dst);
		
		delete(dst_7zip_file);
	}
	
	/** Downloads remote 7Zip file to local destination and decompresses it.
	 * 
	 * Assumes remote source is already a 7zip file.
	 * 
	 * @param cm
	 * @param ctx_str
	 * @param src
	 * @param dst_host
	 * @param dst
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws Exception
	 */
	public void download7ZipAndDecompress(ConsoleManager cm, String ctx_str, String src, AHost dst_host, String dst) throws IllegalStateException, IOException, Exception {
		ensure7Zip(cm, dst_host);
		
		String dst_7zip_file = dst_host.mktempname(ctx_str, ".7z");
		
		download(src, dst_7zip_file);
		
		dst_host.decompress(cm, this, dst_7zip_file, dst);
		
		dst_host.delete(dst_7zip_file);
	}
	
	private boolean install_7zip_attempt;
	protected void ensure7Zip(ConsoleManager cm, AHost ohost) throws Exception {
		if (install_7zip_attempt)
			return;
		install_7zip_attempt = true;
		if (ohost.isRemote() == this.isRemote()) {
			// no host with 7zip
			cm.println(EPrintType.CLUE, getClass(), "No host found with a copy of 7z.exe!");
		} else if (ohost.isRemote()) {
			install7Zip(cm, this, ohost);
		} else {
			install7Zip(cm, ohost, this);
		}
	}
	
	private static void install7Zip(ConsoleManager cm, AHost src_host, AHost dst_host) throws Exception {
		final String dst_7z_path = dst_host.getPfttDir()+"\\bin\\7z.exe";
		
		if (dst_host.exists(dst_7z_path)) {
			cm.println(EPrintType.CLUE, "install7Zip", "7z.exe already installed on: "+dst_host);
			return;
		}
		
		final String src_7z_path = src_host.getPfttDir()+"\\bin\\7z.exe";
		if (!src_host.exists(src_7z_path)) {
			cm.println(EPrintType.CLUE, "install7Zip", "7z.exe not found on source: "+src_host);
			return;
		}
		
		try {
			dst_host.upload(src_7z_path, dst_7z_path);
			
			cm.println(EPrintType.CLUE, "install7Zip", "7z.exe installed on: "+dst_host+" (src="+src_host+")");
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CLUE, "install7Zip", ex, "Unable to install 7z.exe on dst="+dst_host+" from src="+src_host);
			
			throw ex;
		}
	} // end private static void install7Zip
	
	/** decompresses 7zip file into destination directory
	 * 
	 * @param cm
	 * @param ohost
	 * @param zip7_file
	 * @param dst
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws Exception
	 */
	public void decompress(ConsoleManager cm, AHost ohost, String zip7_file, String dst) throws IllegalStateException, IOException, Exception {
		ensure7Zip(cm, ohost);
		
		String base_dir = AHost.basename(dst);
		mkdirs(base_dir);
		
		exec(silenceCmd(getPfttDir()+"\\bin\\7z x -mx=9 -mmt="+getCPUCount()+" -bd -y "+zip7_file), AHost.FOUR_HOURS, base_dir);
	}
	
	/** compresses source directory into destination 7zip file
	 * 
	 * @param cm
	 * @param ohost
	 * @param src
	 * @param zip7_file
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws Exception
	 */
	public void compress(ConsoleManager cm, AHost ohost, String src, String zip7_file) throws IllegalStateException, IOException, Exception {
		ensure7Zip(cm, ohost);
		
		// @see http://docs.bugaco.com/7zip/MANUAL/switches/method.htm
		// x=9 => highest compression
		// mt=[cpus] => use several threads == number of cpus => maximizes speed
		// bd => no progress bar
		exec(silenceCmd(getPfttDir()+"\\bin\\7z a -mx=9 -mmt="+getCPUCount()+" -bd "+zip7_file+" "+src), AHost.FOUR_HOURS, src);
	}
	
	/** modifies command to silence any output. returns modified command that can
	 * be passed to #exec.
	 * 
	 * @see #exec
	 * @param cmd
	 * @return
	 */
	public String silenceCmd(String cmd) {
		if (isWindows())
			return cmd + " > NUL";
		else
			return cmd + " > /dev/null";
	}
	
	/** returns true if this host is X64 (AMD64, EM64T, etc...).
	 * 
	 * If it is X64, it can also run X86 code.
	 * 
	 * If its not X64, assume its only able to run X86 code.
	 * 
	 * @see #getCPUCount
	 * @return
	 */
	private Boolean is_x64;
	@Override
	public boolean isX64() {
		if (is_x64!=null) {
			return is_x64.booleanValue();
		} else if (isWindows()) {
			return is_x64 = StringUtil.equalsIC("amd64", getEnvValue("PROCESSOR_ARCHITECTURE"));
		} else {
			is_x64 = Boolean.FALSE;
			try {
				ByLineReader reader = readFile("/proc/cpuinfo");
				String line;
				
				while ( reader.hasMoreLines() && ( line = reader.readLine() ) != null ) {
					if (line.toLowerCase().contains("AMD64")) {
						is_x64 = Boolean.TRUE;
						break;
					}
				}
				
				reader.close();
			} catch ( Exception ex ) {
				ex.printStackTrace();
			}
			return is_x64;
		}
	} // end public boolean isX64
	
	/** gets a string of info about the host
	 * 
	 * @return
	 * @throws Exception
	 */
	public String getSystemInfo() throws Exception {
		if (sys_info!=null)
			return sys_info;
		else if (isWindows())
			return sys_info = execOut("systeminfo", ONE_MINUTE).output;
		else
			return sys_info = execOut("uname -a", ONE_MINUTE).output;
	}
	
	/** finds path to program on host
	 * 
	 * @param cmd
	 * @return
	 * @throws Exception
	 */
	public String where(String cmd) throws Exception {
		return isWindows() ? cmdOut("WHERE "+cmd, ONE_MINUTE).output : execOut("WHICH "+cmd, ONE_MINUTE).output;
	}
	
	/** returns true if command or program is found
	 * 
	 * if false, passing this command to #exec will result in error because program can't be found
	 * 
	 * @param cmd
	 * @return
	 */
	@Override
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
	public String getLocalhostListenAddress() {
		return isWindows() ? getAddress() : "127.0.0.1";
	}
	
	/** returns number of CPUs on this host
	 * 
	 * if exception, returns 1, always returns at least 1
	 * 
	 * @see #isX64
	 * @return
	 */
	private Integer cpu_count;
	@Override
	public int getCPUCount() {
		if (cpu_count!=null)
			return cpu_count;
		
		try {
			if (isWindows())
				return cpu_count = Integer.parseInt(getEnvValue("NUMBER_OF_PROCESSORS"));
		
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
			return cpu_count = Math.max(1, proc);
		} catch ( Exception ex ) {
			ex.printStackTrace();
			
			return cpu_count = 1;
		}
	} // end public int getCPUCount

	protected abstract String getOSNameOnWindows();
	
	@Override
	public boolean isVistaOrLater() {
		if (!isWindows())
			return false;
		String os_name = getOSNameOnWindows();
		return os_name.contains("Windows Vista") || os_name.contains("Windows 2008") || os_name.contains("Windows 2008") || os_name.contains("Windows 7") || os_name.contains("Windows 8") || os_name.contains("Windows 2012") || os_name.contains("Windows 9");
	}
	@Override
	public boolean isBeforeVista() {
		if (!isWindows())
			return false;
		String os_name = getOSNameOnWindows();
		return os_name.contains("Windows XP") || os_name.contains("Windows 2003") || os_name.contains("Windows 2000");
	}
	@Override
	public boolean isVistaOrBefore() {
		if (!isWindows())
			return false;
		String os_name = getOSNameOnWindows();
		return os_name.contains("Windows Vista") || os_name.contains("Windows 2008") || os_name.contains("Windows 2003") || os_name.contains("Windows XP");
	}
	@Override
	public boolean isVistaExact() {
		if (!isWindows())
			return false;
		String os_name = getOSName();
		// technically Vista SP0 != 2008 RTM but Vista SP1 == 2008 RTM and Vista SP2 == 2008sp2 
		// thats why 2008 RTM is often referred to as 2008sp1 (1 service pack for 2008 after RTM, which is 
		//      2008sp2, so there are 2 versions of Windows 2008, 2008sp1 and 2008sp2)
		return os_name.contains("Windows Vista") || os_name.contains("Windows 2008");
	}
	@Override
	public boolean isWin8Exact() {
		if (!isWindows())
			return false;
		String os_name = getOSName();
		return os_name.contains("Windows 8") || os_name.contains("Windows 2012");
	}
	@Override
	public boolean isWin8OrLater() {
		if (!isWindows())
			return false;
		String os_name = getOSName();
		return os_name.contains("Windows 8") || os_name.contains("Windows 2012") || os_name.contains("Windows 9") || os_name.contains("Windows 2014");
	}
	
	protected String[] getSystemInfoLines() throws Exception {
		return StringUtil.splitLines(getSystemInfo());
	}
	
	@Override
	public long getTotalPhysicalMemoryK() {
		try {
			if (isWindows()) {
				// could use `wmic` but that won't work on winxp, 2003, 2003r2
				for ( String line : getSystemInfoLines() ) {
					// look for line like: `Total Physical Memory:     4,096 MB`
					if (line.startsWith("Total Physical Memory:")) {
						String[] parts = StringUtil.splitWhitespace(line);
						if (parts.length > 2) // should be 3
							return Long.parseLong(parts[1]) * 1024;
					}
				}
			} else {
				String[] lines = execOut("free", ONE_MINUTE).getLines();
				if (lines.length > 2) { // should be 3
					String[] parts = StringUtil.splitWhitespace(lines[1]);
					if (parts.length > 2) // should be 3
						return Long.parseLong(parts[2]);
				}
			}
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
		return 0L;
	} // end public long getTotalPhysicalMemoryK

	@Override
	public String getSystemRoot() {
		if (!isWindows())
			return "/";
		String sr = getEnvValue("SYSTEMROOT");
		if (StringUtil.isNotEmpty(sr))
			return sr;
		else
			// fallback
			return getSystemDrive()+"\\Windows";
	}
	
	@Override
	public abstract long getSize(String file);

	@Override
	public String joinMultiplePaths(String ...paths) {
		if (paths==null||paths.length==0)
			return StringUtil.EMPTY;
		
		StringBuilder sb = new StringBuilder(Math.max(1024, paths[0].length()*2));
		sb.append(paths[0]);
		for ( int i=1 ; i < paths.length ; i++ ) {
			if (paths[i]==null)
				continue;
			sb.append(pathsSeparator());
			sb.append(paths[i]);
		}
		return sb.toString();
	}
	
	@Override
	public boolean unzip(ConsoleManager cm, String zip_file, String base_dir) {
		try {
			mkdirs(base_dir);
			if (exec("unzip "+zip_file+" "+base_dir, ONE_HOUR))
				return true;
			else
				cm.println(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "Unable to unzip: "+zip_file);
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "unzip", ex, "", this, zip_file, base_dir);
		}
		return false;
	}
	
	/** executes Powershell code and returns output.
	 * 
	 * Takes care of making a temporary file, storing the powershell code, executing it, then cleaning up.
	 * 
	 * @param clazz
	 * @param cm
	 * @param ps_code - powershell code to execute (not filename)
	 * @param timeout - max time to allow powershell to run
	 * @return
	 * @throws Exception
	 */
	public TempFileExecOutput powershell(Class<?> clazz, ConsoleManager cm, CharSequence ps_code, int timeout) throws Exception {
		return powershell(toContext(clazz), cm, ps_code, timeout);
	}
	
	private boolean set_unrestricted;
	public TempFileExecOutput powershell(String ctx_str, ConsoleManager cm, CharSequence ps_code, int timeout) throws Exception {
		if (!isWindows())
			throw new IllegalStateException("powershell is only supported on Windows");
		
		if (!set_unrestricted) {
			// do this once
			execElevatedOut("powershell -Command \"set-executionpolicy unrestricted\"", AHost.ONE_MINUTE).printOutputIfCrash(ctx_str, cm);
			
			set_unrestricted = true;
		}
		
		String temp_file = mktempname(ctx_str, ".ps1");
		
		saveTextFile(temp_file, ps_code.toString());
		
		return new TempFileExecOutput(temp_file, execElevatedOut("Powershell -File "+temp_file, timeout));
	} // end public TempFileExecOutput powershell
	
	@Override
	public boolean exec(ConsoleManager cm, String ctx_str, String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_post, Charset charset, String current_dir) throws IllegalStateException, Exception {
		return execOut(cmd, timeout_sec, env, stdin_post, charset, current_dir).printOutputIfCrash(ctx_str, cm).isSuccess();
	}

	@Override
	public boolean exec(ConsoleManager cm, String ctx_str, String commandline, int timeout, Map<String, String> env, byte[] stdin, Charset charset, String chdir, TestPackRunnerThread thread, int thread_slow_sec) throws Exception {
		return execOut(commandline, timeout, env, stdin, charset, chdir, thread, thread_slow_sec).printOutputIfCrash(ctx_str, cm).isSuccess();
	}

	@Override
	public boolean execElevated(ConsoleManager cm, String ctx_str, String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset, String chdir, TestPackRunnerThread test_thread, int slow_timeout_sec) throws Exception {
		return execElevatedOut(cmd, timeout_sec, env, stdin_data, charset, chdir, test_thread, slow_timeout_sec).printOutputIfCrash(ctx_str, cm).isSuccess();
	}
	
} // end public abstract class AHost
