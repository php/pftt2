package com.github.mattficken.io;

import java.io.IOException;

public interface ByLineReader extends CharacterReader {
	String readLine() throws IOException;
	boolean hasMoreLines();
}
