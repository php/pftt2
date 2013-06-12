package com.mostc.pftt.ui;

import javax.swing.JTabbedPane;

import com.mostc.pftt.model.app.PhpUnitSourceTestPack;
import com.mostc.pftt.results.PhpResultPackWriter;
import com.mostc.pftt.results.PhpUnitTestResult;
import com.mostc.pftt.runner.LocalPhpUnitTestPackRunner;

@SuppressWarnings("serial")
public class PhpUnitDebuggerFrame extends JTabbedPane {
	protected PhpUnitHostTab localhost_tab;
	
	public PhpUnitDebuggerFrame(LocalPhpUnitTestPackRunner test_pack_runner, PhpUnitSourceTestPack test_pack) {
		addTab("Localhost", localhost_tab = new PhpUnitHostTab(
				test_pack_runner,
				test_pack_runner.getRunnerHost(),
				test_pack,
				test_pack_runner.getScenarioSetSetup()
			));
	}

	public void setPhpResultPackWriter(PhpResultPackWriter w) {
		localhost_tab.setPhpResultPackWriter(w);
	}

	public void showResult(PhpUnitTestResult result) {
		localhost_tab.updateCount();
	}

	
}
