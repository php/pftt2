package com.mostc.pftt.results;

import java.awt.Container;

import javax.swing.JFrame;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpDebugPack;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.model.phpt.EPhptTestStatus;
import com.mostc.pftt.runner.PhptTestPackRunner;
import com.mostc.pftt.ui.PhptDebuggerFrame;
import com.mostc.pftt.util.ErrorUtil;

public class ConsoleManager {
	protected final boolean force, windebug, results_only, show_gui, disable_debug_prompt, dont_cleanup_test_pack, phpt_not_in_place, pftt_debug;
	protected String source_pack;
	protected PhpDebugPack debug_pack;
	protected PhptDebuggerFrame gui;
		
	public ConsoleManager(String source_pack, PhpDebugPack debug_pack, boolean force, boolean windebug, boolean results_only, boolean show_gui, boolean disable_debug_prompt, boolean dont_cleanup_test_pack, boolean phpt_not_in_place, boolean pftt_debug) {
		this.source_pack = source_pack;
		this.debug_pack = debug_pack;
		this.force = force;
		this.windebug = windebug;
		this.results_only = results_only;
		this.show_gui = show_gui;
		this.disable_debug_prompt = disable_debug_prompt;
		this.dont_cleanup_test_pack = dont_cleanup_test_pack;
		this.phpt_not_in_place = phpt_not_in_place;
		this.pftt_debug = pftt_debug;
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
	
	public boolean isForce() {
		return force;
	}
	
	public boolean isWinDebug() {
		return windebug;
	}
	
	public boolean isPfttDebug() {
		return pftt_debug;
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
	
	public void addGlobalException(Class<?> clazz, String method_name, Exception ex, String msg) {
		addGlobalException(clazz.getSimpleName()+"#"+method_name, ex, msg);
	}

	protected PhptResultPackWriter w;
	public void addGlobalException(String ctx_str, Exception ex, String msg) {
		String ex_str = ErrorUtil.toString(ex);
		if (!results_only) {
			System.err.println(ex_str);
		}
		if (w!=null) {
			synchronized (w.global_exception_writer) {
				w.global_exception_writer.println(ctx_str);
				w.global_exception_writer.println(msg==null?"":msg);
				w.global_exception_writer.print(ex_str);
			}
		}
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

	public PhpDebugPack getDebugPack() {
		return debug_pack;
	}

	public String getSourcePack() {
		return source_pack;
	}
	
} // end public class ConsoleManager
