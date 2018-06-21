package com.mostc.pftt.host;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.mostc.pftt.results.ConsoleManagerUtil;
import com.mostc.pftt.scenario.FileSystemScenario;

public class CommonCommandManager {
	protected SoftReference<List<Win32ProcessInfo>> process_table_query;
	protected final ReentrantLock process_table_query_lock, win_close_all_handles_lock, win_kill_process_lock;
	
	public CommonCommandManager() {
		process_table_query_lock = new ReentrantLock();
		win_close_all_handles_lock = new ReentrantLock();
		win_kill_process_lock = new ReentrantLock();
	}
	
	public void winCloseAllHandles(AHost host, int process_id) {
		try {
			ExecOutput handle_out;
			try {
				win_close_all_handles_lock.tryLock(10, TimeUnit.SECONDS);
			} catch ( InterruptedException ex ) {}
			try {
				handle_out = host.execOut(host.getPfttBinDir()+"\\handle -accepteula -p "+process_id+" -a", 10);
			} finally {
				try {
				win_close_all_handles_lock.unlock();
				} catch ( IllegalMonitorStateException ex ) {}
			}
			for ( String h_line : handle_out.getLines() ) {
				String[] h_part = h_line.trim().split(" ");//StringUtil.splitWhitespace(h_line);
				if (h_part.length>2) {
					String handle_id = h_part[0];
					String handle_type = h_part[1];
					if (handle_id.endsWith(":") && handle_type.equalsIgnoreCase("File")) {
						handle_id = handle_id.substring(0, handle_id.length()-1);
						String handle_cmd = host.getPfttBinDir()+"\\handle -accepteula -p "+process_id+" -y -c "+handle_id;
						try {
							win_close_all_handles_lock.tryLock(10, TimeUnit.SECONDS);
						} catch ( InterruptedException ex ) {}
						try {
							host.exec(handle_cmd, 5);
						} finally {
							try {
							win_close_all_handles_lock.unlock();
							} catch (IllegalMonitorStateException ex) {}
						}
					}
				}
			}
		} catch ( Throwable t ) {
			ConsoleManagerUtil.printStackTrace(CommonCommandManager.class, t);
		}
	}
	
	public void winKillProcess(AHost host, String image_name, int process_id) {
		// image name: ex: `php.exe` 
		// /F => forcefully terminate ('kill')
		// /T => terminate all child processes (process is cmd.exe and PHP is a child)
		//      process.destory might not do this, so thats why its CRITICAL that TASKKILL
		//      be tried before process.destroy
		try {
			// TODO if exit_code == 128 => process not found
			//        next try it should automatically add .exe too
			//        getting image name from command line (especially if its a shell command) should assume adding .exe too?
			if (!image_name.endsWith(".exe")) {
				image_name += ".exe";
				
				try {
					win_kill_process_lock.tryLock(5, TimeUnit.SECONDS);
				} catch ( InterruptedException ex ) {}
				try {
					host.exec(host.getPfttBinDir()+"\\pskill -accepteula -t -p "+process_id, 20);
					//host.exec("TASKKILL /FI \"IMAGENAME eq "+image_name+"\" /FI \"PID eq "+process_id+"\" /F /T", 20);
				} finally {
					try {
					win_kill_process_lock.unlock();
					} catch (IllegalMonitorStateException ex) {}
				}
			} else {
				try {
					win_kill_process_lock.tryLock(5, TimeUnit.SECONDS);
				} catch ( InterruptedException ex ) {}
				try {					
					host.exec(host.getPfttBinDir()+"\\pskill -accepteula -t -p "+process_id, 20);
					//host.exec("TASKKILL /FI \"IMAGENAME eq "+image_name+"\" /FI \"PID eq "+process_id+"\" /F /T", 20);
				} finally {
					try {
					win_kill_process_lock.unlock();
					} catch (IllegalMonitorStateException ex) {}
				}
			}
		} catch (Throwable t3) {
			ConsoleManagerUtil.printStackTrace(CommonCommandManager.class, t3);
		}
	}
	
	protected static class Win32ProcessInfo {
		public final String exe_path;
		public final int pid, parent_pid;
		
