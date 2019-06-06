package com.mostc.pftt.model.sapi;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ConsoleManagerUtil;
import com.mostc.pftt.results.EPrintType;

public enum EApacheVersion {
	APACHE_2_2 {
		public boolean isSupportedEx(ConsoleManager cm, Host host, PhpBuild build) throws Exception {
			if (!host.isWindows())
				return true;
			
			if (build.getVersionMajor(cm, host) == 5) {
				int m = build.getVersionMinor(cm, host);
				// not supported on 5.5 (because there aren't any VC11 builds of Apache-2.2 and 5.5 is built with VC11)
				return m == 3 || m == 4;
			}
			return false; // old or future php?
		}
		@Override
		public EApacheVersion getApacheVersion(ConsoleManager cm, Host host, PhpBuild build) {
			return this;
		}
		@Override
		public ApacheHttpdAndVersion getHttpd(ConsoleManager cm, Host host, PhpBuild build) {
			if (host.isWindows()) {
				return new ApacheHttpdAndVersion("ApacheLounge-2.2.4-VC9-OpenSSL0.9.8y-x86", host.getSystemDrive() + "\\Apache224-VC9-OpenSSL0.9.8y-x86\\bin\\httpd.exe");
			} else {
				return new ApacheHttpdAndVersion("Apache", "/usr/sbin/httpd");
			}
		}
		@Override
		public String getApacheRoot(ConsoleManager cm, Host host, PhpBuild build) {
			if (host.isWindows()) {
				return host.getSystemDrive() + "\\Apache224-VC9-OpenSSL0.9.8y-x86";
			} else {
				return "/usr/local/apache2";
			}
		}
	},
	APACHE_2_4 {
		public boolean isSupportedEx(ConsoleManager cm, Host host, PhpBuild build) throws Exception {
			if (!host.isWindows()) {
				return true;
			} else if (build.getVersionMajor(cm, host) == 7) {
				return true;
			} else if (build.getVersionMajor(cm, host) == 5) {
				return true;
			} else {
				// old or future php?
				return false;
			}
		}
		@Override
		public EApacheVersion getApacheVersion(ConsoleManager cm, Host host, PhpBuild build) {
			return this;
		}
		@Override
		public ApacheHttpdAndVersion getHttpd(ConsoleManager cm, Host host, PhpBuild build) {
			if (host.isWindows()) {
				if (build.is70(cm, host)) {
					if (build.isX64())
						return new ApacheHttpdAndVersion("Apache2416-VC14-OpenSSL1.0.1e-x64", host.getSystemDrive() + "\\Apache2416-VC14-OpenSSL1.0.1e-x64\\bin\\httpd.exe");
					else
						return new ApacheHttpdAndVersion("Apache2416-VC14-OpenSSL1.0.1e-x86", host.getSystemDrive() + "\\Apache2416-VC14-OpenSSL1.0.1e-x86\\bin\\httpd.exe");
				} else if (build.isX64()) {
					return new ApacheHttpdAndVersion("ApacheLounge-2.4.4-VC11-OpenSSL1.0.1e-x64", host.getSystemDrive() + "\\Apache244-VC11-OpenSSL1.0.1e-x64\\bin\\httpd.exe");
				} else {
					return new ApacheHttpdAndVersion("ApacheLounge-2.4.4-VC11-OpenSSL1.0.1e-x86", host.getSystemDrive() + "\\Apache244-VC11-OpenSSL1.0.1e-x86\\bin\\httpd.exe");
				}
			} else {
				return new ApacheHttpdAndVersion("Apache", "/usr/sbin/httpd");
			}
		}
		@Override
		public String getApacheRoot(ConsoleManager cm, Host host, PhpBuild build) {
			if (host.isWindows()) {
				if (build.is70(cm, host)) {
					if (build.isX64())
						return host.getSystemDrive() + "\\Apache2416-VC14-OpenSSL1.0.1e-x64";
					else
						return host.getSystemDrive() + "\\Apache2416-VC14-OpenSSL1.0.1e-x86";
				} else if (build.isX64()) {
					return host.getSystemDrive() + "\\Apache244-VC11-OpenSSL1.0.1e-x64";
				} else {
					return host.getSystemDrive() + "\\Apache244-VC11-OpenSSL1.0.1e-x86";
				}
			} else {
				return "/usr/local/apache2";
			}
		}
	},
	NEWEST_SUPPORTED {
		public boolean isSupportedEx(ConsoleManager cm, Host host, PhpBuild build) throws Exception {
			return APACHE_2_4.isSupportedEx(cm, host, build) || APACHE_2_2.isSupportedEx(cm, host, build);
		}
		@Override
		public EApacheVersion getApacheVersion(ConsoleManager cm, Host host, PhpBuild build) {
			try {
				if (APACHE_2_4.isSupportedEx(cm, host, build))
					return APACHE_2_4;
				else if (APACHE_2_2.isSupportedEx(cm, host, build))
					return APACHE_2_2;
			} catch ( Exception ex ) {
				ConsoleManagerUtil.printStackTrace(EPrintType.CLUE, getClass(), cm, "getApacheVersion", ex, "Couldn't decide which Apache version to use.");
			}
			return FALLBACK;
		}
		@Override
		public ApacheHttpdAndVersion getHttpd(ConsoleManager cm, Host host, PhpBuild build) {
			return getApacheVersion(cm, host, build).getHttpd(cm, host, build);
		}
		@Override
		public String getApacheRoot(ConsoleManager cm, Host host, PhpBuild build) {
			return getApacheVersion(cm, host, build).getApacheRoot(cm, host, build);
		}
	},
	OLDEST_SUPPORTED {
		public boolean isSupportedEx(ConsoleManager cm, Host host, PhpBuild build) throws Exception {
			return APACHE_2_4.isSupportedEx(cm, host, build) || APACHE_2_2.isSupportedEx(cm, host, build);
		}
		@Override
		public EApacheVersion getApacheVersion(ConsoleManager cm, Host host, PhpBuild build) {
			try {
				if (APACHE_2_2.isSupportedEx(cm, host, build))
					return APACHE_2_2;
				else if (APACHE_2_4.isSupportedEx(cm, host, build))
					return APACHE_2_4;
			} catch ( Exception ex ) {
				ConsoleManagerUtil.printStackTrace(EPrintType.CLUE, getClass(), cm, "getApacheVersion", ex, "Couldn't decide which Apache version to use.");
			}
			return FALLBACK;
		}
		@Override
		public ApacheHttpdAndVersion getHttpd(ConsoleManager cm, Host host, PhpBuild build) {
			return getApacheVersion(cm, host, build).getHttpd(cm, host, build);
		}
		@Override
		public String getApacheRoot(ConsoleManager cm, Host host, PhpBuild build) {
			return getApacheVersion(cm, host, build).getApacheRoot(cm, host, build);
		}
	};
	
