package com.mostc.pftt.model.ui;


import com.github.mattficken.Overridable;
import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.model.ui.UIAccount.IUserType;

/** 
 * Should be
 * 1. atomic
 * 
 * @author Matt Ficken
 *
 */

public abstract class UITest {
	protected UIAccount user_account;
	
	public abstract EUITestStatus test(IUITestDriver driver) throws Exception;
	public abstract boolean start(IUITestDriver driver) throws Exception;
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof UITest) {
			return getClass().isAssignableFrom(o.getClass());
		} else {
			return false;
		}
	}
	
	@Overridable
	public String getComment() {
		return null;
	}
	
	public boolean isAnon() {
		return UIAccount.isAnon(user_account);
	}
	
	public static EUITestStatus testChild(UITest child, IUITestDriver driver) throws Exception {
		EUITestStatus status = child.test(driver);
		if (status==null)
			return EUITestStatus.NOT_IMPLEMENTED;
		else
			return status;
	}
	
	public static boolean testChildPass(UITest child, IUITestDriver driver) throws Exception {
		switch(testChild(child, driver)) {
		case PASS:
		case PASS_WITH_WARNING:
			return true;
		default:
			return false;
		}
	}
	
	public String createUniqueTestName(UIAccount account) {
		return ( account == null ? "Anon" : StringUtil.toTitle(account.username) ) + "-" +getName();
	}
	
	public static String createUniqueTestName(IUserType user_type, String name) {
		return user_type==null?name:user_type.getDefaultUserName()+"-"+name;
	}
	
	@Overridable
	public String getName() {
		String name = getClass().getSimpleName();
		if (name.endsWith("Test"))
			name = name.substring(0, name.length()-"Test".length());
		return StringUtil.join(StringUtil.splitOnUpperCase(name), "-");
	}
	
} // end public abstract class UITest
