package com.mostc.pftt.host;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.runner.AbstractTestPackRunner.TestPackRunnerThread;

public abstract class RemoteHost extends AHost {
	
	public abstract boolean isClosed();
	public abstract boolean ensureConnected(ConsoleManager cm);
	
	public boolean ensureConnected() {
		return ensureConnected(null);
	}
	
	private boolean checked_elevate, found_elevate;
	@Override
	public ExecOutput execElevatedOut(String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset, String chdir, TestPackRunnerThread test_thread, int slow_timeout_sec) throws Exception {
		if (isWindows()) {
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
	protected String getOSNameOnWindows() {
		try {
			// look for line like: `OS Name:         Windows 7 Ultimate`
			for ( String line : getSystemInfoLines() ) {
				if (line.startsWith("OS Name:")) {
					return line.substring("OS Name:".length()).trim();
				}
			}
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
		return null;
	}
	
	@Override
	protected void finalize() {
		close();
	}
	
	public abstract String getPassword();
	
	@Override
	public boolean isRemote() {
		return true;
	}
	
	@Override
	public void downloadCompressWith7Zip(ConsoleManager cm, String ctx_str, String src, AHost dst_host, String dst) throws IllegalStateException, IOException, Exception {
		if (cm!=null)
			cm.println(EPrintType.IN_PROGRESS, ctx_str, "downloadCompressWith7Zip src="+src+" dst_host="+dst_host+" dst="+dst);
		ensure7Zip(cm, dst_host);
		dst_host.ensure7Zip(cm, this);
		
		String src_zip7_file = mktempname(ctx_str, ".7z");
		
		String dst_zip7_file = dst_host.mktempname(ctx_str, ".7z");
		
		compress(cm, dst_host, src, src_zip7_file);
		
		download(src_zip7_file, dst_zip7_file);
		
		dst_host.decompress(cm, this, dst_zip7_file, dst);
		
		dst_host.delete(src_zip7_file);
		delete(dst_zip7_file);
	}
	
	@Override
	public void uploadCompressWith7Zip(ConsoleManager cm, String ctx_str, AHost src_host, String src, String dst) throws IllegalStateException, IOException, Exception {
		if (cm!=null)
			cm.println(EPrintType.IN_PROGRESS, ctx_str, "uploadCompressWith7Zip src_host="+src_host+" src="+src+" dst="+dst);
		ensure7Zip(cm, src_host);
		src_host.ensure7Zip(cm, this);
		
		String src_zip7_file = src_host.mktempname(ctx_str, ".7z");
		
		String dst_zip7_file = mktempname(ctx_str, ".7z");
		
		src_host.compress(cm, this, src, src_zip7_file);
		
		upload(src_zip7_file, dst_zip7_file);
		
		decompress(cm, src_host, dst_zip7_file, dst);
		
		src_host.delete(src_zip7_file);
		delete(dst_zip7_file);
	}
	
} // end public abstract class RemoteHost
