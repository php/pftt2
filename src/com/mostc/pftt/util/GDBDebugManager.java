package com.mostc.pftt.util;

import java.nio.charset.Charset;
import java.util.Map;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.AHost.ExecHandle;
import com.mostc.pftt.host.ExecOutput;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.runner.AbstractTestPackRunner.TestPackRunnerThread;
import com.mostc.pftt.scenario.ScenarioSet;

public class GDBDebugManager extends DebuggerManager {

	@Override
	public Debugger newDebugger(ConsoleManager cm, AHost host, ScenarioSet scenario_set, Object server_name, PhpBuild build, int process_id, ExecHandle process) {
		return new GDBDebugger();
	}
	
	public static class GDBDebugger extends Debugger {

		@Override
		public boolean exec(ConsoleManager cm, String ctx_str,
				String commandline, int timeout, Map<String, String> env,
				byte[] stdin, Charset charset, String chdir,
				TestPackRunnerThread thread, int thread_slow_sec)
				throws Exception {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public ExecOutput execOut(String cmd, int timeout_sec,
				Map<String, String> object, byte[] stdin_post, Charset charset)
				throws IllegalStateException, Exception {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public RunRequest createRunRequest(ConsoleManager cm, String ctx_str) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ExecOutput execOut(RunRequest req) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ExecHandle execThread(RunRequest req) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void close(ConsoleManager cm) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean isRunning() {
			// TODO Auto-generated method stub
			return false;
		}
		
	}

}
