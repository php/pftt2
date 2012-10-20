package com.github.mattficken.io;

import java.io.IOException;
import java.io.InputStream;

public class NoCharsetByLineReader extends AbstractNoDetectingReader implements ByLineReader {
	protected final byte[] bbuf;
	protected char[] cbuf;
	protected int bbuf_off, bbuf_len, cbuf_len;
	
	public NoCharsetByLineReader(InputStream in) {
		this(in, DEFAULT_READ_SIZE, DEFAULT_LINE_SIZE);
	}
	
	public NoCharsetByLineReader(InputStream in, int read_size, int line_size) {
		super(in);
		this.bbuf = new byte[read_size];
		this.cbuf = new char[line_size];
	}

	@Override
	public EReadStyle getReadStyle() {
		return EReadStyle.LINE;
	}
	
	private final String getString() {
		String str = new String(cbuf, 0, cbuf_len);
		cbuf_len = 0;
		return str;
	}
	
	@Override
	public boolean hasMoreLines() {
		return has_more;
	}

	@Override
	public String readLine() throws IOException {
		int i;
		for(;;) {
			for ( i=bbuf_off ; i < bbuf_len ; i++ ) {
				if (bbuf[i]=='\r' && i+1 < bbuf_len && bbuf[i+1] == '\n') {
					continue; // ignore \r
				} else if (bbuf[i]=='\n') {
					String str = getString();
					bbuf_off = i+1;
					return str;
				} else if (cbuf_len >= cbuf.length) {
					// resize array
					char[] new_cbuf = new char[cbuf_len * 2];
					System.arraycopy(cbuf, 0, new_cbuf, 0, cbuf_len);
					cbuf = new_cbuf;
				}
				cbuf[cbuf_len] = (char) bbuf[i];
				cbuf_len++;
			}
			bbuf_len = in.read(bbuf);
			
			if (bbuf_len==-1)  {
				has_more = false;
				return getString();
			}
			
			bbuf_off = 0;
		}
	} // end public String readLine

}
