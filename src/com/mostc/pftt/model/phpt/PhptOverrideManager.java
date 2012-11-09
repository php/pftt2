package com.mostc.pftt.model.phpt;

import com.mostc.pftt.host.Host;

/** 
 *
 */

public final class PhptOverrideManager {
		
	public static boolean hasOverrides(Host host) {
		if (host.isVistaExact()) {
			return true;
		} else {
			return false;
		}
	}
	
	public static String replaceWithExactOverrides(Host host, String str) {
		if (host.isVistaExact()) {
			if (vista==null)
				vista = new WindowsLonghorn();
			return vista.replaceWithExactOverrides(str);
		} else {
			return null;
		}
	}
	
	public static String replaceWithRegexOverrides(Host host, String str) {
		if (host.isVistaExact()) {
			if (vista==null)
				vista = new WindowsLonghorn();
			return vista.replaceWithRegexOverrides(str);
		} else {
			return null;
		}
	}
		
	static AbstractOutputOverrides vista;
	protected static abstract class AbstractOutputOverrides {
		protected abstract String replaceWithExactOverrides(String str);
		protected abstract String replaceWithRegexOverrides(String str);
	}
	
	protected static class WindowsLonghorn extends AbstractOutputOverrides {

		@Override
		protected String replaceWithExactOverrides(String str) {
			return str;
		}

		@Override
		protected String replaceWithRegexOverrides(String str) {
			return str;
		}
		
	} // end protected static class WindowsLonghorn
	
	private PhptOverrideManager() {}
	
} // end public final class PhptOverrideManager
