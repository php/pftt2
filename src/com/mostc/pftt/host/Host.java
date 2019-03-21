package com.mostc.pftt.host;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.runner.AbstractTestPackRunner.TestPackRunnerThread;
import com.mostc.pftt.scenario.FileSystemScenario;
import com.mostc.pftt.scenario.FileSystemScenario.IFileChooser;

public abstract class Host {
	/** for development, set DEV > 0 (ex: DEV=1) and it will use PFTT/Dev-N/ instead of PFTT/Current/
	 * and php-sdk/Dev-N instead of php-sdk
	 * 
	 * this allows for running development versions of PFTT on same host as production(Current) version
	 */
	public static final int DEV = 0;
	public static final int HALF_HOUR = 1800;
	public static final int ONE_HOUR = HALF_HOUR * 2;
	/** should always have a timeout... should NOT let something run forever */
	public static final int FOUR_HOURS = ONE_HOUR * 4;
	public static final int ONE_MINUTE = 60;
	protected static final int NO_TIMEOUT = 0;
	/** put PATH in the ENV vars you pass to #exec and it will automatically add that
	 * value to the system's PATH (takes care of merging it for you, so it won't be completely overridden)
	 */
	public static final String PATH = "PATH";
	
	@Override
	public abstract int hashCode();
	@Override
	public abstract String toString();
	@Override
	public abstract boolean equals(Object o);
	
	public boolean isSafePath(String path) {
		if (path.equals(getJobWorkDir()))
			// can't delete /php-sdk
			return false;
		String pftt_dir = getPfttDir();
		if (path.startsWith(pftt_dir)) {
			// don't delete anything in PFTT dir unless its in cache/working or job_work
			if (!path.startsWith(pftt_dir+"/cache/working/") &&
					!path.startsWith(pftt_dir+"\\job_work\\"))
				return false;
		}
		if (isWindows()) {
			// don't mess with windows
			if (path.equals(getSystemDrive()+"\\Windows"))
				return false;
		} else {
			// these dirs aren't safe to mess with
			if (path.startsWith("/usr/")||path.startsWith("/var/")||path.startsWith("/lib/")||path.startsWith("/sbin/")||path.startsWith("/boot/"))
				return false;
		}
		return true;
	}
	
	/** closes any connections to Host and frees up resources
	 * 
	 */
	public void close() {
		
	}
	
	public abstract boolean isClosed();
	
	/** ensures that name is unique by adding a number to the end of it if
	 * it already exists... returns unqiue name.
	 * 
	 * @param base
	 * @return
	 */
	public String uniqueNameFromBase(String base) {
		if (mExists(base)) {
			String name;
			for ( int i=2 ; ; i++ ) {
				name = base + "-" + i;
				if (!mExists(name))
					return name;
			} 
		}
		return base;
	}
	
