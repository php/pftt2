package com.mostc.pftt.host;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import com.github.mattficken.io.ByLineReader;
import com.github.mattficken.io.ByteArrayIOStream;
import com.github.mattficken.io.CharsetByLineReader;
import com.github.mattficken.io.CharsetDeciderDecoder;
import com.github.mattficken.io.IOUtil;
import com.github.mattficken.io.MultiCharsetByLineReader;
import com.github.mattficken.io.NoCharsetByLineReader;
import com.mostc.pftt.runner.AbstractTestPackRunner.TestPackRunnerThread;
import com.mostc.pftt.util.StringUtil;
import com.sshtools.j2ssh.SftpClient;
import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.connection.ChannelState;
import com.sshtools.j2ssh.io.IOStreamConnector;
import com.sshtools.j2ssh.session.SessionChannelClient;
import com.sshtools.j2ssh.sftp.FileAttributes;
import com.sshtools.j2ssh.transport.HostKeyVerification;
import com.sshtools.j2ssh.transport.TransportProtocolException;
import com.sshtools.j2ssh.transport.publickey.SshPublicKey;

/** represents a Remote Host accessed via SSH.
 * 
 * TROUBLESHOOTING
 * 
 * If have problems logging in to OpenSSH (the default SSH server on Linux)
 * 
 * Check /etc/ssh/sshd_config for both:
 * PasswordAuthentication yes
 * PermitRootLogin yes
 * 
 * Then run `/etc/init.d/sshd restart` or `/etc/init.d/ssh restart`
 * 
 * 
 * CONFIGURATION
 * 
 * Doesn't connect to host until first call to a method that requires being connected to host.
 * 
 * This allows SSHHost to be instantiated in config files, etc... without actually creating a connection
 * until one is actually needed.
 * 
 * 
 * SERVER REQUIREMENTS
 * 
 * Remote Host must have an accessible SSH Server.
 * 
 * Support SSH Servers:
 * -OpenSSH on Linux
 * -Apache Mina-SSH on Windows
 * 
 * SSH Server must support SESSION and SFTP channels (SCP channel not required)
 * 
 * NOTE: added 1 line to SftpClient#resolveRemotePath to support checking for [letter]:\ on Windows
 * 
 * @author Matt Ficken
 * 
 */

public class SSHHost extends RemoteHost {
	private static final Timer timer = new Timer();
	protected String address, hostname;
	protected final String username, password;
	protected final int port;
	protected final HostKeyVerification verif;
	protected boolean closed, login_fail;
	@Nullable
	protected String os_name_long;
	@Nullable
	protected Boolean is_windows;
	@Nullable
	protected SshClient ssh;
	@Nullable
	protected SftpClient sftp;
	
	public SSHHost(String hostname, String username, String password) {
		this(hostname, 22, username, password);
	}
	
	public SSHHost(String hostname, int port, String username, String password) {
		this(hostname, port, username, password, new HostKeyVerification() {
				@Override
				public boolean verifyHost(String host, SshPublicKey pk) throws TransportProtocolException {
					return true;
				}
			});
	}
	
	public SSHHost(String hostname, String username, String password, HostKeyVerification verif) {
		this(hostname, 22, username, password, verif);
	}
	
	public SSHHost(String hostname, int port, String username, String password, HostKeyVerification verif) {
		this.address = hostname;
		
		if (hostname.contains(".")||hostname.contains(":")) {
			// use address instead, then ask actual hostname once connected
			// @see #ensureSshOpen
			hostname = null;
		}
		
		this.hostname = hostname;
		this.port = port;
		this.username = username;
		this.password = password;
		this.verif = verif;
	}
	
	protected String normalizePath(String path) {
		return path;
	}
	
