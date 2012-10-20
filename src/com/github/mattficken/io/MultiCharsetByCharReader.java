package com.github.mattficken.io;

import java.io.IOException;
import java.io.InputStream;

public class MultiCharsetByCharReader extends AbstractMultiCharsetReader implements ByCharReader {
	protected byte[] bbuf;
	protected char[] cbuf;
	
	public MultiCharsetByCharReader(InputStream in, CharsetDeciderDecoder cdd) {
		super(in, cdd);
		bbuf = new byte[16];
		cbuf = new char[1];
	}

	@Override
	public boolean hasMoreChars() {
		return has_more;
	}
	
	@Override
	public char readChar() throws IOException {
		int read_len = 1;
		int bbuf_off = 0;
		for (;;) {
			if (in.read(bbuf, bbuf_off, 1)==-1) {
				has_more = false;
				return END_OF_STREAM_CHAR;
			}
			
			detectCharset(bbuf, 0, read_len);
					
			convertChars(bbuf, 0, read_len, cbuf, 0, 1, false);
			
			if (cnv_clen>0) {
				// 
				break;
			} else if (read_len>3) {
				// too many bytes to read
				return END_OF_STREAM_CHAR;
			} else {
				// keep reading bytes
				read_len++;
				bbuf_off++;
			}
		} // end for
		
		return cbuf[0];
	}
	
	@Override
	public EReadStyle getReadStyle() {
		return EReadStyle.CHAR;
	}

}
