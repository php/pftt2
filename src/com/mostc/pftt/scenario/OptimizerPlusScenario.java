package com.mostc.pftt.scenario;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.EAcceleratorType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ConsoleManager.EPrintType;

import java.util.Date;

/** The Optimizer+ provides faster PHP execution through opcode caching and optimization.
 * It improves PHP performance by storing precompiled script bytecode in the shared memory. This
 * eliminates the stages of reading code from the disk and compiling it on future access. In
 * addition, it applies a few bytecode optimization patterns that make code execution faster.
 * 
 * Formerly known as Zend Optimizer+, often abbreviated as o+ or zo+ or Optimizer Plus
 * 
 * @see http://windows.php.net/downloads/pecl/snaps/Optimizer/7.0.0-dev/
 * @see https://github.com/zend-dev/ZendOptimizerPlus
 * @see https://github.com/OSTC/ZendOptimizerPlus - fork for Windows/PHP on Windows
 *
 */

public class OptimizerPlusScenario extends AbstractCodeCacheScenario {
	private String version;

	@Override
	public String getNameWithVersionInfo() {
		// this will return the PHP Version the DLL was build for (ex: 5.4.10)
		// (Get-Item C:\php-sdk\php-5.4-ts-windows-vc9-x86-r064c62e\ext\php_ZendOptimizerPlus.dll).VersionInfo
		
		return "OptimizerPlus-" + (version==null?"":version);
	}

	@Override
	public EAcceleratorType getAcceleratorType() {
		return EAcceleratorType.OPTIMIZER_PLUS;
	}

	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini) {
		if (host.isWindows()) {
			// IMPORTANT: delete any (memory mapped files|file mapping objects|mapped files) that
			// Optimizer+ left behind from previous test run
			//
			// in temp directory. name is like: ZendOptimizer+.MemoryBase@matt
			// @see shared_alloc_win32.c (https://github.com/zend-dev/ZendOptimizerPlus/blob/master/shared_alloc_win32.c)
			host.deleteIfExists(host.getTempDir()+"\\ZendOptimizer+.MemoryBase@"+host.getUsername());
		}
		
		
		// assume SO is in same directory as PHP extensions
		String dll_path;
		try {
			// seems that PHP will load O+ if dll is there even though its not in INI
			//
			// NoCodeCacheScenario may have renamed file to *.dont_load, UNDO that here
			String ext_dir = ini.getExtensionDir();
			if (StringUtil.isEmpty(ext_dir))
				ext_dir = build.getDefaultExtensionDir();
			if (host.isWindows()) {
				dll_path = host.fixPath(ext_dir + "/php_ZendOptimizerPlus.dll");
				
				if (host.exists(dll_path.replace(".dll", ".dont_load")))
					host.move(dll_path.replace(".dll", ".dont_load"), dll_path);
			} else {
				dll_path = host.fixPath(ext_dir + "/php_ZendOptimizerPlus.so");
				
				if (host.exists(dll_path.replace(".so", ".dont_load")))
					host.move(dll_path.replace(".so", ".dont_load"), dll_path);
			}
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CLUE, "setup", ex, "couldn't make sure OptimizerPlus was enabled");
			
			return false;
		}
		//
		
		if (!host.exists(dll_path)) {
			version = host.isWindows() ? "Dll-Missing" : "SO-Missing";
			
			return true;
		}
		
		//
		{
			Date date = new Date(host.getMTime(dll_path));
			
			version = ((date.getYear()+1900) +
				"-" +
				(date.getMonth()+1) +
				"-" +
				date.getDate() +
				"-" +
				date.getHours() +
				"h" +
				date.getMinutes()) +
				"m";
		}
		//
		
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
	} // end public boolean setup

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
