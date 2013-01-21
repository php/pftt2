package com.mostc.pftt.host;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import org.jvnet.winp.WinProcess;

import com.github.mattficken.io.AbstractDetectingCharsetReader;
import com.github.mattficken.io.ByLineReader;
import com.github.mattficken.io.CharsetByLineReader;
import com.github.mattficken.io.CharsetDeciderDecoder;
import com.github.mattficken.io.DefaultCharsetDeciderDecoder;
import com.github.mattficken.io.IOUtil;
import com.github.mattficken.io.MultiCharsetByLineReader;
import com.github.mattficken.io.NoCharsetByLineReader;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.runner.AbstractTestPackRunner.TestPackRunnerThread;
import com.mostc.pftt.util.StringUtil;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT.HANDLE;

/** Represents the local Host that the program is currently running on.
 * 
 * @author Matt Ficken
 *
 */

@SuppressWarnings("unused")
public class LocalHost extends Host {
	private static final Timer timer = new Timer();
	private static final boolean is_windows = System.getProperty("os.name").toLowerCase().contains("windows");
	private static int self_process_id;
	static {
		if (isLocalhostWindows()) {
			// only need this on windows (see LocalExecHandle#close)
			try {
				// this works only on Windows
				self_process_id = Kernel32.INSTANCE.GetCurrentProcessId();
			} catch ( Throwable t ) {
				t.printStackTrace();
			}
		}
	}
	
	public static boolean isLocalhostWindows() {
		return is_windows;
	}
	
	static {
		if (DEV>0) {
			new File(new LocalHost().getPfttDir()).mkdirs();
		}
	}
	
	@Override
	public String getName() {
		return "Localhost";
	}
	
	@Override
	public String pathsSeparator() {
		return File.pathSeparator;
	}
	
	@Override
	public String dirSeparator() {
		return File.separator;
	}
	
	@Override
	public ByLineReader readFile(String file) throws FileNotFoundException, IOException {
		return new NoCharsetByLineReader(new FileInputStream(file));
	}
	
	@Override
	public ByLineReader readFile(String file, Charset cs) throws IllegalStateException, FileNotFoundException, IOException {
		return new CharsetByLineReader(new FileInputStream(file), cs);
	}
	
	@Override
	public ByLineReader readFileDetectCharset(String file, CharsetDeciderDecoder cdd) throws FileNotFoundException, IOException {
		return new MultiCharsetByLineReader(new FileInputStream(file), cdd);
	}
	
	@Override
	public boolean isWindows() {
		return isLocalhostWindows();
	}

	@Override
	public void delete(String path) {
		if (isDirectory(path)) {
			// ensure empty
			try {
				if (isWindows()) {
					path = toWindowsPath(path);
					cmd("RMDIR /Q /S \""+path+"\"", NO_TIMEOUT);
				} else {
					path = toUnixPath(path);
					exec("rm -rf \""+path+"\"", NO_TIMEOUT);
				}
			} catch ( Exception ex ) {
				ex.printStackTrace();
			}
		} else {
			new File(path).delete();
		}
	} // end public void delete

	@Override
	public boolean exists(String path) {
		return new File(path).exists();
	}
	
	@Override
	public boolean isDirectory(String path) {
		return new File(path).isDirectory();
	}

	@Override
	public void saveTextFile(String filename, String text) throws IOException {
		saveTextFile(filename, text, null);
	}

	@Override
	public void saveTextFile(String filename, String text, CharsetEncoder ce) throws IOException {
		if (text==null)
			text = "";
		FileOutputStream fos = new FileOutputStream(filename);
		try {
			if (ce==null) {
				fos.write(text.getBytes());
			} else {
				ByteBuffer bbuf = ByteBuffer.allocate(50+(text.length()*2));
				ce.encode(CharBuffer.wrap(text.toCharArray()), bbuf, true);
				fos.write(bbuf.array(), 0, bbuf.limit());
			}
		} finally {
			fos.close();
		}
	}