	protected void ensureSshOpen() throws IllegalStateException, IOException, UnknownHostException {
		if (login_fail)
			throw new IllegalStateException("Was previously unable to login (didn't try again)");
		else if (isOpen())
			return;
		else if (isClosed()) 
			throw new IllegalStateException("SSH connection administratively/explicitly closed");
		do_close(); // ensure any existing ssh, sftp or scp client gets closed (for gc)
		
		if (hostname!=null) {
			// address isn't IP address (its hostname), resolve it now @see SSHHost#<init>
			address = InetAddress.getByName(hostname).getHostAddress();
		}
		ssh = new SshClient();
		
		ssh.connect(address, port, verif);
		
		PasswordAuthenticationClient pwd = new PasswordAuthenticationClient();

		pwd.setUsername(username);
		pwd.setPassword(password);

		int result = ssh.authenticate(pwd);
		if (result!=AuthenticationProtocolState.COMPLETE) {
			login_fail = true; // IllegalStateException below may get caught/ignored
			throw new IllegalStateException("authentication failed. attempted login as user: "+username+" using password: "+password+" on host: "+hostname+":"+port+" ("+address+":"+port+")");
		}
		
		if (hostname==null) {
			// only have ip address, get hostname
			hostname = isWindows() ? getEnvValue("COMPUTERNAME") : getEnvValue("HOSTNAME");
		}
	} // end protected void ensureSshOpen
	
	protected void ensureSftpOpen() throws UnknownHostException, IOException {
		if (!login_fail && sftp != null && !sftp.isClosed())
			return;
		ensureSshOpen();
		sftp = ssh.openSftpClient();
	}
	
	@Override
	public boolean isClosed() {
		return closed;
	}
	
	@Override
	public void close() {
		closed = true;
		do_close();
	}
	
	protected void do_close() {
		if (sftp!=null) {
			try {
				sftp.quit();
			} catch (IOException e) {
			}
			sftp = null;
		}
		if (ssh!=null) {
			ssh.disconnect();
			ssh = null;
		}
	}
	
	@Override
	public boolean isWindows() {
		if (is_windows!=null)
			return is_windows.booleanValue();
		// even if system drive != C:\, C:\ will still exist if its Windows
		is_windows = new Boolean(exists("C:\\"));
		return is_windows.booleanValue();
	}

	@Override
	public String getName() {
		return getHostname();
	}

