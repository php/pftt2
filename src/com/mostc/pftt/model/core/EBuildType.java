package com.mostc.pftt.model.core;

public enum EBuildType {
	TS,
	NTS;
	
	/** flexibly matches different values, guessing which EBuildType it refers to
	 * 
	 * returns null if no match
	 * 
	 * flexible enough to give input direct from user
	 * 
	 * @param str
	 * @return
	 */
	public static EBuildType guessValueOf(String str) {
		str = str.toLowerCase();
		if (str.equals("ts"))
			return TS;
		else if (str.equals("nts"))
			return NTS;
		else
			return null;
	}
}
