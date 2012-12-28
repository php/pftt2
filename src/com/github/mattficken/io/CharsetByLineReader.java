package com.github.mattficken.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class CharsetByLineReader extends BufferedReader implements ByLineReader {
	protected boolean eof = false;

	public CharsetByLineReader(InputStream in, Charset cs) {
		super(new InputStreamReader(in, cs));
	}

	@Override
	public ECharsetDetectionStyle getCharsetDetectionStyle() {
		return ECharsetDetectionStyle.NONE;
	}

	@Override
	public EReadStyle getReadStyle() {
		return EReadStyle.LINE;
	}

	@Override
	public boolean hasMoreLines() {
		return !eof;
	}
	
	@Override
	public String readLine() throws IOException {
		String line = super.readLine();
		if (line==null)
			eof = true;
		return line;
	}

}
