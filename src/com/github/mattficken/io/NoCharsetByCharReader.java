package com.github.mattficken.io;

import java.io.IOException;
import java.io.InputStream;

public class NoCharsetByCharReader extends AbstractNoDetectingReader implements ByCharReader {
	
	public NoCharsetByCharReader(InputStream in) {
		super(in);
	}

	@Override
	public boolean hasMoreChars() {
		return has_more;
	}
	
	@Override
	public char readChar() throws IOException {
		int b = in.read();
		has_more = b != -1;
		return (char) b;
	}
	
	@Override
	public EReadStyle getReadStyle() {
		return EReadStyle.CHAR;
	}

}