	/** executes the given program
	 * 
	 * @see #FOUR_HOURS
	 * @param cm
	 * @param ctx_str
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
	public abstract boolean exec(ConsoleManager cm, String ctx_str, String cmd, int timeout_sec, Map<String,String> env, byte[] stdin_post, Charset charset, String current_dir) throws IllegalStateException, Exception;
	/** executes the given program. if thread_slow_sec time is exceeded, calls TestPackRunnerThread#slowTest so that the TestPackRunner
	 * can compensate
	 * 
	 * @param cm
	 * @param ctx_str
	 * @param commandline
	 * @param timeout
	 * @param env
	 * @param stdin
	 * @param charset
	 * @param chdir
	 * @param thread
	 * @param thread_slow_sec
	 * 
	 * @return
	 * @throws Exception
	 */
	public abstract boolean exec(ConsoleManager cm, String ctx_str, String commandline, int timeout, Map<String,String> env, byte[] stdin, Charset charset, String chdir, TestPackRunnerThread thread, int thread_slow_sec) throws Exception;
	public boolean exec(ConsoleManager cm, String ctx_str, String commandline, int timeout, String chdir) throws Exception {
		return exec(cm, ctx_str, commandline, timeout, null, null, null, chdir);
	}
	public boolean exec(ConsoleManager cm, String ctx_str, String commandline, int timeout, Map<String,String> env) throws Exception {
		return exec(cm, ctx_str, commandline, timeout, env, (String)null);
	}
	public boolean exec(ConsoleManager cm, String ctx_str, String commandline, int timeout, Map<String,String> env, String chdir) throws Exception {
		return exec(cm, ctx_str, commandline, timeout, env, null, chdir);
	}
	public boolean exec(ConsoleManager cm, Class<?> ctx, String commandline, int timeout, Map<String,String> env) throws Exception {
		return exec(cm, ctx, commandline, timeout, env, (String)null);
	}
	public boolean exec(ConsoleManager cm, Class<?> ctx, String commandline, int timeout, Map<String,String> env, String chdir) throws Exception {
		return exec(cm, ctx, commandline, timeout, env, null, chdir);
	}
	public boolean execElevated(ConsoleManager cm, String ctx_str, String commandline, int timeout, Map<String,String> env) throws Exception {
		return execElevated(cm, ctx_str, commandline, timeout, env, (String)null);
	}
	public boolean execElevated(ConsoleManager cm, String ctx_str, String commandline, int timeout, Map<String,String> env, String chdir) throws Exception {
		return execElevated(cm, ctx_str, commandline, timeout, env, null, chdir);
	}
	public boolean execElevated(ConsoleManager cm, Class<?> ctx, String commandline, int timeout, Map<String,String> env) throws Exception {
		return execElevated(cm, ctx, commandline, timeout, env, (String)null);
	}
	public boolean execElevated(ConsoleManager cm, Class<?> ctx, String commandline, int timeout, Map<String,String> env, String chdir) throws Exception {
		return execElevated(cm, ctx, commandline, timeout, env, null, chdir);
	}
	public boolean exec(ConsoleManager cm, String ctx_str, String commandline, int timeout, Map<String,String> env, Charset charset, String chdir) throws Exception {
		return exec(cm, ctx_str, commandline, timeout, env, null, charset, chdir);
	}
	public boolean exec(ConsoleManager cm, Class<?> clazz, String commandline, int timeout, String chdir) throws Exception {
		return exec(cm, FileSystemScenario.toContext(clazz), commandline, timeout, null, null, null, chdir);
	}	
	public boolean exec(ConsoleManager cm, Class<?> clazz, String commandline, int timeout, Map<String,String> env, Charset charset, String chdir) throws Exception {
		return exec(cm, FileSystemScenario.toContext(clazz), commandline, timeout, env, null, charset, chdir);
	}
	public boolean exec(String commandline, int timeout, String chdir) throws Exception {
		return exec(null, (String)null, commandline, timeout, null, null, null, chdir);
	}	
	public boolean exec(String commandline, int timeout, Map<String,String> env, Charset charset, String chdir) throws Exception {
		return exec(null, (String)null, commandline, timeout, env, null, charset, chdir);
	}
	public boolean exec(ConsoleManager cm, String ctx_str, String cmd, int timeout_sec, Map<String, String> env, Charset charset) throws IllegalStateException, Exception {
		return exec(cm, ctx_str, cmd, timeout_sec, env, charset, null);
	}
	public boolean exec(ConsoleManager cm, String ctx_str, String cmd, int timeout) throws Exception {
		return exec(cm, ctx_str, cmd, timeout, (String)null);
	}
	public boolean exec(ConsoleManager cm, Class<?> clazz, String cmd, int timeout_sec, Map<String, String> env, Charset charset) throws IllegalStateException, Exception {
		return exec(cm, FileSystemScenario.toContext(clazz), cmd, timeout_sec, env, charset, null);
	}
	public boolean exec(ConsoleManager cm, Class<?> clazz, String cmd, int timeout) throws Exception {
		return exec(cm, FileSystemScenario.toContext(clazz), cmd, timeout, (String)null);
	}
	public boolean exec(String cmd, int timeout_sec, Map<String, String> env, Charset charset) throws IllegalStateException, Exception {
		return exec(null, (String)null, cmd, timeout_sec, env, charset, null);
	}
	public boolean exec(String cmd, int timeout) throws Exception {
		return exec(null, (String)null, cmd, timeout, (String)null);
	}
	/** same as #exec, but prompts the local user to elevate the program to Administrator privileges (using Windows UAC)
	 * 
	 * @param cmd
	 * @param timeout
	 * @return
	 * @throws Exception
	 */
	public boolean execElevated(ConsoleManager cm, String ctx_str, String cmd, int timeout_sec) throws Exception {
		return execElevated(cm, ctx_str, cmd, timeout_sec, null, null, null, null, null, FOUR_HOURS);
	}
	public boolean execElevated(ConsoleManager cm, String ctx_str, String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset) throws Exception {
		return execElevated(cm, ctx_str, cmd, timeout_sec, env, stdin_data, charset, null, null, FOUR_HOURS);
	}
	public boolean execElevated(ConsoleManager cm, String ctx_str, String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset, String chdir) throws Exception {
		return execElevated(cm, ctx_str, cmd, timeout_sec, env, stdin_data, charset, chdir, null, FOUR_HOURS);
	}	
	public boolean execElevated(ConsoleManager cm, Class<?> clazz, String cmd, int timeout_sec) throws Exception {
		return execElevated(cm, FileSystemScenario.toContext(clazz), cmd, timeout_sec, null, null, null, null, null, FOUR_HOURS);
	}
	public boolean execElevated(ConsoleManager cm, Class<?> clazz, String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset) throws Exception {
		return execElevated(cm, FileSystemScenario.toContext(clazz), cmd, timeout_sec, env, stdin_data, charset, null, null, FOUR_HOURS);
	}
	public boolean execElevated(ConsoleManager cm, Class<?> clazz, String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset, String chdir) throws Exception {
		return execElevated(cm, FileSystemScenario.toContext(clazz), cmd, timeout_sec, env, stdin_data, charset, chdir, null, FOUR_HOURS);
	}
	public boolean execElevated(String cmd, int timeout_sec) throws Exception {
		return execElevated(null, (String)null, cmd, timeout_sec, null, null, null, null, null, FOUR_HOURS);
	}
	public boolean execElevated(String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset) throws Exception {
		return execElevated(null, (String)null, cmd, timeout_sec, env, stdin_data, charset, null, null, FOUR_HOURS);
	}
	public boolean execElevated(String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset, String chdir) throws Exception {
		return execElevated(null, (String)null, cmd, timeout_sec, env, stdin_data, charset, chdir, null, FOUR_HOURS);
	}
	public abstract boolean execElevated(ConsoleManager cm, String ctx_str, String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset, String chdir, TestPackRunnerThread test_thread, int slow_timeout_sec) throws Exception;
	public boolean execElevated(ConsoleManager cm, String ctx_str, String cmd, int timeout_sec, Map<String, String> env, Charset charset) throws Exception {
		return execElevated(cm, ctx_str, cmd, timeout_sec, env, null, charset, null, null, FOUR_HOURS);
	}
	public boolean execElevated(ConsoleManager cm, String ctx_str, String cmd, int timeout_sec, Map<String, String> env, Charset charset, String chdir) throws Exception {
		return execElevated(cm, ctx_str, cmd, timeout_sec, env, null, charset, chdir, null, FOUR_HOURS);
	}
	public boolean execElevated(ConsoleManager cm, String ctx_str, String cmd, int timeout_sec, String chdir) throws Exception {
		return execElevated(cm, ctx_str, cmd, timeout_sec, null, null, null, chdir, null, FOUR_HOURS);
	}
	public boolean exec(ConsoleManager cm, String ctx_str, String cmd, int timeout_sec, Map<String,String> object, byte[] stdin_post, Charset charset) throws IllegalStateException, Exception {
		return exec(cm, ctx_str, cmd, timeout_sec, object, stdin_post, charset, (String)null);
	}
	public boolean execElevated(ConsoleManager cm, Class<?> clazz, String cmd, int timeout_sec, Map<String, String> env, Charset charset) throws Exception {
		return execElevated(cm, FileSystemScenario.toContext(clazz), cmd, timeout_sec, env, null, charset, null, null, FOUR_HOURS);
	}
	public boolean execElevated(ConsoleManager cm, Class<?> clazz, String cmd, int timeout_sec, Map<String, String> env, Charset charset, String chdir) throws Exception {
		return execElevated(cm, FileSystemScenario.toContext(clazz), cmd, timeout_sec, env, null, charset, chdir, null, FOUR_HOURS);
	}
	public boolean execElevated(ConsoleManager cm, Class<?> clazz, String cmd, int timeout_sec, String chdir) throws Exception {
		return execElevated(cm, FileSystemScenario.toContext(clazz), cmd, timeout_sec, null, null, null, chdir, null, FOUR_HOURS);
	}
	public boolean exec(ConsoleManager cm, Class<?> clazz, String cmd, int timeout_sec, Map<String,String> object, byte[] stdin_post, Charset charset) throws IllegalStateException, Exception {
		return exec(cm, FileSystemScenario.toContext(clazz), cmd, timeout_sec, object, stdin_post, charset, (String)null);
	}
	public boolean execElevated(String cmd, int timeout_sec, Map<String, String> env, Charset charset) throws Exception {
		return execElevated(null, (String)null, cmd, timeout_sec, env, null, charset, null, null, FOUR_HOURS);
	}
	public boolean execElevated(String cmd, int timeout_sec, Map<String, String> env, Charset charset, String chdir) throws Exception {
		return execElevated(null, (String)null, cmd, timeout_sec, env, null, charset, chdir, null, FOUR_HOURS);
	}
	public boolean execElevated(String cmd, int timeout_sec, String chdir) throws Exception {
		return execElevated(null, (String)null, cmd, timeout_sec, null, null, null, chdir, null, FOUR_HOURS);
	}
	public boolean exec(String cmd, int timeout_sec, Map<String,String> object, byte[] stdin_post, Charset charset) throws IllegalStateException, Exception {
		return exec(null, (String)null, cmd, timeout_sec, object, stdin_post, charset, (String)null);
	}
	public boolean cmd(String cmd, int timeout_sec) throws Exception {
		return cmd(cmd, timeout_sec, null, (byte[])null, (Charset)null);
	}
	public boolean cmd(String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset) throws IllegalStateException, Exception {
		return cmd(cmd, timeout_sec, env, stdin_data, charset, null);
	}
	public abstract boolean cmd(String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset, String current_dir) throws IllegalStateException, Exception;
	public boolean cmd(String cmd, int timeout_sec, Map<String, String> env, Charset charset) throws IllegalStateException, Exception {
		return cmd(cmd, timeout_sec, env, null, charset, null);
	}
	public boolean cmd(String cmd, int timeout_sec, Map<String, String> env, Charset charset, String current_dir) throws IllegalStateException, Exception {
		return cmd(cmd, timeout_sec, env, null, charset, current_dir);
	}
	public boolean cmd(String cmd, int timeout_sec, String current_dir) throws IllegalStateException, Exception {
		return cmd(cmd, timeout_sec, null, null, current_dir);
	}
	public boolean cmdElevated(String cmd, int timeout_sec) throws Exception {
		return cmdElevated(cmd, timeout_sec, null, (byte[])null, (Charset)null);
	}
	public boolean cmdElevated(String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset) throws IllegalStateException, Exception {
		return cmdElevated(cmd, timeout_sec, env, stdin_data, charset, null);
	}
	public abstract boolean cmdElevated(String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset, String current_dir) throws IllegalStateException, Exception;
	public boolean cmdElevated(String cmd, int timeout_sec, Map<String, String> env, Charset charset) throws IllegalStateException, Exception {
		return cmdElevated(cmd, timeout_sec, env, null, charset, null);
	}
	public boolean cmdElevated(String cmd, int timeout_sec, Map<String, String> env, Charset charset, String current_dir) throws IllegalStateException, Exception {
		return cmdElevated(cmd, timeout_sec, env, null, charset, current_dir);
	}
	public boolean cmdElevated(String cmd, int timeout_sec, String current_dir) throws IllegalStateException, Exception {
		return cmdElevated(cmd, timeout_sec, null, null, current_dir);
	}
	
	
	/** takes string in form C:\\ and returns `C`. anything else, and null is returned
	 * 
	 * @param path
	 * @return
	 */
	public static String drive(String path) {
		if (path.length() >= 2) {
			if (path.charAt(1)==':') {
				if (Character.isLetter(path.charAt(0)))
					return path.substring(0, 1).toUpperCase();
			}
		}
		return null;
	}
	
