package com.mostc.pftt.results;

import java.util.List;

import javax.annotation.Nonnegative;

import com.mostc.pftt.model.TestCase;
import com.mostc.pftt.model.core.PhpDebugPack;

public interface ConsoleManager {
	
	public boolean isNonInteractive();
	public boolean isNoRestartAll();
	public boolean isDisableDebugPrompt();
	public boolean isOverwrite();
	/**
	 * 
	 * @see -debug_all console option
	 * @return
	 */
	public boolean isDebugAll();
	/**
	 * 
	 * @see -debug_list console option
	 * @param test_case
	 * @return
	 */
	public boolean isInDebugList(TestCase test_case);
	/**
	 * @see -debug_list console option
	 * @return
	 */
	public boolean isDebugList();
	/**
	 * @see -skip_list console option
	 * @param test_case
	 * @return
	 */
	public boolean isInSkipList(TestCase test_case);
	public boolean isPfttDebug();
	public boolean isNoResultFileForPassSkipXSkip();
	
	public void restartingAndRetryingTest(TestCase test_case);
	public void restartingAndRetryingTest(String test_case_name);
	public void println(EPrintType type, String ctx_str, String string);
	public void println(EPrintType type, Class<?> clazz, String string);
	public void addGlobalException(EPrintType type, Class<?> clazz, String method_name, Exception ex, String msg);
	public void addGlobalException(EPrintType type, Class<?> clazz, String method_name, Exception ex, String msg, Object a);
	public void addGlobalException(EPrintType type, Class<?> clazz, String method_name, Exception ex, String msg, Object a, Object b);
	public void addGlobalException(EPrintType type, Class<?> clazz, String method_name, Exception ex, String msg, Object a, Object b, Object c);
	public void addGlobalException(EPrintType type, String ctx_str, Exception ex, String msg);
	public void addGlobalException(EPrintType type, String ctx_str, Exception ex, String msg, Object a);
	public void addGlobalException(EPrintType type, String ctx_str, Exception ex, String msg, Object a, Object b);
	public void addGlobalException(EPrintType type, String ctx_str, Exception ex, String msg, Object a, Object b, Object c);
	public boolean isResultsOnly();
	public boolean isDontCleanupTestPack();
	public boolean isPhptNotInPlace();
	public PhpDebugPack getDebugPack();
	public String getSourcePack();
	/** number of times a test case is run (typically 1)
	 * 
	 * @return
	 */
	@Nonnegative
	public int getRunTestTimesAll();
	@Nonnegative
	public int getRunTestPack();
	/** should test cases be run in (TRUE) random order (different order every time) or normal order (FALSE) 
	 * 
	 * @see -randomize_order console option
	 * @return
	 */
	public boolean isRandomizeTestOrder();
	/**
	 * 
	 * @see -no_nts console option
	 * @return
	 */
	public boolean isThreadSafety();
	public int getRunGroupTimesAll();
	/**
	 * @see -run_test_times_list console option
	 * @param test_case
	 * @return
	 */
	public boolean isInRunTestTimesList(TestCase test_case);
	/** 
	 * 
	 * @see -run_test_times_list console option
	 * @return
	 */
	public int getRunTestTimesListTimes();
	public int getRunGroupTimesListTimes();
	public List<String> getRunGroupTimesList();
	public boolean isRunGroupTimesList();
	public boolean isSkipSmokeTests();
	public int getMaxTestReadCount();
	public int getThreadCount();
	public boolean isRestartEachTestAll();
	public int getDelayBetweenMS();
	public int getRunCount();
	public int getSuspendSeconds();
	public boolean isGetActualIniAll();
	public long getMaxRunTimeMillis();
	
} // end public class ConsoleManager
