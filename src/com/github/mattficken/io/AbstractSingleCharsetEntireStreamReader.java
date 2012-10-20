package com.github.mattficken.io;

import java.io.InputStream;

public abstract class AbstractSingleCharsetEntireStreamReader extends AbstractDetectingCharsetReader {
	protected boolean first;
	
	public AbstractSingleCharsetEntireStreamReader(InputStream in, CharsetDeciderDecoder cdd) {
		super(in, cdd);
		first = true;
	}
	
	@Override
	public ECharsetDetectionStyle getCharsetDetectionStyle() {
		return ECharsetDetectionStyle.SINGLE_CHARSET_BY_ENTIRE_STREAM;
	}

}
