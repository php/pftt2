package com.mostc.pftt.results;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.app.EPhpUnitTestStatus;
import com.mostc.pftt.model.app.PhpUnitTestCase;
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
	
	public PhpUnitTestResult(PhpUnitTestCase test_case, EPhpUnitTestStatus status, ScenarioSet scenario_set, Host host, String output) {
		this.test_case = test_case;
		this.status = status;
		this.scenario_set = scenario_set;
		this.host = host;
		this.output = output;
	}
	
	public String getName() {
		return test_case.getName();
	}
	
	// @see PHPUnit/Util/Log/JUnit.php
	public void serial(XmlSerializer serial) throws IllegalArgumentException, IllegalStateException, IOException {
		serial.startTag(null, "testcase");
		// count of failures due to assertions
		// TODO assertions
		/* TODO test_case.getName();
		test_case.getClass();
		test_case.getFileName();
		name
		class
		file*/
		switch(status) {
		case NOT_IMPLEMENTED:
		case SKIP:
		case XSKIP:
		case CRASH:
		case ERROR:
		case TEST_EXCEPTION:
			// @see #addIncompleteTest and #addSkippedTest and #addError
			serial.startTag(null, "error");
			serial.attribute(null, "type", ""); // TODO
			serial.text(output);
			serial.endTag(null, "error");
			break;
		case FAILURE:
			// @see #addFailure
			serial.startTag(null, "failure");
			serial.attribute(null, "type", ""); // TODO
			serial.text(output);
			serial.endTag(null, "failure");
			break;
		}
		serial.endTag(null, "testcase");
	} // end public void serial
	
} // end public class PhpUnitTestResult
