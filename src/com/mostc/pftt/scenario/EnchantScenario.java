package com.mostc.pftt.scenario;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;

/** 
 * 
 * @author Matt Ficken
 *
 */

public class EnchantScenario extends INIScenario {

	@Override
	public IScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini) {
		// copy dictionary to where the build expects it
		// enchant will be able to find it there, so PHPTs will work
		
		if (!host.isWindows())
			return SETUP_SUCCESS;
		
		// this is only needed for Windows
		try {
			// LATER php on non-windows may need a dictionary too ??
			String dst = build.getBuildPath()+"/share/";
			if (!host.exists(dst)) {
				host.copyElevated(host.getPfttCacheDir()+"/util/enchant/share", dst);
			
				
				// make sure libraries are where they need to be also
				host.copy(build.getBuildPath()+"/libenchant*", build.getBuildPath()+"/lib/enchant/");
			}
			
			return SETUP_SUCCESS;
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, getClass(), "setup", ex, "Unable to install spell checker dictionary");
			return SETUP_FAILED;
		}
	}
	
	@Override
	public boolean isPlaceholder(EScenarioSetPermutationLayer layer) {
		return true;
	}

	@Override
	public Class<?> getSerialKey(EScenarioSetPermutationLayer layer) {
		return getClass();
	}

	@Override
	public String getName() {
		return "Enchant";
	}

	@Override
	public boolean isImplemented() {
		return true;
	}

	public static String getDictionaryFile(AHost host) {
		return host.getPfttCacheDir()+"/util/enchant/share/myspell/dicts/en_US.dic";
	}

} // end public class EnchantScenario