	public static final EApacheVersion DEFAULT = NEWEST_SUPPORTED;
	public static final EApacheVersion FALLBACK = APACHE_2_4;
	
	public boolean isSupported(ConsoleManager cm, Host host, PhpBuild build) throws Exception {
		try {
			return isSupportedEx(cm, host, build);
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "isSupported", ex, "");
			return false;
		}
	}
	
	public String getHttpdPath(ConsoleManager cm, Host host, PhpBuild build) {
		ApacheHttpdAndVersion a = null;
		try {
			a = getHttpd(cm, host, build);
		} catch ( Exception ex ) {
			ConsoleManagerUtil.printStackTrace(EApacheVersion.class, ex);
		}
		return a == null ? null : a.httpd;
	}
	
	
	public abstract String getApacheRoot(ConsoleManager cm, Host host, PhpBuild build);
	public abstract ApacheHttpdAndVersion getHttpd(ConsoleManager cm, Host host, PhpBuild build);
	public abstract boolean isSupportedEx(ConsoleManager cm, Host host, PhpBuild build) throws Exception;
	public abstract EApacheVersion getApacheVersion(ConsoleManager cm, Host host, PhpBuild build) throws Exception;
	
	public static final class ApacheHttpdAndVersion {
		public final String version, httpd;
		
		public ApacheHttpdAndVersion(String version, String httpd) {
			this.version = version;
			this.httpd = httpd;
		}
	}
	
} // end public enum EApacheVersion
