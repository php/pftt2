package com.mostc.pftt.model.core;

public enum EBuildBranch {
	NATIVE_TLS {
		@Override
		public String toString() {
			return "NATIVE_TLS";
		}
		@Override
		public ECPUArch getCPUArch() {
			return null; // could be X86 or X64
		}
	},
	PHP_Master {
		@Override
		public String toString() {
			return "PHP_Master";
		}
		@Override
		public ECPUArch getCPUArch() {
			return null; // could be X86 or X64
		}
	},
	PHP_5_6 {
		@Override
		public ECPUArch getCPUArch() {
			return null; // could be X86 or X64
		}
	},
	PHP_7_0 {
		@Override
		public ECPUArch getCPUArch() {
			return null; // could be X86 or X64
		}
	},
	PHP_7_1 {
		@Override
		public ECPUArch getCPUArch() {
			return null; // could be X86 or X64
		}
	},
	PHP_7_2 {
		@Override
		public ECPUArch getCPUArch() {
			return null; // could be X86 or X64
		}
	},
	PHP_7_3 {
		@Override
		public ECPUArch getCPUArch() {
			return null; // could be X86 or X64
		}
	},
	PHP_7_4 {
		@Override
		public ECPUArch getCPUArch() {
			return null; // could be X86 or X64
		}
	},
	PHP_8_0 {
		@Override
		public ECPUArch getCPUArch() {
			return null; // could be X86 or X64
		}
	},
	STR_SIZE_AND_INT64 {
		@Override
		public ECPUArch getCPUArch() {
			return ECPUArch.X64;
		}
	};
	
	public static EBuildBranch getNewest() {
		return PHP_7_3;
	}
	
	/** flexibly matches different values, guessing which EBuildBranch it refers to
	 * 
	 * returns null if no match
	 * 
	 * flexible enough to give input direct from user
	 * 
	 * @param str
	 * @return
	 */
	public static EBuildBranch guessValueOf(String str) {
		str = str.toLowerCase();
		if (str.equals("php_5_6")||str.equals("5_6")||str.equals("5.6")||str.equals("56")||str.equals("php5_6")||str.equals("php56")||str.equals("php5.6")||str.equals("php_5.6"))
			return PHP_5_6;
		else if (str.equals("php_7_0")||str.equals("7_0")||str.equals("7.0")||str.equals("70")||str.equals("php7_0")||str.equals("php70")||str.equals("php7.0")||str.equals("php_7.0"))
			return PHP_7_0;
		else if (str.equals("php_7_1")||str.equals("7_1")||str.equals("7.1")||str.equals("71")||str.equals("php7_1")||str.equals("php71")||str.equals("php7.1")||str.equals("php_7.1"))
			return PHP_7_1;
		else if (str.equals("php_7_2")||str.equals("7_2")||str.equals("7.2")||str.equals("72")||str.equals("php7_2")||str.equals("php72")||str.equals("php7.2")||str.equals("php_7.2"))
			return PHP_7_2;
		else if (str.equals("php_7_3")||str.equals("7_3")||str.equals("7.3")||str.equals("73")||str.equals("php7_3")||str.equals("php73")||str.equals("php7.3")||str.equals("php_7.3"))
			return PHP_7_3;
		else if (str.equals("php_7_4")||str.equals("7_4")||str.equals("7.4")||str.equals("74")||str.equals("php7_4")||str.equals("php74")||str.equals("php7.4")||str.equals("php_7.4"))
			return PHP_7_4;
		else if (str.equals("master")||str.equals("php_master"))
			return PHP_Master;
		else if (str.equals("str_size_and_int64"))
			return STR_SIZE_AND_INT64;
		else
			return null;
	}
	
	public static EBuildBranch guessValueOfContains(String str) {
		str = str.toLowerCase();
		if (str.contains("php_5_6")||str.contains("php5_6")||str.contains("php56")||str.contains("php5.6")||str.contains("php_5.6"))
			return PHP_5_6;
		else if (str.contains("php_7_0")||str.contains("php7_0")||str.contains("php70")||str.contains("php7.0")||str.contains("php_7.0"))
			return PHP_7_0;
		else if (str.contains("php_7_1")||str.contains("php7_1")||str.contains("php71")||str.contains("php7.1")||str.contains("php_7.1"))
			return PHP_7_1;
		else if (str.contains("php_7_2")||str.contains("php7_2")||str.contains("php72")||str.contains("php7.2")||str.contains("php_7.2"))
			return PHP_7_2;
		else if (str.contains("php_7_3")||str.contains("php7_3")||str.contains("php73")||str.contains("php7.3")||str.contains("php_7.3"))
			return PHP_7_3;
		else if (str.contains("php_7_4")||str.contains("php7_4")||str.contains("php74")||str.contains("php7.4")||str.contains("php_7.4"))
			return PHP_7_4;
		else if (str.contains("master"))
			return PHP_Master;
		else if (str.contains("str_size")||str.contains("int64"))
			return STR_SIZE_AND_INT64;
		else
			return null;
	}

	public abstract ECPUArch getCPUArch();
	
}
