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
		
		return "OptimizerPlus-" + (version==null?"Missing":version);
	}
	
	@Override
	public boolean isSupported(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		String ext_dir = build.getDefaultExtensionDir();
		boolean found = false;
		if (host.isWindows()) {
			found = host.exists(ext_dir + "/php_ZendOptimizerPlus.dll") ||
					host.exists(ext_dir + "/php_ZendOptimizerPlus.dont_load");
			
		} else {
			found = host.exists(ext_dir + "/php_ZendOptimizerPlus.so") ||
					host.exists(ext_dir + "/php_ZendOptimizerPlus.dont_load");
		}
		if (found) {
			if (cm!=null)
				cm.println(EPrintType.CLUE, getClass(), "Found OptimizerPlus in: "+ext_dir);
			return true;
		} else {
			if (cm!=null)
				cm.println(EPrintType.CLUE, getClass(), "Unable to find OptimizerPlus in: "+ext_dir);
			return false;
		}
	} // end public boolean isSupported

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
					host.moveElevated(dll_path.replace(".dll", ".dont_load"), dll_path);
			} else {
				dll_path = host.fixPath(ext_dir + "/php_ZendOptimizerPlus.so");
				
				if (host.exists(dll_path.replace(".so", ".dont_load")))
					host.moveElevated(dll_path.replace(".so", ".dont_load"), dll_path);
			}
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CLUE, "setup", ex, "couldn't make sure OptimizerPlus was enabled");
			
			return false;
		}
		//
		
		if (!host.exists(dll_path)) {
			version = null;
			
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
		
		
		// TODO 
		// by default all passes are run, turn off some
		/*ini.putSingle("zend_optimizerplus.optimization_level",
				ZEND_OPTIMIZER_PASS_3
				|ZEND_OPTIMIZER_PASS_10
				//|ZEND_OPTIMIZER_PASS_4
				//|ZEND_OPTIMIZER_PASS_6
				//|ZEND_OPTIMIZER_PASS_7
				//|ZEND_OPTIMIZER_PASS_8
				//|ZEND_OPTIMIZER_PASS_1 
				//|ZEND_OPTIMIZER_PASS_2
				
				// pass5 and pass9 seem to break reflection
				//|ZEND_OPTIMIZER_PASS_5
				//|ZEND_OPTIMIZER_PASS_9
			);*/
		
		return true;
	} // end public boolean setup
	
	// @see Optimizer/zend_optimizer.h
	public static final int ZEND_OPTIMIZER_PASS_1 = (1<<0); /* CSE, STRING construction */
	public static final int ZEND_OPTIMIZER_PASS_2 = (1<<1); /* Constant conversion and jums */
	public static final int ZEND_OPTIMIZER_PASS_3 = (1<<2); /* ++, +=, series of jumps */
	public static final int ZEND_OPTIMIZER_PASS_4 = (1<<3);
	public static final int ZEND_OPTIMIZER_PASS_5 = (1<<4); /* CFG based optimization */
	public static final int ZEND_OPTIMIZER_PASS_6 = (1<<5);
	public static final int ZEND_OPTIMIZER_PASS_7 = (1<<6);
	public static final int ZEND_OPTIMIZER_PASS_8 = (1<<7);
	public static final int ZEND_OPTIMIZER_PASS_9 = (1<<8); /* TMP VAR usage */
	public static final int ZEND_OPTIMIZER_PASS_10 = (1<<9); /* NOP removal */
	public static final int ZEND_OPTIMIZER_PASS_11 = (1<<10);
	public static final int ZEND_OPTIMIZER_PASS_12 = (1<<11);
	public static final int ZEND_OPTIMIZER_PASS_13 = (1<<12);
	public static final int ZEND_OPTIMIZER_PASS_14 = (1<<13);

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
