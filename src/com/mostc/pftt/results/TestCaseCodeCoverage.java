package com.mostc.pftt.results;

import java.io.IOException;
import java.util.HashMap;

import org.xmlpull.v1.XmlSerializer;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;

public class TestCaseCodeCoverage {
	
	public static enum ELineState {
			EXECUTED,
			NOT_EXECUTED,
			NOT_EXECUTABLE,
			UNKNOWN
		};
		
	protected final HashMap<String,FileInfo> file_map;
	protected final AHost host;
	protected final String file_root;
	protected int class_count, method_count, line_count, class_exe, method_exe, line_exe;
	
	public TestCaseCodeCoverage(AHost host, String file_root) {
		this.host = host;
		this.file_root = file_root;
		file_map = new HashMap<String,FileInfo>();
	}
	
	public TestCaseCodeCoverage(AHost host) {
		this(host, null);
	}
	
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
	
	protected static class FileInfo {
		protected final HashMap<Integer,ELineState> line_map;
		protected final String php_code;
		
		public FileInfo(String php_code) {
			this.php_code = php_code;
			line_map = new HashMap<Integer,ELineState>();
		}
	}
	
	public void setLineState(String filename, int line_num, ELineState stat) {
		FileInfo file_info = file_map.get(filename);
		if (file_info==null) {
			file_info = new FileInfo(host == null ? null : host.readFileAsStringEx(filename));
			
			file_map.put(file_root == null ? filename : AHost.pathFrom(file_root, filename), file_info);
		} else {
			file_info.line_map.put(line_num, stat);
		}
	}
	
	public ELineState getLineState(String filename, int line_num) {
		FileInfo file_info = file_map.get(filename);
		if (file_info==null)
			return ELineState.UNKNOWN;
		ELineState state = file_info.line_map.get(line_num);
		return state == null ? ELineState.UNKNOWN : state;
	}
	
	public boolean isExecuted(String filename, int line_num) {
		return getLineState(filename, line_num) == ELineState.EXECUTED;
	}
	
	public String getPhpCode(String filename) {
		FileInfo file_info = file_map.get(filename);
		return file_info == null ? null : file_info.php_code;
	}

	public void addExecutedLine(String filename, int line_num) {
		setLineState(filename, line_num, ELineState.EXECUTED);
	}
	
	public void addNotExecutedLine(String filename, int line_num) {
		setLineState(filename, line_num, ELineState.NOT_EXECUTED);
	}
	
	public void addNonExecutableLine(String filename, int line_num) {
		setLineState(filename, line_num, ELineState.NOT_EXECUTABLE);
	}

	public void serial(XmlSerializer serial) throws IllegalArgumentException, IllegalStateException, IOException {
		// TODO XSL stylesheet
		serial.startTag("pftt", "codeCoverage");
		serial.attribute("pftt", "countExecutableClasses", Integer.toString(getTotalClassCount()));
		serial.attribute("pftt", "countExecutableMethods", Integer.toString(getTotalMethodCount()));
		serial.attribute("pftt", "countExecutableLines", Integer.toString(getTotalLineCount()));
		serial.attribute("pftt", "countExecutedClasses", Integer.toString(getExecutedClassCount()));
		serial.attribute("pftt", "countExecutedMethods", Integer.toString(getExecutedMethodCount()));
		serial.attribute("pftt", "countExecutedLines", Integer.toString(getExecutedLineCount()));
		
		
		for ( String filename : file_map.keySet() ) {
			FileInfo file_info = file_map.get(filename);
			serial.startTag("pftt", "file");
			serial.attribute("pft", "filename", filename);
			String[] lines = StringUtil.splitLines(file_info.php_code);
			for ( int line_num : file_info.line_map.keySet() ) {
				String line_tag;
				switch(file_info.line_map.get(line_num)) {
				case EXECUTED:
					line_tag = "exeLine";
					break;
				case NOT_EXECUTED:
					line_tag = "notExeLine";
					break;
				case NOT_EXECUTABLE:
					line_tag = "notExecutable";
					break;
				default:
					line_tag = "unknownLine";
					break;
				}
				serial.startTag("pftt", line_tag);
				serial.attribute("pftt", "line_num", Integer.toString(line_num));
				if (line_num < lines.length)
					serial.text(lines[line_num]);
				serial.endTag("pftt", line_tag);
			}
			serial.endTag("pftt", "file");
		}
		
		serial.endTag("pftt", "codeCoverage");
	}
	
} // end public class TestCaseCodeCoverage
