package com.mostc.pftt.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

/** GZIP support for CliTestCaseRunner (GZIP_POST section, etc...)
 * 
 * @author Matt Ficken
 * 
 */

public class GZIPOutputStreamLevel extends GZIPOutputStream {

	public GZIPOutputStreamLevel(OutputStream out, int level) throws IOException {
		super(out);
		this.def.setLevel(level);
	}

}
