package com.github.mattficken.io;

import java.io.IOException;
import java.io.InputStream;

public class SingleCharsetByEntireStreamCharReader extends AbstractSingleCharsetEntireStreamReader implements ByCharReader {
	protected byte[] bbuf;
	protected int bbuf_off;
	
	public SingleCharsetByEntireStreamCharReader(InputStream in, CharsetDeciderDecoder cdd) {
		super(in, cdd);
	}

	@Override
	public boolean hasMoreChars() {
		return has_more;
	}
	
	char[] cbuf = new char[1];
	@Override
	public char readChar() throws IOException {
		if (first) {
			first = false;
			
			bbuf = IOUtil.toBytes(in, IOUtil.HALF_MEGABYTE);
			
			detectCharset(bbuf, 0, bbuf.length);
		}
		
		convertChars(bbuf, bbuf_off, bbuf.length, cbuf, 0, 1, bbuf_off+1>bbuf.length);
	
		bbuf_off += cnv_blen;
	
		has_more = bbuf_off < bbuf.length;
		
		return cbuf[0];
	}
	
	@Override
	public EReadStyle getReadStyle() {
		return EReadStyle.CHAR;
	}

}
