package com.mostc.pftt.scenario;

import java.util.Collection;
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
import com.mostc.pftt.results.ConsoleManagerUtil;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.util.DllVersion;

/** Opcache provides faster PHP execution through opcode caching and optimization.
 * It improves PHP performance by storing precompiled script bytecode in the shared memory. This
 * eliminates the stages of reading code from the disk and compiling it on future access. In
 * addition, it applies a few bytecode optimization patterns that make code execution faster.
 * 
 * 5.5+ PHP builds include Opcache.
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
	protected final DllVersion set_dll;
	
	public OpcacheScenario() {
		this.set_dll = null;
	}
	
	public OpcacheScenario(DllVersion dll) {
		this.set_dll = dll;
	}
	
	@Override
	public void addToDebugPath(ConsoleManager cm, AHost host, PhpBuild build, Collection<String> debug_path) {
		if (this.set_dll!=null) {
			debug_path.add(set_dll.getDebugPath());
		}
	}
	
	@Overridable 
	public boolean prepareINI(ConsoleManager cm, FileSystemScenario fs, AHost host, PhpIni ini, PhpBuild build, String dll_path) {
		// must be absolute path to opcache.so
		ini.putMulti("zend_extension", dll_path);
		try {
			if (!build.isExtensionEnabled(cm, fs, host, null, ini, FileSystemScenario.basename(dll_path))) {
				if (cm!=null)
					cm.println(EPrintType.CLUE, getClass(), "Opcache DLL not loadable: "+dll_path);
				return false;
			}
		} catch ( Exception ex ) {
			if (cm!=null)
				cm.println(EPrintType.CLUE, getClass(), "Could not tell if Opcache DLL was loadable: "+dll_path);
			return false;
		}
		
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
		
		// 0-5 0=> silent (default) 4=debug 5=>extra debug
		// @see http://php.net/manual/en/opcache.configuration.php
		ini.putSingle("opcache.log_verbosity_level", 0);
		
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
		return true;
	}
	
	@Overridable
	protected DllVersion getSoPath55Plus(ConsoleManager cm, FileSystemScenario fs, Host host, PhpBuild build, boolean rename) throws IllegalStateException, Exception {
		String ext_dir = build.getDefaultExtensionDir();
		
		// @see NoCodeCacheScenario for .dont_load
		if (rename && host.mExists(ext_dir + "/php_opcache.dont_load"))
			fs.move(ext_dir + "/php_opcache.dont_load", ext_dir + "/php_opcache.so");
		if (host.mExists(ext_dir + "/php_opcache.so"))
			return new DllVersion(ext_dir, "php_opcache.so", "php_opcache.pdb", build.getVersionRevision(cm, host));
		else
			return null;
	}
	
	@Overridable
	protected DllVersion getDllPath55Plus(ConsoleManager cm, FileSystemScenario fs, Host host, PhpBuild build, boolean rename) throws IllegalStateException, Exception {
		String ext_dir = build.getDefaultExtensionDir();
		
		// @see NoCodeCacheScenario for .dont_load
		if (rename && host.mExists(ext_dir + "/php_opcache.dont_load"))
			fs.move(ext_dir + "/php_opcache.dont_load", ext_dir + "/php_opcache.dll");
		if (host.mExists(ext_dir + "/php_opcache.dll"))
			return new DllVersion(ext_dir, "php_opcache.dll", "php_opcache.pdb", build.getVersionRevision(cm, host));
		else
			return null;
	}
	
	public DllVersion getDllPath(ConsoleManager cm, FileSystemScenario fs, Host host, PhpBuild build) {
		return getDllPath(cm, fs, host, build, false);
	}
	
	protected DllVersion getDllPath(ConsoleManager cm, FileSystemScenario fs, Host host, PhpBuild build, boolean rename) {
		if (this.set_dll!=null)
			return this.set_dll;
		
		DllVersion version = null;
		try {
			switch(build.getVersionBranch(cm, host)) {
			default:
				if (host.isWindows())
					version = getDllPath55Plus(cm, fs, host, build, rename);
				else
					version = getSoPath55Plus(cm, fs, host, build, rename);
			} // end switch
		} catch ( Exception ex ) {
			ConsoleManagerUtil.printStackTrace(EPrintType.CANT_CONTINUE, getClass(), cm, "getDllPath", ex, "Unable to find OpCache");
		}
		
		if (version!=null) {
			if (host.mExists(version.getPath())) {
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
	public boolean isSupported(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) {
		if (getDllPath(cm, FileSystemScenario.getFS(scenario_set, host), host, build) == null) {
			if (cm!=null) {
				cm.println(EPrintType.CLUE, getClass(), "Unable to find Opcache DLL or SO. Can NOT run this Scenario.");
			}
			return false;
		}
		return true;
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
				
				host.mDeleteIfExistsElevated(temp_dir);
				
				startup_handle = null;
			}
		}
		
		@Override
		public boolean prepareINI(ConsoleManager cm, FileSystemScenario fs, AHost host, PhpBuild build, ScenarioSet scenario_set, PhpIni ini) {
			return OpcacheScenario.this.prepareINI(cm, fs, host, ini, build, dll.getPath());
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
	public OpcacheSetup setup(ConsoleManager cm, FileSystemScenario fs, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) {
		if (host.isWindows()) {
			// TODO shouldn't be casting to AHost
			cleanupBaseAddressFile((AHost)host, build, null);
		}
		
		// find dll and rename from .dont_load to .so or .dll if needed (=> true)
		final DllVersion dll = getDllPath(cm, fs, host, build, true);
		if (dll==null)
			return null;
		
		OpcacheSetup setup = null;
		try {
			setup = createOpcacheSetup(dll, host, cm, build);
		} catch ( Exception ex ) {
			ConsoleManagerUtil.printStackTrace(EPrintType.CANT_CONTINUE, getClass(), cm, "setup", ex, "Can't setup Opcache");
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
				setup.temp_dir = fs.mktempname("Opcache_Startup_Process");
				fs.createDirs(setup.temp_dir);
				
				String php_script = setup.temp_dir+"/startup.php";
				
				PhpIni ini = new PhpIni();
				setup.prepareINI(cm, fs, (AHost)host, build, scenario_set, ini);
				fs.saveTextFile(setup.temp_dir+"/php.ini", ini.toString());
				
				// start thread to startup opcache
				fs.saveTextFile(php_script, "<?php while(true){sleep(60000);} ?>");
				
				setup.startup_handle = ((AHost)host).execThread(build.getPhpExe(EExecutableType.CLI)+" -c "+setup.temp_dir+" -f "+php_script);
			} catch ( Exception ex ) {
				ConsoleManagerUtil.printStackTrace(OpcacheScenario.class, cm, ex);
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
		// in temp directory. name is like: ZendOPcache.MemoryBase@*
		// @see shared_alloc_win32.c (https://github.com/php/php-src/blob/PHP-7.4/ext/opcache/shared_alloc_win32.c)
		//
		// for regular users, TEMP_DIR is often
		// for Apache (as service) TEMP_DIR is often C:\Users\NT_Authority? (different than IIS service)
		// for IIS (service) TEMP_DIR is often C:\Windows\Temp
		host.mDeleteIfExistsElevated(host.getTempDir()+"\\ZendOPcache.MemoryBase@*");
		if (test_pack!=null) {
			host.mDeleteIfExistsElevated(test_pack.getRunningDirectory()+"\\ZendOPcache.MemoryBase@*");	
			host.mDeleteIfExistsElevated(test_pack.getStorageDirectory()+"\\ZendOPcache.MemoryBase@*");
		}
		host.mDeleteIfExistsElevated(build.getBuildPath()+"\\ZendOPcache.MemoryBase@*");	
		host.mDeleteIfExistsElevated(host.getJobWorkDir()+"\\ZendOPcache.MemoryBase@*");
		host.mDeleteIfExistsElevated(host.getPfttDir()+"\\ZendOPcache.MemoryBase@*");
		host.mDeleteIfExistsElevated(host.getSystemTempDir()+"\\ZendOPcache.MemoryBase@*");
		host.mDeleteIfExistsElevated(host.getSystemRoot()+"\\ZendOPcache.MemoryBase@*");
		host.mDeleteIfExistsElevated(host.getSystemDrive()+"\\ZendOPcache.MemoryBase@*");
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
	 * @param fs
	 * @param host
	 * @param build
	 * @param ini
	 */
	@Override
	public IScenarioSetup setup(ConsoleManager cm, FileSystemScenario fs, Host host, PhpBuild build, PhpIni ini) {
		final DllVersion dll = this.getDllPath(cm, fs, host, build);
		if (dll!=null) {
			if (prepareINI(cm, fs, (AHost) host, ini, build, dll.getPath()))
				return SETUP_SUCCESS;
		}
		return SETUP_FAILED;
	}
	
} // end public class OpcacheScenario
