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
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import com.github.mattficken.io.AbstractDetectingCharsetReader;
import com.github.mattficken.io.ByLineReader;
import com.github.mattficken.io.CharsetDeciderDecoder;
import com.github.mattficken.io.DefaultCharsetDeciderDecoder;
import com.github.mattficken.io.IOUtil;
import com.github.mattficken.io.MultiCharsetByLineReader;
import com.github.mattficken.io.NoCharsetByLineReader;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.runner.AbstractTestPackRunner.TestPackRunnerThread;
import com.mostc.pftt.util.StringUtil;

/** Represents the local Host that the program is currently running on.
 * 
 * @author Matt Ficken
 *
 */

@SuppressWarnings("unused")
public class LocalHost extends Host {
	private static final Timer timer = new Timer();
	private static final boolean is_windows = System.getProperty("os.name").toLowerCase().contains("windows");
	
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
	public ByLineReader readFileDetectCharset(String file, CharsetDeciderDecoder cdd) throws FileNotFoundException, IOException {
		return new MultiCharsetByLineReader(new FileInputStream(file), cdd);
	}
	
	@Override
	public boolean isWindows() {
		return isLocalhostWindows();
	}

	@Override
	public void delete(String path) {
		new File(path).delete();
	}

	@Override
	public boolean exists(String path) {
		return new File(path).exists();
	}

	@Override
	public void saveText(String filename, String text) throws IOException {
		saveText(filename, text, null);
	}

	@Override
	public void saveText(String filename, String text, Charset charset) throws IOException {
		if (text==null)
			text = "";
		FileOutputStream fos = new FileOutputStream(filename);
		if (charset==null)
			fos.write(text.getBytes());
		else
			fos.write(text.getBytes(charset));
		fos.close();
		
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
			thread.slowTest();
		}
		
	}
	
	@Override
	public String getContents(String file) throws IOException {
		NoCharsetByLineReader reader = new NoCharsetByLineReader(new FileInputStream(file));
		String str = IOUtil.toString(reader);
		reader.close();
		return str;
	}
	
	@Override
	public String getContentsDetectCharset(String file, CharsetDeciderDecoder cdd) throws IOException {
		MultiCharsetByLineReader reader = new MultiCharsetByLineReader(new FileInputStream(file), cdd);
		String str = IOUtil.toString(reader);
		reader.close();
		return str;
	}
	
	@Override
	public void copyFile(String src, String dst) throws Exception {
		if (isWindows()) {
			src = src.replace('/', '\\');
			dst = dst.replace('/', '\\');
			this.exec("cmd /C copy /Y "+src+" "+dst, 0);
		} else
			this.exec("cp "+src+" "+dst, 0);
	}

	@Override
	public String getUsername() {
		return System.getProperty("user.name");
	}
	
	public static class LocalExecHandle extends ExecHandle {
		protected Process process;
		protected OutputStream stdin;
		protected InputStream stdout, stderr;
		protected ExitMonitorTask task;
		protected String cmdline;
		protected Charset charset;
		protected StringBuilder output_sb;
		
		public LocalExecHandle(Process process, OutputStream stdin, InputStream stdout, InputStream stderr, ExitMonitorTask task, String cmdline) {
			this.process = process;
			this.stdin = stdin;
			this.stdout = stdout;
			this.stderr = stderr;
			this.task = task;
			this.cmdline = cmdline;
		}
		
		@Override
		public boolean isRunning() {
			try {
				process.exitValue();
				return false;
			} catch ( Exception ex ) {
				return true;
			}
		}

		@Override
		public void close() {
			for ( int i=0 ; i < 10 ; i++ ) {
				try {
					process.destroy();
				} catch ( Throwable ex ) {
					ex.printStackTrace();
				}
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
				while (reader.hasMoreLines()) {
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
		public boolean isCrashed() {
			return getExitCode() != 0;
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
	} // end public static class LocalExecHandle
	
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
					if (buf.startsWith("\""))
						buf  = buf.substring(1, buf.length()-1);
					
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
	
	protected static LocalExecHandle exec_impl(String[] cmd_array, Map<String,String> env, String chdir, int timeout, byte[] stdin_data) throws IOException, InterruptedException {
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

	    ExitMonitorTask task = null;
	    if (timeout>NO_TIMEOUT) {
	    	task = new ExitMonitorTask(process,stdin, stdout, stderr);
			timer.schedule(task, 5*1000);
	    }
	    
	    return new LocalExecHandle(process, stdin, stdout, stderr, task, StringUtil.toString(cmd_array));
	} // end protected static LocalExecHandle exec_impl
		
	protected static class ExitMonitorTask extends TimerTask {
		protected Process process;
		protected OutputStream stdin;
		protected InputStream stdout, stderr;
		
		protected ExitMonitorTask(Process process, OutputStream stdin, InputStream stdout, InputStream stderr) {
			this.process = process;
			this.stdin = stdin;
			this.stdout = stdout;
			this.stderr = stderr;
		}
		
		@Override
		public void run() {
			// may take multiple tries to make it exit (lots of processes, certain OSes, etc...)
			for ( int tries = 0 ; tries < 10 ; tries++ ) {
				try {
					process.exitValue();
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
						// process.destory doesn't always work on windows, but TASKKILL does
						try {
							// process.getClass() != Process.class
							//
							// kind of a hack to get the process id:
							//      look through hidden fields to find a field like java.lang.ProcessImpl#handle (long)
							for (java.lang.reflect.Field f : process.getClass().getDeclaredFields() ) {
								if (f.getType()==long.class) {
									// this is a private field. without this, #getLong will throw an IllegalAccessException
									f.setAccessible(true); 
									
									long process_id = f.getLong(process);
									
									// TASKKILL!=TSKILL, TASKKILL is better than TSKILL
									//
									// /F => forcefully terminate ('kill')
									// /T => terminate all child processes (process is cmd.exe and PHP is a child)
									//      process.destory might not do this, so thats why its CRITICAL that TASKKILL
									//      be tried before process.destroy
									Runtime.getRuntime().exec("TASKKILL /PID:"+process_id+" /F /T");
									
									break;
								}
							}
						} catch ( Throwable t2 ) {
							t2.printStackTrace();
						}
					}					
					try {
						process.destroy();
					} catch ( Throwable t2 ) {
						t2.printStackTrace();
					}
					
					
					
				} // end try
			}
		} // end public void run
		
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
		copyFile(src, dst);
	}

	@Override
	public void uploadCompressWith7Zip(String src, String dst) throws IllegalStateException, IOException, Exception {
		upload(src, dst);
	}

	@Override
	public void upload(String src, String dst) throws IllegalStateException, IOException, Exception {
		copyFile(src, dst);
	}

	@Override
	public boolean isRemote() {
		return false;
	}

	@Override
	public String getOSNameLong() {
		return System.getProperty("os.name");
	}

	@Override
	public String getAddress() {
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			Enumeration<InetAddress> addrs;
			NetworkInterface ni;
			InetAddress addr;
			String addr_str;
			while (interfaces.hasMoreElements()) {
				ni = interfaces.nextElement();
				addrs = ni.getInetAddresses();
				while (addrs.hasMoreElements()) {
					addr = addrs.nextElement();
					addr_str = addr.getHostAddress();
					if (addr_str.equals("127.0.0.1"))
						continue;
					if (addr_str.split("\\.").length==4)
						// IPv4 address
						return addr_str;
				}
			}
		} catch (SocketException ex) {
			ex.printStackTrace();
		}
		// no network interfaces
		return null;
	} // end public String getAddress
	
} // end public class Host
