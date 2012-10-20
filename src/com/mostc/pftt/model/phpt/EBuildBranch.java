package com.mostc.pftt.model.phpt;

public enum EBuildBranch {
	PHP_5_3,
	PHP_5_4,
	MASTER,
	PHP_5_5;
	
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
		else if (str.equals("master"))
			return MASTER;
		else
			return null;
	}
}
