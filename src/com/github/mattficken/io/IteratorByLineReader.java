package com.github.mattficken.io;

import java.util.Iterator;

public class IteratorByLineReader implements ByLineReader {
	protected final Iterator<String> it;
	
	public IteratorByLineReader(Iterable<String> it) {
		this.it = it.iterator();
	}
	
	public IteratorByLineReader(Iterator<String> it) {
		this.it = it;
	}
	
	@Override
	public void close() {
		// N/A
	}
	
	@Override
	public boolean hasMoreLines() {
		return it.hasNext();
	}

	@Override
	public String readLine() {
		return it.next();
	}

	@Override
	public ECharsetDetectionStyle getCharsetDetectionStyle() {
		return ECharsetDetectionStyle.NONE;
	}

	@Override
	public EReadStyle getReadStyle() {
		return EReadStyle.LINE;
	}

}