	@Override
	public ExecOutput exec(String commandline, int timeout, Map<String,String> env, byte[] stdin, Charset charset, String chdir) throws Exception {
		return exec(commandline, timeout, env, stdin, charset, chdir, null, NO_TIMEOUT);
	}
	@Override
	public LocalExecHandle execThread(String commandline, Map<String,String> env, String chdir, byte[] stdin_data) throws Exception {
		return exec_impl(splitCmdString(commandline), env, chdir, NO_TIMEOUT, stdin_data);
	}
	@Override
	public ExecOutput exec(String commandline, int timeout, Map<String,String> env, byte[] stdin_data, Charset charset, String chdir, TestPackRunnerThread thread, int thread_slow_sec) throws Exception {
		ThreadSlowTask task = null;
		if (thread!=null && thread_slow_sec>NO_TIMEOUT) {
			task = new ThreadSlowTask(thread);
			timer.schedule(task, thread_slow_sec * 1000);
		}

		LocalExecHandle eh = exec_impl(splitCmdString(commandline), env, chdir, timeout, stdin_data); 
			
		eh.run(charset);
		
		if (task!=null)
			task.cancel();
		
		ExecOutput out = new ExecOutput();
		out.output = eh.getOutput();
		out.charset = eh.charset;
		out.exit_code = eh.getExitCode();
		
		return out;	
	}
	
	protected static class ThreadSlowTask extends TimerTask {
		protected final TestPackRunnerThread thread;
		
		protected ThreadSlowTask(TestPackRunnerThread thread) {
			this.thread = thread;
		}
		
		@Override
		public void run() {
			thread.notifySlowTest();
		}
		
	}
	
	@Override
	public String getContents(String file) throws IOException {
		NoCharsetByLineReader reader = new NoCharsetByLineReader(new FileInputStream(file));
		String str = IOUtil.toString(reader, IOUtil.HALF_MEGABYTE);
		reader.close();
		return str;
	}
	
	@Override
	public String getContentsDetectCharset(String file, CharsetDeciderDecoder cdd) throws IOException {
		MultiCharsetByLineReader reader = new MultiCharsetByLineReader(new FileInputStream(file), cdd);
		String str = IOUtil.toString(reader, IOUtil.HALF_MEGABYTE);
		reader.close();
		return str;
	}
	
	@Override
	public void copy(String src, String dst) throws Exception {
		if (isWindows()) {
			src = toWindowsPath(src);
			dst = toWindowsPath(dst);
			
			String cmd = null;
			if (isDirectory(src)) {
				// ensure xcopy sees destination is supposed to be a directory, or xcopy will ask/block forever
				dst += "\\";
				
				cmd = "xcopy /Q /Y /S /E \""+src+"\" \""+dst+"\"";
			} else {
				mkdirs(dirname(dst));
				if (basename(src).equals(basename(dst))) {
					dst = dirname(dst);
					
					cmd = "xcopy /Q /Y /S /E \""+src+"\" \""+dst+"\"";
				}
			}
			if (cmd==null)
				cmd = "cmd /C copy \""+src+"\" \""+dst+"\"";
			
			
			exec(cmd, NO_TIMEOUT);
		} else {
			src = toUnixPath(src);
			dst = toUnixPath(dst);
			exec("cp \""+src+"\" \""+dst+"\"", NO_TIMEOUT);
		}
	}

	@Override
	public String getUsername() {
		return System.getProperty("user.name");
	}
	
	public class LocalExecHandle extends ExecHandle {
		protected Process process;
		protected OutputStream stdin;
		protected InputStream stdout, stderr;
		protected ExitMonitorTask task;
		protected String image_name;
		protected Charset charset;
		protected StringBuilder output_sb;
		
		public LocalExecHandle(Process process, OutputStream stdin, InputStream stdout, InputStream stderr, String[] cmd_array) {
			this.process = process;
			this.stdin = stdin;
			this.stdout = stdout;
			this.stderr = stderr;
			this.image_name = StringUtil.unquote(basename(cmd_array[0]));
		}
		
		@Override
		public boolean isRunning() {
			try {
				process.exitValue();
				return false;
			} catch ( IllegalThreadStateException ex ) {
				return true;
			}
		}

