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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.varia.NullAppender;

import com.github.mattficken.io.ByLineReader;
import com.github.mattficken.io.ByteArrayIOStream;
import com.github.mattficken.io.CharsetByLineReader;
import com.github.mattficken.io.CharsetDeciderDecoder;
import com.github.mattficken.io.IOUtil;
import com.github.mattficken.io.MultiCharsetByLineReader;
import com.github.mattficken.io.NoCharsetByLineReader;
import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ConsoleManager.EPrintType;
import com.mostc.pftt.runner.AbstractTestPackRunner.TestPackRunnerThread;
import com.sshtools.j2ssh.SftpClient;
import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.connection.ChannelState;
import com.sshtools.j2ssh.io.IOStreamConnector;
import com.sshtools.j2ssh.session.SessionChannelClient;
import com.sshtools.j2ssh.sftp.FileAttributes;
import com.sshtools.j2ssh.sftp.SftpFile;
import com.sshtools.j2ssh.transport.HostKeyVerification;
import com.sshtools.j2ssh.transport.TransportProtocolException;
import com.sshtools.j2ssh.transport.publickey.SshPublicKey;
import com.sshtools.j2ssh.util.InvalidStateException;

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

// TODO add support for wildcards * in #copy #move and #delete
@SuppressWarnings("unused")
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
	protected final CommonCommandManager ccm;
	
	static {
		if (DEV>0)
			BasicConfigurator.configure(new ConsoleAppender());
		else
			// suppress any log4j warning message on first call to SSHHost#<init>
			BasicConfigurator.configure(new NullAppender());
	}
	
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
		
		ccm = new CommonCommandManager();
	}
	
	protected String normalizePath(String path) {
		// multiple backslashes can cause problems for windows
		return path.replace("\\\\", "\\");
	}
	
	@Override
	public boolean ensureConnected(ConsoleManager cm) {
		try {
			ensureSshOpen();
			return isWindows() || exists("/");
		} catch ( Exception ex ) {
			if (cm!=null)
				cm.addGlobalException(EPrintType.WARNING, getClass(), "ensureConnected", ex, "can't connect to remote ssh host");
			else
				ex.printStackTrace();
			return false;
		}
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
	public boolean delete(String path) {
		return ccm.delete(this, path, false);
	}
	
	@Override
	public boolean deleteElevated(String path) {
		return ccm.delete(this, path, true);
	}

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
	public boolean copy(String src, String dst) throws Exception {
		return ccm.copy(this, src, dst, false);
	}
	
	@Override
	public boolean copyElevated(String src, String dst) throws Exception {
		return ccm.copy(this, src, dst, true);
	}
	
	@Override
	public boolean move(String src, String dst) throws Exception {
		return ccm.move(this, src, dst, false);
	}
	
	@Override
	public boolean moveElevated(String src, String dst) throws Exception {
		return ccm.move(this, src, dst, true);
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
	public boolean saveTextFile(String filename, String text) throws IOException {
		return saveTextFile(filename, text, null);
	}
	
	private String _path;
	protected SessionChannelClient do_exec(String cmd, Map<String, String> env, String chdir, byte[] stdin_post, OutputStream out) throws IOException, IllegalStateException {
		ensureSshOpen();
		SessionChannelClient session = ssh.openSessionChannel();
		
		// prepare to execute
		if (StringUtil.isNotEmpty(chdir)) {
			// would be nice if there were a better way to do this
			if (isWindows()) {
				if (cmd.startsWith("cmd /C "))
					cmd = cmd.substring("cmd /C ".length());
				
				// 'cd' is not a program
				cmd = "cmd /C cd \""+chdir+"\" && "+cmd;
			} else {
				if (cmd.startsWith("bash -c "))
					cmd = cmd.substring("bash -c ".length());
				
				cmd = "bash -c cd \""+chdir+"\" && "+cmd;
			}
		}
		//
		
		if (env!=null) {
			//
			if (env.containsKey(PATH)) {
				if (_path==null)
					// cache for later
					_path = getEnvValue(PATH);
				
				String path = env.get(PATH);
				if (StringUtil.isNotEmpty(_path) && StringUtil.isNotEmpty(path)) {
					// merge
					path = path + pathsSeparator() + _path;
					
					env.put(PATH, path);
				}
			}
			//
			
			// send ENV vars
			for (String name : env.keySet()) {
				session.setEnvironmentVariable(name, env.get(name));
			}
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
	} // end protected SessionChannelClient do_exec
	
	@Override
	public ExecHandle execThread(String cmd, Map<String, String> env, String chdir, byte[] stdin_post) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
		SessionChannelClient session = do_exec(cmd, env, chdir, stdin_post, out);
		
		return new SSHExecHandle(session, out);
	}
	
	protected class SSHExecHandle extends ExecHandle {
		protected final SessionChannelClient session;
		protected final ByteArrayOutputStream out;
		
		protected SSHExecHandle(SessionChannelClient session, ByteArrayOutputStream out) {
			this.session = session;
			this.out = out;
		}
		
		@Override
		public void close(boolean force) {
			/* TODO ccm.winCloseAllHandles(SSHHost.this, process_id);
			ccm.winKillProcess(SSHHost.this, image_name, process_id);
			ccm.ensureWERFaultIsNotRunning(SSHHost.this, process_id); */
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
		public String getOutput(int max_len) {
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

		@Override
		public void run(StringBuilder output_sb, Charset charset, int timeout_sec, final TestPackRunnerThread thread, int slow_sec, boolean suspend) throws IOException, InterruptedException {
			do_run(session, charset, timeout_sec, thread, slow_sec);
			output_sb.append(out.toString());
		}
		
	} // end protected class SSHExecHandle
	
	@Override
	public ExecOutput execOut(final String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_post, Charset charset, String chdir, final TestPackRunnerThread thread, int slow_sec) throws Exception {
		final ByteArrayIOStream out = new ByteArrayIOStream(1024);
		
		final ExecOutput eo = new ExecOutput();
		
		//
		final SessionChannelClient session = do_exec(cmd, env, chdir, stdin_post, out);
		
		do_run(session, charset, timeout_sec, thread, slow_sec);
		
		
		//
		eo.cmd = cmd;
		eo.exit_code = session.getExitCode();
		/* TODO if (reader instanceof AbstractDetectingCharsetReader)
			eo.charset = ((AbstractDetectingCharsetReader)reader).cs; */
		eo.output = out.toString();
		//
		
		return eo;
	} // end public ExecOutput execOut
	
	protected void do_run(final SessionChannelClient session, Charset charset, int timeout_sec, final TestPackRunnerThread thread, int slow_sec) throws InvalidStateException, InterruptedException {
		final AtomicBoolean run = new AtomicBoolean(true);
		if (timeout_sec>NO_TIMEOUT) {
			timer.schedule(new TimerTask() {
					public void run() {
						try {
							run.set(false);
							
							session.close();
						} catch ( Exception ex ) {
							ex.printStackTrace();
						}
					}
				}, timeout_sec*1000);
		}
		if (thread != null && slow_sec>FOUR_HOURS) {
			timer.schedule(new TimerTask() {
					public void run() {
						thread.notifySlowTest();
					}
				}, slow_sec*1000);
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
	}

	@Override
	public ExecOutput execOut(String cmd, int timeout, String chdir) throws Exception {
		return execOut(cmd, timeout, null, null, chdir);
	}

	@Override
	public ExecOutput execOut(String cmd, int timeout, Map<String, String> env, Charset charset, String chdir) throws Exception {
		return execOut(cmd, timeout, env, null, charset, chdir);
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public String getEnvValue(String name) {
		try {
			if (isWindows())
				return StringUtil.chomp(cmdOut("ECHO %"+name+"%", ONE_MINUTE).output);
			else
				return execOut("echo $"+name, ONE_MINUTE).output;
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
	public boolean mkdirs(String path) throws IllegalStateException, IOException {
		ensureSftpOpen();
		path = normalizePath(path);
		// dont check #isSafePath here!
		if (isWindows()) {
			try {
				sftp.stat(path);
				return true; // already exists
			} catch ( Exception ex ) {
			}
			StringBuilder ppath_sb = new StringBuilder(path.length());
			String ppath = "";
			for ( String part : Host.splitPath(path) ) {
				if (ppath_sb.length()==0 && part.length()==2 && part.charAt(1)==':' && Character.isLetter(part.charAt(0))) {
					// drive letter support
					ppath_sb.append(part);
					continue;
				}
				ppath_sb.append('\\');
				ppath_sb.append(part);
				ppath = ppath_sb.toString();
				try {
					sftp.stat(ppath);
				} catch ( Exception ex ) {
					sftp.mkdir(ppath);
				}
			}
		} else {
			sftp.mkdirs(normalizePath(path));
		}
		return true;
	} // end public boolean mkdirs

	@Override
	public boolean download(String src, String dst) throws IllegalStateException, IOException, Exception {
		ensureSftpOpen();
		new File(dst).getParentFile().mkdirs();
		sftp.get(normalizePath(src), new BufferedOutputStream(new FileOutputStream(dst)));
		return true;
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
	public boolean upload(String src, String dst) throws IllegalStateException, IOException {
		ensureSftpOpen();
		dst = normalizePath(dst);
		
		File fsrc = new File(src);
		mkdirs(dirname(dst));
		if (fsrc.isDirectory()) {
			do_upload(src, fsrc.listFiles(), dst);
		} else {
			// uploading single file
			sftp.put(fsrc.getAbsolutePath(), dst);
		}
		return true;
	}

	@Override
	public ExecOutput execOut(String cmd, int timeout, Map<String, String> env, byte[] stdin_post, Charset charset, String chdir) throws Exception {
		return execOut(cmd, timeout, env, stdin_post, charset, chdir, null, FOUR_HOURS);
	}

	@Override
	public String getOSNameLong() {
		if (os_name_long!=null)
			return os_name_long;
		if (isWindows())
			return os_name_long = getOSNameOnWindows();
		String v;
		try {
			v = " "+execOut("uname -a", FOUR_HOURS).output;
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

	@SuppressWarnings({ "rawtypes"})
	@Override
	public String[] list(String path) {
		try {
			ensureSftpOpen();
			List list = sftp.ls(normalizePath(path));
			ArrayList<String> names = new ArrayList<String>(list.size());
			for (Object f : list) {
				names.add(((SftpFile)f).getFilename());
			}
			return (String[]) names.toArray(new String[names.size()]);
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
	public long getMTime(String file) {
		try {
			ensureSftpOpen();
			FileAttributes fa = sftp.stat(normalizePath(file));
			return fa.getModifiedTime().longValue();
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
	public boolean saveTextFile(String filename, String text, CharsetEncoder ce) throws IllegalStateException, IOException {
		if (!isSafePath(filename))
			return false;
		filename = normalizePath(filename);
		if (text==null)
			text = "";
		ensureSftpOpen();
		if (ce==null) {
			byte[] text_bytes = text.getBytes();
			sftp.put(new ByteArrayInputStream(text_bytes), filename);
		} else {
			ByteBuffer bbuf = ByteBuffer.allocate(50+text.length()*2);
			ce.encode(CharBuffer.wrap(text.toCharArray()), bbuf, true);
			sftp.put(new ByteBufferInputStream(bbuf), filename);
		}
		return true;
	}
	
	@Override
	public boolean deleteFileExtension(String dir, String ext) {
		if (!isSafePath(dir))
			return false;
		if (!ext.startsWith("."))
			ext = "." + ext;
		try {
			if (isWindows()) {
				cmd("forfiles /p "+dir+" /s /m *"+ext+" /c \"cmd /C del /Q @path\"", ONE_MINUTE*20);
			} else {
				exec("rm -rF "+dir+"/*"+ext, ONE_MINUTE*20);
			}
			return true;
		} catch ( Exception ex ) {
			return false;
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

	@Override
	protected boolean deleteSingleFile(String path) {
		try {
			ensureSftpOpen();
			sftp.rm(path);
			return true;
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
		return false;
	}

} // end public class SSHHost
