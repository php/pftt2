
def describe() {
	"Adds a Dynamic Extension to a Php Build or replaces one that is already there"
}

def helpMsg() {
	requiredConsoleOptions() + """

== custom_extension_dll example ==
-ext_file ...\\20130615\\php_opcache.dll -ext_name_replace Opcache -ext_name_with Opcache-20130615

cn -ext_file c:\\php-sdk\\php-5.5-ts-windows-vc11-x86-r157ccaf\\ext\\php_bz2.dll -q -d -c custom_extension_dll,apache php-5.5.0-Win32-VC11-x86 php-test-pack-5.5.0 bz2

"""
}

def requiredConsoleOptions() {
	"""== custom_extension_dll Console Options ==
-ext_file <file path> - path to DLL file to add REQUIRED
-ext_name_replace <string> - replaces this part of Scenario Set name OPTIONAL
-ext_name_with <string> - with this string (to indicate the DLL version, etc...) OPTIONAL
-ext_zend - flag to indicate its a zend extension (optional unless it is a zend extension)
"""
}

// @Field is required to make this field accessible to methods
@Field def ext_file = ''
@Field def ext_name_dll = ''
@Field def ext_name_replace = ''
@Field def ext_name_with = ''
@Field def is_zend_ext = false

def processConsoleOptions(List options) {
	int a = StringUtil.indexOfCS(options, "-ext_file");
	if (a!=-1) {
		ext_file = LocalHost.ensureAbsolutePathCWD(options[a+1]);
		options.remove(a);options.remove(a);
	}
	int b = StringUtil.indexOfCS(options, "-ext_name_replace");
	if (b!=-1) {
		ext_name_replace = options[b+1];options.remove(b);options.remove(b);
	}
	int c = StringUtil.indexOfCS(options, "-ext_name_with");
	if (c!=-1) {
		ext_name_with = options[c+1];options.remove(c);options.remove(c);
	}
	if (a==-1) {
		System.out.println("custom_extension_dll: Missing required console option: -ext_file");
		System.out.println();
		System.out.println(requiredConsoleOptions());
		System.exit(-200)
		return;
	}
	int d = StringUtil.indexOfCS(options, "-ext_zend");
	if (d!=-1) {
		is_zend_ext = true;
		options.remove(d);
	}
	//

	ext_name_dll = AHost.basename(ext_file);
	
	// assume these are zend extensions
	if (!is_zend_ext && (ext_name_dll.equals("php_opcache.dll")||ext_name_dll.equals("php_opcache.so")||ext_name_dll.equals("php_xdebug.dll")||ext_name_dll.equals("php_xdebug.so")))
		is_zend_ext = true;
	
	System.out.println("custom_extension_dll: ");
	System.out.println("custom_extension_dll: Custom DLL file ${ext_file}");
	System.out.println("custom_extension_dll: Name ${ext_name_dll}  Zend extension: ${is_zend_ext}");
	System.out.println("custom_extension_dll: Scenario Name replace '${ext_name_replace}' with '${ext_name_with}'");
	System.out.println("custom_extension_dll: ");
} // end def processConsoleOptions

def prepareINI(PhpIni ini) {
	// remove existing DLL (if found)
	ini.removeValue(PhpIni.EXTENSION, ext_name_dll);
	
	if (is_zend_ext) {
		// can store zend_extension DLLs in their source location (really can store wherever)
		// note: CustomExtScenario will still be used but won't actually do anything, see #setup below
		ini.putMulti(PhpIni.ZEND_EXTENSION, ext_file);
	} else {
		// need to move DLL into php build's extension_dir
		// (thats what CustomExtScenario.CustomExtDirScenarioSetup is used for, below)
		ini.putMulti(PhpIni.EXTENSION, ext_name_dll);
	}
}

class CustomExtScenario extends INIScenario {
	static final String CUSTOM_EXT = "CUSTOM-EXT";
	// important: keep a handle to the script's variable scope so CustomExtScenario
	//            can access those variables
	def script;
	//
	String orig_dll_path;
	boolean first_setup = true;
	
	@Override
	protected String processNameAndVersionInfo(String name) {
		// hide this scenario in the name
		name = name.replace("${CUSTOM_EXT}_", "").replace(CUSTOM_EXT, "").replace("____", "").replace("___", "").replace("__", "");
		
		if (StringUtil.isNotEmpty(script.ext_name_replace) && StringUtil.isNotEmpty(script.ext_name_with)) {
			// add DLL info to name
			name = name.replace(script.ext_name_replace, script.ext_name_with);
		}
		
		return name;
	}
	
	public IScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		// if !is_zend_ext, need to do this during ScenarioSetSetup#setupScenarios
		return setup(cm, host, build, (PhpIni) null); 
	}

	// TODO include custom DLL with build sent to cluster of test machines
	@Override
	public IScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini) {
		if (script.is_zend_ext) {
			if (first_setup) {
				System.out.println("custom_extension_dll:");
				System.out.println("custom_extension_dll: using ${script.ext_file}")
				System.out.println("custom_extension_dll:");
				first_setup = false;
			}
			return SETUP_SUCCESS;
		} else {
			if (first_setup) {
				orig_dll_path = build.getDefaultExtensionDir() + "/" + script.ext_name_dll;
				
				// save original dll if present (will restore in #close)
				System.out.println("custom_extension_dll:");
				if (host.exists(orig_dll_path)) {
					System.out.println("custom_extension_dll: moving ${orig_dll_path} to ${orig_dll_path}.orig");
					host.move(orig_dll_path, orig_dll_path+".orig");
				}
				System.out.println("custom_extension_dll: moving ${script.ext_file} to ${orig_dll_path}");
				System.out.println("custom_extension_dll:");
				host.copy(script.ext_file, orig_dll_path);
		
				first_setup = false;
			}
			// return custom setup to be notified when the ScenarioSetSetup is closed
			return new CustomExtDirScenarioSetup(host);
		}
	}
	
	class CustomExtDirScenarioSetup extends SimpleScenarioSetup {
		Host host;

		@Override
		public void close(ConsoleManager cm) {
			System.out.println("custom_extension_dll: close");
			if (script.is_zend_ext)
				return;
			
			// move original dll back
			System.out.println("custom_extension_dll: restoring: moving ${orig_dll_path}.orig BACK to ${orig_dll_path}");
			System.out.println("custom_extension_dll: ");
			host.move(orig_dll_path+".orig", orig_dll_path);
		}

		@Override
		public String getNameWithVersionInfo() {
			return CUSTOM_EXT;
		}

		@Override
		public String getName() {
			return CUSTOM_EXT;
		}
		
		def CustomExtDirScenarioSetup(Host host) {
			this.host = host;
		}
		
	} // end class CustomExtDirScenarioSetup

	@Override
	public Class<?> getSerialKey(EScenarioSetPermutationLayer layer) {
		// be included in every ScenarioSet that is permuted
		return getClass();
	}

	@Override
	public String getName() {
		return CUSTOM_EXT;
	}
	
	@Override
	public boolean isPlaceholder() {
		return true;
	}
	
	@Override
	public boolean ignoreForShortName() {
		return true;
	}

	@Override
	public boolean isImplemented() {
		return true;
	}

} // end class CustomExtScenario

def scenarios() {
	new CustomExtScenario(script: this)
}
