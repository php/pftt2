package com.mostc.pftt.host;

import java.io.IOException;

import com.mostc.pftt.results.ConsoleManager;

public abstract class RemoteHost extends AHost {
	
	public abstract boolean isClosed();
	
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
	public void downloadCompressWith7Zip(ConsoleManager cm, String ctx_str, AHost src_host, String src, String dst) throws IllegalStateException, IOException, Exception {
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
	
	@Override
	public void uploadCompressWith7Zip(ConsoleManager cm, String ctx_str, String src, AHost dst_host, String dst) throws IllegalStateException, IOException, Exception {
		ensure7Zip(cm, dst_host);
		dst_host.ensure7Zip(cm, this);
		
		String src_zip7_file = mktempname(ctx_str, ".7z");
		
		String dst_zip7_file = dst_host.mktempname(ctx_str, ".7z");
		
		compress(cm, dst_host, src, src_zip7_file);
		
		download(src_zip7_file, dst_zip7_file);
		
		dst_host.decompress(cm, this, dst_zip7_file, dst);
		
		dst_host.delete(dst_zip7_file);
		delete(src_zip7_file);
	}
	
} // end public abstract class RemoteHost
