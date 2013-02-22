package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.EAcceleratorType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;

/** The Optimizer+ provides faster PHP execution through opcode caching and optimization.
 * It improves PHP performance by storing precompiled script bytecode in the shared memory. This
 * eliminates the stages of reading code from the disk and compiling it on future access. In
 * addition, it applies a few bytecode optimization patterns that make code execution faster.
 * 
 * Formerly known as Zend Optimizer+, often abbreviated as o+ or zo+
 * 
 * @see https://github.com/zend-dev/ZendOptimizerPlus
 * @see https://github.com/OSTC/ZendOptimizerPlus - fork for Windows/PHP on Windows
 *
 */

public class OptimizerPlusScenario extends AbstractCodeCacheScenario {

	@Override
	public String getNameWithVersionInfo() {
		return "OptimizerPlus"; // XXX version
	}

	@Override
	public EAcceleratorType getAcceleratorType() {
		return EAcceleratorType.OPTIMIZER_PLUS;
	}

	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini) {
		// assume SO is in same directory as PHP extensions
		String dll_path = ini.getExtensionDir() + "/php_ZendOptimizerPlus." + (host.isWindows() ? "dll" : "so" );
		
		// must be absolute path to ZendOptimizerPlus.so
		ini.putMulti("zend_extension", dll_path);
		
		// CRITICAL: for CliScenario
		ini.putSingle("zend_optimizerplus.enable_cli", 1);

		// recommended settings, @see https://github.com/zend-dev/ZendOptimizerPlus
		// (recommended settings differ from some of the documented default settings)
		ini.putSingle("zend_optimizerplus.memory_consumption", 128);
		ini.putSingle("zend_optimizerplus.interned_strings_buffer", 8);
		ini.putSingle("zend_optimizerplus.max_accelerated_files", 4000);
		ini.putSingle("zend_optimizerplus.revalidate_freq", 60);
		ini.putSingle("zend_optimizerplus.save_comments", 0);
		ini.putSingle("zend_optimizerplus.fast_shutdown", 1);
		ini.putSingle("zend_optimizerplus.enable_file_override", 1);
		
		return true;
	}

	@Override
	public String getName() {
		// use 'plus' instead of + symbol which may cause problems (ex: on certain filesystems)
		return "OptimizerPlus";
	}

	@Override
	public boolean isImplemented() {
		return true;
	}
	
} // end public class ZendOptimizerPlusScenario
