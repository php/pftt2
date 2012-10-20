package com.github.mattficken.io;

import java.io.InputStream;

public abstract class AbstractNoDetectingReader extends AbstractReader {
	
	public AbstractNoDetectingReader(InputStream in) {
		super(in);
	}
	
	@Override
	public ECharsetDetectionStyle getCharsetDetectionStyle() {
		return ECharsetDetectionStyle.NONE;
	}
	
}
