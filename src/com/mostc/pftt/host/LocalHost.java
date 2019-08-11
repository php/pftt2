package com.mostc.pftt.host;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Thread.UncaughtExceptionHandler;
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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jvnet.winp.WinProcess;

import com.github.mattficken.io.AbstractDetectingCharsetReader;
import com.github.mattficken.io.ArrayUtil;
import com.github.mattficken.io.ByLineReader;
import com.github.mattficken.io.CharsetByLineReader;
import com.github.mattficken.io.CharsetDeciderDecoder;
import com.github.mattficken.io.DefaultCharsetDeciderDecoder;
import com.github.mattficken.io.IOUtil;
import com.github.mattficken.io.MultiCharsetByLineReader;
import com.github.mattficken.io.NoCharsetByLineReader;
import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.main.PfttMain;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ConsoleManagerUtil;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.runner.AbstractTestPackRunner.TestPackRunnerThread;
import com.mostc.pftt.scenario.FileSystemScenario;
import com.mostc.pftt.scenario.FileSystemScenario.IFileChooser;
import com.mostc.pftt.util.TimerUtil;
import com.mostc.pftt.util.TimerUtil.ObjectRunnable;
import com.mostc.pftt.util.TimerUtil.TimerThread;
import com.mostc.pftt.util.TimerUtil.WaitableRunnable;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT.HANDLE;

/** Represents the local Host that the program is currently running on.
 * 
 * LocalHost is fairly straightforward for Linux.
 * 
 * Windows has many issues running large numbers of processes or large numbers of filesystem operations. LocalHost
 * has several special internal mechanisms to try to contain and manage those issues so the rest of PFTT doesn't have to deal with them.
 *
 * @see SSHHost
 * @author Matt Ficken
 *
 */

@SuppressWarnings("unused")
public abstract class LocalHost extends AHost {
	private static final boolean is_windows = System.getProperty("os.name").toLowerCase().contains("windows");
	protected static int self_process_id;
	private static LocalHost localhost_instance;
	static {
		if (isLocalhostWindows()) {
			// only need this on windows (see WindowsLocalExecHandle#close)
			try {
				// this works only on Windows
				self_process_id = Kernel32.INSTANCE.GetCurrentProcessId();
			} catch ( Throwable t ) {
			}
			
			localhost_instance = new WindowsLocalHost();
		} else {
			localhost_instance = new PosixLocalHost();
		}
		
		// ensure TEMP dir exists
		try {
			localhost_instance.mCreateDirs(localhost_instance.getTempDir());
		} catch ( Exception ex ) {}
	}
	
	public static LocalHost getInstance() {
		return localhost_instance;
	}
	
	protected final CommonCommandManager ccm; // share some 'shelling out' code with SSHHost
	protected final HashMap<Thread,Object> close_thread_set; // for LocalExecHandle#close
	protected static final AtomicInteger active_proc_counter = new AtomicInteger();
	
	public LocalHost() {
		close_thread_set = new HashMap<Thread,Object>();
		ccm = new CommonCommandManager();
	}
	
	public int getActiveProcessCount() {
		return staticGetActiveProcessCount();
	}
	
	public int staticGetActiveProcessCount() {
		return active_proc_counter.get();
	}
	
	protected static final AtomicInteger wait_runnable_thread_counter = new AtomicInteger();
	protected <E extends Object> E runWaitRunnable(String name_prefix, int seconds, final ObjectRunnable<E> r) throws Exception {
		int a = wait_runnable_thread_counter.incrementAndGet();
		if (a > Math.max(600, getActiveProcessCount()*1.8)) {
			int i=0;
			do {
				try {
					i++;
					Thread.sleep(50*i);
				} catch (InterruptedException e) {}
			} while (wait_runnable_thread_counter.get() > Math.max(400, getActiveProcessCount()*1.4) && i < 40);
		}
		WaitableRunnable<E> h;
		try {
			h = TimerUtil.runWaitSeconds(name_prefix, seconds, r);
		} finally {
			wait_runnable_thread_counter.decrementAndGet();
		}
		if (h==null)
			return null;
		else if (h.getException()!=null)
			throw h.getException();
		else
			return h.getResult();
	} // end protected E runWaitRunnable
	
