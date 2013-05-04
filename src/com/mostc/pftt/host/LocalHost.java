package com.mostc.pftt.host;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import org.jvnet.winp.WinProcess;

import com.github.mattficken.io.AbstractDetectingCharsetReader;
import com.github.mattficken.io.ByLineReader;
import com.github.mattficken.io.CharsetByLineReader;
import com.github.mattficken.io.CharsetDeciderDecoder;
import com.github.mattficken.io.DefaultCharsetDeciderDecoder;
import com.github.mattficken.io.IOUtil;
import com.github.mattficken.io.MultiCharsetByLineReader;
import com.github.mattficken.io.NoCharsetByLineReader;
import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ConsoleManager.EPrintType;
import com.mostc.pftt.runner.AbstractTestPackRunner.TestPackRunnerThread;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT.HANDLE;

/** Represents the local Host that the program is currently running on.
 * 
 * @author Matt Ficken
 *
 */

@SuppressWarnings("unused")
public class LocalHost extends AHost {
	private static final Timer timer = new Timer();
	private static final boolean is_windows = System.getProperty("os.name").toLowerCase().contains("windows");
	private static int self_process_id;
	static {
		if (isLocalhostWindows()) {
			// only need this on windows (see LocalExecHandle#close)
			try {
				// this works only on Windows
				self_process_id = Kernel32.INSTANCE.GetCurrentProcessId();
			} catch ( Throwable t ) {
				t.printStackTrace();
			}
		}
	}
	protected final CommonCommandManager ccm;
	
	public LocalHost() {
		ccm = new CommonCommandManager();
	}
	
	public static boolean isLocalhostWindows() {
		return is_windows;
	}
	
	static {
		if (DEV>0) {
			new File(new LocalHost().getPfttDir()).mkdirs();
		}
	}
	
	@Override
	public String getName() {
		return "Localhost";
	}
	
	@Override
	public String pathsSeparator() {
		return File.pathSeparator;
	}
	
	@Override
	public String dirSeparator() {
		return File.separator;
	}
	
	@Override
	public ByLineReader readFile(String file) throws FileNotFoundException, IOException {
		return new NoCharsetByLineReader(new FileInputStream(file));
	}
	
	@Override
	public ByLineReader readFile(String file, Charset cs) throws IllegalStateException, FileNotFoundException, IOException {
		return new CharsetByLineReader(new FileInputStream(file), cs);
	}
	
	@Override
	public ByLineReader readFileDetectCharset(String file, CharsetDeciderDecoder cdd) throws FileNotFoundException, IOException {
		return new MultiCharsetByLineReader(new FileInputStream(file), cdd);
	}
	
	@Override
	public boolean isWindows() {
		return isLocalhostWindows();
	}

	@Override
	public boolean delete(String path) {
		return ccm.delete(this, path, false);
	}
	
	@Override
	public boolean deleteElevated(String path) {
		return ccm.delete(this, path, true);
	}
	
	@Override
	public boolean exists(String path) {
		return new File(path).exists();
	}
	
	@Override
	public boolean isDirectory(String path) {
		return new File(path).isDirectory();
	}

	@Override
	public boolean saveTextFile(String filename, String text) throws IOException {
		return saveTextFile(filename, text, null);
	}

	@Override
	public boolean saveTextFile(String filename, String text, CharsetEncoder ce) throws IOException {
		if (!isSafePath(filename))
			return false;
		if (text==null)
			text = "";
		FileOutputStream fos = new FileOutputStream(filename);
		try {
			if (ce==null) {
				fos.write(text.getBytes());
			} else {
				ByteBuffer bbuf = ByteBuffer.allocate(50+(text.length()*2));
				ce.encode(CharBuffer.wrap(text.toCharArray()), bbuf, true);
				fos.write(bbuf.array(), 0, bbuf.limit());
			}
		} finally {
			fos.close();
		}
		return true;
	}

