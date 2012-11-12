package com.mostc.pftt.host;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.mattficken.io.AbstractDetectingCharsetReader;
import com.github.mattficken.io.ByLineReader;
import com.github.mattficken.io.ByteArrayIOStream;
import com.github.mattficken.io.CharsetDeciderDecoder;
import com.github.mattficken.io.DefaultCharsetDeciderDecoder;
import com.github.mattficken.io.IOUtil;
import com.github.mattficken.io.MultiCharsetByLineReader;
import com.github.mattficken.io.NoCharsetByLineReader;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.runner.AbstractTestPackRunner.TestPackRunnerThread;
import com.mostc.pftt.util.StringUtil;
import com.sshtools.j2ssh.ScpClient;
import com.sshtools.j2ssh.SftpClient;
import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.connection.ChannelState;
import com.sshtools.j2ssh.io.IOStreamConnector;
import com.sshtools.j2ssh.session.SessionChannelClient;
import com.sshtools.j2ssh.sftp.FileAttributes;

/** represents a Remote Host accessed via SSH.
 * 
 * Doesn't connect to host until first call to a method that requires being connected to host.
 * 
 * Remote Host must have an accessible SSH Server.
 * 
 * Support SSH Servers:
 * -OpenSSH on Linux
 * -Apache Mina-SSH on Windows
 * 
 * SSH Server must support SESSION, SFTP and SCP channels (most do).
 * 
 * @author Matt Ficken
 *
 */

public class SSHHost extends RemoteHost {
	private static final Timer timer = new Timer();
	protected final String hostname, username, password;
	protected final int port;
	protected boolean closed;
	protected String address, os_name_long;
	protected Boolean is_windows;
	protected SshClient ssh;
	protected ScpClient scp;
	protected SftpClient sftp;
	
	public SSHHost(String hostname, String username, String password) {
		this(hostname, 22, username, password);
	}
	
	public SSHHost(String hostname, int port, String username, String password) {
		this.hostname = hostname;
		this.port = port;
		this.username = username;
		this.password = password;
		this.address = hostname;
	}
	
	protected void ensureSshOpen() throws IllegalStateException, IOException, UnknownHostException {
		if (isOpen())
			return;
		if (isClosed())
			throw new IllegalStateException("SSH connection administratively/explicitly closed");
		close(); // ensure any existing sftp or scp client gets closed (for gc)
		
		address = InetAddress.getByName(hostname).getHostAddress();
		
		ssh = new SshClient();
		PasswordAuthenticationClient pwd = new PasswordAuthenticationClient();

		pwd.setUsername(username);
		pwd.setPassword(password);

		int result = ssh.authenticate(pwd);
		if (result!=AuthenticationProtocolState.COMPLETE) {
			throw new IllegalStateException("authentication failed. attempted login as user: "+username+" using password: "+password+" on host: "+hostname+":"+port);
		}
	}
	
	protected void ensureScpOpen() throws UnknownHostException, IOException {
		if (scp != null)
			return;
		ensureSshOpen();
		scp = ssh.openScpClient();
	}
	
