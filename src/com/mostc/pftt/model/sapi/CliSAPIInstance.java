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
import com.mostc.pftt.util.DebuggerManager;

public class CliSAPIInstance extends SAPIInstance {
	protected final PhpBuild build;
	protected String ini_dir;
	protected DebuggerManager db_mgr;
	
	public CliSAPIInstance(ConsoleManager cm, AHost host, PhpBuild build, PhpIni ini) {
		super(host, ini);
		this.build = build;
		
		db_mgr = cm.getDebuggerManager();
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
	
	public ExecOutput execute(EExecutableType exe_type, String name, String php_filename, String extra_args, Map<String,String> env, int timeout_sec, boolean debugger_attached) throws Exception {
		return execute(exe_type, name, php_filename, extra_args, timeout_sec, env, null, debugger_attached);
	}
	
	public String createPhpCommand(EExecutableType exe_type, String php_filename, String extra_args, boolean debugger_attached) {
		StringBuilder sb = new StringBuilder();
		
		//
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

	public ExecOutput execute(EExecutableType exe_type, String name, String php_filename, String extra_args, int timeout_sec, Map<String,String> env, String chdir, boolean debugger_attached) throws Exception {
		return db_mgr == null ?
				host.execOut(createPhpCommand(exe_type, php_filename, extra_args, debugger_attached), timeout_sec, env, chdir, true) :
				db_mgr.execOut(host, name, createPhpCommand(exe_type, php_filename, extra_args, debugger_attached), timeout_sec, env, chdir, true);
	}
	
	public ExecHandle execThread(ConsoleManager cm, String name, ScenarioSet scenario_set, String cmd, String chdir, Map<String,String> env, byte[] stdin_data, boolean debugger_attached) throws Exception {
		// TODO use this with CliPhpUnitTestCaseRunner
		
		ExecHandle eh = db_mgr == null ?
				host.execThread(cmd, env, chdir, stdin_data, !debugger_attached) :
				db_mgr.execThread(host, name, cmd, env, chdir, stdin_data, !debugger_attached);
		if (debugger_attached && eh instanceof LocalExecHandle) {
			db_mgr.newDebugger(cm, host, scenario_set, name, build, (LocalExecHandle)eh);
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
