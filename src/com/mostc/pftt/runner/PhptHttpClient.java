package com.mostc.pftt.runner;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;

public class PhptHttpClient extends DefaultHttpClient {
	
	@Override
	protected ClientConnectionManager createClientConnectionManager() {
		return new PhptClientConnectionManager();
	}
	
}
