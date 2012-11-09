package com.mostc.pftt.host;

import java.io.IOException;

import com.mostc.pftt.util.StringUtil;

public abstract class RemoteHost extends Host {
	
	public abstract boolean isClosed();
	
	@Override
	protected String getOSNameOnWindows() {
		try {
			// look for line like: `OS Name:         Windows 7 Ultimate`
			for ( String line : getSystemInfoLines() ) {
				if (line.startsWith("OS Name:")) {
					return StringUtil.join(StringUtil.splitWhitespace(line), 1, " ");
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
	public void downloadCompressWith7Zip(String src, String dst) throws IllegalStateException, IOException, Exception {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void uploadCompressWith7Zip(String src, String dst) throws IllegalStateException, IOException, Exception {
		// TODO Auto-generated method stub
		
	}
	
} // end public abstract class RemoteHost
