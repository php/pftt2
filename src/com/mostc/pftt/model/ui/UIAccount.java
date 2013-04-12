package com.mostc.pftt.model.ui;

import com.github.mattficken.io.StringUtil;

/**
 * 
 * @author Matt Ficken
 *
 */

public class UIAccount {
	public final String username, password;
	public final IUserType type;
	
	public UIAccount(IUserType type, String password) {
		this(type, type.getDefaultUserName(), password);
	}
	
	public UIAccount(IUserType type, String username, String password) {
		this.type = type;
		this.username = username;
		this.password = password;
	}
	
	public UIAccount(String username, String password) {
		this(null, username, password);
	}
	
	public UIAccount(UIAccount user, String new_password) {
		this(user.type, user.username, new_password);
	}
	
	public boolean isAnon() {
		return StringUtil.isEmpty(username) || (type!=null&&type.isAnonymous()) || StringUtil.equalsIC(username, "anon") || StringUtil.equalsIC(username, "anonymous");
	}

	public static boolean isAnon(UIAccount user_account) {
		return user_account == null ? true : user_account.isAnon();
	}
	
	public interface IUserType {
		public String getType();
		public boolean isAnonymous();
		public String getDefaultUserName();
	}
}