	@Override
	public ExecOutput execOut(String commandline, int timeout, Map<String,String> env, byte[] stdin, Charset charset, String chdir) throws Exception {
		return execOut(commandline, timeout, env, stdin, charset, chdir, null, NO_TIMEOUT);
	}
	@Override
	public LocalExecHandle execThread(String commandline, Map<String,String> env, String chdir, byte[] stdin_data) throws Exception {
		return exec_impl(splitCmdString(commandline), env, chdir, stdin_data);
	}
	@Override
	public ExecOutput execOut(final String commandline, int timeout, Map<String,String> env, byte[] stdin_data, Charset charset, String chdir, TestPackRunnerThread thread, int thread_slow_sec) throws Exception {
		

		LocalExecHandle eh = exec_impl(splitCmdString(commandline), env, chdir, stdin_data); 
			
		StringBuilder output_sb = new StringBuilder(1024);
		
		eh.run(output_sb, charset, timeout, thread, thread_slow_sec);
		
		
		
		final ExecOutput out = new ExecOutput();
		out.cmd = commandline;
		out.output = output_sb.toString();
		out.charset = eh.charset;
		out.exit_code = eh.getExitCode();
		
		return out;	
	}
	
	protected static class ThreadSlowTask extends TimerTask {
		protected final TestPackRunnerThread thread;
		
		protected ThreadSlowTask(TestPackRunnerThread thread) {
			this.thread = thread;
		}
		
		@Override
		public void run() {
			thread.notifySlowTest();
		}
		
	}
	
	@Override
	public String getContents(String file) throws IOException {
		NoCharsetByLineReader reader = new NoCharsetByLineReader(new FileInputStream(file));
		String str = IOUtil.toString(reader, IOUtil.HALF_MEGABYTE);
		reader.close();
		return str;
	}
	
	@Override
	public String getContentsDetectCharset(String file, CharsetDeciderDecoder cdd) throws IOException {
		MultiCharsetByLineReader reader = new MultiCharsetByLineReader(new FileInputStream(file), cdd);
		String str = IOUtil.toString(reader, IOUtil.HALF_MEGABYTE);
		reader.close();
		return str;
	}
	
	@Override
	public boolean copy(String src, String dst) throws Exception {
		return ccm.copy(this, src, dst, false);
	}
	
	@Override
	public boolean copyElevated(String src, String dst) throws Exception {
		return ccm.copy(this, src, dst, true);
	}
	
	@Override
	public boolean move(String src, String dst) throws Exception {
		return ccm.move(this, src, dst, false);
	}
	
	@Override
	public boolean moveElevated(String src, String dst) throws Exception {
		return ccm.move(this, src, dst, true);
	}
	
	@Override
	public String getUsername() {
		return System.getProperty("user.name");
	}
	
	public class LocalExecHandle extends ExecHandle {
		protected int exit_code = 0;
		protected Process process;
		protected OutputStream stdin;
		protected InputStream stdout, stderr;
		protected ExitMonitorTask task;
		protected String image_name;
		protected Charset charset;
		
		public LocalExecHandle(Process process, OutputStream stdin, InputStream stdout, InputStream stderr, String[] cmd_array) {
			this.process = process;
			this.stdin = stdin;
			this.stdout = stdout;
			this.stderr = stderr;
			this.image_name = StringUtil.unquote(basename(cmd_array[0]));
			if (isLocalhostWindows()) {
				if (this.image_name.endsWith(".cmd"))
					this.image_name = "cmd.exe"; // IMPORTANT: this is how its identified in the Windows process table
				else
					this.image_name = this.image_name.toLowerCase();
			}
		}
		
		@Override
		public boolean isRunning() {
			try {
				process.exitValue();
				return false;
			} catch ( IllegalThreadStateException ex ) {
				return true;
			}
		}