	public static boolean isLocalhostWindows() {
		return is_windows;
	}
	
	static {
		if (DEV>0) {
			new File(LocalHost.getInstance().getPfttDir()).mkdirs();
		}
	}
	
	@Override
	public String getName() {
		return "Localhost";
	}
	
	@Override
	public String mPathsSeparator() {
		return File.pathSeparator;
	}
	
	@Override
	public String mDirSeparator() {
		return File.separator;
	}
	
	@Override
	public ByLineReader mReadFile(String file) throws FileNotFoundException, IOException {
		return new NoCharsetByLineReader(new FileInputStream(file));
	}
	
	@Override
	public ByLineReader mReadFile(String file, Charset cs) throws IllegalStateException, FileNotFoundException, IOException {
		return new CharsetByLineReader(new FileInputStream(file), cs);
	}
	
	@Override
	public ByLineReader mReadFileDetectCharset(String file, CharsetDeciderDecoder cdd) throws FileNotFoundException, IOException {
		return new MultiCharsetByLineReader(new FileInputStream(file), cdd);
	}
	
	@Override
	public boolean mDelete(String path) {
		return ccm.delete(this, path, false);
	}
	
	@Override
	public boolean mDeleteElevated(String path) {
		return ccm.delete(this, path, true);
	}
	
