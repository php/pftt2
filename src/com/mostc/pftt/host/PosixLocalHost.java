package com.mostc.pftt.host;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Map;

import com.mostc.pftt.runner.AbstractTestPackRunner.TestPackRunnerThread;

public class PosixLocalHost extends LocalHost {
	
	@Override
	public boolean isWindows() {
		return false;
	}

	@Override
	protected Process guardStart(ProcessBuilder builder) throws Exception, InterruptedException {
		return builder.start();
	}
	
	@Override
	protected LocalExecHandle createLocalExecHandle(Process process, OutputStream stdin, InputStream stdout, InputStream stderr, String[] cmd_array) {
		return new PosixLocalExecHandle(process, stdin, stdout, stderr, cmd_array);
	}
	
	public class PosixLocalExecHandle extends LocalExecHandle {
		
		public PosixLocalExecHandle(Process process, OutputStream stdin, InputStream stdout, InputStream stderr, String[] cmd_array) {
			super(process, stdin, stdout, stderr, cmd_array);
		}
		
		@Override
		public boolean isRunning() {
			final Process p = this.process.get();
			if (p==null)
				return false;
			return doIsRunning(p);
		}
		
		public int getProcessID() {
			final Process p = process.get();
			return 0; // TODO
		}
		
		protected void exec_copy_lines(final StringBuilder sb, final int max_chars, final InputStream in, final Charset charset) throws IOException {
			do_exec_copy_lines(sb, max_chars, in, charset);
		}

		@Override
		protected void runSuspend(Process p, int suspend_seconds) {
			// TODO 
		}

		@Override
		protected void doClose(Process p, int tries) {
			p.destroy();
		}

		@Override
		protected void ensureClosedAfterRun(Process p) {
			p.destroy();
		}
		
	} // end public class PosixLocalExecHandle
	
	@Override
	protected Process handleExecImplException(Exception ex, ProcessBuilder builder) throws Exception {
		if (ex.getMessage().contains("file busy")) {
			// randomly sometimes on Linux, get this problem (CLI scenario's shell scripts) ... wait and try again
			Thread.sleep(100);
			return guardStart(builder);
		} else {
			return null;
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public ExecOutput execElevatedOut(String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset, String chdir, TestPackRunnerThread test_thread, int slow_timeout_sec, boolean wrap_child) throws Exception {
		return execOut(cmd, timeout_sec, env, stdin_data, charset, chdir, test_thread, slow_timeout_sec, wrap_child);
	}
	
	@Override
	protected String[] wrapSplitCmdString(boolean wrap_child, String command) {
		return splitCmdString(command);
	}
	
	@Override
	public boolean isBusy() {
		return active_proc_counter.get() < 400;
	}
	
	@Override
	public boolean mkdirs(String path) throws IllegalStateException, IOException {
		if (!isSafePath(path))
			return false;
		new File(path).mkdirs();
		return true;
	} // end public boolean mkdirs
	
} // end public class PosixLocalHost