		boolean run = true;
		@Override
		public synchronized void close(boolean force) {
			run = false;
			
			// may take multiple tries to make it exit (lots of processes, certain OSes, etc...)
			for ( int tries = 0 ; tries < 10 ; tries++ ) {
				// 
				//
				try {
					process.exitValue();
					break; 
					// process terminated, stop trying (or may terminate new process reusing the same id)
				} catch ( Throwable t ) {
					// kill it
					//
					// Windows BN: process trees on Windows won't get terminated correctly by calling Process#destroy
					// have to do some special stuff on Windows
					if (isLocalhostWindows()&&!image_name.equals("taskkill")&&!image_name.equals("taskkill.exe")) {
						try {
							// @see https://github.com/kohsuke/winp
							WinProcess wprocess = new WinProcess(process);
							final int pid = getWindowsProcessIDReflection(process);// TODO wprocess.getPid();
							
							// make sure we found a process id (safety check: make sure its not our process id)
							if (pid!=self_process_id) {
								if (!image_name.equals("cmd.exe")&&!image_name.equals("conhost.exe")) {
									// Process#destroy works for processes except for those that
									// are launched using cmd.exe (the parent of those processes is conhost.exe)
									if (tries==0) {
										// may cause AV in JVM if you call both WinProcess#killRecursively and then WinProcess#kill (vice-versa)
										//
										// calls Win32 TerminateProcess()
										wprocess.kill();
										// also, WinProcess#killRecursively only checks by process id (not image/program name)
										// while that should be enough, experience on Windows has shown that it isn't and somehow gets PFTT killed eventually
										//
										// Windows Note: Windows does NOT automatically terminate child processes when the parent gets killed
										//               the only way that happens is if you search for the child processes FIRST yourself,
										//               (and then their children, etc...) and then kill them.
									} else if (tries==1) {
										// NOTE: on Windows, if WER is not disabled(enabled by default), if a process crashes,
										//       WER popup will appear and process will block until popup closed
										//       -this can be detected by looking for `C:\Windows\SysWOW64\WerFault.exe -u -p <process id> -s 1032`
										if (ccm.ensureWERFaultIsNotRunning(LocalHost.this, pid)) {
											// WER just killed, try again
											wprocess.kill();
										}
									}
								}
								if (force&&tries==1||tries>3) {
									if(!image_name.equals("handle")&&!image_name.equals("handle.exe")) {
										// may have left some handles open... particularly for \devices\AFD, which may be preventing it from closing
										ccm.winCloseAllHandles(LocalHost.this, pid);
									}
								}
								ccm.winKillProcess(LocalHost.this, image_name, pid);
							}
						} catch ( Throwable t2 ) {
							final int pid = getWindowsProcessIDReflection(process);
							
							// make sure we found a process id (safety check: make sure its not our process id)
							if (pid!=self_process_id) {
								if (force&&tries==0||tries>3) {
									if(!image_name.equals("handle")&&!image_name.equals("handle.exe")) {
										// may have left some handles open... particularly for \devices\AFD, which may be preventing it from closing
										ccm.winCloseAllHandles(LocalHost.this, pid);
									}
								}
								ccm.ensureWERFaultIsNotRunning(LocalHost.this, pid);
								ccm.winKillProcess(LocalHost.this, image_name, pid);
							}
						}
					} // end if
					//				
					
					// terminate through java Process API
					// this works on Linux and is a fallback on Windows
					try {
						process.destroy();
					} catch ( Throwable t2 ) {
						t2.printStackTrace();
					}
					//
				} // end try
			} // end for
		} // end public void close
		
		protected void run(StringBuilder output_sb, Charset charset) throws IOException, InterruptedException {
			exec_copy_lines(output_sb, stdout, charset);
			// ignores STDERR
			
			//
			if (isWindows()) {
				// BN: sometimes crashing processes can cause an infinite loop in Process#waitFor
				//       it is unclear what makes those crashing processes different... may be the order they occur in.
				for (int time = 50;;) {
					try {
						exit_code = process.exitValue();
						break;
					} catch ( IllegalThreadStateException ex ) {}
					try {
						Thread.sleep(time);
					} catch ( InterruptedException ex ) {
						break;
					}
					time *= 2;
					if (time>=400)
						time = 50; // 50 100 200 400
				}
			} else {
				exit_code = process.waitFor();
			}
			//
			
			try {
				process.destroy();
			} catch ( Exception ex ) {}
			if (task!=null)
				task.cancel();
		} // end protected void run
				
