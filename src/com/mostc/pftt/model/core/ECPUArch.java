package com.mostc.pftt.model.core;

public enum ECPUArch {
	X86,
	X64;

	public static ECPUArch guessValueOf(String string) {
		if (string.equalsIgnoreCase("x64"))
			return X64;
		else if (string.equalsIgnoreCase("x86"))
			return X86;
		else
			return null;
	}
}
