package com.github.mattficken.io;

public enum EReadStyle {
	/** reads the specified number of characters or until end of stream */
	CHUNK,
	/** reads until the end of the line (an unknown number of characters) or until end of stream (usually to \r\n or \n) */
	LINE,
	/** reads a single character at a time */
	CHAR,
}