		protected void exec_copy_lines(StringBuilder sb, InputStream in, Charset charset) throws IOException {
			DefaultCharsetDeciderDecoder d = charset == null ? null : PhptTestCase.newCharsetDeciderDecoder();
			ByLineReader reader = charset == null ? new NoCharsetByLineReader(in) : new MultiCharsetByLineReader(in, d);
			String line;
			try {
				while (reader.hasMoreLines()&&run) {
					line = reader.readLine();
					if (line==null)
						break;
					sb.append(line);
					sb.append('\n');
				}
			} catch ( IOException ex ) {
				//ex.printStackTrace();
			}
			
			in.close();
			
			if (reader instanceof AbstractDetectingCharsetReader)
				this.charset = ((AbstractDetectingCharsetReader)reader).cs;// TODO d.getCommonCharset();
		}

		@Override
		public String getOutput(int max_len)  throws IOException {
			return IOUtil.toString(getSTDOUT(), max_len);
		}

		@Override
		public int getExitCode() {
			return exit_code;
		}

		public int getProcessID() {
			return isLocalhostWindows() ? getWindowsProcessID(process) : 0;
		}

		@Override
		public InputStream getSTDOUT() {
			return process.getInputStream();
		}

		@Override
		public OutputStream getSTDIN() {
			return process.getOutputStream();
		}

		@Override
		public void run(StringBuilder output_sb, Charset charset, int timeout_sec, TestPackRunnerThread thread, int thread_slow_sec) throws IOException, InterruptedException {
			ExitMonitorTask a = null;
			ThreadSlowTask b = null;
			if (thread!=null && thread_slow_sec>NO_TIMEOUT) {
				b = new ThreadSlowTask(thread);
				timer.schedule(b, thread_slow_sec * 1000);
			}
			
			if (timeout_sec>NO_TIMEOUT) {
				a = new ExitMonitorTask(this);
				timer.schedule(a, timeout_sec*1000);
			}
			
			
			this.run(output_sb, charset);
			
			if (a!=null)
				a.cancel();
			if (b!=null)
				b.cancel();
		}
		
	} // end public class LocalExecHandle
	
	public static int getWindowsProcessID(Process process) {
		try {
			// clean way
			WinProcess wproc = new WinProcess(process);
			return wproc.getPid();
		} catch ( Throwable wt ) {
			return getWindowsProcessIDReflection(process);
		}
	}
	
	protected static int getWindowsProcessIDReflection(Process process) {
		// WinProcess native code couldn't be loaded
		// (maybe it wasn't included or maybe somebody didn't compile it)
		//
		// fallback on some old code using reflection, etc...
		try {
			// process.getClass() != Process.class
			//
			// kind of a hack to get the process id:
			//      look through hidden fields to find a field like java.lang.ProcessImpl#handle (long)
			for (java.lang.reflect.Field f : process.getClass().getDeclaredFields() ) {
				if (f.getType()==long.class) { // ProcessImpl#handle
					// this is a private field. without this, #getLong will throw an IllegalAccessException
					f.setAccessible(true); 
					
					long handle = f.getLong(process);
					
					HANDLE h = new HANDLE();
					h.setPointer(Pointer.createConstant(handle));
					return Kernel32.INSTANCE.GetProcessId(h);
				}
			} // end for
		} catch ( Throwable t2 ) {
			t2.printStackTrace();
		} // end try
		return 0;
	} // end protected static int getWindowsProcessIDReflection
	
	private static final Pattern PAT_QUOTE = Pattern.compile("\\\"");
	public static String[] splitCmdString(String command) {
		LinkedList<String> parts = new LinkedList<String>();
		String buf = "";
		char c;
		boolean in_quote = false;
		for ( int i=0 ; i < command.length() ; i++ ) {
			c = command.charAt(i);
			if (c=='\"' && (i==0||command.charAt(i-1) != '\\')) {
				in_quote = !in_quote;
			}
			if (c == ' ' && !in_quote) {
				buf = buf.trim();
				if (buf.length() > 0) {
					buf = StringUtil.unquote(buf);
					
					buf = StringUtil.replaceAll(PAT_QUOTE, "\"", buf);
					
					parts.add(buf);
				}
				buf = "";
				continue;
			}
			buf += c;
		}
		buf = buf.trim();
		if (buf.length() > 0) {
			if (buf.startsWith("\"")) {
				buf = buf.substring(1, buf.length()-1);
				
				buf = StringUtil.replaceAll(PAT_QUOTE, "\"", buf);
			}
			parts.add(buf);
		}
		
		return (String[])parts.toArray(new String[]{});
	} // end public static String[] splitCmdString
	
