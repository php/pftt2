package com.mostc.pftt.util;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.AHost.ExecHandle;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ConsoleManager.EPrintType;

/** handles integrating with WinDebug.
 * 
 * provides WinDebug with
 * -debug symbols
 * -source code
 * -build image
 * -misc default settings
 * -titles the windebug window with the test case(s) that were run
 * 
 * @author Matt Ficken
 *
 */

public class WinDebugManager extends DebuggerManager {
	private String win_dbg_exe;
	private AHost win_dbg_host;
	private boolean displayed_windbg_tips = false;
	
	protected void ensureFindWinDbgExe(ConsoleManager cm, AHost host) {
		if (this.win_dbg_host==host)
			return;
		
		this.win_dbg_host = host;
		this.win_dbg_exe = findWinDebugExe(host);
		
		if (StringUtil.isEmpty(this.win_dbg_exe))
			cm.println(EPrintType.SKIP_OPERATION, getClass(), "WinDebug not found. Install WinDebug to any: "+StringUtil.toString(getWinDebugPaths(host)));
		else
			this.win_dbg_exe = StringUtil.ensureQuoted(this.win_dbg_exe);
	}
	
	@Override
	public WinDebug newDebugger(ConsoleManager cm, AHost host, Object server_name, PhpBuild build, int process_id) {
		ensureFindWinDbgExe(cm, host);
		
		WinDebug dbg = null;
		if (StringUtil.isNotEmpty(win_dbg_exe)) {
			ensureFindSourceAndDebugPack(cm, host, build);
			
			try {
				dbg = new WinDebug(host, win_dbg_exe, toServerName(server_name), src_path, debug_path, build.getBuildPath(), process_id);
			} catch ( Exception ex ) {
				cm.addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "newDebugger", ex, "", host, win_dbg_exe);
			}
			
			if (dbg != null && dbg.attached) {
				if (!displayed_windbg_tips) {
					displayed_windbg_tips = true;
					
					displayWindebugTips(cm);
				}
			}			
		}
		return dbg;
	}
	
	protected void displayWindebugTips(ConsoleManager cm) {
		cm.println(EPrintType.TIP, getClass(), "  WinDebug command: k       - show callstack");
		cm.println(EPrintType.TIP, getClass(), "  WinDebug command: g       - go (until next exception)");
		cm.println(EPrintType.TIP, getClass(), "  WinDebug command: <F9>    - set breakpoint");
	}

	public static class WinDebug extends Debugger {
		protected ExecHandle debug_handle;
		protected final String log_file;
		protected final AHost host;
		protected boolean attached, wait;
		
		protected WinDebug(final AHost host, String win_dbg_exe, String server_name, String src_path, String debug_path, String image_path, int process_id) throws Exception {
			this.host = host;
			
			log_file = host.mktempname(getClass(), ".log");
			
			if (true)
				return; // TODO
			
			//
			// generate windebug command (with lots of extra options, etc...)
			// @see http://msdn.microsoft.com/en-us/library/windows/hardware/ff561306%28v=vs.85%29.aspx
			StringBuilder sb = new StringBuilder();
			sb.append(win_dbg_exe);
			// -g => run debuggee immediately after attaching
			sb.append(" -g");
			// -p => PID of debuggee - do first in case command gets cut short
			sb.append(" -p ");sb.append(process_id);
			// -T => set window title => server name (usually test case names) t
			sb.append(" -T \"");sb.append(server_name);sb.append("\"");
			// -y => provide directory with debug symbol .pdb files
			if (StringUtil.isNotEmpty(debug_path)) {
				sb.append(" -y \"");sb.append(host.fixPath(debug_path));sb.append("\"");
			}
			// -srcpath => provide source code
			if (StringUtil.isNotEmpty(src_path)) {
				sb.append(" -srcpath ");sb.append(host.fixPath(src_path));
			}
			// -i => provide path to executable image
			sb.append(" -i \"");sb.append(host.fixPath(image_path));sb.append("\"");
			// -logo => log output to file
			sb.append(" -logo \"");sb.append(host.fixPath(log_file));sb.append("\"");
			// -QY => suppress save workspace dialog (don't change workspace file)
			sb.append(" -QY ");
			// -n => noisy symbol load => provide extra info about symbol loading to trace any symbol problems
			sb.append(" -n");
			// -WF => provide default workspace file, which will automatically dock the command window within the windebug window
			String workspace_file = host.fixPath(host.joinIntoOnePath(host.getPfttDir(), "\\bin\\pftt_workspace.WEW"));
			if (host.exists(workspace_file)) {
				sb.append(" -WF \"");sb.append(workspace_file);sb.append("\"");
			}
			//
			
			// now execute windebug
			debug_handle = host.execThread(sb.toString());
			
			
			// wait for log file to be created and reach a minimum size
			// then, assume that the debugger is attached (give up waiting after too long though)
			wait = true;
			for ( int i=0 ; i < 500 && wait ; i++ ) {
				Thread.sleep(100);
				if ( host.getSize(log_file) > 800 ) {
					attached = true;
					wait = false;
					break;
				}
			}
			System.err.println("debug "+sb);
			//System.err.println(IOUtil.toString(debug_handle.getSTDOUT(), IOUtil.HALF_MEGABYTE));
		}
		
		@Override
		public void close() {
			/* TODO debug_handle.close(true);
			
			wait = false;
			
			try {
				host.delete(log_file);
			} catch (Exception e) {
				e.printStackTrace();
			}*/
		}

		@Override
		public boolean isRunning() {
			return true; // TODO debug_handle.isRunning();
		}
		
	} // end public static class WinDebug
	
	/** returns the file paths that are checked for WinDebug.
	 * 
	 * windebug must be installed to one of these paths.
	 * 
	 * @param host
	 * @return
	 */
	public static String[] getWinDebugPaths(Host host) {
		return new String[] {
				host.getSystemDrive()+"\\Program Files\\Debugging Tools for Windows (x64)\\WinDbg.exe",
				host.getSystemDrive()+"\\Program Files\\Debugging Tools for Windows\\WinDbg.exe",
				host.getSystemDrive()+"\\Program Files\\Debugging Tools for Windows (x86)\\WinDbg.exe",
				host.getSystemDrive()+"\\Program Files (x86)\\Debugging Tools for Windows\\WinDbg.exe",
				host.getSystemDrive()+"\\Program Files (x86)\\Debugging Tools for Windows (x86)\\WinDbg.exe"
			};
	}

	/** returns the path that WinDebug is installed at, or returns null if windebug is not found.
	 * 
	 * @see #getWinDebugPaths
	 * @param host
	 * @return
	 */
	public static String findWinDebugExe(Host host) {
		return host.anyExist(getWinDebugPaths(host));
	}
	
} // end public class WinDebugManager