		boolean run = true;
		@Override
		public synchronized void close(boolean force) {
			run = false;
			
			// may take multiple tries to make it exit (lots of processes, certain OSes, etc...)
			for ( int tries = 0 ; tries < 10 ; tries++ ) {
				// 
				//
				try {
					process.exitValue();
					break; 
					// process terminated, stop trying (or may terminate new process reusing the same id)
				} catch ( Throwable t ) {
					// hasn't exited yet
					if (tries==0) {
						// try closing streams to encourage it to exit
						try {
							stdin.close();
						} catch ( Throwable t2 ) {
							t2.printStackTrace();
						}
						try {
							stdout.close();
						} catch ( Throwable t2 ) {
							t2.printStackTrace();
						}
						try {
							stderr.close();
						} catch ( Throwable t2 ) {
							t2.printStackTrace();
						}
					}
				
					// kill it
					if (isLocalhostWindows()) {
						// Windows BN: process trees on Windows won't get terminated correctly by calling Process#destroy
						//
						// have to do some ugly hacks on Windows to kill process trees
						int process_id = 0;
						
						// first: find the process id
						getWindowsProcessID(process);
						
						
						// NOTE: on Windows, if WER is not disabled(enabled by default), if a process crashes,
						//       WER popup will appear and process will block until popup closed
						//       -this can be detected by looking for `C:\Windows\SysWOW64\WerFault.exe -u -p <process id> -s 1032`
						if (force && (tries > 1 && tries < 5)) {
							// need to kill this process (execution timeout or other critical)
							// but have failed at least once to kill it
							//
							// see if its exit/killing is blocked by werfault.exe
							//
							try {
								String[] lines = exec("WMIC path win32_process get Processid,Commandline", Host.ONE_MINUTE).getLines();
								String prev_line = "";
								for ( String line : lines ) {
									line = line.toLowerCase();
									if (line.contains("werfault.exe") && line.contains("-p "+process_id)) {
										// blocking werfault.exe (not actually the process parent, its a separate SVC)
										int blocking_process_id = Integer.parseInt(prev_line.trim());
										
										// kill werfault.exe so we can try killing the target process again
										winKillProcess("werfault.exe", blocking_process_id);
									}
									prev_line = line;
								}
							} catch ( Exception ex ) {
								ex.printStackTrace();
							}
						}
						//
						
						// second: make sure we found a process id (safety check: make sure its not our process id)
						if (process_id != 0 && process_id!=self_process_id && (image_name.equals("cmd.exe")||image_name.equals("conhost.exe"))) {
							// also, WinProcess#killRecursively only checks by process id (not image/program name)
							// while that should be enough, experience on Windows has shown that it isn't and somehow gets PFTT killed eventually
							//
							//
							// Process#destroy works for processes except for those that
							// are launched using cmd.exe (the parent of those processes is conhost.exe)
							//
							// actually, if a bunch of httpd.exe processes are being
							// killed at ~same time, we can have an explosion of taskkill.exe processes
							// so its better to not use taskkill.exe for processes that don't need it
							// (ie only use taskkill.exe for cmd.exe or conhost.exe)
							// 
							winKillProcess(image_name, process_id);
						}
					} // end if
					//
					
					
					// terminate through java Process API
					// this is works on Linux and is a fallback on Windows
					try {
						process.destroy();
					} catch ( Throwable t2 ) {
						t2.printStackTrace();
					}
					//
					
				} // end try
			} // end for
		} // end public void close
		
		protected void winKillProcess(String image_name, int process_id) {
			// third:instead, run TASKKILL and provide it both the process id and image/program name
			//
			// image name: ex: `php.exe` 
			// /F => forcefully terminate ('kill')
			// /T => terminate all child processes (process is cmd.exe and PHP is a child)
			//      process.destory might not do this, so thats why its CRITICAL that TASKKILL
			//      be tried before process.destroy
			try {
				Runtime.getRuntime().exec("TASKKILL /FI \"IMAGENAME eq "+image_name+"\" /FI \"PID eq "+process_id+"\" /F /T");
			} catch (Throwable t3) {
				t3.printStackTrace();
			}
		}
		
		protected void run(Charset charset) throws IOException, InterruptedException {
			output_sb = new StringBuilder(1024);
			exec_copy_lines(output_sb, stdout, charset);
			//
			// ignores STDERR
			//exec_copy_lines(stderr, null);
			
			int w = process.waitFor();
			
			try {
				process.destroy();
			} catch ( Exception ex ) {
			
			}
			if (task!=null)
				task.cancel();
		}
				
		protected void exec_copy_lines(StringBuilder sb, InputStream in, Charset charset) throws IOException {
			DefaultCharsetDeciderDecoder d = charset == null ? null : PhptTestCase.newCharsetDeciderDecoder();
			ByLineReader reader = charset == null ? new NoCharsetByLineReader(in) : new MultiCharsetByLineReader(in, d);
			String line;
			try {
				while (reader.hasMoreLines()&&run) {
					line = reader.readLine();
					if (line==null)
						break;
					sb.append(line);
					sb.append('\n');
				}
			} catch ( IOException ex ) {
				//ex.printStackTrace();
			}
			
			in.close();
			
			if (reader instanceof AbstractDetectingCharsetReader)
				this.charset = ((AbstractDetectingCharsetReader)reader).cs;// TODO d.getCommonCharset();
		}

