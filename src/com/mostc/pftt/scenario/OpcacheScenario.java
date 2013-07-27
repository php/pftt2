package com.mostc.pftt.scenario;

import java.util.Map;

import com.github.mattficken.Overridable;
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
import com.mostc.pftt.util.DllVersion;

/** Opcache provides faster PHP execution through opcode caching and optimization.
 * It improves PHP performance by storing precompiled script bytecode in the shared memory. This
 * eliminates the stages of reading code from the disk and compiling it on future access. In
 * addition, it applies a few bytecode optimization patterns that make code execution faster.
 * 
 * 5.5+ PHP builds include Opcache. This Scenario installs Opcache on 5.3 and 5.4 builds. 
 * 
 * Formerly known as Optimizer+, Zend Optimizer+, often abbreviated as o+ or zo+ or Optimizer Plus.
 * 
 * Opcache provides only code caching, so it can be used alongside APCU or WincacheU.
 * 
 * @see http://windows.php.net/downloads/pecl/releases/opcache/7.0.2/
 * @see https://github.com/zend-dev/ZendOptimizerPlus
 *
 */

public class OpcacheScenario extends CodeCacheScenario {
	
	@Overridable 
	public void prepareINI(PhpIni ini, String dll_path) {
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
	}
	
	@Overridable
	protected DllVersion getSoPath55Plus(ConsoleManager cm, Host host, PhpBuild build, boolean rename) throws IllegalStateException, Exception {
		String ext_dir = build.getDefaultExtensionDir();
		
		// @see NoCodeCacheScenario for .dont_load
		if (rename && host.exists(ext_dir + "/php_opcache.dont_load"))
			host.move(ext_dir + "/php_opcache.dont_load", ext_dir + "/php_opcache.so");
		if (host.exists(ext_dir + "/php_opcache.so"))
			return new DllVersion(ext_dir + "/php_opcache.so", build.getVersionRevision(cm, host));
		else
			return null;
	}
	
	@Overridable
	protected DllVersion getDllPath55Plus(ConsoleManager cm, Host host, PhpBuild build, boolean rename) throws IllegalStateException, Exception {
		String ext_dir = build.getDefaultExtensionDir();
		
		// @see NoCodeCacheScenario for .dont_load
		if (rename && host.exists(ext_dir + "/php_opcache.dont_load"))
			host.move(ext_dir + "/php_opcache.dont_load", ext_dir + "/php_opcache.dll");
		if (host.exists(ext_dir + "/php_opcache.dll"))
			return new DllVersion(ext_dir + "/php_opcache.dll", build.getVersionRevision(cm, host));
		else
			return null;
	}
	
	@Overridable
	protected DllVersion getDllPath53TS(Host host) {
		return new DllVersion(host.getPfttDir()+"/cache/dep/opcache/php_opcache-7.0.2-5.3-ts-vc9-x86/php_opcache.dll", "7.0.2");
	}
	
	@Overridable
	protected DllVersion getDllPath53NTS(Host host) {
		return new DllVersion(host.getPfttDir()+"/cache/dep/opcache/php_opcache-7.0.2-5.3-nts-vc9-x86/php_opcache.dll", "7.0.2");
	}
	
	@Overridable
	protected DllVersion getDllPath54TS(Host host) {
		return new DllVersion(host.getPfttDir()+"/cache/dep/opcache/php_opcache-7.0.2-5.4-ts-vc9-x86/php_opcache.dll", "7.0.2");
	}
	
	@Overridable
	protected DllVersion getDllPath54NTS(Host host) {
		return new DllVersion(host.getPfttDir()+"/cache/dep/opcache/php_opcache-7.0.2-5.4-nts-vc9-x86/php_opcache.dll", "7.0.2");
	}
	
	public DllVersion getDllPath(ConsoleManager cm, Host host, PhpBuild build) {
		return getDllPath(cm, host, build, false);
	}
	
	protected DllVersion getDllPath(ConsoleManager cm, Host host, PhpBuild build, boolean rename) {
		DllVersion version = null;
		try {
			switch(build.getVersionBranch(cm, host)) {
			case PHP_5_3:
				if (host.isWindows()) {
					if (build.isNTS(host))
						version = getDllPath53NTS(host);
					else
						version = getDllPath53TS(host);
				}
				break;
			case PHP_5_4:
				if (host.isWindows()) {
					if (build.isNTS(host))
						version = getDllPath54NTS(host);
					else
						version = getDllPath54TS(host);
				}
				break;
			default:
				if (host.isWindows())
					version = getDllPath55Plus(cm, host, build, rename);
				else
					version = getSoPath55Plus(cm, host, build, rename);
			} // end switch
		} catch ( Exception ex ) {
			if (cm==null)
				ex.printStackTrace();
			else
				cm.addGlobalException(EPrintType.CANT_CONTINUE, getClass(), "getDllPath", ex, "Unable to find OpCache");
		}
		
		if (version!=null) {
			if (host.exists(version.getPath())) {
				if (cm!=null)
					cm.println(EPrintType.CLUE, getClass(), "Found Opcache in: "+version.getPath());		
			} else {
				if (cm!=null)
					cm.println(EPrintType.WARNING, getClass(), "Opcache expected, but not found at: "+version.getPath());
				version = null;
			}
		}
		if (version==null) {
			if (cm!=null)
				cm.println(EPrintType.WARNING, getClass(), "Unable to find Opcache for: "+build.getBuildPath());	
		}
		return version;
	} // end protected DllVersion getDllPath
	