	protected LocalExecHandle exec_impl(String[] cmd_array, Map<String,String> env, String chdir, byte[] stdin_data) throws IOException, InterruptedException {
		ProcessBuilder builder = new ProcessBuilder(cmd_array);
		if (env!=null) {
			//
			if (env.containsKey(PATH)) {
				String a = System.getenv(PATH);
				String b = env.get(PATH);
				
				if (StringUtil.isNotEmpty(a) && StringUtil.isNotEmpty(b)) {
					b = a + pathsSeparator() + b;
					
					env.put(PATH, b);
				}
			}
			//
			
			builder.environment().putAll(env);
		}
		if (StringUtil.isNotEmpty(chdir))
			builder.directory(new File(chdir));
		builder.redirectErrorStream(true);
			      
		// start the process
		Process process;
		try {
			process = builder.start();
		} catch ( IOException ex ) {
			if (ex.getMessage().contains("file busy")) {
				// randomly sometimes on Linux, get this problem ... wait and try again
				Thread.sleep(100);
				process = builder.start();
			} else {
				throw ex;
			}
		}
			      
		OutputStream stdin = process.getOutputStream();
		
		if (stdin_data!=null && stdin_data.length>0) {
			stdin.write(stdin_data);
			stdin.flush();
		}
		
		InputStream stdout = process.getInputStream();
		InputStream stderr = process.getErrorStream();

		LocalExecHandle h = new LocalExecHandle(process, stdin, stdout, stderr, cmd_array);
		
		
		
		return h;
	} // end protected LocalExecHandle exec_impl
		
	protected static class ExitMonitorTask extends TimerTask {
		protected final LocalExecHandle h;
		
		protected ExitMonitorTask(LocalExecHandle h) {
			this.h = h;
		}
		
		@Override
		public void run() {
			// go further trying to kill the process
			//
			// LocalHostExecHandle#close checks for WerFault.exe blocking on Windows
			h.close(true);
		}
		
	} // end protected static class ExitMonitorTask	

	@Override
	public String getEnvValue(String name) {
		return System.getenv(name);
	}

	@Override
	public boolean equals(Object o) {
		return o == this || o instanceof LocalHost;
	}
	
	@Override
	public int hashCode() {
		return Integer.MAX_VALUE;
	}