	protected void ensureSftpOpen() throws UnknownHostException, IOException {
		if (sftp != null && !sftp.isClosed())
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
		if (sftp!=null) {
			try {
				sftp.quit();
			} catch (IOException e) {
			}
			sftp = null;
		}
		scp = null;
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
		try {
			if (isDirectory(path)) {
				// ensure empty
				if (isWindows()) {
					path = toWindowsPath(path);
					cmd("RMDIR /Q /S \""+path+"\"", NO_TIMEOUT);
				} else {
					path = toUnixPath(path);
					exec("rm -rf \""+path+"\"", NO_TIMEOUT);
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
			FileAttributes fa = sftp.stat(path);
			return fa.isFile() || fa.isDirectory();
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
		return false;
	}
	
	@Override
	public void copy(String src, String dst) throws Exception {
		if (isWindows()) {
			src = toWindowsPath(src);
			dst = toWindowsPath(dst);
			if (isDirectory(src))
				// ensure xcopy sees destination is supposed to be a directory, or xcopy will ask/block forever
				dst += "\\"; 
			exec("xcopy /Q /Y /S /E \""+src+"\" \""+dst+"\"", NO_TIMEOUT);
		} else {
			src = toUnixPath(src);
			dst = toUnixPath(dst);
			exec("cp \""+src+"\" \""+dst+"\"", NO_TIMEOUT);
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
		ensureScpOpen();
		NoCharsetByLineReader reader = new NoCharsetByLineReader(scp.get(file));
		String str = IOUtil.toString(reader);
		reader.close();
		return str;
	}

	@Override
	public String getContentsDetectCharset(String file, CharsetDeciderDecoder cdd) throws IOException {
		ensureScpOpen();
		MultiCharsetByLineReader reader = new MultiCharsetByLineReader(scp.get(file), cdd);
		String str = IOUtil.toString(reader);
		reader.close();
		return str;
	}

	@Override
	public ByLineReader readFile(String file) throws FileNotFoundException, IOException {
		ensureScpOpen();
		return new NoCharsetByLineReader(scp.get(file));
	}

	@Override
	public ByLineReader readFileDetectCharset(String file, CharsetDeciderDecoder cdd) throws FileNotFoundException, IOException {
		ensureScpOpen();
		return new MultiCharsetByLineReader(scp.get(file), cdd);
	}
	
	@Override
	public void saveTextFile(String filename, String text, Charset charset) throws IOException {
		if (text==null)
			text = "";
		ensureScpOpen();
		byte[] bytes = charset == null ? text.getBytes() : text.getBytes(charset);
		scp.put(new ByteArrayInputStream(bytes), bytes.length, filename, filename);
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
		public void close() {
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
		if (thread != null && thread_slow_sec>NO_TIMEOUT) {
			timer.schedule(new TimerTask() {
					public void run() {
						thread.notifySlowTest();
					}
				}, thread_slow_sec*1000);
		}
		//
		
		// read output from command
		StringBuilder sb = new StringBuilder(1024);
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
		
		out.close();
		
		// wait for exit
		session.getState().waitForState(ChannelState.CHANNEL_CLOSED);
		
		//
		eo.exit_code = session.getExitCode();
		if (reader instanceof AbstractDetectingCharsetReader)
			eo.charset = ((AbstractDetectingCharsetReader)reader).cs;
		eo.output = sb.toString();
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
				return cmd("ECHO %"+name+"%", ONE_MINUTE).output;
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
		sftp.mkdirs(path);
	}

	@Override
	public void download(String src, String dst) throws IllegalStateException, IOException, Exception {
		ensureScpOpen();
		IOUtil.copy(scp.get(src), new BufferedOutputStream(new FileOutputStream(dst)));
	}
	
	protected static void walk(File[] files, LinkedList<String> file_list) {
		for (File file : files) {
			if (file.isDirectory())
				walk(file.listFiles(), file_list);
			else
				file_list.add(file.getAbsolutePath());
		}
	}

	@Override
	public void upload(String src, String dst) throws IllegalStateException, IOException, Exception {
		ensureScpOpen();
		
		File fsrc = new File(src);
		if (fsrc.isDirectory()) {
			LinkedList<String> file_list = new LinkedList<String>();
			
			walk(fsrc.listFiles(), file_list);
			
			scp.put((String[]) file_list.toArray(new String[file_list.size()]), dst, false);
		} else {
			// uploading single file
			scp.put(new BufferedInputStream(new FileInputStream(fsrc)), fsrc.length(), src, dst);
		}
	}

	@Override
	public ExecOutput exec(String cmd, int timeout, Map<String, String> env, byte[] stdin_post, Charset charset, String chdir) throws Exception {
		return exec(cmd, timeout, env, stdin_post, charset, chdir, null, NO_TIMEOUT);
	}

	@Override
	public String getOSNameLong() {
		if (os_name_long!=null)
			return os_name_long;
		if (isWindows())
			return os_name_long = getOSNameOnWindows();
		String v;
		try {
			v = " "+exec("uname -a", NO_TIMEOUT).output;
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
			FileAttributes fa = sftp.stat(path);
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
			List list = sftp.ls(path);
			return (String[]) list.toArray(new String[list.size()]);
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
		return StringUtil.EMPTY_ARRAY;
	}

} // end public class SSHHost