	@Override
	public boolean isSupported(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		return getDllPath(cm, host, build) != null;
	}

	public class OpcacheSetup extends SimpleScenarioSetup {
		protected final DllVersion dll;
		protected final Host host;
		protected final ConsoleManager cm;
		protected final PhpBuild build;
		protected String version;
		
		public OpcacheSetup(DllVersion dll, Host host, ConsoleManager cm, PhpBuild build) throws Exception {
			this.dll = dll;
			this.host = host;
			this.cm = cm;
			this.build = build;
			
			this.version = dll.getVersion()==null||dll.getVersion().equals(build.getVersionRevision(cm, host))?"Opcache":"Opcache-"+dll.getVersion();
		}
		
		@Override
		public String getNameWithVersionInfo() {
			// this will return the PHP Version the DLL was build for (ex: 5.4.10)
			// (Get-Item C:\php-sdk\php-5.4-ts-windows-vc9-x86-r064c62e\ext\php_opcache.dll).VersionInfo
			return version;
		}	
		@Override
		public String getName() {
			return OpcacheScenario.this.getName();
		}
		private ExecHandle startup_handle;
		private String temp_dir;
		
		@Override
		public void close(ConsoleManager cm) {
			if (startup_handle!=null) {
				startup_handle.close(cm, true);
				
				host.deleteIfExistsElevated(temp_dir);
				
				startup_handle = null;
			}
		}
		
		@Override
		public void prepareINI(ConsoleManager cm, AHost host, PhpBuild build, ScenarioSet scenario_set, PhpIni ini) {
			OpcacheScenario.this.prepareINI(ini, dll.getPath());
		}

		@Override
		public void getENV(Map<String, String> env) {
		}

		@Override
		public void setGlobals(Map<String, String> globals) {
		}

		@Override
		public boolean hasENV() {
			return false;
		}
		
	} // end public class OpcacheSetup
	
	@Overridable
	protected OpcacheSetup createOpcacheSetup(DllVersion dll, Host host, ConsoleManager cm, PhpBuild build) throws Exception {
		return new OpcacheSetup(dll, host, cm, build);
	}

	@Override
	public OpcacheSetup setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		if (host.isWindows()) {
			// TODO shouldn't be casting to AHost
			cleanupBaseAddressFile((AHost)host, build, null);
		}
		
		// find dll and rename from .dont_load to .so or .dll if needed (=> true)
		final DllVersion dll = getDllPath(cm, host, build, true);
		if (dll==null)
			return null;
		
		OpcacheSetup setup = null;
		try {
			setup = createOpcacheSetup(dll, host, cm, build);
		} catch ( Exception ex ) {
			if (cm==null)
				ex.printStackTrace();
			else
				cm.addGlobalException(EPrintType.CANT_CONTINUE, getClass(), "setup", ex, "Can't setup Opcache");
		}
		
		//
		if (setup != null && shouldUseStartupProcess(host)) {
			// need to start a process to startup Opcache and leave it running
			// to ensure that the SharedMemoryArea is never closed
			//
			// if there is a lot of process-churn with Opcache on Windows, this may happen
			// (handles to the SharedMemoryArea are closed whenever a process exits.
			//  if processes exit in the right order, all the handles will be closed.
			//  the SharedMemoryArea will be closed if all handles to it are closed.
			//  this causes the 'Fatal Error: Unable to reattach to base address' msg)
			try {
				setup.temp_dir = host.mktempname("Opcache_Startup_Process");
				host.mkdirs(setup.temp_dir);
				
				String php_script = setup.temp_dir+"/startup.php";
				
				PhpIni ini = new PhpIni();
				setup.prepareINI(cm, (AHost)host, build, scenario_set, ini);
				host.saveTextFile(setup.temp_dir+"/php.ini", ini.toString());
				
				// start thread to startup opcache
				host.saveTextFile(php_script, "<?php while(true){sleep(60000);} ?>");
				
				setup.startup_handle = ((AHost)host).execThread(build.getPhpExe(EExecutableType.CLI)+" -c "+setup.temp_dir+" -f "+php_script);
			} catch ( Exception ex ) {
				ex.printStackTrace();
			}
		}
		//
		
		return setup;
	} // end public OpcacheSetup setup
	
	protected boolean shouldUseStartupProcess(Host host) {
		return host.isWindows();
	}

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
		host.deleteIfExistsElevated(host.getSystemRoot()+"\\ZendOptimizer+.MemoryBase@"+host.getUsername());
		host.deleteIfExistsElevated(host.getSystemDrive()+"\\ZendOptimizer+.MemoryBase@"+host.getUsername());
	}
	
	@Override
	public EAcceleratorType getAcceleratorType() {
		return EAcceleratorType.OPCACHE;
	}
	
	@Override
	public String getName() {
		return "Opcache";
	}

	@Override
	public boolean isImplemented() {
		return true;
	}

	/** configures PhpBuild to use Opcache, but does not create the Opcache Startup Process
	 * for that, see the other #setup method
	 * 
	 * @param cm
	 * @param host
	 * @param build
	 * @param ini
	 */
	@Override
	public IScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini) {
		final DllVersion dll = this.getDllPath(cm, host, build);
		if (dll==null) {
			return SETUP_FAILED;
		} else {
			prepareINI(ini, dll.getPath());
			return SETUP_SUCCESS;
		}
	}
	
} // end public class OpcacheScenario
