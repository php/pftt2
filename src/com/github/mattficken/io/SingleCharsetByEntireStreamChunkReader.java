package com.github.mattficken.io;

import java.io.IOException;
import java.io.InputStream;

public class SingleCharsetByEntireStreamChunkReader extends AbstractSingleCharsetEntireStreamReader implements ByChunkReader {
	protected byte[] bbuf;
	protected int bbuf_off;
	
	public SingleCharsetByEntireStreamChunkReader(InputStream in, CharsetDeciderDecoder cdd) {
		this(in, cdd, DEFAULT_READ_SIZE);
	}
	
	public SingleCharsetByEntireStreamChunkReader(InputStream in, CharsetDeciderDecoder cdd, int buf_size) {
		super(in, cdd);
	}
	
	@Override
	public int readChunk(char[] chars, int coff, int clen) throws IOException {
		if (this.bbuf==null) {
			this.bbuf = IOUtil.toBytes(this.in, IOUtil.HALF_MEGABYTE);
		
			detectCharset(this.bbuf, 0, this.bbuf.length);
		}
		
		char[] cbuf = new char[1];
		
		int i=0;
		for ( ; i < clen && bbuf_off < this.bbuf.length ; i++, coff++, bbuf_off+=cnv_blen ) {
			convertChars(this.bbuf, bbuf_off, this.bbuf.length, cbuf, 0, 1, false);
			
			chars[coff] = cbuf[0];
		}
		
		return i;
	}
		
	@Override
	public boolean hasMoreChunks() {
		return has_more;
	}

	@Override
	public EReadStyle getReadStyle() {
		return EReadStyle.CHUNK;
	}

}
