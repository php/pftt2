package com.mostc.pftt.host;

import java.lang.ref.SoftReference;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class CommonCommandManager {
	protected SoftReference<String[]> wer_fault_query;
	protected final ReentrantLock wer_fault_query_lock, win_close_all_handles_lock, win_kill_process_lock;
	
	public CommonCommandManager() {
		wer_fault_query_lock = new ReentrantLock();
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
				handle_out = host.execOut("handle -p "+process_id+" -a", 10);
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
						String handle_cmd = "handle -p "+process_id+" -y -c "+handle_id;
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
			t.printStackTrace(); // TODO
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
					host.exec("pskill -t -p "+process_id, 20);
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
					host.exec("pskill -t -p "+process_id, 20);
					//host.exec("TASKKILL /FI \"IMAGENAME eq "+image_name+"\" /FI \"PID eq "+process_id+"\" /F /T", 20);
				} finally {
					try {
					win_kill_process_lock.unlock();
					} catch (IllegalMonitorStateException ex) {}
				}
			}
		} catch (Throwable t3) {
			t3.printStackTrace();
		}
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
		// lock if no other thread is waiting
		final boolean no_other_thread_is_waiting = wer_fault_query_lock.tryLock();

		if (!no_other_thread_is_waiting)
			// wait and lock until other thread is done
			wer_fault_query_lock.lock();
		
		
		String[] lines = null;
		if (wer_fault_query!=null)
			lines = wer_fault_query.get();
		
		if (lines==null||no_other_thread_is_waiting) {
			// only query again if:
			//    a. no query result cached
			//    b. didn't have to wait for another thread
			//          if did have to wait for another thread, use the cached query result if available
			//          to limit the number of WMIC processes that are launched
			try {
				// run wmic to find all the werfault.exe processes
				lines = host.execOut("WMIC path win32_process get Processid,Commandline", 20).getLines();
			} catch ( Exception ex ) {
			}
			
			wer_fault_query = new SoftReference<String[]>(lines);
		}
		wer_fault_query_lock.unlock();
		
		if (lines==null)
			return false; // just in case
		
		String prev_line = "";
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
		}
		return false;
	}
	
	public boolean move(AHost host, String src, String dst, boolean elevated) throws Exception {
		if (!host.isSafePath(dst))
			return false;
		if (host.isWindows()) {
			src = AHost.toWindowsPath(src);
			dst = AHost.toWindowsPath(dst);
			
			if (elevated)
				host.cmdElevated("move \""+src+"\" \""+dst+"\"", AHost.NO_TIMEOUT);
			else
				host.cmd("move \""+src+"\" \""+dst+"\"", AHost.NO_TIMEOUT);
		} else {
			src = AHost.toUnixPath(src);
			dst = AHost.toUnixPath(dst);
			host.exec("mv \""+src+"\" \""+dst+"\"", AHost.NO_TIMEOUT);
		}
		return true;
	}
	
	public boolean copy(AHost host, String src, String dst, boolean elevated) throws Exception {
		if (!host.isSafePath(dst))
			return false;
		if (host.isWindows()) {
			src = AHost.toWindowsPath(src);
			dst = AHost.toWindowsPath(dst);
			
			String cmd = null;
			if (host.isDirectory(src)) {
				// ensure xcopy sees destination is supposed to be a directory, or xcopy will ask/block forever
				if (!dst.endsWith("\\"))
					dst += "\\";
				
				// /I is only for directories
				// TODO try /J => performance improvement?
				cmd = "xcopy /Q /Y /C /I /E /G /R /H \""+src+"\" \""+dst+"\"";
			} else {
				host.mkdirs(AHost.dirname(dst));
				if (AHost.basename(src).equals(AHost.basename(dst))) {
					dst = AHost.dirname(dst);
					
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
			src = AHost.toUnixPath(src);
			dst = AHost.toUnixPath(dst);
			host.exec("cp \""+src+"\" \""+dst+"\"", AHost.NO_TIMEOUT);
		}
		return true;
	} 
	
	public boolean delete(AHost host, String path, boolean elevated) {
		if (!host.isSafePath(path)) {
			return false;
		} else if (host.isDirectory(path)) {
			// ensure empty
			try {
				if (host.isWindows()) {
					path = AHost.toWindowsPath(path);
					if (elevated)
						host.cmdElevated("RMDIR /Q /S \""+path+"\"", AHost.NO_TIMEOUT);
					else
						host.cmd("RMDIR /Q /S \""+path+"\"", AHost.NO_TIMEOUT);
				} else {
					path = AHost.toUnixPath(path);
					host.exec("rm -rf \""+path+"\"", AHost.NO_TIMEOUT);
				}
			} catch ( Exception ex ) {
				ex.printStackTrace();
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
				ex.printStackTrace();
				return host.deleteSingleFile(path);
			}
		}
		return host.deleteSingleFile(path);
	}
	
} // end public class CommonCommandManager