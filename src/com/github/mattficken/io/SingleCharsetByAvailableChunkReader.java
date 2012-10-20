package com.github.mattficken.io;

import java.io.IOException;
import java.io.InputStream;

public class SingleCharsetByAvailableChunkReader extends AbstractSingleCharsetAvailableReader implements ByChunkReader {
	protected byte[] buf;
	
	public SingleCharsetByAvailableChunkReader(InputStream in, CharsetDeciderDecoder cdd) {
		this(in, cdd, DEFAULT_READ_SIZE);
	}
	
	public SingleCharsetByAvailableChunkReader(InputStream in, CharsetDeciderDecoder cdd, int buf_size) {
		super(in, cdd);
		this.buf = new byte[buf_size];
	}
	
	@Override
	public int readChunk(char[] chars, int coff, int clen) throws IOException {
		if (this.buf.length < clen)
			// buf can't be reused, replace it
			this.buf = new byte[clen];
		
		boolean eof = false;
		int blen = 0, cclen = 0;
		do {
			// count of bytes >= count of chars (one char will always be represented by 1+ bytes)
			if (!eof)
				blen = in.read(this.buf, cclen, clen-cclen);
			
			
			if (blen==-1) {
				eof = true;
			} else {
				// here's the DIFFERENCE with MultiCharsetByChunkReader
				detectCharset(this.buf, 0, blen);
							
				convertChars(this.buf, 0, blen, chars, coff, clen, !(has_more && cclen < clen)); // sets cnv_clen and cnv_blen
				
				cclen += Math.max(0, cnv_clen);
			}
			
			// keep running loop until enough chars are converted until chars[] filled or end of stream
		} while ( has_more && cclen < clen );
		
		return cclen;
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
