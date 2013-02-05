package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;

/** Xdebug is a PHP extension which provides debugging and profiling
 * capabilities. It uses the DBGp debugging protocol.
 * 
 * The debug information that Xdebug can provide includes the following:
 * 
 * 		stack and function traces in error messages with:
 * 			full parameter display for user defined functions
 * 			function name, file name and line indications
 * 			support for member functions
 * 			memory allocation
 * 			protection for infinite recursions
 * 
 * Xdebug also provides:
 * 
 * 	profiling information for PHP scripts
 * 	code coverage analysis
 * 	capabilities to debug your scripts interactively with a debugger front-end.
 * 
 * 
 * @see http://www.xdebug.org/
 *
 */

public class XDebugScenario extends AbstractDebugScenario {

	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "XDebug";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}
	
}
