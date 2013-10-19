package com.mostc.pftt.model.sapi;

import java.util.Map;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.AHost.ExecHandle;
import com.mostc.pftt.host.ExecOutput;
import com.mostc.pftt.host.LocalHost.LocalExecHandle;
import com.mostc.pftt.model.core.EExecutableType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.util.WinDebugManager;

public class CliSAPIInstance extends SAPIInstance {
	protected final PhpBuild build;
	protected String ini_dir;
	
	public CliSAPIInstance(AHost host, PhpBuild build, PhpIni ini) {
		super(host, ini);
		this.build = build;
	}
	
	@Override
	protected String doGetIniActual(String php_code) throws Exception {
		return build.eval(host, php_code)
			.printHasFatalError(getClass().getSimpleName(), (ConsoleManager)null)
			.cleanupSuccess(host)
			.output;
	}
	
	public void prepare() throws Exception {
		if (ini_dir!=null)
			return;
		
		ini_dir = host.mktempname(getClass());
		host.mkdirs(ini_dir);
		
		// now store the entire INI php should be using
		host.saveTextFile(host.joinIntoOnePath(ini_dir, "php.ini"), ini.toString());
	}
	
	public ExecOutput execute(EExecutableType exe_type, String php_filename, String extra_args, Map<String,String> env, int timeout_sec) throws Exception {
		return execute(exe_type, php_filename, extra_args, timeout_sec, env, null);
	}
	
	public String createPhpCommand(EExecutableType exe_type, String php_filename, String extra_args) {
		StringBuilder sb = new StringBuilder();
		sb.append(build.getPhpExe(exe_type));
		if (exe_type==EExecutableType.CGI) {
			// -C => important: don't chdir
			sb.append(" -C ");
		}
		sb.append(" -c ");
		sb.append(ini_dir);
		sb.append(" -f ");
		sb.append(host.fixPath(php_filename));
		if (extra_args!=null)
			sb.append(extra_args);
		return sb.toString();
	}

	public ExecOutput execute(EExecutableType exe_type, String php_filename, String extra_args, int timeout_sec, Map<String,String> env, String chdir) throws Exception {
		return host.execOut(createPhpCommand(exe_type, php_filename, extra_args), timeout_sec, env, chdir, true);
	}
	
	WinDebugManager db_mgr = new WinDebugManager();
	public ExecHandle execThread(ConsoleManager cm, String name, ScenarioSet scenario_set, String cmd, String chdir, Map<String,String> env, byte[] stdin_data, boolean debugger_attached) throws Exception {
		// TODO use this with CliPhpUnitTestCaseRunner
		
		LocalExecHandle eh = (LocalExecHandle) host.execThread(cmd, env, chdir, stdin_data, !debugger_attached);
		if (debugger_attached) {
			db_mgr.newDebugger(cm, host, scenario_set, name, build, eh);
		}
		return eh;
	}
	
	@Override
	public void close(ConsoleManager cm) {
		try {
			host.delete(ini_dir);
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
	}
	
	@Override
	public String getSAPIOutput() {
		return null;
	}

	@Override
	public boolean isRunning() {
		return true;
	}

	@Override
	public String getInstanceInfo(ConsoleManager cm) {
		return null;
	}

	@Override
	public boolean isCrashedOrDebuggedAndClosed() {
		return false;
	}

	@Override
	public String getSAPIConfig() {
		return null;
	}

	@Override
	public String getNameWithVersionInfo() {
		return getName();
	}

	@Override
	public String getName() {
		return "Cli";
	}
	
} // end public class CliSAPIInstance
