package com.mostc.pftt.host;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.runner.AbstractTestPackRunner.TestPackRunnerThread;

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
		if (exists(base)) {
			String name;
			for ( int i=2 ; ; i++ ) {
				name = base + "-" + i;
				if (!exists(name))
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
		return exec(cm, toContext(clazz), commandline, timeout, null, null, null, chdir);
	}	
	public boolean exec(ConsoleManager cm, Class<?> clazz, String commandline, int timeout, Map<String,String> env, Charset charset, String chdir) throws Exception {
		return exec(cm, toContext(clazz), commandline, timeout, env, null, charset, chdir);
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
		return exec(cm, toContext(clazz), cmd, timeout_sec, env, charset, null);
	}
	public boolean exec(ConsoleManager cm, Class<?> clazz, String cmd, int timeout) throws Exception {
		return exec(cm, toContext(clazz), cmd, timeout, (String)null);
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
		return execElevated(cm, toContext(clazz), cmd, timeout_sec, null, null, null, null, null, FOUR_HOURS);
	}
	public boolean execElevated(ConsoleManager cm, Class<?> clazz, String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset) throws Exception {
		return execElevated(cm, toContext(clazz), cmd, timeout_sec, env, stdin_data, charset, null, null, FOUR_HOURS);
	}
	public boolean execElevated(ConsoleManager cm, Class<?> clazz, String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset, String chdir) throws Exception {
		return execElevated(cm, toContext(clazz), cmd, timeout_sec, env, stdin_data, charset, chdir, null, FOUR_HOURS);
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
		return execElevated(cm, toContext(clazz), cmd, timeout_sec, env, null, charset, null, null, FOUR_HOURS);
	}
	public boolean execElevated(ConsoleManager cm, Class<?> clazz, String cmd, int timeout_sec, Map<String, String> env, Charset charset, String chdir) throws Exception {
		return execElevated(cm, toContext(clazz), cmd, timeout_sec, env, null, charset, chdir, null, FOUR_HOURS);
	}
	public boolean execElevated(ConsoleManager cm, Class<?> clazz, String cmd, int timeout_sec, String chdir) throws Exception {
		return execElevated(cm, toContext(clazz), cmd, timeout_sec, null, null, null, chdir, null, FOUR_HOURS);
	}
	public boolean exec(ConsoleManager cm, Class<?> clazz, String cmd, int timeout_sec, Map<String,String> object, byte[] stdin_post, Charset charset) throws IllegalStateException, Exception {
		return exec(cm, toContext(clazz), cmd, timeout_sec, object, stdin_post, charset, (String)null);
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
	public static String toContext(Class<?> clazz) {
		return clazz == null ? null : clazz.getSimpleName();
	}
	public static String toContext(Class<?> clazz, String method_name) {
		return clazz == null ? method_name : clazz.getSimpleName()+"#"+method_name;
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
	
	public boolean deleteIfExists(String path) {
		try {
			return delete(path);
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
	
	/** moves file/directory
	 * 
	 * @param src
	 * @param dst
	 * @return
	 * @throws IllegalStateException
	 * @throws Exception
	 */
	public abstract boolean move(String src, String dst) throws IllegalStateException, Exception ;

	/** returns the character to separate directories within one path
	 * 
	 * On Windows this is \\
	 * On Linux this is /
	 * Note: on Windows, if #exec or #cmd not being used, then / can still be used
	 * 
	 * @return
	 */
	public abstract String dirSeparator();

	static final Pattern PAT_fs = Pattern.compile("[/]+");
	static final Pattern PAT_bs = Pattern.compile("[\\\\]+");
	/** fixes path so it uses the appropriate / or \\ for the Host
	 * 
	 * @param test_dir
	 * @return
	 */
	public String fixPath(String path) {
		return isWindows() ? toWindowsPath(path) : toUnixPath(path);
	}
	
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
	
	public abstract boolean isDirectory(String string);

	public abstract boolean exists(String string);
	public abstract boolean mkdirs(String path) throws IllegalStateException, IOException;

	/** returns the character to separate several different paths on Host
	 * 
	 * On Windows this is ; 
	 * On Linux this is :
	 * 
	 * @return
	 */
	public abstract String pathsSeparator();

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
	public abstract boolean dirContainsExact(String path, String name);
	public abstract String[] list(String path);
	public abstract boolean dirContainsFragment(String string, String string2);
	
	public abstract String getUsername();

	public abstract String getHomeDir();
	public abstract String getPhpSdkDir();
	public abstract String getPfttDir();

	public String joinIntoOnePath(String ...parts) {
		if (parts==null||parts.length==0)
			return StringUtil.EMPTY;
		
		StringBuilder sb = new StringBuilder(Math.max(1024, parts[0].length()*2));
		sb.append(parts[0]);
		for ( int i=1 ; i < parts.length ; i++ ) {
			if (parts[i]==null)
				continue;
			sb.append(dirSeparator());
			sb.append(parts[i]);
		}
		return sb.toString();
	}
	
	public String anyExist(String...files) {
		for (String file:files) {
			if (exists(file))
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
	
	public abstract long getSize(String file);
	public abstract long getMTime(String file);
	public abstract String joinMultiplePaths(String ...paths);

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
		from = toUnixPath(from);
		to = toUnixPath(to);
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

} // end public abstract class Host