	@Override
	public void delete(String path) {
		path = normalizePath(path);
		
		try {
			if (isDirectory(path)) {
				// ensure empty
				if (isWindows()) {
					path = toWindowsPath(path);
					cmd("RMDIR /Q /S \""+path+"\"", FOUR_HOURS);
				} else {
					path = toUnixPath(path);
					exec("rm -rf \""+path+"\"", FOUR_HOURS);
				}
			} else {
				ensureSftpOpen();
				sftp.rm(path);
			}
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
	} // end public void delete

	@Override
	public boolean exists(String path) {
		try {
			ensureSftpOpen();
			FileAttributes fa = sftp.stat(normalizePath(path));
			return fa.isFile() || fa.isDirectory();
		} catch ( Exception ex ) {
			// throws Exception if it doesn't exist
			//ex.printStackTrace();
		}
		return false;
	}
	
	@Override
	public void copy(String src, String dst) throws Exception {
		src = normalizePath(src);
		dst = normalizePath(dst);
		if (isWindows()) {
			src = toWindowsPath(src);
			dst = toWindowsPath(dst);
			if (isDirectory(src))
				// ensure xcopy sees destination is supposed to be a directory, or xcopy will ask/block forever
				dst += "\\"; 
			exec("xcopy /Q /Y /S /E \""+src+"\" \""+dst+"\"", FOUR_HOURS);
		} else {
			src = toUnixPath(src);
			dst = toUnixPath(dst);
			exec("cp \""+src+"\" \""+dst+"\"", FOUR_HOURS);
		}
	}

	@Override
	public String pathsSeparator() {
		return isWindows() ? ";" : ":";
	}

	@Override
	public String dirSeparator() {
		return isWindows() ? "\\" : "/";
	}

	@Override
	public String getContents(String file) throws IOException {
		ensureSftpOpen();
		ByteArrayIOStream local = new ByteArrayIOStream(1024);
		sftp.get(normalizePath(file), local);
		NoCharsetByLineReader reader = new NoCharsetByLineReader(local.getInputStream());
		String str = IOUtil.toString(reader, IOUtil.HALF_MEGABYTE);
		reader.close();
		return str;
	}

	@Override
	public String getContentsDetectCharset(String file, CharsetDeciderDecoder cdd) throws IOException {
		ensureSftpOpen();
		ByteArrayIOStream local = new ByteArrayIOStream(1024);
		sftp.get(normalizePath(file), local);
		MultiCharsetByLineReader reader = new MultiCharsetByLineReader(local.getInputStream(), cdd);
		String str = IOUtil.toString(reader, IOUtil.HALF_MEGABYTE);
		reader.close();
		return str;
	}

	@Override
	public ByLineReader readFile(String file) throws FileNotFoundException, IOException {
		ensureSftpOpen();
		ByteArrayIOStream local = new ByteArrayIOStream(1024);
		sftp.get(normalizePath(file), local);
		return new NoCharsetByLineReader(local.getInputStream());
	}

	@Override
	public ByLineReader readFileDetectCharset(String file, CharsetDeciderDecoder cdd) throws FileNotFoundException, IOException {
		ensureSftpOpen();
		ByteArrayIOStream local = new ByteArrayIOStream(1024);
		sftp.get(normalizePath(file), local);
		return new MultiCharsetByLineReader(local.getInputStream(), cdd);
	}

	@Override
	public void saveTextFile(String filename, String text) throws IOException {
		saveTextFile(filename, text, null);
	}
	
	protected SessionChannelClient do_exec(String cmd, Map<String, String> env, String chdir, byte[] stdin_post, OutputStream out) throws IOException, IllegalStateException {
		ensureSshOpen();
		SessionChannelClient session = ssh.openSessionChannel();
		
		// prepare to execute
		if (StringUtil.isNotEmpty(chdir)) {
			// would be nice if there were a better way to do this
			cmd = "cd \""+chdir+"\" && "+cmd;
		}
		if (env!=null) {
			for (String name : env.keySet())
				session.setEnvironmentVariable(name, env.get(name));
		}
		if (stdin_post!=null) {
			session.getOutputStream().write(stdin_post);
		}
		//
		
		if (session.executeCommand(cmd)) {
			IOStreamConnector output = new IOStreamConnector();
			output.connect(session.getInputStream(), out);
			return session;
		} else {
			throw new IllegalStateException("command not executed: "+cmd);
		}
	}
	
	@Override
	public ExecHandle execThread(String cmd, Map<String, String> env, String chdir, byte[] stdin_post) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
		SessionChannelClient session = do_exec(cmd, env, chdir, stdin_post, out);
		
		return new SSHExecHandle(session, out);
	}
	
	protected static class SSHExecHandle extends ExecHandle {
		protected final SessionChannelClient session;
		protected final ByteArrayOutputStream out;
		
		protected SSHExecHandle(SessionChannelClient session, ByteArrayOutputStream out) {
			this.session = session;
			this.out = out;
		}
		
		@Override
		public void close(boolean force) {
			try {
				session.close();
			} catch ( Exception ex ) {
				ex.printStackTrace();
			}
		}

		@Override
		public boolean isRunning() {
			return session.isOpen();
		}

		@Override
		public String getOutput() {
			return out.toString();
		}

		@Override
		public int getExitCode() {
			Integer ec = session.getExitCode();
			return ec == null ? -1 : ec.intValue();
		}

		@Override
		public InputStream getSTDOUT() {
			return session.getInputStream();
		}

		@Override
		public OutputStream getSTDIN() {
			return session.getOutputStream();
		}
		
	} // end protected static class SSHExecHandle
	
