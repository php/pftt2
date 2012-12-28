package com.mostc.pftt.runner;

import java.util.concurrent.TimeUnit;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionRequest;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.conn.DefaultClientConnection;
import org.apache.http.impl.conn.DefaultClientConnectionOperator;

public class PhptClientConnectionManager implements ClientConnectionManager {

	@Override
	public SchemeRegistry getSchemeRegistry() {
		SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", 80, new PhptDebuggingSocketFactory()));
        return registry;
	}

	@Override
	public ClientConnectionRequest requestConnection(HttpRoute route,
			Object state) {
		return new ClientConnectionRequest() {

			@Override
			public ManagedClientConnection getConnection(long timeout,
					TimeUnit tunit) throws InterruptedException,
					ConnectionPoolTimeoutException {
				//return new DebuggingSocket();
				DefaultClientConnection socket = new DefaultClientConnection();
				
				// TODO DnsResolver dns_resolver = new DnsResolver();
				
				DefaultClientConnectionOperator op = new DefaultClientConnectionOperator(getSchemeRegistry());//, dns_resolver);
				
				return new ManagedClientConnectionAdapter(PhptClientConnectionManager.this, op, socket);
			}

			@Override
			public void abortRequest() {
				// TODO Auto-generated method stub
				
			}
			
		};
	}

	@Override
	public void releaseConnection(ManagedClientConnection conn,
			long validDuration, TimeUnit timeUnit) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void closeIdleConnections(long idletime, TimeUnit tunit) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void closeExpiredConnections() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
		
	}

} // end public class PhptClientConnectionManager
