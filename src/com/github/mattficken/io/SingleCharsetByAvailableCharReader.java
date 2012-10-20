package com.github.mattficken.io;

import java.io.IOException;
import java.io.InputStream;

public class SingleCharsetByAvailableCharReader extends AbstractSingleCharsetAvailableReader implements ByCharReader {
	protected byte[] bbuf;
	protected int bbuf_off, bbuf_len;
	protected char[] cbuf;
	protected boolean first;
	
	public SingleCharsetByAvailableCharReader(InputStream in, CharsetDeciderDecoder cdd) {
		super(in, cdd);
		first = true;
		bbuf = new byte[32];
		cbuf = new char[1];
	}

	@Override
	public boolean hasMoreChars() {
		return has_more;
	}
	
	@Override
	public char readChar() throws IOException {
		int read_len = 1;
		while (has_more) {
			// read in all available bytes
			if (first || read_len > bbuf_len) {
				first = false;
				// 1 byte must always be considered/required to be available or there'll be nothing to decode
				bbuf_len = Math.max(in.available(), 1);
				if (bbuf_len > bbuf.length)
					bbuf = new byte[bbuf_len];
				
				bbuf_len = in.read(bbuf, 0, bbuf_len);
				if (bbuf_len==-1) {
					has_more = false;
					return END_OF_STREAM_CHAR;
				}
				read_len = 0;
				bbuf_off = 0;
				
				detectCharset(bbuf, 0, bbuf_len);
			}
			
			
			
						
			if (bbuf_off>=bbuf.length) {
				has_more = false;
				break;
			}
			
			convertChars(bbuf, bbuf_off, bbuf_off+read_len, cbuf, 0, 1, false);
			
			// convert byte(s) for one char
			if (cnv_clen<1) {//false) {//convertChars(bbuf, bbuf_off, read_len+bbuf_off, cbuf, 0, 1)==NEED_MORE) {
				//System.out.println("57");.
				read_len++;
				continue;
			} else {
				//System.out.println("60");
				bbuf_off += read_len;
				return cbuf[0];
			}
		} // end for
		//System.out.println("65");
		return END_OF_STREAM_CHAR;
	}
	
	@Override
	public EReadStyle getReadStyle() {
		return EReadStyle.CHAR;
	}

}