		protected Win32ProcessInfo(String exe_path, int pid, int parent_pid) {
			this.exe_path = exe_path;
			this.pid = pid;
			this.parent_pid = parent_pid;
		}
	}
	
	protected List<Win32ProcessInfo> getWindowsProcessTable(AHost host) {
		// lock if no other thread is waiting
		final boolean no_other_thread_is_waiting = process_table_query_lock.tryLock();

		if (!no_other_thread_is_waiting)
			// wait and lock until other thread is done
			process_table_query_lock.lock();
		
		
		List<Win32ProcessInfo> table = null;
		if (process_table_query!=null)
			table = process_table_query.get();
		
		if (table==null||no_other_thread_is_waiting) {
			// only query again if:
			//    a. no query result cached
			//    b. didn't have to wait for another thread
			//          if did have to wait for another thread, use the cached query result if available
			//          to limit the number of WMIC processes that are launched
			table = new ArrayList<Win32ProcessInfo>(400);
			String[] lines;
			try {
				lines = host.execOut("WMIC path win32_process GET ExecutablePath,Processid,ParentProcessId", 20).getLines();
			
				for ( String line : lines ) {
					String[] parts = line.split("[ |\\t]{2,}");
					
					if (parts.length!=3||parts[0].equals("ExecutablePath")||parts[0].length()==0)
						continue;
					
					table.add(new Win32ProcessInfo(parts[0], Integer.parseInt(parts[2]), Integer.parseInt(parts[1])));
				}
			} catch ( Exception ex ) {
				ConsoleManagerUtil.printStackTrace(CommonCommandManager.class, ex);
			}
			
			process_table_query = new SoftReference<List<Win32ProcessInfo>>(table);
		}
		process_table_query_lock.unlock();
		
		return table;
	}
	
	/** finds and kills any WERFault.exe processes (WER popup message) created for given process.
	 * 
	 * this method will only launch one WMIC query process at a time per Localhost instance. this added complexity (complexity
	 * entirely handled internally by this method) is needed to avoid a WMIC process-storm if this method is called many times (30+)
	 * in quick succession.
	 * 
	 * @param host
	 * @param process_id
	 */
	public boolean ensureWERFaultIsNotRunning(AHost host, int process_id) {
		List<Win32ProcessInfo> lines = getWindowsProcessTable(host);
		
		if (lines==null)
			return false; // just in case
		
		/* TODO temp String prev_line = "";
		for ( String line : lines ) {
			line = line.toLowerCase();
			// search werfault.exe process list for a werfault.exe created for process_id
			if (line.contains("werfault.exe") && line.contains("-p "+process_id)) {
				//
				// blocking werfault.exe (not actually the process parent, its a separate SVC service)
				int blocking_process_id = Integer.parseInt(prev_line.trim());
				
				// kill werfault.exe so we can try killing the target process again
				winKillProcess(host, "werfault.exe", blocking_process_id);
				return true;
			}
			prev_line = line;
		}*/
		return false;
	}
	
	public boolean ensureWinDebugIsNotRunning(LocalHost host, int pid) {
		// look for `windbg [other args] -p <process id> [other args]`
		List<Win32ProcessInfo> lines = getWindowsProcessTable(host);
		
		if (lines==null)
			return false; // just in case
		
		/* TODO temp String prev_line = "";
		for ( String line : lines ) {
			line = line.toLowerCase();
			// @see WinDebugManager for command line args
			if (line.contains("windbg.exe") && line.contains("-p "+pid)) {
				
				// get PID of windbg process to kill
				int blocking_process_id = Integer.parseInt(prev_line.trim());
				
				// kill it
				winKillProcess(host, "windbg.exe", blocking_process_id);
				
				// now target process (pid) should be able to terminate 
				//  (should terminate immediately after windbg killed actually)
				return true;
			}
			prev_line = line;
		}*/
		return false;
	}
	
