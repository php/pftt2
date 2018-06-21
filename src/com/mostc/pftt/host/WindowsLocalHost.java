package com.mostc.pftt.host;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jvnet.winp.WinProcess;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.CommonCommandManager.Win32ProcessInfo;
import com.mostc.pftt.results.ConsoleManagerUtil;
import com.mostc.pftt.runner.AbstractTestPackRunner.TestPackRunnerThread;
import com.mostc.pftt.scenario.FileSystemScenario;
import com.mostc.pftt.util.TimerUtil;
import com.mostc.pftt.util.TimerUtil.ObjectRunnable;

public class WindowsLocalHost extends LocalHost {
	private boolean checked_elevate, found_elevate;
	
	@Override
	public boolean isWindows() {
		return true;
	}
	
	@Override
	public ExecOutput execElevatedOut(String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset, String chdir, @SuppressWarnings("rawtypes") TestPackRunnerThread test_thread, int slow_timeout_sec, boolean wrap_child) throws Exception {
		if (StringUtil.isEmpty(getEnvValue("PFTT_SHELL"))) {
			// check if %PFTT_SHELL% is defined then PFTT is running in the
			// PFTT shell which is already elevated, so don't run elevate.exe
			//
			//
			if (!checked_elevate) {
				found_elevate = mExists(getPfttBinDir()+"\\elevate.exe");
				
				checked_elevate = true;
			}
			if (found_elevate) {
				// execute command with this utility that will elevate the program using Windows UAC
				cmd = getPfttBinDir() + "\\elevate "+cmd;
			}
		}
		
		return execOut(cmd, timeout_sec, env, stdin_data, charset, chdir, test_thread, slow_timeout_sec);
	}
	
	@Override
	protected String[] wrapSplitCmdString(boolean wrap_child, String command) {
		if (wrap_child) {
			return new String[]{
					"cmd",
					"/S",
					"/C",
					"\""+command+" & EXIT\""
				};
		} else {
			return splitCmdString(command);
		}
	}
	
	@Override
	protected Process guardStart(ProcessBuilder builder) throws Exception, InterruptedException {
		Process p = doGuardStart(builder);
		// try twice
		return p == null ? doGuardStart(builder) : p;
	}
	
	protected Process doGuardStart(final ProcessBuilder builder) throws Exception, InterruptedException {
		// Windows BN: ProcessBuilder#start can sometimes block forever, observed only with the builtin web server (usually)
		//             and sometimes CLI
		//
		// call ProcessBuilder#start in separate thread to monitor it
		return runWaitRunnable("ProcessBuilder", 120, new ObjectRunnable<Process>() {
				public Process run() throws IOException {
					return builder.start();
				}
			});
	}
	
	@Override
	protected LocalExecHandle createLocalExecHandle(Process process, OutputStream stdin, InputStream stdout, InputStream stderr, String[] cmd_array) {
		return new WindowsLocalExecHandle(process, stdin, stdout, stderr, cmd_array);
	}
	
	public class WindowsLocalExecHandle extends LocalExecHandle {
		
		public WindowsLocalExecHandle(Process process, OutputStream stdin, InputStream stdout, InputStream stderr, String[] cmd_array) {
			super(process, stdin, stdout, stderr, cmd_array);
			if (this.image_name.endsWith(".cmd")) {
				// IMPORTANT: this is how its identified in the Windows process table
				this.image_name = "cmd.exe"; 
			} else {
				this.image_name = FileSystemScenario.basename(this.image_name).toLowerCase();
				if (image_name.indexOf('.')==-1)
					this.image_name += ".exe";
			}
		}
		
		public int getProcessID() {
			final Process p = process.get();
			return p!=null ? getWindowsProcessID(p) : 0;
		}

		@Override
		public boolean isRunning() {
			final Process p = this.process.get();
			if (p==null)
				return false;
			Boolean b = null;
			try {
				b = runWaitRunnable("IsRunning", 10, new ObjectRunnable<Boolean>() {
						public Boolean run() {
							return doIsRunning(p);
						}
					});
			} catch ( Exception ex ) {}
			return b == null ? false : b.booleanValue();
		}
		
