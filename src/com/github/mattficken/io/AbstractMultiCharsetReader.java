package com.github.mattficken.io;

import java.io.InputStream;

public abstract class AbstractMultiCharsetReader extends AbstractDetectingCharsetReader {

	public AbstractMultiCharsetReader(InputStream in, CharsetDeciderDecoder cdd) {
		super(in, cdd);
	}
	
	@Override
	public ECharsetDetectionStyle getCharsetDetectionStyle() {
		return ECharsetDetectionStyle.MULTI_CHARSET;
	}

}
