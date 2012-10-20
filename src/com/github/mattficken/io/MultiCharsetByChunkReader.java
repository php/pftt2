package com.github.mattficken.io;

import java.io.IOException;
import java.io.InputStream;

public class MultiCharsetByChunkReader extends AbstractMultiCharsetReader implements ByChunkReader {
	protected byte[] buf;
	
	public MultiCharsetByChunkReader(InputStream in, CharsetDeciderDecoder cdd) {
		this(in, cdd, DEFAULT_LINE_SIZE);
	}
	
	public MultiCharsetByChunkReader(InputStream in,  CharsetDeciderDecoder cdd, int buf_size) {
		super(in, cdd);
		this.buf = new byte[buf_size];
	}
	
	@Override
	public int readChunk(char[] chars, int coff, int clen) throws IOException {
		if (this.buf.length < clen)
			// buf can't be reused, replace it
			this.buf = new byte[clen];
				
		int need_len = clen; // initially, assume chars==bytes
		int bbuf_off = 0, bbuf_len = 0;
		int blen;
		for(;;) {
			System.out.println("28 "+this.buf.length+" "+bbuf_off+" "+need_len);
			blen = in.read(this.buf, bbuf_off, need_len);
			
			if (blen!=-1)
				bbuf_len += blen;
			
			detectCharset(this.buf, 0, bbuf_len);
			
			convertChars(this.buf, 0, bbuf_len, chars, coff, clen, false);
						
			// if end of stream not reached and more bytes are needed
			if (blen != -1 && cnv_clen < clen) {
				// read more bytes and use all read bytes (from all iterations) to do detection and conversion
				bbuf_off += cnv_blen;
				
				// guess how many more bytes to read (may need more, but will never need less)
				need_len = clen - cnv_clen;
				
				if (this.buf.length<bbuf_len+need_len) {
					// ensure buffer is large enough
					byte[] new_buf = new byte[bbuf_len+(2*need_len)];
					System.arraycopy(this.buf, 0, new_buf, 0, bbuf_len);
					this.buf = new_buf;
				}
				
				if (bbuf_off+need_len<this.buf.length)
					continue;
			}
			// read enough bytes || read to end of stream
			break;
		}
		
		// count from (only) the most recent call to #convertChars
		return cnv_clen;
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