		@Override
		protected void runSuspend(Process p, int suspend_seconds) throws InterruptedException {
			if (suspend_seconds > 0) {
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
		}
		
		@SuppressWarnings("deprecation")
		protected void exec_copy_lines(final StringBuilder sb, final int max_chars, final InputStream in, final Charset charset) throws IOException {
			final AtomicBoolean copy_thread_lock = new AtomicBoolean(true);
			Thread copy_thread = TimerUtil.runThread("ExecCopyLines", new Runnable() {
					public void run() {
						try {
							do_exec_copy_lines(sb, max_chars, in, charset);
							copy_thread_lock.set(false);
							run.set(false);
							/*synchronized(run) {
								run.notifyAll();
							}*/
						} catch (RuntimeException ex) {
							// ignore ... see below copy_thread.stop(new RuntimeException()...
						} catch (Throwable e) {
							ConsoleManagerUtil.printStackTrace(WindowsLocalHost.class, e);
						}
					}
				});
			copy_thread.setUncaughtExceptionHandler(IGNORE_EXCEPTION_HANDLER);
			while (wait.get()) {
				//synchronized(run) {
					try {
						//run.wait(30000);
						Thread.sleep(300);
					} catch ( InterruptedException ex ) {}
				//}
				if (!run.get()) {
					// try killing copy thread since its still running after it was supposed to stop
					copy_thread.stop(new RuntimeException() {
						@Override
						public void printStackTrace() {
							
						}
					});
					break;
				} else if (!copy_thread_lock.get()) {
					// stopped normally
					break;
				}
			}
			Process p = process.get();
			if (p!=null) {
				// ensure process gets terminated
				// if #close sets #run to FALSE, will get here even if process is still running
				// that way, this will stop blocking the calling code even if the process hasn't
				// exited yet (maybe a pending IO request?)
				if (doIsRunning(p)) {
					int pid = getWindowsProcessIDReflection(p);
					if (pid>0) {
						ccm.winKillProcess(WindowsLocalHost.this, image_name, pid);
					}
				}
			}
		} // end protected void exec_copy_lines

		/** On Windows, processes/process tress often won't get terminated correctly just by calling
		 * Process#destroy, for several reasons:
		 * 
		 * 1. Open Handles
		 * 2. Pending IO requests in Kernel (which can still be in a pending state without any open handles)
		 * 3. Process#destroy calls Win32 TerminateProcess() which does not check the process name, etc... to
		 * make sure its still the target process... with enough process churn its possible that that PID could
		 * have been allocated to a different process (which we don't want to terminate) before we realized that
		 * the target process had terminated (polling processes is also slow).
		 * 4. WinDebug may be attached to process
		 * 5. WERFault.exe may be attached to process (Windows Error Reporting dialog box)
		 * 6. may terminate itself (hasn't been observed with Process#destroy but has with WinProcess#killRecursively)
		 * 7. Process#destroy on Windows doesn't terminate any child processes (WinProcess#killRecursively does)... 
		 * On Windows, child processes can continue running after their parent has terminated.
		 * This means Process#destroy is useless for processes killing processes launched by using CMD.EXE
		 * 
		 */
		@Override
		protected void doClose(Process p, int tries) {
			// Windows BN?: if TerminateProcess() called with a PID that doesn't exist anymore (but might again soon)
			//              does TerminateProcess() block forever (or does it appear that way because of Windows slow process
			//              management?(Windows is optimized to run a few processes only (because thats what users did with it in the '90s)))
			String image_name = FileSystemScenario.basename(this.image_name);
			if (image_name.equals("taskkill.exe")||image_name.equals("handle.exe")||image_name.equals("pskill.exe")
					||image_name.equals("taskkill")||image_name.equals("handle")||image_name.equals("pskill")) {
				// can't use taskkill, could create an infinite loop
				//
				// @see https://github.com/kohsuke/winp
				WinProcess wprocess = new WinProcess(p);
				wprocess.killRecursively();
				
				return;
			}
			int pid = getWindowsProcessIDReflection(p); // NOT WinProcess#getPID?
			// for closing handles and other special stuff to work here, must get the actual process
			// not just cmd.exe (it won't have problems with stray handles, etc...)
			//
			// IMPORTANT: #doClose may be called multiple times, so don't change this#doClose
			if (image_name.equals("cmd.exe")||image_name.equals("cmd")) {
				List<Win32ProcessInfo> table = ccm.getWindowsProcessTable(WindowsLocalHost.this);
				for ( Win32ProcessInfo info : table ) {
					if (info.parent_pid==pid) {
						// found child
						//
						// IMPORTANT: basename() because TaskKill doesn't take paths for image name
						image_name = FileSystemScenario.basename(info.exe_path);
						pid = info.pid;
					}
				}
			}
			// NOTE: on Windows, if WER is not disabled(enabled by default), if a process crashes,
			//       WER popup will appear and process will block until popup closed
			//       -this can be detected by looking for `C:\Windows\SysWOW64\WerFault.exe -u -p <process id> -s 1032`
			if (tries==1 && ccm.ensureWERFaultIsNotRunning(WindowsLocalHost.this, pid)) {
				// now can try again
			}
			// prevent self termination
			if (pid!=self_process_id) {
				if (tries==2||tries==8) {
					// expensive op(make sure we've tried a few times to avoid this):
					//   try closing any remaining handles
					// Note: Windows is designed to not let processes be terminated if there are pending IO requests for the process
					//    there can be pending IO requests even if file handles are closed.
					//    this is particularly more complex if DFS/SMB is used, since the pending IO request in Windows
					//    will normally remain pending unless and until it gets a response from the File server
					//    (only then can the process be terminated or exit normally).
					//    to help with this, delete files using the DFS client so it may realize the IO request is not pending any more
					//    @see AbstractPhptTestCaseRunner2#doRunTestClean
					ccm.winCloseAllHandles(WindowsLocalHost.this, pid);
				}
				// provide TASKKILL the image name of the process to try avoiding killing the wrong process
				try {
					// /T => terminate child processes too
					exec("TASKKILL /FI \"IMAGENAME eq "+image_name+"\" /FI \"PID eq "+pid+"\" /F /T", 20);
					// also, WinProcess#killRecursively only checks by process id (not image/program name)
					// while that should be enough, experience on Windows has shown that it isn't and somehow gets PFTT killed eventually
					//
					// Windows Note: Windows does NOT automatically terminate child processes when the parent gets killed
					//               the only way that happens is if you search for the child processes FIRST yourself,
					//               (and then their children, etc...) and then kill them.
				} catch ( Exception ex ) {
					ConsoleManagerUtil.printStackTrace(WindowsLocalHost.class, ex);
					// fallback
					//
					// @see https://github.com/kohsuke/winp
					WinProcess wprocess = new WinProcess(p);
					wprocess.kill();
				}
			}
		} // end protected void doClose

		@Override
		protected void ensureClosedAfterRun(final Process p) {
			// on Windows, it can block forever
			try {
				runWaitRunnable("Destroy", 60, new ObjectRunnable<Boolean>() {
						public Boolean run() {
							p.destroy();
							return true;
						}
					});
			} catch ( Throwable t ) {
				ConsoleManagerUtil.printStackTrace(WindowsLocalHost.class, t);
			}
		}
		
	} // end public class WindowsLocalExecHandle
	
