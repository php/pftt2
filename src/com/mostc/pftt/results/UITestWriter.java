package com.mostc.pftt.results;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.kxml2.io.KXmlSerializer;
import org.xmlpull.v1.XmlSerializer;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.PhpBuildInfo;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.ui.EUITestStatus;

@NotThreadSafe
public class UITestWriter extends AbstractUITestRW {
	protected final AHost host;
	protected XmlSerializer serial;
	protected BufferedOutputStream out;
	protected final HashMap<EUITestStatus,Integer> count;
	
	public UITestWriter(AHost host, File dir, PhpBuildInfo build_info, String scenario_set_str, String test_pack_name_and_version, String web_browser_name_and_version, String notes) throws IllegalArgumentException, IllegalStateException, FileNotFoundException, IOException {
		super(dir, build_info);
		this.scenario_set_str = scenario_set_str;
		this.test_pack_name_and_version = test_pack_name_and_version;
		this.web_browser_name_and_version = web_browser_name_and_version;
		this.notes = notes;
		this.host = host;
		
		dir.mkdirs();
		
		count = new HashMap<EUITestStatus,Integer>();
		for ( EUITestStatus status : EUITestStatus.values() )
			count.put(status, 0);
		
		serial  = new KXmlSerializer();
		
		serial.setOutput(out = new BufferedOutputStream(new FileOutputStream(new File(dir+"/tally.xml"))), null);
		
		// setup serializer to indent XML (pretty print) so its easy for people to read
		serial.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
	}

	@Override
	public void addResult(String test_name, String comment, EUITestStatus status) {
		addResult(test_name, comment, status, null, null, null, null);
	}
	
	public void addResult(String test_name, String comment, EUITestStatus status, String verified_html, byte[] screenshot_png, String sapi_output, String sapi_config) {
		if (screenshot_png!=null) {
			try {
				// record screenshot
				FileOutputStream fout = new FileOutputStream(new File(getScreenshotFilename(test_name)));
				fout.write(screenshot_png);
				fout.close();
			} catch ( Throwable t ) {
				t.printStackTrace();
			}
		}
		
		// make sure name is unique
		/* TODO dead if (hasTestNamed(test_name)) {
			for ( int i=2 ; i < 100 ; i++ ) {
				if (!hasTestNamed(test_name+"-"+i)) {
					test_name = test_name + "-" + i;
					break;
				}
			}
		} */
		//
		
		switch(status) {
		case FAIL:
		case FAIL_WITH_WARNING:
		case PASS_WITH_WARNING:
		case TEST_EXCEPTION:
		case SKIP:
			// record html
			if (verified_html!=null) {
				try {
					FileOutputStream fout = new FileOutputStream(getHTMLURL(test_name));
					fout.write(verified_html.getBytes());
					fout.close();
				} catch ( IOException ex ) {
					ex.printStackTrace();
				}
			}
			break;
		default:
			break;
		}
		// write to xml file
		
		// increment
		count.put(status, count.get(status) + 1);
		
		super.addResult(test_name, comment, status);
		
		try {
			serial.startTag(null, "test");
			serial.attribute(null, "name", test_name);
			serial.attribute(null, "status", status.toString());
			if (StringUtil.isNotEmpty(comment)) {
				serial.attribute(null, "comment", comment);
			}
			if (StringUtil.isNotEmpty(sapi_config)) {
				serial.startTag(null, "SAPIConfig");
				serial.text(sapi_config);
				serial.endTag(null, "SAPIConfig");
			}
			if (StringUtil.isNotEmpty(sapi_output)) {
				serial.startTag(null, "SAPIOutput");
				serial.text(sapi_output);
				serial.endTag(null, "SAPIOutput");
			}
			serial.endTag(null, "test");
		} catch ( IOException ex ) {
			ex.printStackTrace();
		}
	} // end public void addResult
	
	public void close() throws IllegalArgumentException, IllegalStateException, IOException {
		serial.startTag(null, "tally");
		serial.attribute(null, "scenario_set", scenario_set_str);
		serial.attribute(null, "test_pack_name_and_version", test_pack_name_and_version);
		serial.attribute(null, "web_browser_name_and_version", web_browser_name_and_version);
		serial.attribute(null, "pass", Integer.toString(count.get(EUITestStatus.PASS)));
		serial.attribute(null, "pass_with_warning", Integer.toString(count.get(EUITestStatus.PASS_WITH_WARNING)));
		serial.attribute(null, "fail", Integer.toString(count.get(EUITestStatus.FAIL)));
		serial.attribute(null, "fail_with_warning", Integer.toString(count.get(EUITestStatus.FAIL_WITH_WARNING)));
		serial.attribute(null, "xfail", Integer.toString(count.get(EUITestStatus.XFAIL)));
		serial.attribute(null, "crash", Integer.toString(count.get(EUITestStatus.CRASH)));
		serial.attribute(null, "skip", Integer.toString(count.get(EUITestStatus.SKIP)));
		serial.attribute(null, "test_exception", Integer.toString(count.get(EUITestStatus.TEST_EXCEPTION)));
		serial.attribute(null, "not_implemented", Integer.toString(count.get(EUITestStatus.NOT_IMPLEMENTED)));
		serial.endTag(null, "tally");	
		out.close(); 
	}

	@Override
	public String getOSName() {
		return host.getOSNameLong();
	}

	@Override
	public String getNotes() {
		return notes;
	}

	public void addNotes(String notes) {
		this.notes = notes;

		try {
			serial.startTag(null, "notes");
			serial.text(notes);
			serial.endTag(null, "notes");
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
	}

	@Override
	@Nonnull
	public PhpIni getPhpIni() {
		return new PhpIni();
	}
	
} // end public class UITestWriter