	@Override
	public ExecOutput exec(String cmd, int timeout, Map<String, String> env, byte[] stdin_post, Charset charset, String chdir, final TestPackRunnerThread thread, int thread_slow_sec) throws Exception {
		ByteArrayIOStream out = new ByteArrayIOStream(1024);
		
		ExecOutput eo = new ExecOutput();
		
		//
		final AtomicBoolean run = new AtomicBoolean(true);
		final SessionChannelClient session = do_exec(cmd, env, chdir, stdin_post, out);
		if (timeout>NO_TIMEOUT) {
			timer.schedule(new TimerTask() {
					public void run() {
						try {
							run.set(false);
							
							session.close();
						} catch ( Exception ex ) {
							ex.printStackTrace();
						}
					}
				}, timeout*1000);
		}
		if (thread != null && thread_slow_sec>FOUR_HOURS) {
			timer.schedule(new TimerTask() {
					public void run() {
						thread.notifySlowTest();
					}
				}, thread_slow_sec*1000);
		}
		//
		
		// read output from command
		/* TODO StringBuilder sb = new StringBuilder(1024);
		DefaultCharsetDeciderDecoder d = charset == null ? null : PhptTestCase.newCharsetDeciderDecoder();
		ByLineReader reader = charset == null ? new NoCharsetByLineReader(out.getInputStream()) : new MultiCharsetByLineReader(out.getInputStream(), d);
		String line;
		try {
			while (reader.hasMoreLines()&&run.get()) {
				line = reader.readLine();
				if (line==null)
					break;
				sb.append(line);
				sb.append('\n');
			}
		} catch ( IOException ex ) {
			//ex.printStackTrace();
		}
		
		out.close();*/
		
		// wait for exit
		session.getState().waitForState(ChannelState.CHANNEL_CLOSED);
		
		//
		eo.exit_code = session.getExitCode();
		/* TODO if (reader instanceof AbstractDetectingCharsetReader)
			eo.charset = ((AbstractDetectingCharsetReader)reader).cs; */
		eo.output = out.toString();
		//
		
		return eo;
	} // end public ExecOutput exec

	@Override
	public ExecOutput exec(String cmd, int timeout, String chdir) throws Exception {
		return exec(cmd, timeout, null, null, chdir);
	}

