package com.mostc.pftt.host;

import java.nio.charset.Charset;
import java.util.Map;

import com.mostc.pftt.host.AHost.ExecHandle;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.runner.AbstractTestPackRunner.TestPackRunnerThread;

public interface IProgramRunner {
	/*
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
	*/
	/*
	public RunRequest createRunRequest();
	public ExecOutput execOut(RunRequest req);
	public ExecHandle execThread(RunRequest req);
	public boolean exec(RunRequest req);
	public static abstract class RunRequest {
		void setEnv(Map<String,String> env);
	}
	*/
}
