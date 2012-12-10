package com.mostc.pftt.model.sapi;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

public enum EApacheVersion {
	APACHE_2_2 {
		public boolean isSupportedEx(ConsoleManager cm, Host host, PhpBuild build) throws Exception {
			if (!host.isWindows())
				return true;
			
			return build.getVersionMajor(cm, host) == 5 &&
					build.getVersionMinor(cm, host) >= 3;
		}
	},
	APACHE_2_4 {
		public boolean isSupportedEx(ConsoleManager cm, Host host, PhpBuild build) throws Exception {
			if (!host.isWindows()) {
				return true;
			} else if (build.getVersionMajor(cm, host) == 5) {
				int minor = build.getVersionMinor(cm, host);
				if (minor==4)
					// Apache 2.4 support was added in: 5.4.10
					return build.getVersionRelease(cm, host) < 10;
				else if (minor>4)
					// Apache 2.4 support always in 5.5
					return true;
				else
					// 5.0 5.1 5.2 5.3 5.4.0-5.4.9
					return false;
			} else {
				// old or future php?
				return false;
			}
		}
	};
	
	public static final EApacheVersion DEFAULT = APACHE_2_4;
	
	public boolean isSupported(ConsoleManager cm, Host host, PhpBuild build) throws Exception {
		try {
			return isSupportedEx(cm, host, build);
		} catch ( Exception ex ) {
			cm.printStackTrace(ex);
			return false;
		}
	}
	
	
	public abstract boolean isSupportedEx(ConsoleManager cm, Host host, PhpBuild build) throws Exception;
	
} // end public enum EApacheVersion
