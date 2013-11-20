package com.mostc.pftt.util;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.AHost.ExecHandle;
import com.mostc.pftt.host.AHost.IExecHandleCleanupNotify;
import com.mostc.pftt.host.ExecOutput;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.host.ICrashDetector;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.AbstractTestResultRW;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.scenario.ScenarioSet;

/** Handles integration with Time-Travel-Tracing, a debugging feature on Windows. 
 * 
 * For info on how to use a TTT trace (PFTT takes care of recording them for you)
 * @see http://sharepoint/sites/cse/tttwiki/ (internal Microsoft only :()
 *
 */

public class TimeTravelTraceDebugManager extends WindowsDebuggerToolsManager {

	@Override
	public Debugger newDebugger(ConsoleManager cm, AHost host, ScenarioSet scenario_set, Object server_name, PhpBuild build, int process_id, ExecHandle process) {
		return null;
	}
	
	protected boolean displayed_tips = false;
	protected void displayTips(ConsoleManager cm) {
		if (cm==null)
			return;
		if (displayed_tips)
			return;
		displayed_tips = true;
		cm.println(EPrintType.CLUE, getClass(), "PFTT is recording Time-Travel Traces of crashed processes in Result-Pack");
		cm.println(EPrintType.CLUE, getClass(), "");
		cm.println(EPrintType.CLUE, getClass(), "Common TTT Replay commands");
		cm.println(EPrintType.CLUE, getClass(), "");
		cm.println(EPrintType.CLUE, getClass(), "Open TTT in WinDebug using File > Open Crash Dump");
		cm.println(EPrintType.CLUE, getClass(), "!idna.events - shows event timeline including thread creation");
		cm.println(EPrintType.CLUE, getClass(), "gu - run selected thread until unhandled exception");
		cm.println(EPrintType.CLUE, getClass(), "k - stack trace");
		cm.println(EPrintType.CLUE, getClass(), "");
		cm.println(EPrintType.CLUE, getClass(), "Problems loading idna DLL (WinDebug extension) into WinDebug:");
		cm.println(EPrintType.CLUE, getClass(), "1. copy C:\\Program Files\\Debugging Tools to c:\\debuggers (windbg commands can't have ` ` or \")");
		cm.println(EPrintType.CLUE, getClass(), "2. .extpath + c:\\debuggers\\TTT");
		cm.println(EPrintType.CLUE, getClass(), "3. `!c:\\debuggers\\TTT\\idna` instead of `!idna`");
		cm.println(EPrintType.CLUE, getClass(), "fe !c:\\debuggers\\TTT\\idna.position");
	}
	
	public Debugger newDebugger(ConsoleManager cm, AHost host, ScenarioSet scenario_set, PhpBuild build) {
		String ttt_exe = null;
		for ( String path : getToolPaths(host, build, "TTT\\TTTracer.exe") ) {
			if (host.exists(path)) {
				ttt_exe = path;
				break;
			}
		}
		if (ttt_exe==null)
			return null;
		
		displayTips(cm);
		return new TTTDebugger(host, ttt_exe);
	}
	
	protected class TTTDebugger extends Debugger {
		protected final AHost host;
		// c:\\Program Files (x86)\\Debugging Tools for Windows (x86)\\TTT\\TTTracer.exe
		protected final String ttt_exe;
		
		protected TTTDebugger(AHost host, String ttt_exe) {
			this.host = host;
			this.ttt_exe = ttt_exe;
		}

		@Override
		public ExecOutput execOut(String cmd, int timeout_sec,
				Map<String, String> object, byte[] stdin_post, Charset charset)
				throws IllegalStateException, Exception {
			final String ttt_file = createTTTFilenameFromCommand(host, cmd);
			ExecOutput eo = host.execOut(cmd, timeout_sec, object, stdin_post, charset);
			handleCleanup(ttt_file, eo, null);
			return eo;
		}

		@Override
		public RunRequest createRunRequest(ConsoleManager cm, String ctx_str) {
			return host.createRunRequest(cm, ctx_str);
		}

		@Override
		public ExecOutput execOut(RunRequest req) {
			final String ttt_file = createTTTFilenameFromCommand(host, req.getCommandline());
			ExecOutput eo = host.execOut(req);
			handleCleanup(ttt_file, eo, null);
			return eo;
		}

		@Override
		public ExecHandle execThread(RunRequest req) {
			final String ttt_file = createTTTFilenameFromCommand(host, req.getCommandline());
			return handleThread(ttt_file, host.execThread(req));
		}

		@Override
		public void close(ConsoleManager cm) {
			
		}

		@Override
		public boolean isRunning() {
			return host.isOpen();
		}

		@Override
		public ExecHandle execThread(String commandline,
				Map<String, String> env, String chdir, byte[] stdin_data)
				throws Exception {
			final String ttt_file = createTTTFilenameFromCommand(host, commandline);
			return handleThread(ttt_file, host.execThread(wrapCommand(ttt_exe, ttt_file, commandline), env, chdir, stdin_data));
		}
		
		protected ExecHandle handleThread(final String ttt_file, ExecHandle eh) {
			eh.cleanup_notify = new IExecHandleCleanupNotify() {
					@Override
					public void cleanupNotify(ExecHandle eh, AbstractTestResultRW rw) {
						handleCleanup(ttt_file, eh, rw);
					}
				};
			return eh;
		}
		
		protected void handleCleanup(String ttt_file, ICrashDetector eh, AbstractTestResultRW rw) {
			if (eh.isCrashed()) {
				if (rw!=null) {
					try {
						// move TTT file to result-pack (we want it, its a crash)
						// and rename it to .run
						host.move(ttt_file, rw.getPath()+"/"+Host.basename(ttt_file)+".run");
					} catch ( Exception ex ) {
						ex.printStackTrace();
					}
				}
			} else {
				try {
					host.delete(ttt_file);
				} catch ( Exception ex ) {}
			}
		}
		
	} // end protected class TTTDebugger
	
	AtomicInteger i = new AtomicInteger(0);
	protected String createTTTFilenameFromCommand(AHost host, String cmd) {
		if (cmd.startsWith("\"")) {
			int i = cmd.indexOf('"', 1);
			cmd = cmd.substring(1, i);
		} else {
			int i = cmd.indexOf(' ');
			cmd = cmd.substring(1, i);
		}
		String name = Host.basename(cmd);
		
		
		return host.getTempDir()+"\\"+name+i.incrementAndGet()+".run";
	}
	
	protected String wrapCommand(String ttt_exe, String ttt_file, String cmd) {
		// passThroughExit => critical to detect crash
		// launch => critical to actually run the process
		return "\""+ttt_exe+"\" -passThroughExit -noUI -timer 60 -autoStart -saveCrash "+ttt_file+" -launch \"" + cmd +"\"";
	}
	
} // end public class TimeTravelTraceDebugManager