	@Override
	public String getHostname() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			return null;
		}
	}

	@Override
	public boolean mkdirs(String path) throws IllegalStateException, IOException {
		if (!isSafePath(path))
			return false;
		
		new File(path).mkdirs();
		return true;
	}

	@Override
	public void downloadCompressWith7Zip(ConsoleManager cm, String ctx_str, String src, AHost src_host, String dst) throws IllegalStateException, IOException, Exception {
		if (cm!=null)
			cm.println(EPrintType.IN_PROGRESS, ctx_str, "copying src="+src+" dst="+dst);
		download(src, dst);
	}

	@Override
	public boolean download(String src, String dst) throws IllegalStateException, IOException, Exception {
		return copy(src, dst);
	}

	@Override
	public void uploadCompressWith7Zip(ConsoleManager cm, String ctx_str, AHost dst_host, String src, String dst) throws IllegalStateException, IOException, Exception {
		if (cm!=null)
			cm.println(EPrintType.IN_PROGRESS, ctx_str, "copying src="+src+" dst="+dst);
		upload(src, dst);
	}

	@Override
	public boolean upload(String src, String dst) throws IllegalStateException, IOException, Exception {
		if (!isSafePath(dst))
			return false;
		
		return copy(src, dst);
	}

	@Override
	public boolean isRemote() {
		return false;
	}

	@Override
	public String getOSNameLong() {
		return System.getProperty("os.name");
	}

	protected String addr;
	@Override
	public String getAddress() {
		if (this.addr!=null)
			return this.addr;
		try {
			Enumeration<NetworkInterface> interfaces;
			Enumeration<InetAddress> addrs;
			NetworkInterface ni;
			InetAddress addr;
			String addr_str;
			interfaces = NetworkInterface.getNetworkInterfaces();
			// try to get an IPv4 address first (its shorter, easier for people to work with)
			while (interfaces.hasMoreElements()) {
				ni = interfaces.nextElement();
				addrs = ni.getInetAddresses();
				while (addrs.hasMoreElements()) {
					addr = addrs.nextElement();
					addr_str = addr.getHostAddress();
					if (addr_str.equals("127.0.0.1"))
						// loopback address, which is not bindable on Windows
						continue;
					else if (addr_str.split("\\.").length==4)
						// IPv4 address
						return this.addr = addr_str;
				}
			}
			// fallback on an IPv6 address
			interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				ni = interfaces.nextElement();
				addrs = ni.getInetAddresses();
				while (addrs.hasMoreElements()) {
					addr = addrs.nextElement();
					addr_str = addr.getHostAddress();
					if (addr_str.equals("0:0:0:0:0:0:0:1")||addr_str.equals("0:0:0:0:0:0:0:0"))
						// loopback address, which is not bindable on Windows
						continue;
					else if (addr_str.contains(":")&&!addr_str.contains("%"))
						// ex: 2001:0:4137:9e76:3cb8:730:3f57:feaf
						return this.addr = "["+addr_str+"]";
				}
			}
		} catch (SocketException ex) {
			ex.printStackTrace();
		}
		// no network interfaces!
		return null;
	} // end public String getAddress

	@Override
	public boolean isOpen() {
		return true;
	}

	@Override
	protected String getOSNameOnWindows() {
		return getOSNameLong();
	}

	@Override
	public boolean dirContainsExact(String path, String name) {
		for ( File file : new File(path).listFiles() ) {
			if (file.getName().equalsIgnoreCase(name))
				return true;
		}
		return false;
	}

	@Override
	public boolean dirContainsFragment(String path, String name_fragment) {
		name_fragment = name_fragment.toLowerCase();
		for ( File file : new File(path).listFiles() ) {
			if (file.getName().toLowerCase().contains(name_fragment))
				return true;
		}
		return false;
	}

	@Override
	public String[] list(String path) {
		return new File(path).list();
	}
	
	public static String getLocalPfttDir() {
		if (isLocalhostWindows()) {
			String sd = System.getenv("SYSTEMDRIVE");
			if (StringUtil.isEmpty(sd))
				sd = "C:";
			if (DEV>0)
				return sd+"\\php-sdk\\PFTT\\Dev-"+DEV+"\\";
			else
				return sd+"\\php-sdk\\PFTT\\Current\\";
		} else if (DEV>0) {
			return System.getenv("HOME")+"/php-sdk/PFTT/dev-"+DEV+"/";
		} else {
			return System.getenv("HOME")+"/php-sdk/PFTT/current/";
		}
	}

	@Override
	public long getSize(String file) {
		return new File(file).length();
	}
	
	@Override
	public long getMTime(String file) {
		return new File(file).lastModified();
	}

	@Override
	public boolean deleteFileExtension(String dir_str, String ext) {
		if (!isSafePath(dir_str))
			return false;
		
		_deleteFileExtension(new File(dir_str), ext);
		return true;
	}
	
	protected void _deleteFileExtension(File dir, String ext) {
		File[] files = dir.listFiles();
		if (files==null)
			return;
		for ( File file : files ) {
			if ( file.isDirectory() )
				_deleteFileExtension(file, ext);
			else if ( file.getName().endsWith(ext) )
				file.delete();
		}
		
	}

	@Override
	protected boolean deleteSingleFile(String path) {
		return new File(path).delete();
	}
	
} // end public class Host
