package com.mostc.pftt.host;

import java.nio.charset.Charset;
import java.util.Map;

import com.mostc.pftt.host.AHost.ExecHandle;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.runner.AbstractTestPackRunner.TestPackRunnerThread;

public interface IProgramRunner {
	
	public boolean exec(ConsoleManager cm, String ctx_str, String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_post, Charset charset, String current_dir) throws IllegalStateException, Exception;
	public boolean exec(ConsoleManager cm, String ctx_str, String commandline, int timeout, Map<String, String> env, byte[] stdin, Charset charset, String chdir, @SuppressWarnings("rawtypes") TestPackRunnerThread thread, int thread_slow_sec) throws Exception;
	public ExecHandle execThread(String commandline) throws Exception;
	public ExecHandle execThread(String commandline, byte[] stdin_data) throws Exception;
	public ExecHandle execThread(String commandline, String chdir) throws Exception;
	public ExecHandle execThread(String commandline, String chdir, byte[] stdin_data) throws Exception;
	public ExecHandle execThread(String commandline, Map<String,String> env, byte[] stdin_data) throws Exception;
	public ExecHandle execThread(String commandline, Map<String,String> env, String chdir) throws Exception;
	public ExecHandle execThread(String commandline, Map<String,String> env, String chdir, byte[] stdin_data) throws Exception;
	public ExecOutput execOut(String cmd, int timeout_sec, Map<String,String> object, byte[] stdin_post, Charset charset) throws IllegalStateException, Exception;
	
	
	public RunRequest createRunRequest();
	public RunRequest createRunRequest(ConsoleManager cm, String ctx_str);
	public RunRequest createRunRequest(ConsoleManager cm, Class<?> ctx_clazz);
	public ExecOutput execOut(RunRequest req);
	public ExecHandle execThread(RunRequest req);
	public boolean exec(RunRequest req);
	public static abstract class RunRequest {
		protected Map<String,String> env;
		protected String commandline, chdir;
		protected byte[] stdin_data;
		protected int timeout_sec;
		protected Charset cs;
		
		public void setEnv(Map<String,String> env) {
			this.env = env;
		}
		public void setCommandline(String commandline) {
			this.commandline = commandline;
		}
		public void setCurrentDir(String chdir) {
			this.chdir = chdir;
		}
		public void setStdinData(byte[] stdin_data) {
			this.stdin_data = stdin_data;
		}
		public void setcharset(Charset cs) {
			this.cs = cs;
		}
		public void setTimeoutSeconds(int timeout_sec) {
			this.timeout_sec = timeout_sec;
		}
		public Map<String,String> getEnv() {
			return env;
		}
		public String getCommandline() {
			return commandline;
		}
		public String getCurrentDir() {
			return chdir;
		}
		public byte[] getStdinData() {
			return stdin_data;
		}
		public Charset getcharset() {
			return cs;
		}
		public int getTimeoutSeconds() {
			return timeout_sec;
		}
	}
	
}
