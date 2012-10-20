package com.github.mattficken.io;

import java.io.IOException;

public interface ByChunkReader extends CharacterReader {
	public int readChunk(char[] chars, int off, int len) throws IOException;
	public boolean hasMoreChunks();
}
