package com.mostc.pftt.model.sapi;

import java.util.Map;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.AHost.ExecHandle;
import com.mostc.pftt.host.ExecOutput;
import com.mostc.pftt.model.core.EExecutableType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ScenarioSet;

public class CliSAPIInstance extends SAPIInstance {
	protected final AHost host;
	protected final PhpBuild build;
	protected final PhpIni ini;
	protected String ini_dir;
	
	public CliSAPIInstance(AHost host, PhpBuild build, PhpIni ini) {
		this.host = host;
		this.build = build;
		this.ini = ini;
	}
	
	@Override
	public void prepareINI(ConsoleManager cm, AHost host, PhpBuild build, ScenarioSet scenario_set, PhpIni ini) {
		
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
		return host.execOut(createPhpCommand(exe_type, php_filename, extra_args), timeout_sec, env, chdir);
	}
	
	public ExecHandle execThread(String cmd, String chdir, Map<String,String> env, byte[] stdin_data) throws Exception {
		return host.execThread(cmd, env, chdir, stdin_data);
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
