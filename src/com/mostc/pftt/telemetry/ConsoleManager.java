package com.mostc.pftt.telemetry;

import java.awt.Container;

import javax.swing.JFrame;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.model.phpt.EPhptTestStatus;
import com.mostc.pftt.runner.PhptTestPackRunner;
import com.mostc.pftt.ui.PhptDebuggerFrame;

public class ConsoleManager {
	protected final boolean results_only, show_gui, disable_debug_prompt;
	protected PhptDebuggerFrame gui;
		
	public ConsoleManager(boolean results_only, boolean show_gui, boolean disable_debug_prompt) {
		this.results_only = results_only;
		this.show_gui = show_gui;
		this.disable_debug_prompt = disable_debug_prompt;
		
	}
	
	public void showGUI(PhptTestPackRunner test_pack_runner) {
		if (show_gui) {
			gui = new PhptDebuggerFrame(test_pack_runner);
			show_gui("", gui);
		}
	}
	
	protected static void show_gui(String title, Container c) {
		JFrame jf = new JFrame("PFTT - "+title);
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jf.setContentPane(c);
		jf.pack();
		jf.setExtendedState(JFrame.MAXIMIZED_BOTH);				
		jf.setVisible(true);
	}
	
	public boolean isDisableDebugPrompt() {
		return disable_debug_prompt;
	}

	public void finishedTest(PhptTestCase test_case, EPhptTestStatus status) {
		System.out.println(status+" "+test_case.getName());
	}
	
	public void restartingAndRetryingTest(PhptTestCase test_case) {
		if (results_only)
			return;
		
		System.out.println("RESTARTING_AND_RETRYING "+test_case.getName());
	}
	
	public void showResult(Host host, int totalCount, int completed, PhptTestResult result) {
		if (gui!=null)
			gui.showResult(host, totalCount, completed, result);
	}

	public void println(String ctx_str, String string) {
		if (results_only)
			return;
		
		System.out.println("PFTT: "+ctx_str+": "+string);
	}
}
