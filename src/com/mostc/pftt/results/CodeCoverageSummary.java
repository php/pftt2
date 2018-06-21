package com.mostc.pftt.results;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.kxml2.io.KXmlSerializer;
import org.xmlpull.v1.XmlSerializer;

import com.mostc.pftt.scenario.ScenarioSet;

public class CodeCoverageSummary implements ISerializer {
	protected final HashMap<ScenarioSet,ScenarioSetCoverage> cov_map;
	protected final String test_pack_name_and_version;

	public CodeCoverageSummary(String test_pack_name_and_version) {
		this.test_pack_name_and_version = test_pack_name_and_version;
		cov_map = new HashMap<ScenarioSet,ScenarioSetCoverage>();
	}
	
	public class ScenarioSetCoverage {
		protected int class_count, method_count, line_count, class_exe, method_exe, line_exe;
		
		public int getTotalClassCount() {
			return class_count;
		}
		public int getTotalMethodCount() {
			return method_count;
		}
		public int getTotalLineCount() {
			return line_count;
		}
		public int getExecutedClassCount() {
			return class_exe;
		}
		public int getExecutedMethodCount() {
			return method_exe;
		}
		public int getExecutedLineCount() {
			return line_exe;
		}
		
		public void add(TestCaseCodeCoverage cc) {
			class_count += cc.getTotalClassCount();
			class_exe += cc.getExecutedClassCount();
			method_count += cc.getTotalMethodCount();
			method_exe += cc.getExecutedMethodCount();
			line_count += cc.getTotalLineCount();
			line_exe += cc.getExecutedLineCount();
		}
		
		public float classCoverage() {
			return 100.0f * ((float)class_exe) / ((float)class_count);
		}
		
		public float methodCoverage() {
			return 100.0f * ((float)method_exe) / ((float)method_count);
		}
		
		public float lineCoverage() {
			return 100.0f * ((float)line_exe) / ((float)line_count);
		}
	}
	
	public void addTestCase(ScenarioSet scenario_set, TestCaseCodeCoverage cc) {
		ScenarioSetCoverage cov = cov_map.get(scenario_set);
		if (cov==null) {
			cov = new ScenarioSetCoverage();
			cov_map.put(scenario_set, cov);
		}
		
		cov.add(cc);
	}
	
	@Override
	public void serial(XmlSerializer serial) throws IllegalArgumentException, IllegalStateException, IOException {
		ScenarioSetCoverage cov;
		serial.startTag("pftt", "codeCoverageSummary");
		for ( ScenarioSet scenario_set : cov_map.keySet() ) {
			cov = cov_map.get(scenario_set);
			
			serial.startTag("pftt", "scenarioSetCoverage");
			serial.attribute("pftt", "scenario_set", scenario_set.getName());
			serial.attribute("pftt", "classCoveragePercent", Float.toString(cov.classCoverage()));
			serial.attribute("pftt", "methodCoveragePercent", Float.toString(cov.methodCoverage()));
			serial.attribute("pftt", "lineCoveragePercent", Float.toString(cov.lineCoverage()));
			serial.attribute("pftt", "totalClassCount", Integer.toString(cov.getTotalClassCount()));
			serial.attribute("pftt", "totalMethodCount", Integer.toString(cov.getTotalMethodCount()));
			serial.attribute("pftt", "totalLineCount", Integer.toString(cov.getTotalLineCount()));
			serial.attribute("pftt", "executedClassCount", Integer.toString(cov.getExecutedClassCount()));
			serial.attribute("pftt", "executedMethodCount", Integer.toString(cov.getExecutedMethodCount()));
			serial.attribute("pftt", "executedLineCount", Integer.toString(cov.getExecutedLineCount()));
			serial.endTag("pftt", "scenarioSetCoverage");
		}
		serial.endTag("pftt", "codeCoverageSummary");
	} // end public void serial
	
	public void close(File dir) {
		try {
			KXmlSerializer serial = new KXmlSerializer();
			serial.setOutput(new FileWriter(new File(dir, "code_coverage.xml")));
			
			// setup serializer to indent XML (pretty print) so its easy for people to read
			serial.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
			serial.startDocument("utf-8", Boolean.TRUE);
			serial(serial);
			serial.endDocument();
		} catch ( Exception ex ) {
			ConsoleManagerUtil.printStackTrace(CodeCoverageSummary.class, ex);
		}
	}
	
} // end public class CodeCoverageSummary
