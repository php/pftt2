package com.mostc.pftt.model.core;

public enum EBuildBranch {
	PHP_5_3 {
		@Override
		public ECPUArch getCPUArch() {
			return ECPUArch.X86;
		}
	},
	PHP_5_4 {
		@Override
		public ECPUArch getCPUArch() {
			return ECPUArch.X86;
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
	PHP_5_5 {
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
	STR_SIZE_AND_INT64 {
		@Override
		public ECPUArch getCPUArch() {
			return ECPUArch.X64;
		}
	};
	
	public static EBuildBranch getNewest() {
		return STR_SIZE_AND_INT64;
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
		if (str.equals("php_5_3")||str.equals("5_3")||str.equals("5.3")||str.equals("53")||str.equals("php5_3")||str.equals("php53")||str.equals("php5.3")||str.equals("php_5.3"))
			return PHP_5_3;
		else if (str.equals("php_5_4")||str.equals("5_4")||str.equals("5.4")||str.equals("54")||str.equals("php5_4")||str.equals("php54")||str.equals("php5.4")||str.equals("php_5.4"))
			return PHP_5_4;
		else if (str.equals("php_5_5")||str.equals("5_5")||str.equals("5.5")||str.equals("55")||str.equals("php5_5")||str.equals("php55")||str.equals("php5.5")||str.equals("php_5.5"))
			return PHP_5_5;
		else if (str.equals("php_5_6")||str.equals("5_6")||str.equals("5.6")||str.equals("56")||str.equals("php5_6")||str.equals("php56")||str.equals("php5.6")||str.equals("php_5.6"))
			return PHP_5_6;
		else if (str.equals("master")||str.equals("php_master"))
			return PHP_Master;
		else if (str.equals("str_size_and_int64"))
			return STR_SIZE_AND_INT64;
		else
			return null;
	}
	
	public static EBuildBranch guessValueOfContains(String str) {
		str = str.toLowerCase();
		if (str.contains("php_5_3")||str.contains("php5_3")||str.contains("php53")||str.contains("php5.3")||str.contains("php_5.3"))
			return PHP_5_3;
		else if (str.contains("php_5_4")||str.contains("php5_4")||str.contains("php54")||str.contains("php5.4")||str.contains("php_5.4"))
			return PHP_5_4;
		else if (str.contains("php_5_5")||str.contains("php5_5")||str.contains("php55")||str.contains("php5.5")||str.contains("php_5.5"))
			return PHP_5_5;
		else if (str.contains("php_5_6")||str.contains("php5_6")||str.contains("php56")||str.contains("php5.6")||str.contains("php_5.6"))
			return PHP_5_6;
		else if (str.contains("master"))
			return PHP_Master;
		else if (str.contains("str_size")||str.contains("int64"))
			return STR_SIZE_AND_INT64;
		else
			return null;
	}

	public abstract ECPUArch getCPUArch();
	
}
