package com.github.mattficken.io;

import java.io.IOException;
import java.io.InputStream;

public class SingleCharsetByEntireStreamLineReader extends AbstractSingleCharsetEntireStreamReader implements ByLineReader {
	protected byte[] bbuf;
	protected byte[] line_buf;
	protected int bbuf_off, bbuf_len, line_buf_len;
	
	public SingleCharsetByEntireStreamLineReader(InputStream in, CharsetDeciderDecoder cdd) {
		this(in, cdd, DEFAULT_READ_SIZE, DEFAULT_LINE_SIZE);
	}
	
	public SingleCharsetByEntireStreamLineReader(InputStream in,  CharsetDeciderDecoder cdd, int read_size, int line_size) {
		super(in, cdd);
		this.bbuf = new byte[read_size];
		this.line_buf = new byte[line_size];
	}

	@Override
	public EReadStyle getReadStyle() {
		return EReadStyle.LINE;
	}
	
	private final String getString(boolean end_of_input) {
		String str = convertLine(line_buf, 0, line_buf_len, end_of_input);
		line_buf_len = 0;
		return str;
	}
	
	@Override
	public boolean hasMoreLines() {
		return has_more;
	}

	@Override
	public String readLine() throws IOException {
		if (first) {
			first = false;
			
			// read entire stream
			bbuf = IOUtil.toBytes(in, IOUtil.HALF_MEGABYTE);
			bbuf_off = 0;
			bbuf_len = bbuf.length;					
			
			// detect character set
			detectCharset(bbuf, bbuf_off, bbuf_len);
		}
		
		for ( int i=bbuf_off ; i < bbuf_len ; i++ ) {
			if (bbuf[i]=='\r' && i+1 < bbuf_len && bbuf[i+1] == '\n') {
				continue; // ignore \r
			} else if (bbuf[i]=='\n') {
				String str = getString(false);
				bbuf_off = i+1;
				return str;
			} else if (line_buf_len >= line_buf.length) {
				// resize array
				byte[] new_cbuf = new byte[line_buf_len * 2];
				System.arraycopy(line_buf, 0, new_cbuf, 0, line_buf_len);
				line_buf = new_cbuf;
			}
			line_buf[line_buf_len] = bbuf[i];
			line_buf_len++;
		}
		
		has_more = false;
		return getString(true);
	} // end public String readLine

}