		@Override
		public String getOutput() {
			StringBuilder sb = output_sb;
			return sb == null ? "" : sb.toString();
		}

		@Override
		public int getExitCode() {
			try {
				return process.exitValue();
			} catch ( Exception ex ) {
				return 0;
			}
		}

		public int getProcessID() {
			return isLocalhostWindows() ? getWindowsProcessID(process) : 0;
		}

		@Override
		public InputStream getSTDOUT() {
			return process.getInputStream();
		}

		@Override
		public OutputStream getSTDIN() {
			return process.getOutputStream();
		}
		
	} // end public class LocalExecHandle
	
	public static int getWindowsProcessID(Process process) {
		try {
			// clean way
			WinProcess wproc = new WinProcess(process);
			return wproc.getPid();
		} catch ( Throwable wt ) {
			// WinProcess native code couldn't be loaded
			// (maybe it wasn't included or maybe somebody didn't compile it)
			//
			// fallback on some old code using reflection, etc...
			try {
				// process.getClass() != Process.class
				//
				// kind of a hack to get the process id:
				//      look through hidden fields to find a field like java.lang.ProcessImpl#handle (long)
				for (java.lang.reflect.Field f : process.getClass().getDeclaredFields() ) {
					if (f.getType()==long.class) { // ProcessImpl#handle
						// this is a private field. without this, #getLong will throw an IllegalAccessException
						f.setAccessible(true); 
						
						long handle = f.getLong(process);
						
						HANDLE h = new HANDLE();
						h.setPointer(Pointer.createConstant(handle));
						return Kernel32.INSTANCE.GetProcessId(h);
					}
				} // end for
			} catch ( Throwable t2 ) {
				t2.printStackTrace();
			} // end try
		} // end try
		return 0;
	} // end public static int getWindowsProcessID
	
	private static final Pattern PAT_QUOTE = Pattern.compile("\\\"");
	public static String[] splitCmdString(String command) {
		LinkedList<String> parts = new LinkedList<String>();
		String buf = "";
		char c;
		boolean in_quote = false;
		for ( int i=0 ; i < command.length() ; i++ ) {
			c = command.charAt(i);
			if (c=='\"' && (i==0||command.charAt(i-1) != '\\')) {
				in_quote = !in_quote;
			}
			if (c == ' ' && !in_quote) {
				buf = buf.trim();
				if (buf.length() > 0) {
					buf = StringUtil.unquote(buf);
					
					buf = StringUtil.replaceAll(PAT_QUOTE, "\"", buf);
					
					parts.add(buf);
				}
				buf = "";
				continue;
			}
			buf += c;
		}
		buf = buf.trim();
		if (buf.length() > 0) {
			if (buf.startsWith("\"")) {
				buf = buf.substring(1, buf.length()-1);
				
				buf = StringUtil.replaceAll(PAT_QUOTE, "\"", buf);
			}
			parts.add(buf);
		}
		
		return (String[])parts.toArray(new String[]{});
	} // end public static String[] splitCmdString
	
	protected LocalExecHandle exec_impl(String[] cmd_array, Map<String,String> env, String chdir, int timeout, byte[] stdin_data) throws IOException, InterruptedException {
		ProcessBuilder builder = new ProcessBuilder(cmd_array);
		if (env!=null)
			builder.environment().putAll(env);
		if (StringUtil.isNotEmpty(chdir))
			builder.directory(new File(chdir));
		builder.redirectErrorStream(true);
			      
		// start the process
		Process process;
		try {
			process = builder.start();
		} catch ( IOException ex ) {
			if (ex.getMessage().contains("file busy")) {
				// randomly sometimes on Linux, get this problem ... wait and try again
				Thread.sleep(100);
				process = builder.start();
			} else {
				throw ex;
			}
		}
			      
		OutputStream stdin = process.getOutputStream();
		
		if (stdin_data!=null && stdin_data.length>0) {
	    	stdin.write(stdin_data);
	    	stdin.flush();
	    }
		
	    InputStream stdout = process.getInputStream();
	    InputStream stderr = process.getErrorStream();

	    LocalExecHandle h = new LocalExecHandle(process, stdin, stdout, stderr, cmd_array);
	    
	    if (timeout>NO_TIMEOUT) {
	    	h.task = new ExitMonitorTask(h);
			timer.schedule(h.task, 5*1000);
	    }
	    
	    return h;
	} // end protected LocalExecHandle exec_impl
		
	protected static class ExitMonitorTask extends TimerTask {
		protected final LocalExecHandle h;
		
