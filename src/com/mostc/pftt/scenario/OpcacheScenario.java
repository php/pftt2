package com.mostc.pftt.scenario;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.AHost.ExecHandle;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.EAcceleratorType;
import com.mostc.pftt.model.core.EExecutableType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.core.PhptActiveTestPack;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;

/** Opcache provides faster PHP execution through opcode caching and optimization.
 * It improves PHP performance by storing precompiled script bytecode in the shared memory. This
 * eliminates the stages of reading code from the disk and compiling it on future access. In
 * addition, it applies a few bytecode optimization patterns that make code execution faster.
 * 
 * 5.5+ PHP builds include OpCache. This Scenario installs OpCache on 5.3 and 5.4 builds. 
 * 
 * Formerly known as Optimizer+, Zend Optimizer+, often abbreviated as o+ or zo+ or Optimizer Plus
 * 
 * @see http://windows.php.net/downloads/pecl/releases/opcache/7.0.2/
 * @see https://github.com/zend-dev/ZendOptimizerPlus
 *
 */

public class OpcacheScenario extends AbstractCodeCacheScenario {
	private String version;
	

	@Override
	public String getNameWithVersionInfo() {
		// this will return the PHP Version the DLL was build for (ex: 5.4.10)
		// (Get-Item C:\php-sdk\php-5.4-ts-windows-vc9-x86-r064c62e\ext\php_opcache.dll).VersionInfo
		return version==null?"Opcache":"Opcache-"+version;
	}
	
	@Override
	public boolean isSupported(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		String ext_dir = build.getDefaultExtensionDir();
		boolean found = false;
		if (host.isWindows()) {
			found = host.exists(ext_dir + "/php_opcache.dll") ||
					host.exists(ext_dir + "/php_opcache.dont_load");
			
		} else {
			found = host.exists(ext_dir + "/php_opcache.so") ||
					host.exists(ext_dir + "/php_opcache.dont_load");
		}
		if (found) {
			if (cm!=null)
				cm.println(EPrintType.CLUE, getClass(), "Found OpCache in: "+ext_dir);
			return true;
		} else {
			if (host.isWindows()) {
				// 5.3 and 5.4 builds don't include opcache. try to install it.
				try {
					String dll_path = null;
					switch(build.getVersionBranch(cm, host)) {
					case PHP_5_3:
						if (build.isNTS(host))
							dll_path = host.getPfttDir()+"/cache/dep/opcache/php_opcache-7.0.2-5.3-nts-vc9-x86/php_opcache.dll";
						else
							dll_path = host.getPfttDir()+"/cache/dep/opcache/php_opcache-7.0.2-5.3-ts-vc9-x86/php_opcache.dll";
						break;
					case PHP_5_4:
						if (build.isNTS(host))
							dll_path = host.getPfttDir()+"/cache/dep/opcache/php_opcache-7.0.2-5.4-nts-vc9-x86/php_opcache.dll";
						else
							dll_path = host.getPfttDir()+"/cache/dep/opcache/php_opcache-7.0.2-5.4-ts-vc9-x86/php_opcache.dll";
						break;
					default:
						break;
					} // end switch
					if (dll_path!=null) {
						return host.exists(dll_path);
					}
				} catch ( Exception ex ) {
					if (cm!=null)
						cm.addGlobalException(EPrintType.SKIP_OPERATION, getClass(), "setup", ex, "failed to install opcache");
					else
						ex.printStackTrace();
				} 
			} // end if
			if (cm!=null)
				cm.println(EPrintType.CLUE, getClass(), "Unable to find OpCache in: "+ext_dir);
			return false;
		} // end if
	} // end public boolean isSupported

	@Override
	public EAcceleratorType getAcceleratorType() {
		return EAcceleratorType.OPCACHE;
	}
	
