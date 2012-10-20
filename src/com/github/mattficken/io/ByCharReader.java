package com.github.mattficken.io;

import java.io.IOException;

public interface ByCharReader extends CharacterReader {
	public boolean hasMoreChars();
	public char readChar() throws IOException;
}