		protected ExitMonitorTask(LocalExecHandle h) {
			this.h = h;
		}
		
		@Override
		public void run() {
			// go further trying to kill the process
			//
			// LocalHostExecHandle#close checks for WerFault.exe blocking on Windows
			h.close(true);
		}
		
	} // end protected static class ExitMonitorTask	

	@Override
	public String getEnvValue(String name) {
		return System.getenv(name);
	}

	@Override
	public boolean equals(Object o) {
		return o == this || o instanceof LocalHost;
	}

	@Override
	public String getHostname() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			return null;
		}
	}

	@Override
	public void mkdirs(String path) throws IllegalStateException, IOException {
		new File(path).mkdirs();
	}

	@Override
	public void downloadCompressWith7Zip(String src, String dst) throws IllegalStateException, IOException, Exception {
		download(src, dst);
	}

	@Override
	public void download(String src, String dst) throws IllegalStateException, IOException, Exception {
		copy(src, dst);
	}

	@Override
	public void uploadCompressWith7Zip(String src, String dst) throws IllegalStateException, IOException, Exception {
		upload(src, dst);
	}

	@Override
	public void upload(String src, String dst) throws IllegalStateException, IOException, Exception {
		copy(src, dst);
	}

	@Override
	public boolean isRemote() {
		return false;
	}

	@Override
	public String getOSNameLong() {
		return System.getProperty("os.name");
	}

	protected String addr;
	@Override
	public String getAddress() {
		if (this.addr!=null)
			return this.addr;
		try {
			Enumeration<NetworkInterface> interfaces;
			Enumeration<InetAddress> addrs;
			NetworkInterface ni;
			InetAddress addr;
			String addr_str;
			interfaces = NetworkInterface.getNetworkInterfaces();
			// try to get an IPv4 address first (its shorter, easier for people to work with)
			while (interfaces.hasMoreElements()) {
				ni = interfaces.nextElement();
				addrs = ni.getInetAddresses();
				while (addrs.hasMoreElements()) {
					addr = addrs.nextElement();
					addr_str = addr.getHostAddress();
					if (addr_str.equals("127.0.0.1"))
						// loopback address, which is not bindable on Windows
						continue;
					else if (addr_str.split("\\.").length==4)
						// IPv4 address
						return this.addr = addr_str;
				}
			}
			// fallback on an IPv6 address
			interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				ni = interfaces.nextElement();
				addrs = ni.getInetAddresses();
				while (addrs.hasMoreElements()) {
					addr = addrs.nextElement();
					addr_str = addr.getHostAddress();
					if (addr_str.equals("0:0:0:0:0:0:0:1")||addr_str.equals("0:0:0:0:0:0:0:0"))
						// loopback address, which is not bindable on Windows
						continue;
					else if (addr_str.contains(":")&&!addr_str.contains("%"))
						// ex: 2001:0:4137:9e76:3cb8:730:3f57:feaf
						return this.addr = "["+addr_str+"]";
				}
			}
		} catch (SocketException ex) {
			ex.printStackTrace();
		}
		// no network interfaces!
		return null;
	} // end public String getAddress

	@Override
	public boolean isOpen() {
		return true;
	}

	@Override
	protected String getOSNameOnWindows() {
		return getOSNameLong();
	}

	@Override
	public boolean dirContainsExact(String path, String name) {
		for ( File file : new File(path).listFiles() ) {
			if (file.getName().equalsIgnoreCase(name))
				return true;
		}
		return false;
	}

	@Override
	public boolean dirContainsFragment(String path, String name_fragment) {
		name_fragment = name_fragment.toLowerCase();
		for ( File file : new File(path).listFiles() ) {
			if (file.getName().toLowerCase().contains(name_fragment))
				return true;
		}
		return false;
	}

	@Override
	public String[] list(String path) {
		return new File(path).list();
	}
	
	public static String getLocalPfttDir() {
		if (isLocalhostWindows()) {
			String sd = System.getenv("SYSTEMDRIVE");
			if (StringUtil.isEmpty(sd))
				sd = "C:";
			if (DEV>0)
				return sd+"\\php-sdk\\PFTT\\Dev-"+DEV+"\\";
			else
				return sd+"\\php-sdk\\PFTT\\Current\\";
		} else if (DEV>0) {
			return System.getenv("HOME")+"/php-sdk/PFTT/dev-"+DEV+"/";
		} else {
			return System.getenv("HOME")+"/php-sdk/PFTT/current/";
		}
	}

	@Override
	public long getSize(String file) {
		return new File(file).length();
	}
	
} // end public class Host