	public boolean move(AHost host, String src, String dst, boolean elevated) throws Exception {
		if (!host.isSafePath(dst))
			return false;
		if (host.isWindows()) {
			src = FileSystemScenario.toWindowsPath(src);
			dst = FileSystemScenario.toWindowsPath(dst);
			
			if (elevated)
				host.cmdElevated("move /Y \""+src+"\" \""+dst+"\"", AHost.NO_TIMEOUT);
			else
				host.cmd("move /Y \""+src+"\" \""+dst+"\"", AHost.NO_TIMEOUT);
		} else {
			src = FileSystemScenario.toUnixPath(src);
			dst = FileSystemScenario.toUnixPath(dst);
			host.exec("mv \""+src+"\" \""+dst+"\"", AHost.NO_TIMEOUT);
		}
		return true;
	}
	
	public boolean copy(AHost host, String src, String dst, boolean elevated) throws Exception {
		if (!host.isSafePath(dst))
			return false;
		if (host.isWindows()) {
			src = FileSystemScenario.toWindowsPath(src);
			dst = FileSystemScenario.toWindowsPath(dst);
			
			String cmd = null;
			if (host.mIsDirectory(src)) {
				// ensure xcopy sees destination is supposed to be a directory, or xcopy will ask/block forever
				if (!dst.endsWith("\\"))
					dst += "\\";
				
				// /I is only for directories
				// TODO try /J => performance improvement?
				cmd = "xcopy /Q /Y /C /I /E /G /R /H \""+src+"\" \""+dst+"\"";
			} else {
				host.mCreateDirs(FileSystemScenario.dirname(dst));
				if (FileSystemScenario.basename(src).equals(FileSystemScenario.basename(dst))) {
					dst = FileSystemScenario.dirname(dst);
					
					cmd = "xcopy /Q /Y /E /G /R /H /C \""+src+"\" \""+dst+"\"";
				}
			}
			if (cmd==null)
				// /B => binary file copy
				cmd = "cmd /C copy /B /Y \""+src+"\" \""+dst+"\"";
			
			if (elevated)
				host.execElevated(cmd, AHost.NO_TIMEOUT);
			else
				host.exec(cmd, AHost.NO_TIMEOUT);
		} else {
			src = FileSystemScenario.toUnixPath(src);
			dst = FileSystemScenario.toUnixPath(dst);
			host.exec("cp \""+src+"\" \""+dst+"\"", AHost.NO_TIMEOUT);
		}
		return true;
	} 
	
	public boolean delete(AHost host, String path, boolean elevated) {
		if (!host.isSafePath(path)) {
			return false;
		} else if (host.mIsDirectory(path)) {
			// ensure empty
			try {
				if (host.isWindows()) {
					path = FileSystemScenario.toWindowsPath(path);
					if (elevated)
						host.cmdElevated("RMDIR /Q /S \""+path+"\"", AHost.NO_TIMEOUT);
					else
						host.cmd("RMDIR /Q /S \""+path+"\"", AHost.NO_TIMEOUT);
				} else {
					path = FileSystemScenario.toUnixPath(path);
					host.exec("rm -rf \""+path+"\"", AHost.NO_TIMEOUT);
				}
			} catch ( Exception ex ) {
				ConsoleManagerUtil.printStackTrace(CommonCommandManager.class, ex);
			}
		} else if (host.isWindows() && path.contains("*")) {
			// XXX wildcard support on linux
			path = host.fixPath(path);
			try {
				if (elevated)
					host.execElevated("CMD /C DEL /F /Q "+path+"", AHost.NO_TIMEOUT);
				else
					host.exec("CMD /C DEL /F /Q "+path+"", AHost.NO_TIMEOUT);
				if (elevated)
					host.execElevated("CMD /C CMD /C FOR /D %f IN ("+path+") DO RMDIR /S /Q %f", AHost.NO_TIMEOUT);
				else
					host.exec("CMD /C CMD /C FOR /D %f IN ("+path+") DO RMDIR /S /Q %f", AHost.NO_TIMEOUT);
			} catch ( Exception ex ) {
				ConsoleManagerUtil.printStackTrace(CommonCommandManager.class, ex);
				return host.deleteSingleFile(path);
			}
		}
		return host.deleteSingleFile(path);
	}
	
} // end public class CommonCommandManager