	public static boolean hasDrive(String path) {
		return StringUtil.isNotEmpty(drive(path));
	}

	
	

	/** returns TRUE if host is Windows
	 * 
	 * this is a fast method which caches its values, so you don't have to do that in your code
	 * 
	 * generally if !isWindows == isPosix
	 *
	 * @see #isVistaOrBefore
	 * @see #isVistaExact
	 * @see #isWin8OrLater
	 * @return
	 */
	public abstract boolean isWindows();
	
	public abstract boolean isVistaExact();

	/** returns the Host's SystemDrive.
	 * 
	 * On Windows this is usually C:\\ but not always.
	 * 
	 * On Linux, it is always /.
	 * 
	 * @return
	 */
	public abstract String getSystemDrive();
	

	/** returns the character to separate several different paths on Host
	 * 
	 * On Windows this is ; 
	 * On Linux this is :
	 * 
	 * @return
	 */
	public abstract String mPathsSeparator();

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
	public abstract boolean isOpen();

	/** Uploads file from local source to remote destination
	 * 
	 * @param src
	 * @param dst
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws Exception
	 */
	public abstract boolean upload(String local_file, String remote_file) throws IllegalStateException, IOException, Exception;

	/** on Windows, returns the directory where windows is stored, typically C:\Windows.
	 * 
	 * on other OSes, it returns /
	 * 
	 * @return
	 */
	public abstract String getSystemRoot();
	public abstract boolean mDirContainsExact(String path, String name);
	public abstract String[] mList(String path);
	public abstract boolean mDirContainsFragment(String string, String string2);
	
