package com.mostc.pftt.model.ui;

import com.github.mattficken.io.StringUtil;

public class UIAccount {
	public final String username, password;
	
	public UIAccount(String username, String password) {
		this.username = username;
		this.password = password;
	}
	
	public boolean isAnon() {
		return StringUtil.isEmpty(username) || StringUtil.equalsIC(username, "anon") || StringUtil.equalsIC(username, "anonymous");
	}

	public static boolean isAnon(UIAccount user_account) {
		return user_account == null ? true : user_account.isAnon();
	}
	
}