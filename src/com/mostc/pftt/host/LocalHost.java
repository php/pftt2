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
import java.util.concurrent.atomic.AtomicReference;
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
import com.mostc.pftt.main.PfttMain;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.runner.AbstractTestPackRunner.TestPackRunnerThread;
import com.mostc.pftt.util.TimerUtil;
import com.mostc.pftt.util.TimerUtil.TimerThread;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT.HANDLE;

/** Represents the local Host that the program is currently running on.
 * 
 * LocalHost is fairly straightforward for Linux.
 * 
 * Windows has issues running large numbers of processes or large numbers of filesystem operations. LocalHost
 * has several special internal mechanisms to try to contain and manage those issues so the rest of PFTT doesn't have to deal with them.
 *
 * @see SSHHost
 * @author Matt Ficken
 *
 */

@SuppressWarnings("unused")
public class LocalHost extends AHost {
	private static final boolean is_windows = System.getProperty("os.name").toLowerCase().contains("windows");
	private static int self_process_id;
	static {
		if (isLocalhostWindows()) {
			// only need this on windows (see LocalExecHandle#close)
			try {
				// this works only on Windows
				self_process_id = Kernel32.INSTANCE.GetCurrentProcessId();
			} catch ( Throwable t ) {
			}
		}
	}
	protected final CommonCommandManager ccm; // share some 'shelling out' code with SSHHost
	protected final HashMap<Thread,Object> close_thread_set; // for LocalExecHandle#close
	
