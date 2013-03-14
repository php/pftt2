package com.mostc.pftt.model.ui;

import com.mostc.pftt.model.ui.UITestRunner.UIAccount;
import com.mostc.pftt.model.ui.UITestRunner.UITest;

public interface IUITestRunner {
	IUITestRunner test(UITest test);
	IUITestRunner test(UIAccount account, UITest test);
	EUITestStatus status();
	UIAccount getUserAccount();
	boolean isDummy();
}