	protected void cleanupBaseAddressFile(AHost host, PhpBuild build, PhptActiveTestPack test_pack) {
		// IMPORTANT: delete the `base address` file that
		// Opcache left behind from previous test run
		//
		// in temp directory. name is like: ZendOptimizer+.MemoryBase@matt
		// @see shared_alloc_win32.c (https://github.com/zend-dev/opcache/blob/master/shared_alloc_win32.c)
		//
		// for regular users, TEMP_DIR is often
		// for Apache (as service) TEMP_DIR is often C:\Users\NT_Authority? (different than IIS service)
		// for IIS (service) TEMP_DIR is often C:\Windows\Temp
		host.deleteIfExistsElevated(host.getTempDir()+"\\ZendOptimizer+.MemoryBase@"+host.getUsername());
		if (test_pack!=null) {
			host.deleteIfExistsElevated(test_pack.getRunningDirectory()+"\\ZendOptimizer+.MemoryBase@"+host.getUsername());	
			host.deleteIfExistsElevated(test_pack.getStorageDirectory()+"\\ZendOptimizer+.MemoryBase@"+host.getUsername());
		}
		host.deleteIfExistsElevated(build.getBuildPath()+"\\ZendOptimizer+.MemoryBase@"+host.getUsername());	
		host.deleteIfExistsElevated(host.getPhpSdkDir()+"\\ZendOptimizer+.MemoryBase@"+host.getUsername());
		host.deleteIfExistsElevated(host.getPfttDir()+"\\ZendOptimizer+.MemoryBase@"+host.getUsername());
	}
	
	@Override
	public boolean prepare(ConsoleManager cm, AHost host, PhpBuild build, ScenarioSet scenario_set, PhptActiveTestPack test_pack) {
		if (host.isWindows()) {
			cleanupBaseAddressFile(host, build, test_pack);
		}
		return true;
	}
	
