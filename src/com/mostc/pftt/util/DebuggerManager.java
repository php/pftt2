package com.mostc.pftt.util;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.host.LocalHost.LocalExecHandle;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.results.ConsoleManager;

/** handles using a debugger on a build that has a test case run against it
 * 
 * @author Matt Ficken
 *
 */

public abstract class DebuggerManager {
	protected String src_path, debug_path;
	
	/** provides a new debugger for the running instance of build/process represented by handle, or 
	 * returns null if debugger could not be created
	 * 
	 * @param cm
	 * @param host
	 * @param server_name
	 * @param build
	 * @param handle
	 * @return
	 */
	public Debugger newDebugger(ConsoleManager cm, Host host, Object server_name, PhpBuild build, LocalExecHandle handle) {
		int pid = handle.getProcessID();
		if (pid<1)
			return null;
		
		return newDebugger(cm, host, server_name, build, pid);
	}
	
	/** provides a new debugger for the running process (identified by process_id) or returns
	 * null if debugger could not be created
	 * 
	 * @param cm
	 * @param host
	 * @param server_name
	 * @param build
	 * @param process_id
	 * @return
	 */
	public abstract Debugger newDebugger(ConsoleManager cm, Host host, Object server_name, PhpBuild build, int process_id);
	
	/** guesses the source pack and debug pack locations based on build, unless
	 * -src_pack and -debug_pack console options are given 
	 * 
	 * @param cm
	 * @param host
	 * @param build
	 */
	private PhpBuild found_src_debug_pack_build;
	protected void ensureFindSourceAndDebugPack(ConsoleManager cm, Host host, PhpBuild build) {
		if (build==found_src_debug_pack_build)
			return;
		this.found_src_debug_pack_build = build; // cache
		if (!host.isWindows()) {
			// only PHP on Windows has standard conventions for naming/locating source and debug packs
			this.src_path = cm.getSourcePack();
			this.debug_path = cm.getDebugPack().getPath();
			return;
		}
		
		// use any source and debug packs given on command line
		String def_source_path = cm.getSourcePack();
		String def_debug_path = cm.getDebugPack().getPath();
		
		// (in addition to )guessing the source pack and debug pack from the build (PHP-on-Windows follows conventions that allow this)
		try {
			if (StringUtil.isEmpty(def_source_path))
				this.src_path = build.guessSourcePackPath(cm, host);
			else
				this.src_path = host.joinMultiplePaths(def_source_path, build.guessSourcePackPath(cm, host));
		} catch ( Exception ex ) {
			cm.addGlobalException(getClass(), "ensureFindSourceAndDebugPack", ex, "");
		}
		try {
			if (StringUtil.isEmpty(def_debug_path))
				this.debug_path = build.guessDebugPackPath(cm, host);
			else
				this.debug_path = host.joinMultiplePaths(def_debug_path, build.guessDebugPackPath(cm, host));
		} catch ( Exception ex ) {
			cm.addGlobalException(getClass(), "ensureFindSourceAndDebugPack", ex, "");
		}
	} // end protected void ensureFindSourceAndDebugPack
	
	/** turns generic objects into server name to give to debugger.
	 * 
	 * this focuses primarily on PhptTestCases.
	 * 
	 * @param input
	 * @return
	 */
	public static String toServerName(Object input) {
		String str = null;
		if (input instanceof Object[]) {
			StringBuilder sb = new StringBuilder();
			for ( Object a : ((Object[])input)) {
				_toServerName(sb, a);
				if (sb.length() > 100)
					break; // limit length
			}
			str = sb.toString();
		} else if (input instanceof Iterable<?>) { // Collection List LinkedBlockingQueue
			StringBuilder sb = new StringBuilder();
			for ( Object a : ((Iterable<?>)input)) {
				_toServerName(sb, a);
				if (sb.length() > 100)
					break; // limit length
			}
			str = sb.toString();
		} else if (input instanceof PhptTestCase) {
			// single test case - use whole test name (w/o .phpt)
			str = ((PhptTestCase)input).getBaseName();
		} else if (input!=null) {
			str = input.toString();
		}
		if (StringUtil.isEmpty(str))
			return "PFTT";
		else
			return str;
	}
	
	private static void _toServerName(StringBuilder sb, Object a) {
		if (a instanceof PhptTestCase) {
			PhptTestCase t = (PhptTestCase) a;
			if (sb.length() > 0)
				sb.append(' ');
			if (sb.indexOf(t.getFolder())!=-1)
				// with lots of tests running, name needs to be kept short - use short name instead of full name
				sb.append(t.getShortName());
			else
				sb.append(t.getBaseName());
		} else {
			sb.append(a.toString());
		}
	}

	public static abstract class Debugger {

		public abstract void close();
		
	}
	
} // end public abstract class DebuggerManager
