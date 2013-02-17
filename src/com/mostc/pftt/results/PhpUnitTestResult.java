package com.mostc.pftt.results;

import java.io.IOException;
import org.xmlpull.v1.XmlSerializer;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.app.EPhpUnitTestStatus;
import com.mostc.pftt.model.app.PhpUnitTestCase;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.scenario.ScenarioSet;

/** result of running a PhpUnitTestCase
 * 
 * @author Matt Ficken
 *
 */

public class PhpUnitTestResult {
	public final PhpUnitTestCase test_case;
	public final EPhpUnitTestStatus status;
	public final ScenarioSet scenario_set;
	public final Host host;
	public final String output;
	public String http_response;
	protected String sapi_output;
	public PhpIni ini;
	
	public PhpUnitTestResult(PhpUnitTestCase test_case, EPhpUnitTestStatus status, ScenarioSet scenario_set, Host host, String output) {
		this.test_case = test_case;
		this.status = status;
		this.scenario_set = scenario_set;
		this.host = host;
		this.output = output;
	}
	
	public PhpUnitTestResult(PhpUnitTestCase test_case, EPhpUnitTestStatus status, ScenarioSet scenario_set, Host host, String output, PhpIni ini, String sapi_output) {
		this(test_case, status, scenario_set, host, output);
		this.sapi_output = sapi_output;
		this.ini = ini;
	}
	
	public String getName() {
		return test_case.getName();
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
	
	public void serial(XmlSerializer serial) throws IllegalArgumentException, IllegalStateException, IOException {
		serial(serial, shouldStoreAllInfo(status));
	}
	
	// @see PHPUnit/Util/Log/JUnit.php
	public void serial(XmlSerializer serial, boolean include_all) throws IllegalArgumentException, IllegalStateException, IOException {
		serial.startTag(null, "testcase");
		// count of failures due to assertions
		serial.attribute(null, "name", test_case.methodName);
		serial.attribute(null, "class", test_case.className);
		serial.attribute(null, "file", test_case.filename);
		
		serial.attribute("pftt", "status", status.toString());
		
		switch(status) {
		case NOT_IMPLEMENTED:
		case SKIP:
		case XSKIP:
		case CRASH:
		case ERROR:
		case DEPRECATED:
		case WARNING:
		case NOTICE:
		case BORK:
		case UNSUPPORTED:
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
		default:
			break;
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
		}
		//
		
		serial.endTag(null, "testcase");
	} // end public void serial
	
} // end public class PhpUnitTestResult
