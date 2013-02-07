package com.mostc.pftt.results;

import java.awt.Container;
import javax.swing.JFrame;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.TestCase;
import com.mostc.pftt.model.core.EPhptTestStatus;
import com.mostc.pftt.model.core.PhpDebugPack;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.runner.LocalPhptTestPackRunner;
import com.mostc.pftt.ui.PhptDebuggerFrame;
import com.mostc.pftt.util.ErrorUtil;

public class LocalConsoleManager implements ConsoleManager {
	protected final boolean force, windebug, results_only, show_gui, disable_debug_prompt, dont_cleanup_test_pack, phpt_not_in_place, pftt_debug, no_result_file_for_pass_xskip_skip, randomize_order;
	protected final int run_times;
	protected String source_pack;
	protected PhpDebugPack debug_pack;
	protected PhptDebuggerFrame gui;
		
	public LocalConsoleManager(String source_pack, PhpDebugPack debug_pack, boolean force, boolean windebug, boolean results_only, boolean show_gui, boolean disable_debug_prompt, boolean dont_cleanup_test_pack, boolean phpt_not_in_place, boolean pftt_debug, boolean no_result_file_for_pass_xskip_skip, boolean randomize_order, int run_times) {
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
		this.no_result_file_for_pass_xskip_skip = no_result_file_for_pass_xskip_skip;
		this.randomize_order = randomize_order;
		this.run_times = run_times;
	}
	
	public void showGUI(LocalPhptTestPackRunner test_pack_runner) {
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

	protected void finishedTest(PhptTestCase test_case, EPhptTestStatus status) {
		System.out.println(status+" "+test_case.getName());
	}
	
	@Override
	public void restartingAndRetryingTest(TestCase test_case) {
		restartingAndRetryingTest(test_case.getName());
	}
	
	@Override
	public void restartingAndRetryingTest(String test_case_name) {
		if (results_only)
			return;
		
		System.out.println("RESTARTING_AND_RETRYING "+test_case_name);
	}
	
	protected void showResult(AHost host, int totalCount, int completed, PhptTestResult result) {
		if (gui!=null)
			gui.showResult(host, totalCount, completed, result);
	}
	
	@Override
	public void println(EPrintType type, String ctx_str, String string) {
		if (results_only)
			return;
		
		switch(type) {
		case SKIP_OPERATION: // TODO UNABLE_TO_START_OPERATION
		case CANT_CONTINUE:
		case OPERATION_FAILED_CONTINUING:
			System.err.println(type+": "+ctx_str+": "+string);
			break;
		case CLUE:
			System.out.println(type+": "+ctx_str+": "+string);
			break;
		case TIP:
			System.out.println("PFTT: "+string);
			break;
		default:
			System.out.println("PFTT: "+ctx_str+": "+string);
		}
		// TODO record in result-pack
	}
	@Override
	public void println(EPrintType type, Class<?> clazz, String string) {
		println(type, Host.toContext(clazz), string);
	}
	@Override
	public void addGlobalException(EPrintType type, Class<?> clazz, String method_name, Exception ex, String msg) {
		addGlobalException(type, clazz, method_name, ex, msg, null);
	}
	@Override
	public void addGlobalException(EPrintType type, Class<?> clazz, String method_name, Exception ex, String msg, Object a) {
		addGlobalException(type, clazz, method_name, ex, msg, a, null);
	}
	@Override
	public void addGlobalException(EPrintType type, Class<?> clazz, String method_name, Exception ex, String msg, Object a, Object b) {
		addGlobalException(type, clazz, method_name, ex, msg, a, b, null);
	}
	@Override
	public void addGlobalException(EPrintType type, Class<?> clazz, String method_name, Exception ex, String msg, Object a, Object b, Object c) {
		addGlobalException(type, Host.toContext(clazz, method_name), ex, msg, a, b, c);
	}

	protected PhpResultPackWriter w;
	@Override
	public void addGlobalException(EPrintType type, String ctx_str, Exception ex, String msg) {
		addGlobalException(type, ctx_str, ex, msg, null);
	}
	@Override
	public void addGlobalException(EPrintType type, String ctx_str, Exception ex, String msg, Object a) {
		addGlobalException(type, ctx_str, ex, msg, a, null);
	}
	@Override
	public void addGlobalException(EPrintType type, String ctx_str, Exception ex, String msg, Object a, Object b) {
		addGlobalException(type, ctx_str, ex, msg, a, b, null);
	}
	@Override
	public void addGlobalException(EPrintType type, String ctx_str, Exception ex, String msg, Object a, Object b, Object c) {
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

	@Override
	public boolean isResultsOnly() {
		return results_only;
	}

	@Override
	public boolean isDontCleanupTestPack() {
		return dont_cleanup_test_pack;
	}

	@Override
	public boolean isPhptNotInPlace() {
		return phpt_not_in_place;
	}

	@Override
	public PhpDebugPack getDebugPack() {
		return debug_pack;
	}

	@Override
	public String getSourcePack() {
		return source_pack;
	}

	@Override
	public boolean isNoResultFileForPassSkipXSkip() {
		return no_result_file_for_pass_xskip_skip;
	}

	@Override
	public int getRunTestTimes() {
		return run_times;
	}

	@Override
	public boolean isRandomizeTestOrder() {
		return randomize_order;
	}
	
} // end public class ConsoleManager