	@Override
	public ExecOutput exec(String cmd, int timeout, Map<String, String> env, Charset charset, String chdir) throws Exception {
		return exec(cmd, timeout, env, null, charset, chdir);
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public String getEnvValue(String name) {
		try {
			if (isWindows())
				return StringUtil.chomp(cmd("ECHO %"+name+"%", ONE_MINUTE).output);
			else
				return exec("echo $"+name, ONE_MINUTE).output;
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (o==this) {
			return true;
		} else if (o instanceof SSHHost) {
			SSHHost so = (SSHHost) o;
			return so.port==this.port && so.hostname.equalsIgnoreCase(this.hostname);
		} else {
			return false;
		}
	}

	@Override
	public String getHostname() {
		return hostname;
	}

	@Override
	public void mkdirs(String path) throws IllegalStateException, IOException {
		ensureSftpOpen();
		sftp.mkdirs(normalizePath(path));
	}

	@Override
	public void download(String src, String dst) throws IllegalStateException, IOException, Exception {
		ensureSftpOpen();
		sftp.get(normalizePath(src), new BufferedOutputStream(new FileOutputStream(dst)));
	}
	
	protected void do_upload(String base, File[] files, String dst) throws IOException {
		for (File file : files) {
			if (file.isDirectory()) {
				do_upload(base, file.listFiles(), dst);
			} else {
				String remote_file_path = joinIntoOnePath(dst, pathFrom(base, file.getAbsolutePath()));
				
				sftp.put(file.getAbsolutePath(), remote_file_path);
			}
		}
	}

	@Override
	public void upload(String src, String dst) throws IllegalStateException, IOException {
		ensureSftpOpen();
		dst = normalizePath(dst);
		
		File fsrc = new File(src);
		if (fsrc.isDirectory()) {
			do_upload(src, fsrc.listFiles(), dst);
		} else {
			// uploading single file
			sftp.put(fsrc.getAbsolutePath(), dst);
		}
	}

	@Override
	public ExecOutput exec(String cmd, int timeout, Map<String, String> env, byte[] stdin_post, Charset charset, String chdir) throws Exception {
		return exec(cmd, timeout, env, stdin_post, charset, chdir, null, FOUR_HOURS);
	}

	@Override
	public String getOSNameLong() {
		if (os_name_long!=null)
			return os_name_long;
		if (isWindows())
			return os_name_long = getOSNameOnWindows();
		String v;
		try {
			v = " "+exec("uname -a", FOUR_HOURS).output;
		} catch ( Exception ex ) {
			ex.printStackTrace();
			v = ""; // don't try again
		}
		if (exists("/etc/redhat-release"))
			return os_name_long = "Redhat" + v;
		else if (exists("/etc/fedora-release"))
			return os_name_long = "Fedora" + v;
		else if (exists("/etc/centos-release"))
			return os_name_long = "CentOS" + v;
		else if (exists("/etc/ubuntu-release"))
			return os_name_long = "Ubuntu" + v;
		else if (exists("/etc/debian-release"))
			return os_name_long = "Debian" + v;
		else if (exists("/etc/gentoo-release"))
			return os_name_long = "Gentoo" + v;
		else
			return os_name_long = v;
	} // end public String getOSNameLong

	@Override
	public String getAddress() {
		return address;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public boolean isOpen() {
		return ssh!=null && ssh.isConnected();
	}

	@Override
	public boolean isDirectory(String path) {
		try {
			ensureSftpOpen();
			FileAttributes fa = sftp.stat(normalizePath(path));
			return fa.isDirectory();
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean dirContainsExact(String path, String name) {
		for ( String a : list(path) ) {
			if (a.equalsIgnoreCase(name))
				return true;
		}
		return false;
	}

	@Override
	public boolean dirContainsFragment(String path, String name_fragment) {
		for ( String a : list(path) ) {
			if (a.contains(name_fragment))
				return true;
		}
		return false;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public String[] list(String path) {
		try {
			ensureSftpOpen();
			List list = sftp.ls(normalizePath(path));
			return (String[]) list.toArray(new String[list.size()]);
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
		return StringUtil.EMPTY_ARRAY;
	}

	@Override
	public long getSize(String file) {
		try {
			ensureSftpOpen();
			FileAttributes fa = sftp.stat(normalizePath(file));
			return fa.getSize().longValue();
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
		return 0;
	}

	@Override
	public ByLineReader readFile(String file, Charset cs) throws IllegalStateException, FileNotFoundException, IOException {
		return new CharsetByLineReader(new FileInputStream(normalizePath(file)), cs);
	}

	@Override
	public void saveTextFile(String filename, String text, CharsetEncoder ce) throws IllegalStateException, IOException {
		if (text==null)
			text = "";
		ensureSftpOpen();
		filename = normalizePath(filename);
		
		if (ce==null) {
			byte[] text_bytes = text.getBytes();
			sftp.put(new ByteArrayInputStream(text_bytes), filename);
		} else {
			ByteBuffer bbuf = ByteBuffer.allocate(50+text.length()*2);
			ce.encode(CharBuffer.wrap(text.toCharArray()), bbuf, true);
			sftp.put(new ByteBufferInputStream(bbuf), filename);
		}
	}
	
	protected static class ByteBufferInputStream extends InputStream {
		protected final ByteBuffer bbuf;
		
		public ByteBufferInputStream(ByteBuffer bbuf) {
			this.bbuf = bbuf;
		}
		
		@Override
		public int read() {
			return bbuf.get();
		}
	}

} // end public class SSHHost
