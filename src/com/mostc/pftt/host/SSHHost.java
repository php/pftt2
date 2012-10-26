package com.mostc.pftt.host;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import com.github.mattficken.io.ByLineReader;
import com.github.mattficken.io.CharsetDeciderDecoder;
import com.mostc.pftt.runner.AbstractTestPackRunner.TestPackRunnerThread;

public class SSHHost extends RemoteHost {
	
	@Override
	protected void finalize() {
		close();
	}
	
	@Override
	public void close() {
		
	}
	
	protected Boolean is_windows;
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void saveText(String filename, String text, Charset charset)
			throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(String diff_filename) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean exists(String string) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public void copyFile(String string, String test_file) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String pathsSeparator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String dirSeparator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getContents(String file) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getContentsDetectCharset(String file,
			CharsetDeciderDecoder cdd) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ByLineReader readFile(String file) throws FileNotFoundException,
			IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ByLineReader readFileDetectCharset(String file,
			CharsetDeciderDecoder cdd) throws FileNotFoundException,
			IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void saveText(String filename, String text) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ExecOutput exec(String cmd, int timeout,
			Map<String, String> env, Charset charset, String chdir)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ExecOutput exec(String cmd, int timeout,
			String chdir) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ExecOutput exec(String cmd, int timeout,
			Map<String, String> object, byte[] stdin_post, Charset charset,
			String chdir) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getUsername() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getEnvValue(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean equals(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getHostname() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void mkdirs(String path) throws IllegalStateException, IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void download(String src, String dst) throws IllegalStateException,
			IOException, Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void upload(String src, String dst) throws IllegalStateException,
			IOException, Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ExecOutput exec(String commandline, int timeout,
			Map<String, String> env, byte[] stdin, Charset charset,
			String chdir, TestPackRunnerThread thread, int thread_slow_sec)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getOSNameLong() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ExecHandle execThread(String commandline, Map<String, String> env,
			String chdir, byte[] stdin_data) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getAddress() {
		// TODO Auto-generated method stub
		return null;
	}

}
