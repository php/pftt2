package com.mostc.pftt.telemetry;

import java.awt.Container;

import javax.swing.JFrame;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.model.phpt.EPhptTestStatus;
import com.mostc.pftt.runner.PhptTestPackRunner;
import com.mostc.pftt.ui.PhptDebuggerFrame;
import com.mostc.pftt.util.ErrorUtil;

public class ConsoleManager {
	protected final boolean results_only, show_gui, disable_debug_prompt, dont_cleanup_test_pack, phpt_not_in_place;
	protected PhptDebuggerFrame gui;
		
	public ConsoleManager(boolean results_only, boolean show_gui, boolean disable_debug_prompt, boolean dont_cleanup_test_pack, boolean phpt_not_in_place) {
		this.results_only = results_only;
		this.show_gui = show_gui;
		this.disable_debug_prompt = disable_debug_prompt;
		this.dont_cleanup_test_pack = dont_cleanup_test_pack;
		this.phpt_not_in_place = phpt_not_in_place;
	}
	
	public void showGUI(PhptTestPackRunner test_pack_runner) {
		if (show_gui) {
			if (gui!=null)
				((JFrame)gui.getRootPane().getParent()).dispose();
			
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
	
	public void println(Class<?> clazz, String string) {
		println(clazz.getSimpleName(), string);
	}

	public void printStackTrace(Exception ex) {
		if (results_only)
			return;
		
		String ex_str = ErrorUtil.toString(ex);
		System.err.println(ex_str);
	}

	public boolean isResultsOnly() {
		return results_only;
	}

	public boolean isDontCleanupTestPack() {
		return dont_cleanup_test_pack;
	}

	public boolean isPhptNotInPlace() {
		return phpt_not_in_place;
	}
	
} // end public class ConsoleManager
