package com.mostc.pftt.results;

import javax.annotation.Nonnegative;

import com.mostc.pftt.model.TestCase;
import com.mostc.pftt.model.core.PhpDebugPack;

public interface ConsoleManager {
	
	public boolean isDisableDebugPrompt();
	public boolean isForce();
	public boolean isWinDebug();
	public boolean isPfttDebug();
	public boolean isNoResultFileForPassSkipXSkip();
	
	public void restartingAndRetryingTest(TestCase test_case);
	public void restartingAndRetryingTest(String test_case_name);
	public void println(EPrintType type, String ctx_str, String string);
	public static enum EPrintType {
		SKIP_OPERATION,
		XSKIP_OPERATION, 
		SKIP_OPTIONAL,
		CLUE,
		CANT_CONTINUE,
		IN_PROGRESS,
		COMPLETED_OPERATION,
		OPERATION_FAILED_CONTINUING,
		TIP
	}
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
	public int getRunTestTimes();
	/** should test cases be run in (TRUE) random order (different order every time) or normal order (FALSE) 
	 * 
	 * @return
	 */
	public boolean isRandomizeTestOrder();
	
} // end public class ConsoleManager