	public abstract String getUsername();

	public abstract String getHomeDir();
	public abstract String getJobWorkDir();
	public abstract String getPfttDir();
	
	public String getPfttBinDir() {
		return joinIntoOnePath(getPfttDir(), "bin");
	}
	public String getPfttCacheDir() {
		return joinIntoOnePath(getPfttDir(), "cache");
	}
	public String getPfttConfDir() {
		return joinIntoOnePath(getPfttDir(), "conf");
	}

	public String joinIntoOnePath(String ...parts) {
		if (parts==null||parts.length==0)
			return StringUtil.EMPTY;
		
		StringBuilder sb = new StringBuilder(Math.max(1024, parts[0].length()*2));
		sb.append(fixPath(parts[0]));
		for ( int i=1 ; i < parts.length ; i++ ) {
			if (parts[i]==null)
				continue;
			sb.append(mDirSeparator());
			sb.append(fixPath(parts[i]));
		}
		return sb.toString();
	}
	
	public String joinIntoOnePath(List<String> parts) {
		if (parts==null||parts.isEmpty())
			return StringUtil.EMPTY;
		
		StringBuilder sb = new StringBuilder(Math.max(1024, parts.get(0).length()*2));
		sb.append(fixPath(parts.get(0)));
		for ( int i=1 ; i < parts.size() ; i++ ) {
			sb.append(mDirSeparator());
			sb.append(fixPath(parts.get(i)));
		}
		return sb.toString();
	}
	