	protected Process handleExecImplException(Exception ex, ProcessBuilder builder) throws Exception {
		if (ex.getMessage().contains("Not enough storage")) {
			//
			// Windows kernel is out of resource handles ... can happen when running lots of processes (100s+)
			// (handles in use by running processes + handles this process needed > # of resource handles windows can allocate)
			//
			//
			// Wait a while and then try again 3 times
			for ( int i=1 ; i < 4 ; i++ ) {
				Thread.sleep(10000 * i); // 10 20 30 => 60 total
			
				try {
					return guardStart(builder);
				} catch ( IOException ex2 ) {
					if (ex2.getMessage().contains("Not enough storage")) {
						// wait longer and try again
					} else {
						throw new Exception(ex2);
					}
				}
			} // end for
		}
		return null;
	}
	
	@Override
	public boolean isBusy() {
		// REMINDER: processes launched by cmd.exe on Windows automatically create
		//           a second process (conhost.exe) to manage the console
		//           so the actual number of processes will be doubled on Windows
		return active_proc_counter.get() < 192;
	}
	
	@Override
	public boolean mCreateDirs(String path) throws IllegalStateException, IOException {
		// TODO temp if (!isSafePath(path))
			//return false;
		File f = new File(path);
		if (f.isDirectory())
			return true;
		for ( int i=0 ; i < 3 ; i++ ) {
			f.mkdirs();
			
			// Windows BN: sometimes it takes a while for the directory to be created (most often a problem with remote file systems).
			//             make sure it gets created before returning.
			try {
				Thread.sleep(50);
			} catch ( InterruptedException ex ) {}
			
			if (f.exists())
				break;
		}
		System.out.println("mCreateDirs "+path);
		return true;
	} // end public boolean mkdirs
	
	@Override
	public String fixPath(String src_path) {
		return FileSystemScenario.toWindowsPath(src_path);
	}
	
	public static int getWindowsProcessID(Process process) {
		try {
			// clean way
			WinProcess wproc = new WinProcess(process);
			return wproc.getPid();
		} catch ( Throwable wt ) {
			return getWindowsProcessIDReflection(process);
		}
	}
	
} // end public class WindowsLocalHost
