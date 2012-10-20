package com.github.mattficken.io;

import java.io.IOException;

// TODO if FAIL, autodetect charset and redo
//               -
// TODO ISO 8859 15 for tests/basic/022.phpt ext/standard/tests/strings/bug37244.php
// TODO UTF 8 for ext/standard/tests/strings/html_entity_decode_html5.phpt ext/standard/tests/serialize/006.phpt ext/standard/tests/general_functions/bug49056.php ext/standard/tests/array/bug34066.php
// TODO KOI8-R for ext/standard/tests/general_functions/002.phpt ext/standard/tests/general_functions/006.php
public interface CharacterReader {
	// XXX may have problems if reading in 512 or 1536 character blocks
	static final int DEFAULT_READ_SIZE = 4096;
	static final int DEFAULT_LINE_SIZE = 256;
	static final int END_OF_STREAM = -1;
	static final char END_OF_STREAM_CHAR = (char) -1;
	
	ECharsetDetectionStyle getCharsetDetectionStyle();
	EReadStyle getReadStyle();
	void close() throws IOException;
}