	public String joinIntoMultiplePath(List<String> parts) {
		if (parts==null||parts.isEmpty())
			return StringUtil.EMPTY;
		
		StringBuilder sb = new StringBuilder(Math.max(1024, parts.get(0).length()*2));
		sb.append(fixPath(parts.get(0)));
		for ( int i=1 ; i < parts.size() ; i++ ) {
			sb.append(mPathsSeparator());
			sb.append(fixPath(parts.get(i)));
		}
		return sb.toString();
	}
	
	public String joinIntoMultiplePath(String... paths) {
		return joinIntoMultiplePath(null, paths);
	}

	public String joinIntoMultiplePath(List<String> paths, String... paths2) {
		StringBuilder sb = new StringBuilder();
		if (paths!=null&&paths.size()>0) {
			sb.append(fixPath(paths.get(0)));
			for ( int i=1 ; i < paths.size() ; i++ ) {
				sb.append(mPathsSeparator());
				sb.append(fixPath(paths.get(i)));
			}
		}
		if (paths2!=null&&paths2.length>0) {
			sb.append(fixPath(paths2[0]));
			for ( int i=1 ; i < paths2.length ; i++ ) {
				sb.append(mPathsSeparator());
				sb.append(fixPath(paths2[i]));
			}
		}
		return sb.toString();
	}
	
	public String anyExist(String...files) {
		for (String file:files) {
			if (mExists(file))
				return file;
		}
		return null;
	}

	/** unzips .ZIP file into base_dir
	 * 
	 * @param cm
	 * @param zip_file
	 * @param base_dir
	 * @return
	 */
	public abstract boolean unzip(ConsoleManager cm, String zip_file, String app_dir);

	public abstract boolean isVistaOrBefore();
	public abstract boolean isBeforeVista();
	public abstract boolean isVistaOrLater();
	public abstract boolean isWin8Exact();
	public abstract boolean isWin8OrLater();
	public abstract long getTotalPhysicalMemoryK();
	
