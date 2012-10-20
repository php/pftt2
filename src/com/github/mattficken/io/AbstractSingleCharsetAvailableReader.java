package com.github.mattficken.io;

import java.io.InputStream;

public abstract class AbstractSingleCharsetAvailableReader extends AbstractDetectingCharsetReader {

	public AbstractSingleCharsetAvailableReader(InputStream in, CharsetDeciderDecoder cdd) {
		super(in, cdd);
	}
	
	@Override
	public ECharsetDetectionStyle getCharsetDetectionStyle() {
		return ECharsetDetectionStyle.SINGLE_CHARSET_BY_AVAILABLE_STREAM;
	}

}
