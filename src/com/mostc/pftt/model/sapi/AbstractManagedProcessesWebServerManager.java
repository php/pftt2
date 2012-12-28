package com.mostc.pftt.model.sapi;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.host.Host.ExecHandle;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.util.DebuggerManager;
import com.mostc.pftt.util.DebuggerManager.Debugger;
import com.mostc.pftt.util.ErrorUtil;
import com.mostc.pftt.util.WinDebugManager;

public abstract class AbstractManagedProcessesWebServerManager extends WebServerManager {
	protected static final int PORT_RANGE_START = 40000;
	// over 49151 may be used by client side of tcp sockets or other dynamic uses
	// Windows seems to start at a lower range (44000+)
	protected static final int PORT_RANGE_STOP = 44000;
	//
	protected AtomicInteger last_port = new AtomicInteger(PORT_RANGE_START-1);
	//
	protected final Timer timer;
	protected final DebuggerManager dbg_mgr;
	
	public AbstractManagedProcessesWebServerManager() {
		timer = new Timer();
		
		dbg_mgr = new WinDebugManager();
		
		if (LocalHost.isLocalhostWindows()) {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					System.out.println("PFTT: "+AbstractManagedProcessesWebServerManager.this.getName()+": terminating web server instances...");
					// Windows BN: may leave `php.exe -S` or Apache running if they aren't terminated here
					// NOTE: if you click stop in Eclipse, you won't get here
					// on Windows, that means that `php.exe -S` instances will still be running
					// you'll need to do: taskkill /im:php.exe /f /t
					// or: taskkill /im:httpd.exe /f /t
					close();
				}
			});
		}
	}
	
	@Override
	protected WebServerInstance createWebServerInstance(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini, Map<String,String> env, String docroot, Object server_name) {
		String sapi_output = "";
		int port_attempts;
		boolean found_port;
		int total_attempts = 0;
		int port = PORT_RANGE_START;
		for ( ; total_attempts < 3 ; total_attempts++) {
			
			// find port number not currently in use
			port_attempts = 0;
			found_port = false;
			// important: don't wrap this method, for or while loops in a synchronize
			//            it'll block things up too much between threads (will get ~1 httpd.exe for every 4 threads)
			//            instead, just use an AtomicInteger to coordinate port allocation
			while (port_attempts < 3) {
				port = last_port.incrementAndGet();
				if (!isLocalhostTCPPortUsed(port)) {
					found_port = true;
					break;
				} else if (port > PORT_RANGE_STOP) {
					// start over and hope ports at start of range are free
					last_port.set(PORT_RANGE_START);
					port_attempts++;
				}
			}
			
			if (!found_port) {
				// try again
				sapi_output += "PFTT: Couldn't find unused local port\n";
				continue;
			}
			
			// Windows BN: php -S won't accept connections if it listens on localhost|127.0.0.1 on Windows
			//             php won't complain about this though, it will run, but be inaccessible
			String listen_address = host.getLocalhostListenAddress();
			
			Host.ExecHandle handle = null;
			try {
				// provide a ManagedProcessWebServerInstance to handle the running web server instance
				final ManagedProcessWebServerInstance web = createManagedProcessWebServerInstance(cm, host, build, ini, env, docroot, listen_address, port);
				
				//System.err.println(web.getCmdString());
				handle = host.execThread(web.getCmdString(), web.getEnv(), docroot);
				
				final Host.ExecHandle handlef = handle;
								
				// ensure server can be connected to
				{
					Socket sock = null;
					for ( int i=0 ; i < 3 ; i++ ) {
						try {
							sock = new Socket();
							sock.setSoTimeout(10);
							sock.connect(new InetSocketAddress(listen_address, port));
							if (sock.isConnected())
								break;
						} catch ( IOException ex ) {
							
						} finally {
							sock.close();
						}
					}
					if (!sock.isConnected())
						// kill server and try again
						throw new IOException("socket not connected");
				}
				//
				
				//
				// if -windebug console option, run web server with windebug
				if (cm.isWinDebug() && host.isWindows() && handle instanceof LocalHost.LocalExecHandle)
					web.debug_handle = dbg_mgr.newDebugger(cm, host, server_name, build, (LocalHost.LocalExecHandle) handle);
				//
				
				web.setProcess(handle);
				
				// check web server periodically to see if it has crashed
				timer.scheduleAtFixedRate(new TimerTask() {
						@Override
						public void run() {
							if (!handlef.isRunning()) {
								try {
									if (handlef.isCrashed())
										// notify of web server crash
										//
										// provide output and exit code
										web.notifyCrash(handlef.getOutput(), handlef.getExitCode());
								} finally {
									// don't need to check any more
									cancel();
								}
							}
						} // end public void run
					}, 0, 500);
				//
				
				return web;
			} catch ( Exception ex ) {
				if (handle!=null && !handle.isCrashed())
					// make sure process is killed in this case (don't kill crashed process)
					handle.close();
				
				sapi_output += ErrorUtil.toString(ex) + "\n";
			}
		} // end for
		
		// fallback
		sapi_output += "PFTT: wasn't able to start web server instance (after "+total_attempts+" attempts)... giving up.\n";
		
		// return this failure message to client code
		return new CrashedWebServerInstance(this, ini, env, sapi_output);
	} // end protected synchronized WebServerInstance createWebServerInstance
	
	protected abstract ManagedProcessWebServerInstance createManagedProcessWebServerInstance(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini, Map<String, String> env, String docroot, String listen_address, int port);
	
	public static abstract class ManagedProcessWebServerInstance extends WebServerInstance {
		protected Debugger debug_handle;
		protected final int port;
		protected final String hostname, cmd;
		protected ExecHandle process;
		
		public ManagedProcessWebServerInstance(AbstractManagedProcessesWebServerManager ws_mgr, String cmd, PhpIni ini, Map<String,String> env, String hostname, int port) {
			super(ws_mgr, LocalHost.splitCmdString(cmd), ini, env);
			this.cmd = cmd;
			this.hostname = hostname;
			this.port = port;
		}
		
		@Override
		public String toString() {
			return hostname+":"+port;
		}
		
		public String getCmdString() {
			return cmd;
		}

		public void setProcess(ExecHandle handle) {
			this.process = handle;
		}

		@Override
		public String hostname() {
			return hostname;
		}

		@Override
		public int port() {
			return port;
		}
		
		@Override
		public boolean isCrashed() {
			return super.isCrashed() || process.isCrashed();
		}

		@Override
		protected void do_close() {
			if (isCrashed() && this.debug_handle!=null)
				// leave process running so it can be debugged
				return;
			if (this.debug_handle!=null)
				this.debug_handle.close();
			
			process.close();
		}

		@Override
		public boolean isRunning() {
			return process.isRunning() && !isCrashed();
		}
		
	} // end public static abstract class ManagedProcessWebServerInstance
	
} // end public abstract class AbstractManagedProcessesWebServerManager
