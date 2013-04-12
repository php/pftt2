package com.mostc.pftt.results;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.annotation.Nonnull;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.mostc.pftt.model.core.PhpBuildInfo;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.ui.EUITestStatus;

public class UITestReader extends AbstractUITestRW {
	protected PhpIni ini;

	public UITestReader(File dir, PhpBuildInfo build_info) {
		super(dir, build_info);
		ini = new PhpIni(); // placeholder
	}
	
	public void open(ConsoleManager cm, File dir) {
		try {
			readTally(new File(dir+"/tally.xml"));
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
	}
	
	protected void readTally(File tally_file) throws NumberFormatException, XmlPullParserException, IOException {
		KXmlParser parser = new KXmlParser();
		FileReader fr = new FileReader(tally_file);
		try {
			parser.setInput(fr);
			
			// TODO check counts after reading tally
			int pass, pass_with_warning, fail, fail_with_warning, xfail, crash, skip, test_exception, not_implemented;
			
			String tag_name = "";
			main_loop:
			while(true) {
				parser.next();
				switch(parser.getEventType()) {
				case XmlPullParser.START_TAG:
					tag_name = parser.getName();
					
					if (tag_name.equals("tally")) {
						this.scenario_set_str = parser.getAttributeValue(null, "scenario_set");
						this.test_pack_name_and_version = parser.getAttributeValue(null, "test_pack_name_and_version");
						this.web_browser_name_and_version = parser.getAttributeValue(null, "web_browser_name_and_version");
						
						pass = Integer.parseInt(parser.getAttributeValue(null, "pass"));
						pass_with_warning = Integer.parseInt(parser.getAttributeValue(null, "pass_with_warning"));
						fail = Integer.parseInt(parser.getAttributeValue(null, "fail"));
						fail_with_warning = Integer.parseInt(parser.getAttributeValue(null, "fail_with_warning"));
						xfail = Integer.parseInt(parser.getAttributeValue(null, "xfail"));
						crash = Integer.parseInt(parser.getAttributeValue(null, "crash"));
						skip = Integer.parseInt(parser.getAttributeValue(null, "skip"));
						test_exception = Integer.parseInt(parser.getAttributeValue(null, "test_exception"));
						not_implemented = Integer.parseInt(parser.getAttributeValue(null, "not_implemented"));
					} else if (tag_name.equals("test")) {
						String test_name = parser.getAttributeValue(null, "name");
						EUITestStatus status = EUITestStatus.valueOf(parser.getAttributeValue(null, "status"));
						String comment = parser.getAttributeValue(null, "comment");
						
						UITestResult result = new UITestResult(test_name, comment, status, null);
						
						results_by_name.put(test_name, result);
						results_by_status.get(status).add(result);
					}
					
					break;
				case XmlPullParser.END_TAG:
					break main_loop;
				case XmlPullParser.END_DOCUMENT:
					break main_loop;
				case XmlPullParser.TEXT:
					
					if (tag_name.equals("ini")) {
						this.ini = new PhpIni(parser.getText());
					} else if (tag_name.equals("SAPIConfig")) {
						
					} else if (tag_name.equals("SAPIOutput")) {
						
					} else if (tag_name.equals("notes")) {
						this.notes = parser.getText();
					}
					
					break;
				default:
				} // end switch
			} // end while
			
		} finally {
			fr.close();
		}
	} // end protected void readTally

	@Override
	public String getHTMLURL(String test_name) {
		// @see UITestWriter#addResult
		return dir+File.separator+test_name+".html";
	}
	
	@Override
	public String getOSName() {
		return os_name;
	}

	@Override
	public String getNotes() {
		return notes;
	}

	@Override
	@Nonnull
	public PhpIni getPhpIni() {
		return ini;
	}

} // end public class UITestReader