	public abstract long mSize(String file);
	public abstract long mMTime(String file);
	public abstract String joinMultiplePaths(String ...paths);
	public abstract String joinMultiplePaths(List<String> paths, String ...paths2);

	public boolean unzip(String zip_file, String base_dir) {
		return unzip(null, zip_file, base_dir);
	}
	
	/** reboots host
	 * 
	 * @throws Exception
	 */
	public void reboot() throws Exception {
		exec("shutdown -r -t 0", ONE_MINUTE);
	}
	
	public abstract boolean isX64();
	public abstract boolean hasCmd(String cmd);
	public abstract int getCPUCount();
	
	/** counts the number of parent directories from from to to (its parent, parent of its parent, etc...)
	 * 
	 * countUp('a/b/c', 'a') => 2
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	public static int countUp(String from, String to) {
		from = FileSystemScenario.toUnixPath(from);
		to = FileSystemScenario.toUnixPath(to);
		if (from.equals(to))
			return 0;
		else if (from.startsWith(to))
			from = from.substring(to.length());
		return from.split("/").length+1;
	}

	/** returns part of path separating to from from.
	 * 
	 *  ex: pathFrom(/a/b, /a/b/c/d) => /c/d
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	public static String pathFrom(String from, String to) {
		if (to.startsWith(from)) {
			String path = to.substring(from.length());
			if (path.startsWith("/")||path.startsWith("\\"))
				path = path.substring(1);
			return path;
		} else {
			return to;
		}
	}
	
	public String mCreateTempName(String ctx_str) {
		return mCreateTempName(ctx_str, (String) null);
	}
	static final Random rand = new Random();
	/** generates the name of a temporary file that is not in use
	 * 
	 * @param ctx_str - part of PFTT that needs this temporary filename
	 * @param suffix - string that is appended to end of file name (ex: .php file extension)
	 * @return
	 */
	public String mCreateTempName(String ctx_str, String suffix) {
		return mCreateTempName(getTempDir(), ctx_str, suffix);
	}
	/** generates the name of a temporary file in a custom directory
	 * 
	 * @param temp_dir
	 * @param ctx_str
	 * @param suffix
	 * @return
	 */
	public String mCreateTempName(String temp_dir, String ctx_str, String suffix) {
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
			if (mExists(str)) {
				sb.setLength(0); // important
				continue;
			} else {
				break;
			}
		}
		
		return str;
	} // end public String mCreateTempName
	public String mCreateTempName(Class<?> clazz, String suffix) {
		return mCreateTempName(FileSystemScenario.toContext(clazz), suffix);
	}
	public String mCreateTempName(Class<?> clazz) {
		return mCreateTempName(FileSystemScenario.toContext(clazz));
	}
	public String mCreateTempName(String temp_dir, Class<?> clazz, String suffix) {
		return mCreateTempName(temp_dir, FileSystemScenario.toContext(clazz), suffix);
	}
	public String mCreateTempName(String temp_dir, Class<?> clazz) {
		return mCreateTempName(temp_dir, FileSystemScenario.toContext(clazz), null);
	}
	
	public abstract boolean mExists(String phpExe);
	public abstract boolean mDeleteIfExists(String string);
	public abstract boolean mMove(String string, String string2) throws IllegalStateException, Exception;
	
	public abstract boolean mCreateDirs(String path2) throws IllegalStateException, IOException;
	
	public abstract boolean mDelete(String php_file) throws IllegalStateException, IOException;
	public abstract boolean mSaveTextFile(String php_file, String php_code) throws IOException;

	public abstract boolean mDeleteIfExistsElevated(String string);

	public abstract String fixPath(String src_path);
	
	public abstract boolean mCopyElevated(String string, String dirname) throws IllegalStateException, Exception;

	public abstract boolean mDeleteElevated(String string) throws IllegalStateException, IOException;
	public abstract String mDirSeparator();
	
	public abstract boolean mCopy(String string, String datadir) throws IllegalStateException, Exception;
	
	public abstract boolean mIsDirectory(String path);
	public abstract boolean mSaveTextFile(String filename, String text, CharsetEncoder ce) throws IllegalStateException, IOException;
	public abstract boolean mMoveElevated(String src, String dst) throws IllegalStateException, Exception;
	public abstract String getTempDir();
	public abstract boolean mDeleteFileExtension(String dir, String ext);
	public abstract boolean mDeleteChosenFiles(String dir, IFileChooser chr);

} // end public abstract class Host