	private ExecHandle startup_handle;
	private boolean first = true;
	private String temp_dir;
	public boolean stop(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, PhpIni _ini) {
		if (startup_handle!=null) {
			startup_handle.close(cm, true);
			
			host.deleteIfExistsElevated(temp_dir);
			
			startup_handle = null;
			first = true;
		}
		return true;
	}

	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini) {
		if (host.isWindows()) {
			// TODO shouldn't be casting to AHost
			cleanupBaseAddressFile((AHost)host, build, null);
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
				dll_path = host.fixPath(ext_dir + "/php_opcache.dll");
				
				if (host.exists(dll_path.replace(".dll", ".dont_load")))
					// make sure PHP doesn't find it and load it automatically
					host.moveElevated(dll_path.replace(".dll", ".dont_load"), dll_path);
				
				if (host.exists(dll_path)) {
					// may have already setup scenario
					if (build.is54(cm, host)||build.is53(cm, host))
						// 5.4 and 5.3 don't include opcache, so provide the version of opcache being used
						version = "7.0.2";
				} else {
					// try to install it for 5.3 and 5.4 builds
					try {
						String src_dll_path = null;
						switch(build.getVersionBranch(cm, host)) {
						case PHP_5_3:
							// @see #isSuccessful (it checks if these dlls exist!)
							if (build.isNTS(host))
								src_dll_path = host.getPfttDir()+"/cache/dep/opcache/php_opcache-7.0.2-5.3-nts-vc9-x86/php_opcache.dll";
							else
								src_dll_path = host.getPfttDir()+"/cache/dep/opcache/php_opcache-7.0.2-5.3-ts-vc9-x86/php_opcache.dll";
							break;
						case PHP_5_4:
							if (build.isNTS(host))
								src_dll_path = host.getPfttDir()+"/cache/dep/opcache/php_opcache-7.0.2-dev-5.4-nts-vc9-x86/php_opcache.dll";
							else
								src_dll_path = host.getPfttDir()+"/cache/dep/opcache/php_opcache-7.0.2-dev-5.4-ts-vc9-x86/php_opcache.dll";
							break;
						default:
							break;
						} // end switch
						if (src_dll_path!=null) {
							host.copy(src_dll_path, dll_path);
							
							// install succeeded
							version = "7.0.2"; // XXX detect version
						}
					} catch ( Exception ex ) {
						if (cm!=null)
							cm.addGlobalException(EPrintType.SKIP_OPERATION, getClass(), "setup", ex, "failed to install opcache");
						else
							ex.printStackTrace();
					} 
				}
			} else {
				dll_path = host.fixPath(ext_dir + "/php_opcache.so");
				
				if (host.exists(dll_path.replace(".so", ".dont_load")))
					host.moveElevated(dll_path.replace(".so", ".dont_load"), dll_path);
			}
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CLUE, "setup", ex, "couldn't make sure OpCache was enabled");
			
			return false;
		}
		//
		
		if (!host.exists(dll_path)) {
			version = null;
			
			return false; // install failed
		}
		
		// must be absolute path to opcache.so
		ini.putMulti("zend_extension", dll_path);
		
		ini.putSingle("opcache.enable", 1);
		// CRITICAL: for CliScenario
		ini.putSingle("opcache.enable_cli", 1);
		
		// recommended settings, @see https://github.com/zend-dev/opcache
		// (recommended settings differ from some of the documented default settings)
		ini.putSingle("opcache.memory_consumption", 128);
		ini.putSingle("opcache.interned_strings_buffer", 8);
		ini.putSingle("opcache.max_accelerated_files", 4000);
		ini.putSingle("opcache.force_restart_timeout", 180);
		ini.putSingle("opcache.revalidate_freq", 60);
		ini.putSingle("opcache.save_comments", 0);
		ini.putSingle("opcache.fast_shutdown", 1);
		ini.putSingle("opcache.enable_file_override", 1);
		
		// by default all passes are run, turn off some
		/*ini.putSingle("opcache.optimization_level",
				ZEND_OPTIMIZER_PASS_3
				|ZEND_OPTIMIZER_PASS_10
				// passes other than 3 & 10 (especially 5 & 9) can break reflection (for Doctrine/Symfony)
				//|ZEND_OPTIMIZER_PASS_4
				//|ZEND_OPTIMIZER_PASS_6
				//|ZEND_OPTIMIZER_PASS_7
				//|ZEND_OPTIMIZER_PASS_8
				//|ZEND_OPTIMIZER_PASS_1 
				//|ZEND_OPTIMIZER_PASS_2
				//|ZEND_OPTIMIZER_PASS_5
				//|ZEND_OPTIMIZER_PASS_9
			);*/
		
		//
		if (host.isWindows() && first) {
			first = false;
			
			// need to start a process to startup Opcache and leave it running
			// to ensure that the SharedMemoryArea is never closed
			//
			// if there is a lot of process-churn with Opcache on Windows, this may happen
			// (handles to the SharedMemoryArea are closed whenever a process exits.
			//  if processes exit in the right order, all the handles will be closed.
			//  the SharedMemoryArea will be closed if all handles to it are closed.
			//  this causes the 'Fatal Error: Unable to reattach to base address' msg)
			try {
				temp_dir = host.mktempname("Opcache_Startup_Process");
				host.mkdirs(temp_dir);
				
				String php_script = temp_dir+"/startup.php";
				
				host.saveTextFile(temp_dir+"/php.ini", ini.toString());
				
				// start thread to startup opcache
				host.saveTextFile(php_script, "<?php while(true){sleep(60000);} ?>");
				
				startup_handle = ((AHost)host).execThread(build.getPhpExe(EExecutableType.CLI)+" -c "+temp_dir+" -f "+php_script);
				
			} catch ( Exception ex ) {
				ex.printStackTrace();
			}
		}
		//
		
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
		return "OpCache";
	}

	@Override
	public boolean isImplemented() {
		return true;
	}
	
} // end public class OpcacheScenario
