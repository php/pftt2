package com.github.mattficken.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public abstract class AbstractReader implements CharacterReader {
	protected final InputStream in;
	protected boolean has_more;
	
	public AbstractReader(InputStream in) {
		this.in = in;
		has_more = true;
	}
	
	@Override
	public void close() throws IOException {
		in.close();
	}
	
	public Iterator<Character> toIterator(final ByCharReader r) {
		return new Iterator<Character>() {
				@Override
				public boolean hasNext() {
					return r.hasMoreChars();
				}
				@Override
				public Character next() {
					try {
						return r.readChar();
					} catch ( IOException ex ) {
						return END_OF_STREAM_CHAR;
					}
				}
				@Override
				public void remove() {
				}			
			};
	}
	public Iterator<String> toIterator(final ByChunkReader r) {
		return new Iterator<String>() {
				@Override
				public boolean hasNext() {
					return r.hasMoreChunks();
				}
				@Override
				public String next() {
					char[] chars = new char[1024];
					int len = 0;
					try {
						len = r.readChunk(chars, 0, chars.length);
					} catch ( IOException ex ) {						
					}
					return new String(chars, 0, len);
				}
				@Override
				public void remove() {
				}			
			};
	}
	public Iterator<String> toIterator(final ByLineReader r) {
		return new Iterator<String>() {
				@Override
				public boolean hasNext() {
					return r.hasMoreLines();
				}
				@Override
				public String next() {
					try {
						return r.readLine();
					} catch ( IOException ex ) {
						return null;
					}
				}
				@Override
				public void remove() {
				}			
			};
	}
	
	public Iterable<Character> toIterable(final ByCharReader r) {
		return new Iterable<Character>() {
				@Override
				public Iterator<Character> iterator() {
					return toIterator(r);
				}
			};
	}
	public Iterable<String> toIterable(final ByChunkReader r) {
		return new Iterable<String>() {
				@Override
				public Iterator<String> iterator() {
					return toIterator(r);
				}
			};
	}
	public Iterable<String> toIterable(final ByLineReader r) {
		return new Iterable<String>() {
				@Override
				public Iterator<String> iterator() {
					return toIterator(r);
				}
			};
	}
	
}
