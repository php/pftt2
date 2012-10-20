package com.github.mattficken.io;

import java.io.IOException;
import java.io.InputStream;

public class NoCharsetByChunkReader extends AbstractNoDetectingReader implements ByChunkReader {
	protected byte[] buf;
	
	public NoCharsetByChunkReader(InputStream in) {
		this(in, DEFAULT_READ_SIZE);
	}
	
	public NoCharsetByChunkReader(InputStream in, int buf_size) {
		super(in);
		this.buf = new byte[buf_size];
	}
	
	@Override
	public int readChunk(char[] chars, int off, int len) throws IOException {
		if (this.buf.length < len)
			// buf can't be reused, replace it
			this.buf = new byte[len];
		
		len = in.read(this.buf, 0, len);
		
		int i = 0;
		for ( ; i < len ; i++, off++ )
			chars[off] = (char) this.buf[i];
		
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