	public LocalHost() {
		close_thread_set = new HashMap<Thread,Object>();
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
	
	private boolean checked_elevate, found_elevate;
	@Override
	public ExecOutput execElevatedOut(String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset, String chdir, TestPackRunnerThread test_thread, int slow_timeout_sec) throws Exception {
		if (isWindows() && StringUtil.isEmpty(getEnvValue("PFTT_SHELL"))) {
			// check if %PFTT_SHELL% is defined then PFTT is running in the
			// PFTT shell which is already elevated, so don't run elevate.exe
			//
			//
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
		
		eh.run(null, output_sb, charset, timeout, thread, thread_slow_sec, 0);
		
		
		
		final ExecOutput out = new ExecOutput();
		out.cmd = commandline;
		out.output = output_sb.toString();
		out.charset = eh.charset;
		out.exit_code = eh.getExitCode();
		
		return out;	
	}
	
	protected static class ThreadSlowTask implements Runnable {
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
	
	protected static final UncaughtExceptionHandler  IGNORE_EXCEPTION_HANDLER = new UncaughtExceptionHandler () {
			@Override
			public void uncaughtException(Thread arg0, Throwable arg1) {
				// ignore, do nothing
			}
		};
	public class LocalExecHandle extends ExecHandle {
		protected int exit_code = 0;
		protected final AtomicReference<Process> process;
		protected OutputStream stdin;
		protected InputStream stdout, stderr;
		protected String image_name;
		protected Charset charset;
		protected final AtomicBoolean run = new AtomicBoolean(true), wait = new AtomicBoolean(true), timedout = new AtomicBoolean(false);
		
		public LocalExecHandle(Process process, OutputStream stdin, InputStream stdout, InputStream stderr, String[] cmd_array) {
			this.process = new AtomicReference<Process>(process);
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
			final Process p = this.process.get();
			if (p==null)
				return false;
			try {
				p.exitValue();
				return false;
			} catch ( IllegalThreadStateException ex ) {
				return true;
			}
		}

		@Override
		public synchronized void close(ConsoleManager cm, final boolean force) {
			if (cm != null && cm.isPfttDebug())
				new IllegalArgumentException().printStackTrace();
			if (!run.get())
				return; // already trying|tried to close
			final Process p = this.process.get();
			if (p==null)
				return;
			// @see #exec_copy_lines
			run.set(false);
			synchronized(run) {
				run.notifyAll();
			}
			
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
			
			final Thread close_thread = new Thread() {
					@Override
					public void run() {
						// may take multiple tries to make it exit (lots of processes, certain OSes, etc...)
						for ( int tries = 0 ; tries < 10 ; tries++ ) {
							// 
							//
							try {
								p.exitValue();
								break; 
								// process terminated, stop trying (or may terminate new process reusing the same id)
							} catch ( Throwable t ) {
								if (stdout!=null) {
									try {
										stdout.close();
									} catch ( Throwable t2 ) {}
									stdout = null;
								}
								// kill it
								//
								// Windows BN: process trees on Windows won't get terminated correctly by calling Process#destroy
								// have to do some special stuff on Windows
								if (isLocalhostWindows()&&!image_name.equals("pskill")&&!image_name.equals("pskill.exe")) {
									try {
										// @see https://github.com/kohsuke/winp
										WinProcess wprocess = new WinProcess(p);
										final int pid = getWindowsProcessIDReflection(p);// NOT? wprocess.getPid();
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
													// can kill off windebug if running under PUTS (windebug shouldn't be running at all, sometimes does)
													if (!PfttMain.is_puts && ccm.ensureWinDebugIsNotRunning(LocalHost.this, pid)) {
														// process should terminate on its own, so don't call #kill here 
														//  (it may kill a different process that has been allocated the same PID)
														//
														// instead, wait for loop to reach #winKillProcess because that'll check by image name and PID
														//   to ensure process is terminated
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
											continue;
										}
									} catch ( Throwable t2 ) {
										final int pid = getWindowsProcessIDReflection(p);
										// make sure we found a process id (safety check: make sure its not our process id)
										if (pid!=self_process_id) {
											if (force&&tries==0||tries>3) {
												if(!image_name.equals("handle")&&!image_name.equals("handle.exe")) {
													// may have left some handles open... particularly for \devices\AFD, which may be preventing it from closing
													//
													// Note: Windows is designed to not let processes be terminated if there are pending IO requests for the process
													//    there can be pending IO requests even if file handles are closed.
													//    this is particularly more complex if DFS/SMB is used, since the pending IO request in Windows
													//    will normally remain pending unless and until it gets a response from the File server
													//    (only then can the process be terminated or exit normally).
													//    to help with this, delete files using the DFS client so it may realize the IO request is not pending any more
													//    @see AbstractPhptTestCaseRunner2#doRunTestClean
													ccm.winCloseAllHandles(LocalHost.this, pid);
												}
											}
											ccm.ensureWERFaultIsNotRunning(LocalHost.this, pid);
											ccm.winKillProcess(LocalHost.this, image_name, pid);
											
											continue;
										}
									}
								} // end if
								//	
								//
								// terminate through java Process API
								// on Linux, this is all we need to do in #close
								// on Windows, this should only be used if all the above failed. 
								//         Windows: #destroy calls Win32 TerminateProcess
								//                  you should not call TerminateProcess if the above succeeded in killing the
								//                  process because TerminateProcess may block (forever) in such cases
								//
								// JVM should realize process was destroyed without Process#destory call when Process#exitValue called
								try {
									p.destroy();
								} catch ( Throwable t2 ) {
									t2.printStackTrace();
								}
								//
							} // end try
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
				};
			close_thread.setName("Close"+close_thread.getName()); // label this thread so it stands out in profiler
			close_thread.start();
		} // end public void close
		
		protected void run(StringBuilder output_sb, Charset charset, int suspend_seconds) throws IOException, InterruptedException {
			final Process p = process.get();
			if (p==null)
				return;
			//
			if (isWindows() && suspend_seconds > 0) {
				final int pid = getWindowsProcessID(p);
				
				final long suspend_millis = suspend_seconds*1000;
				final long suspend_start_time = System.currentTimeMillis();
				// suspend
				try {
					execOut("pssuspend "+pid, 10);
				} catch ( Exception ex ) {}
				final long suspend_run_time = Math.abs(System.currentTimeMillis() - suspend_start_time);
				
				// wait before resuming
				// (assume it'll take same amount of time to resume as it took to suspend => *2)
				Thread.sleep(suspend_millis - (suspend_run_time*2));
				
				// resume
				try {
					execOut("pssuspend -r "+pid, 10);
				} catch ( Exception ex ) {}
			}
			//
			
			// read process' output (block until #close or exit)
			exec_copy_lines(output_sb, stdout, charset);
			// ignores STDERR
			
			// wait for process exit (shouldn't get here until exit or #close though)
			for (int time = 50;wait.get();) {
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
			}
			//
			
			// free up process handle
			if (process.get()!=null) {
				// don't call #destroy on this process if #close already has
				//
				// on Windows, it can block forever
				try {
					p.destroy();
				} catch ( Exception ex ) {}
			}
			
			// encourage JVM to free up the Windows process handle (may have problems if too many are left open too long)
			process.set(null);
			System.gc();
		} // end protected void run
				
		@SuppressWarnings("deprecation")
		protected void exec_copy_lines(final StringBuilder sb, final InputStream in, final Charset charset) throws IOException {
			if (isWindows()) {
				final AtomicBoolean copy_thread_lock = new AtomicBoolean(true);
				Thread copy_thread = new Thread() {
						public void run() {
							try {
								do_exec_copy_lines(sb, in, charset);
								copy_thread_lock.set(false);
								synchronized(run) {
									run.notifyAll();
								}
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					};
				copy_thread.setName("Copy"+copy_thread.getName());
				copy_thread.setDaemon(true);
				copy_thread.setUncaughtExceptionHandler(IGNORE_EXCEPTION_HANDLER);
				copy_thread.start();
				while (true) {
					synchronized(run) {
						try {
							run.wait();
						} catch ( InterruptedException ex ) {}
					}
					if (!copy_thread_lock.get()) {
						// stopped normally
						break;
					} else if (!run.get()) {
						// try killing copy thread since its still running after it was supposed to stop
						copy_thread.stop(new RuntimeException());
						break;
					}
				}
			} else {
				do_exec_copy_lines(sb, in, charset);
			}
		}
		
		protected void do_exec_copy_lines(StringBuilder sb, InputStream in, Charset charset) throws IOException {
			DefaultCharsetDeciderDecoder d = charset == null ? null : PhptTestCase.newCharsetDeciderDecoder();
			ByLineReader reader = charset == null ? new NoCharsetByLineReader(new java.io.BufferedInputStream(in)) : new MultiCharsetByLineReader(in, d);
			String line;
			try {
				while (reader.hasMoreLines()&&run.get()) {
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
			final Process p = process.get();
			return p!=null && isLocalhostWindows() ? getWindowsProcessID(p) : 0;
		}

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
		public void run(ConsoleManager cm, StringBuilder output_sb, Charset charset, int timeout_sec, TestPackRunnerThread thread, int thread_slow_sec, int suspend_seconds) throws IOException, InterruptedException {
			TimerThread a = null, b = null;
			if (thread!=null && thread_slow_sec>NO_TIMEOUT) {
				b = TimerUtil.waitSeconds(thread_slow_sec, new ThreadSlowTask(thread));
			}
			
			if (timeout_sec>NO_TIMEOUT) {
				a = TimerUtil.waitSeconds(timeout_sec, new ExitMonitorTask(cm, this));
			}
						
			this.run(output_sb, charset, suspend_seconds);
			
			if (a!=null)
				a.cancel();
			if (b!=null)
				b.cancel();
		}

		@Override
		public boolean isTimedOut() {
			return timedout.get();
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
	
	static final UncaughtExceptionHandler IGNORE = new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread arg0, Throwable arg1) {
			}
		};
	@SuppressWarnings("deprecation")
	protected Process guardStart(final ProcessBuilder builder) throws IOException, InterruptedException {
		if (!isWindows())
			return builder.start();
		
		// Windows BN: ProcessBuilder#start can sometimes block forever, observed only with the builtin web server (usually)
		//             and sometimes CLI
		//
		// call ProcessBuilder#start in separate thread to monitor it
		final AtomicReference<IOException> ex_ref = new AtomicReference<IOException>();
		final AtomicReference<Process> proc_ref = new AtomicReference<Process>();
		final Thread start_thread = new Thread() {
				public void run() {
					try {
						proc_ref.set(builder.start());
						synchronized(proc_ref) {
							proc_ref.notifyAll();
						}
					} catch ( IOException ex ) {
						ex_ref.set(ex);
					}
				}
			};
		start_thread.setUncaughtExceptionHandler(IGNORE);
		start_thread.setDaemon(true);
		start_thread.setName("ProcessBuilder"+start_thread.getName());
		start_thread.start();
		// wait up to 120 seconds for ProcessBuilder#start
		try {
			synchronized(proc_ref) {
				proc_ref.wait(120000);
			}
		} catch ( Exception ex ) {}
		Process proc = proc_ref.get();
		if (proc==null) {
			// try to kill off the thread (ProcessBuilder#start is native code though)
			start_thread.stop(new RuntimeException("ProcessBuilder#start timeout (Localhost)"));
			
			IOException ex = ex_ref.get();
			if (ex!=null)
				throw ex;
		}
		return proc;
	} // end protected Process guardStart
	
	protected LocalExecHandle exec_impl(String[] cmd_array, Map<String,String> env, String chdir, byte[] stdin_data) throws IOException, InterruptedException {
		Process process = null;
		{
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
				
				if (env!=null)
					builder.environment().putAll(env);
			}
			if (StringUtil.isNotEmpty(chdir))
				builder.directory(new File(chdir));
			builder.redirectErrorStream(true);
				      
			// start the process
			try {
				process = guardStart(builder);
				if (process==null && isWindows())
					// try again
					process = guardStart(builder);
			} catch ( IOException ex ) {
				if (isWindows() && ex.getMessage().contains("Not enough storage")) {
					//
					// Windows kernel is out of resource handles ... can happen when running lots of processes (100s+)
					// (handles in use by running processes + handles this process needed > # of resource handles windows can allocate)
					//
					//
					// Wait a while and then try again 3 times
					for ( int i=1 ; i < 4 ; i++ ) {
						Thread.sleep(10000 * i); // 10 20 30 => 60 total
					
						try {
							process = guardStart(builder);
							break;
						} catch ( IOException ex2 ) {
							if (ex2.getMessage().contains("Not enough storage")) {
								// wait longer and try again
							} else {
								throw ex2;
							}
						}
					} // end for
				} else if (ex.getMessage().contains("file busy")) {
					// randomly sometimes on Linux, get this problem (CLI scenario's shell scripts) ... wait and try again
					Thread.sleep(100);
					process = guardStart(builder);
				} else {
					throw ex;
				}
			} // end try
		}
		if (process==null)
			return new LocalExecHandle(process, null, null, null, null);
		
		OutputStream stdin = process.getOutputStream();
		
		if (stdin_data!=null && stdin_data.length>0) {
			stdin.write(stdin_data);
			try {
				stdin.flush();
			} catch ( Exception ex ) {}
		}
		
		InputStream stdout = process.getInputStream();
		InputStream stderr = process.getErrorStream();

		return new LocalExecHandle(process, stdin, stdout, stderr, cmd_array);
	} // end protected LocalExecHandle exec_impl
		
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
			// LocalHostExecHandle#close checks for WerFault.exe blocking on Windows
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
	public boolean mkdirs(String path) throws IllegalStateException, IOException {
		if (!isSafePath(path))
			return false;
		if (isWindows()) {
			File f = new File(path);
			if (f.isDirectory())
				return true;
			for ( int i=0 ; i < 3 ; i++ ) {
				f.mkdirs();
				if (f.exists())
					break;
				// Windows BN: sometimes it takes a while for the directory to be created (most often a problem with remote file systems).
				//             make sure it gets created before returning.
				try {
					Thread.sleep(50);
				} catch ( InterruptedException ex ) {}
			}
		} else {
			new File(path).mkdirs();
		}
		return true;
	} // end public boolean mkdirs

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
	
	public static String ensureAbsolutePathCWD(String path) {
		if (isLocalhostWindows() &&	StringUtil.isNotEmpty(AHost.drive(path)))
			return path;
		else if (path.startsWith("/"))
			return path;
		else
			return new File(new File(cwd()), path).getAbsolutePath();
	}
	
	public static String cwd() {
		return System.getenv("user.dir");
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
