package com.mostc.pftt.host;

import java.io.IOException;

public abstract class RemoteHost extends Host {
	
	@Override
	public boolean isRemote() {
		return true;
	}
	
	@Override
	public void downloadCompressWith7Zip(String src, String dst)
			throws IllegalStateException, IOException, Exception {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void uploadCompressWith7Zip(String src, String dst)
			throws IllegalStateException, IOException, Exception {
		// TODO Auto-generated method stub
		
	}
}