	@Override
	public boolean mDeleteFileExtension(String dir_str, String ext) {
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
	public boolean mExists(String path) {
		return new File(path).exists();
	}
	
	@Override
	public boolean mIsDirectory(String path) {
		return new File(path).isDirectory();
	}

	@Override
	public boolean mSaveTextFile(String filename, String text) throws IOException {
		return mSaveTextFile(filename, text, null);
	}
	
	@Override
	public boolean mSaveFile(String filename, byte[] stdin_post) throws IllegalStateException, IOException {
		if (!isSafePath(filename)) {
			return false;
		} else if (stdin_post==null) {
			return false;
		}		
		mCreateDirs(FileSystemScenario.dirname(filename));// TODO temp
		FileOutputStream fos = new FileOutputStream(filename);
		fos.write(stdin_post);
		fos.close();
		return true;
	}

	@Override
	public boolean mSaveTextFile(String filename, String text, CharsetEncoder ce) throws IOException {
		if (!isSafePath(filename))
			return false;
		if (text==null)
			text = "";
		mCreateDirs(FileSystemScenario.dirname(filename));// TODO temp
		FileOutputStream fos = new FileOutputStream(filename);
		try {
			if (ce==null) {
				fos.write(text.getBytes());
			} else {
				ByteBuffer bbuf = ByteBuffer.allocate(50+(text.length()*2));
				ce.encode(CharBuffer.wrap(text.toCharArray()), bbuf, true);
				fos.write(bbuf.array(), 0, bbuf.position());
			}
		} finally {
			fos.close();
		}
		return true;
	}

	@Override
	public ExecOutput execOut(String commandline, int timeout, Map<String,String> env, byte[] stdin, Charset charset, String chdir, boolean wrap_child) throws Exception {
		return execOut(commandline, timeout, env, stdin, charset, chdir, null, NO_TIMEOUT, wrap_child);
	}
	@Override
	public LocalExecHandle execThread(String commandline, Map<String,String> env, String chdir, byte[] stdin_data, boolean wrap_child) throws Exception {
		return exec_impl(wrapSplitCmdString(wrap_child, commandline), env, chdir, stdin_data);
	}
	@SuppressWarnings("rawtypes")
	@Override
	public ExecOutput execOut(final String commandline, int timeout, Map<String,String> env, byte[] stdin_data, Charset charset, String chdir, TestPackRunnerThread thread, int thread_slow_sec, boolean wrap_child) throws Exception {
		LocalExecHandle eh = exec_impl(wrapSplitCmdString(wrap_child, commandline), env, chdir, stdin_data); 
			
		StringBuilder output_sb = new StringBuilder(1024);
		
		eh.run(null, output_sb, charset, timeout, thread, thread_slow_sec, 0, IOUtil.HALF_MEGABYTE);
		
		
		
		final ExecOutput out = new ExecOutput();
		out.cmd = commandline;
		out.output = output_sb.toString();
		out.charset = eh.charset;
		out.exit_code = eh.getExitCode();
		
		return out;	
	}
	
	protected static class ThreadSlowTask implements Runnable {
		@SuppressWarnings("rawtypes")
		protected final TestPackRunnerThread thread;
		
		protected ThreadSlowTask(@SuppressWarnings("rawtypes") TestPackRunnerThread thread) {
			this.thread = thread;
		}
		
		@Override
		public void run() {
			thread.notifySlowTest();
		}
		
	}
	
	@Override
	public String mGetContents(String file) throws IOException {
		NoCharsetByLineReader reader = new NoCharsetByLineReader(new FileInputStream(file));
		String str = IOUtil.toString(reader, IOUtil.HALF_MEGABYTE);
		reader.close();
		return str;
	}
	
	@Override
	public String mGetContentsDetectCharset(String file, CharsetDeciderDecoder cdd) throws IOException {
		MultiCharsetByLineReader reader = new MultiCharsetByLineReader(new FileInputStream(file), cdd);
		String str = IOUtil.toString(reader, IOUtil.HALF_MEGABYTE);
		reader.close();
		return str;
	}
	
	@Override
	public boolean mCopy(String src, String dst) throws Exception {
		return ccm.copy(this, src, dst, false);
	}
	
	@Override
	public boolean mCopyElevated(String src, String dst) throws Exception {
		return ccm.copy(this, src, dst, true);
	}
	
	@Override
	public boolean mMove(String src, String dst) throws Exception {
		return ccm.move(this, src, dst, false);
	}
	
	@Override
	public boolean mMoveElevated(String src, String dst) throws Exception {
		return ccm.move(this, src, dst, true);
	}
	
	@Override
	public String getUsername() {
		return System.getProperty("user.name");
	}
	
	protected static final UncaughtExceptionHandler  IGNORE_EXCEPTION_HANDLER = new UncaughtExceptionHandler () {
			@Override
			public void uncaughtException(Thread arg0, Throwable arg1) {
				// ignore, do nothing
			}
		};
	public abstract class LocalExecHandle extends ExecHandle {
		protected int exit_code = 0;
		protected final AtomicReference<Process> process;
		protected OutputStream stdin;
		protected InputStream stdout, stderr;
		protected String image_name;
		protected Charset charset;
		protected final AtomicBoolean wait = new AtomicBoolean(true), timedout = new AtomicBoolean(false);
		
		public LocalExecHandle(Process process, OutputStream stdin, InputStream stdout, InputStream stderr, String[] cmd_array) {
			this.process = new AtomicReference<Process>(process);
			this.stdin = stdin;
			this.stdout = stdout;
			this.stderr = stderr;
			this.image_name = cmd_array==null||cmd_array.length==0?"":StringUtil.unquote(FileSystemScenario.basename(cmd_array[0]));
		}
		
		protected boolean doIsRunning(Process p) {
			try {
				p.exitValue();
				return false;
			} catch ( IllegalThreadStateException ex ) {
				return true;
			}
		}
		
		protected abstract void doClose(Process p, int tries);

		@Override
		public synchronized void close(ConsoleManager cm, final boolean force) {
			if (cm != null && cm.isPfttDebug())
				new IllegalArgumentException().printStackTrace();
			final Process p = this.process.get();
			if (p==null)
				return;
			/*synchronized(run) {
				run.notifyAll();
			}*/
			
			// sometimes it can take a while to #close a process(especially on Windows)... do it in a thread
			// to avoid blocking for too long. however, we don't want to have too many threads
			//
			// don't let any calling thread have more than 1 close thread 
			final Thread calling_thread = Thread.currentThread();
			final Object tlock;
			Object lock;
			synchronized(close_thread_set) {
				lock = close_thread_set.get(calling_thread);
				if (lock==null) {
					tlock = new Object();
					close_thread_set.put(calling_thread, tlock);
				} else {
					tlock = lock;
				}
			}
			if (lock!=null) {
				synchronized(tlock) {
					try {
						lock.wait(30000);
					} catch (InterruptedException e) {}
				}
			}
			
			final Thread close_thread = TimerUtil.runThread("Close", new Runnable() {
					@Override
					public void run() {
						// may take multiple tries to make it exit (lots of processes, certain OSes, etc...)
						for ( int tries = 0 ; tries < 10 ; tries++ ) {
							// 
							//
							if (doIsRunning(p)) {
								if (stdout!=null) {
									try {
										stdout.close();
									} catch ( Throwable t2 ) {}
									stdout = null;
								}
								// kill it
								//
								doClose(p, tries);
								//
							} else {
								// process terminated, stop trying (or may terminate new process reusing the same id)
								break;
							}
						} // end for
						// by now process should be dead/should have stopped writing
						// so #exec_copy_lines should stop (which will stop blocking whatever called #exec_impl or #exec or #execOut)
						wait.set(false);
						
						synchronized(close_thread_set) {
							if (close_thread_set.get(calling_thread)==tlock)
								close_thread_set.remove(calling_thread);
						}
						synchronized(tlock) {
							tlock.notifyAll();
						}
						
						// encourage JVM to free up the Windows process handle (may have problems if too many are left open too long)
						process.set(null);
						System.gc();
					} // end public void run
				});
		} // end public void close
		
		protected abstract void runSuspend(Process p, int suspend_seconds) throws InterruptedException;
		
		protected void run(StringBuilder output_sb, int max_chars, Charset charset, int suspend_seconds) throws IOException, InterruptedException {
			final Process p = process.get();
			if (p==null)
				return;
			runSuspend(p, suspend_seconds);
			
			// read process' output (block until #close or exit)
			exec_copy_lines(output_sb, max_chars, stdout, charset);
			// ignores STDERR
			for(;;) {
				if (!doIsRunning(p))
					break;
				Thread.sleep(50);
			}
			// wait for process exit (shouldn't get here until exit or #close though)
			/*for (int time = 50;wait.get();) {
				try {
					exit_code = p.exitValue();
					break;
				} catch ( IllegalThreadStateException ex ) {}
				try {
					Thread.sleep(time);
				} catch ( InterruptedException ex ) {
					break;
				}
				time *= 2; // wait longer next time
				if (time>=400) {
					if (run.get()) {
						// limit max growth of time between checking `wait`
						time = 50; // 50 100 200 400
					} else {
						// #close has now been called, but may not have
						// finished yet (`wait` may still be true)
						//
						// waited long enough (750ms) anyway though, since #close
						// will terminate the process eventually, return control to
						// code that originally called #exec or #execThread
						break;
					}
				}
			}*/
			//

			// try to set exit code after execution
			try {
				exit_code = p.exitValue();
			} catch ( IllegalThreadStateException ex ) {}
			
			active_proc_counter.decrementAndGet();
			
			// free up process handle
			if (process.get()!=null) {
				// don't call #destroy on this process if #close already has
				//
				ensureClosedAfterRun(p);
			}
			
			// encourage JVM to free up the Windows process handle (may have problems if too many are left open too long)
			process.set(null);
			System.gc();
		} // end protected void run
		
		protected abstract void ensureClosedAfterRun(Process p);
				
		protected abstract void exec_copy_lines(final StringBuilder sb, final int max_chars, final InputStream in, final Charset charset) throws IOException;
		
		protected void do_exec_copy_lines(StringBuilder sb, int max_chars, InputStream in, Charset charset) throws IOException {
			DefaultCharsetDeciderDecoder d = charset == null ? null : PhptTestCase.newCharsetDeciderDecoder();
			ByLineReader reader = charset == null ? new NoCharsetByLineReader(new java.io.BufferedInputStream(in)) : new MultiCharsetByLineReader(in, d);
			String line;
			try {
				while (reader.hasMoreLines()&&wait.get()&&(max_chars<1||sb.length()<max_chars)) {
					line = reader.readLine();
					if (line==null)
						break;
					sb.append(line);
					sb.append('\n');
				}
			} catch ( IOException ex ) {
				ConsoleManagerUtil.printStackTrace(LocalHost.class, ex);
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

		public abstract int getProcessID();

		@Override
		public InputStream getSTDOUT() {
			final Process p = process.get();
			return p==null?null:p.getInputStream();
		}

		@Override
		public OutputStream getSTDIN() {
			final Process p = process.get();
			return p==null?null:p.getOutputStream();
		}

		@Override
		public void run(ConsoleManager cm, StringBuilder output_sb, Charset charset, int timeout_sec, @SuppressWarnings("rawtypes") TestPackRunnerThread thread, int thread_slow_sec, int suspend_seconds, int max_chars) throws IOException, InterruptedException {
			TimerThread a = null, b = null;
			if (thread!=null && thread_slow_sec>NO_TIMEOUT) {
				// TODO get rid of thread_slow_sec feature - just use AbstractLocalTestPackRunner
				//b = TimerUtil.waitSeconds(thread_slow_sec, new ThreadSlowTask(thread));
			}
			
			if (timeout_sec>NO_TIMEOUT) {
				a = TimerUtil.waitSeconds(timeout_sec, new ExitMonitorTask(cm, this));
			}
						
			this.run(output_sb, max_chars, charset, suspend_seconds);
			
			if (a!=null)
				a.close();
			if (b!=null)
				b.close();
		}

		@Override
		public boolean isTimedOut() {
			return timedout.get();
		}
		
	} // end public class LocalExecHandle
	
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
			ConsoleManagerUtil.printStackTrace(LocalHost.class, t2);
		} // end try
		return 0;
	} // end protected static int getWindowsProcessIDReflection
	
	protected abstract String[] wrapSplitCmdString(boolean wrap_child, String command);
	
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
	
	protected abstract Process guardStart(final ProcessBuilder builder) throws Exception, InterruptedException;
	
	protected abstract Process handleExecImplException(Exception ex, ProcessBuilder builder) throws InterruptedException, Exception;
	
	protected LocalExecHandle exec_impl(String[] cmd_array, Map<String,String> env, String chdir, byte[] stdin_data) throws Exception, InterruptedException {
		Process process = null;
		{
			ProcessBuilder builder = new ProcessBuilder(cmd_array);
			if (env!=null) {
				//
				if (env.containsKey(PATH)) {
					String a = System.getenv(PATH);
					String b = env.get(PATH);
					
					if (StringUtil.isNotEmpty(a) && StringUtil.isNotEmpty(b)) {
						b = a + mPathsSeparator() + b;
						
						env.put(PATH, b);
					}
				}
				//
				
				if (env!=null)
					builder.environment().putAll(env);
			}
			if (StringUtil.isNotEmpty(chdir))
				builder.directory(new File(chdir));
			builder.redirectErrorStream(true);
				      
			// start the process
			try {
				process = guardStart(builder);
			} catch ( IOException ex ) {
				process = handleExecImplException(ex, builder);
				if (process==null)
					throw new Exception(ex);
			}
		}
		if (process==null)
			return createLocalExecHandle(process, null, null, null, null);
		
		active_proc_counter.incrementAndGet();
		OutputStream stdin = process.getOutputStream();
		
		if (stdin_data!=null && stdin_data.length>0) {
			stdin.write(stdin_data);
			try {
				stdin.flush();
			} catch ( Exception ex ) {}
		}
		
		InputStream stdout = process.getInputStream();
		InputStream stderr = process.getErrorStream();

		return createLocalExecHandle(process, stdin, stdout, stderr, cmd_array);
	} // end protected LocalExecHandle exec_impl
	
	protected abstract LocalExecHandle createLocalExecHandle(Process process, OutputStream stdin, InputStream stdout, InputStream stderr, String[] cmd_array);
		
	protected static class ExitMonitorTask implements Runnable {
		protected final ConsoleManager cm;
		protected final LocalExecHandle h;
		
		protected ExitMonitorTask(ConsoleManager cm, LocalExecHandle h) {
			this.cm = cm;
			this.h = h;
		}
		
		@Override
		public void run() {
			// go further trying to kill the process
			//
			// WindowsLocalHostExecHandle#close checks for WerFault.exe blocking on Windows
			h.timedout.set(true);
			h.close(cm, true);
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
	public void downloadCompressWith7Zip(ConsoleManager cm, String ctx_str, String src, AHost src_host, String dst) throws IllegalStateException, IOException, Exception {
		if (cm!=null)
			cm.println(EPrintType.IN_PROGRESS, ctx_str, "copying src="+src+" dst="+dst);
		download(src, dst);
	}

	@Override
	public boolean download(String src, String dst) throws IllegalStateException, IOException, Exception {
		return mCopy(src, dst);
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
		
		return mCopy(src, dst);
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
			ConsoleManagerUtil.printStackTrace(LocalHost.class, ex);
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
	public boolean mDirContainsExact(String path, String name) {
		for ( File file : new File(path).listFiles() ) {
			if (file.getName().equalsIgnoreCase(name))
				return true;
		}
		return false;
	}

	@Override
	public boolean mDirContainsFragment(String path, String name_fragment) {
		name_fragment = name_fragment.toLowerCase();
		for ( File file : new File(path).listFiles() ) {
			if (file.getName().toLowerCase().contains(name_fragment))
				return true;
		}
		return false;
	}

	@Override
	public String[] mList(String path) {
		return new File(path).list();
	}
	
	public static String getLocalPfttDir() {
		return System.getenv("PFTT_HOME");
		/*if (isLocalhostWindows()) {
			String php_sdk_dir = System.getenv("PHP_SDK");
			if (null == php_sdk_dir) {
				String sd = System.getenv("SYSTEMDRIVE");
				if (StringUtil.isEmpty(sd))
					sd = "C:";
				php_sdk_dir = sd + "\\php-sdk";
			}
			if (DEV>0)
				return php_sdk_dir+"\\PFTT\\Dev-"+DEV+"\\";
			else
				return php_sdk_dir+"\\PFTT\\Current\\";
		} else if (DEV>0) {
			return System.getenv("HOME")+"/php-sdk/PFTT/dev-"+DEV+"/";
		} else {
			return System.getenv("HOME")+"/php-sdk/PFTT/current/";
		}*/
	}

	@Override
	public long mSize(String file) {
		return new File(file).length();
	}
	
	@Override
	public long mMTime(String file) {
		return new File(file).lastModified();
	}

	@Override
	public boolean mDeleteChosenFiles(String dir, IFileChooser chr) {
		if (!isSafePath(dir))
			return false;
		
		_deleteChosenFiles(new File(dir), chr);
		
		return true;
	}
	
	protected void _deleteChosenFiles(File dir, IFileChooser chr) {
		File[] files = dir.listFiles();
		if (files==null)
			return;
		for ( File file : files ) {
			if ( file.isDirectory() ) {
				_deleteChosenFiles(file, chr);
				if ( chr.choose(dir.getAbsolutePath(), file.getName(), true) )
					file.delete();
			} else if ( chr.choose(dir.getAbsolutePath(), file.getName(), false) ) {
				file.delete();
			}
		}
		
	}

	@Override
	protected boolean deleteSingleFile(String path) {
		return new File(path).delete();
	}
	
	public static String ensureAbsolutePathCWD(String path) {
		if (isLocalhostWindows() &&	StringUtil.isNotEmpty(AHost.drive(path)))
			return path;
		else if (path.startsWith("/"))
			return path;
		else
			return new File(new File(cwd()), path).getAbsolutePath();
	}
	
	public static String cwd() {
		return System.getProperty("user.dir");
	}

	@Override
	public RunRequest createRunRequest(ConsoleManager cm, String ctx_str) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ExecOutput execOut(RunRequest req) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ExecHandle execThread(RunRequest req) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean exec(RunRequest req) {
		// TODO Auto-generated method stub
		return false;
	}
	
} // end public class Host
