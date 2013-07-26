package com.mostc.pftt.results;

import java.io.IOException;

import javax.annotation.Nullable;

import org.xmlpull.v1.XmlSerializer;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.app.EPhpUnitTestStatus;
import com.mostc.pftt.model.app.PhpUnitTestCase;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.scenario.ScenarioSetSetup;

/** result of running a PhpUnitTestCase
 * 
 * @author Matt Ficken
 *
 */

public class PhpUnitTestResult implements ISerializer {
	public final PhpUnitTestCase test_case;
	public final EPhpUnitTestStatus status;
	public final ScenarioSetSetup scenario_set;
	public final Host host;
	public final String output;
	public String http_response;
	protected String sapi_output, sapi_config;
	public PhpIni ini;
	public final float run_time_micros;
	public TestCaseCodeCoverage code_coverage;
	@Nullable public ISerializer extra;
	
	public PhpUnitTestResult(PhpUnitTestCase test_case, EPhpUnitTestStatus status, ScenarioSetSetup scenario_set, Host host, String output, float run_time_micros, TestCaseCodeCoverage code_coverage) {
		this.test_case = test_case;
		this.status = status;
		this.scenario_set = scenario_set;
		this.host = host;
		this.output = output;
		this.run_time_micros = run_time_micros;
		this.code_coverage = code_coverage;
	}
	
	public PhpUnitTestResult(PhpUnitTestCase test_case, EPhpUnitTestStatus status, ScenarioSetSetup scenario_set, Host host, String output, PhpIni ini, float run_time_micros, TestCaseCodeCoverage code_coverage, String sapi_output, String sapi_config) {
		this(test_case, status, scenario_set, host, output, run_time_micros, code_coverage);
		this.sapi_output = sapi_output;
		this.sapi_config = sapi_config;
		this.ini = ini;
	}
	
	public String toString() {
		return getName();
	}
	
	public String getName() {
		return test_case.getName();
	}
	
	public String getSAPIConfig() {
		return sapi_config;
	}
	
	public String getSAPIOutput() {
		return sapi_output;
	}
	
	public static boolean shouldStoreAllInfo(EPhpUnitTestStatus status) {
		switch(status) {
		case NOT_IMPLEMENTED:
		case CRASH:
		case ERROR:
		case DEPRECATED:
		case WARNING:
		case NOTICE:
		case BORK:
		case UNSUPPORTED:
		case TEST_EXCEPTION:
		case FAILURE:
		case SKIP:
			return true;
		default:
			return false;
		}
	}
	
	@Override
	public void serial(XmlSerializer serial) throws IllegalArgumentException, IllegalStateException, IOException {
		serial(serial, shouldStoreAllInfo(status));
	}
	
	// @see PHPUnit/Util/Log/JUnit.php
	public void serial(XmlSerializer serial, boolean include_all) throws IllegalArgumentException, IllegalStateException, IOException {
		serial.startTag(null, "testcase");
		// count of failures due to assertions
		serial.attribute(null, "name", test_case.getMethodName());
		serial.attribute(null, "class", test_case.getClassName());
		serial.attribute(null, "file", test_case.getFileName());
		
		if (status!=null)
			serial.attribute("pftt", "status", status.toString());
		serial.attribute("pftt", "runTimeMicros", Float.toString(run_time_micros));
		
		if (StringUtil.isNotEmpty(output)) {
			switch(status) {
			case NOT_IMPLEMENTED:
			case SKIP:
			case XSKIP:
			case CRASH:
			case DEPRECATED:
			case WARNING:
			case NOTICE:
			case BORK:
			case UNSUPPORTED:
				serial.startTag("pftt", "output");
				serial.text(output);
				serial.endTag("pftt", "output");
				break;
			case ERROR:
				// @see #addIncompleteTest and #addSkippedTest and #addError
				serial.startTag(null, "error");
				serial.text(output);
				serial.endTag(null, "error");
				break;
			case TEST_EXCEPTION:
				serial.startTag("pftt", "testException");
				serial.text(output);
				serial.endTag("pftt", "testException");
				break;
			case FAILURE:
				// @see #addFailure
				serial.startTag(null, "failure");
				serial.text(output);
				serial.endTag(null, "failure");
				break;
			case PASS:
				serial.startTag("pftt", "output");
				serial.text(output);
				serial.endTag("pftt", "output");
			default:
				break;
			}
		}
		
		// 
		if (include_all) {
			if (StringUtil.isNotEmpty(http_response)) {
				serial.startTag("pftt", "httpResponse");
				serial.text(http_response);
				serial.endTag("pftt", "httpResponse");
			}
			
			if (ini!=null) {
				serial.startTag("pftt", "ini");
				serial.text(ini.toString());
				serial.endTag("pftt", "ini");
			}
			
			if (StringUtil.isNotEmpty(sapi_output)) {
				serial.startTag(null, "SAPIOutput");
				serial.text(sapi_output);
				serial.endTag(null, "SAPIOutput");
			}
			
			if (StringUtil.isNotEmpty(sapi_config)) {
				serial.startTag(null, "SAPIConfig");
				serial.text(sapi_config);
				serial.endTag(null, "SAPIConfig");
			}
			
		}
		//
		
		serial.endTag(null, "testcase");
	} // end public void serial
	
} // end public class PhpUnitTestResult
