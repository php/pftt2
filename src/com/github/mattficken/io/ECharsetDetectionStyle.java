package com.github.mattficken.io;

import java.io.InputStream;
import java.nio.charset.Charset;

import com.ibm.icu.text.CharsetRecognizer;

// XXX read by token
//          -different token in different charsets
// XXX read by caller control
//          -let caller control when character set is detected
//          -debugging support for this
// XXX read byte ranges - from XML (between tags), etc....
public enum ECharsetDetectionStyle {
	NONE {
			@Override
			public String getStyleDescription(EReadStyle rs) {
				switch(rs) {
				case CHUNK:
					return "No charset detection. Casts bytes direct to chars (dangerous) - reading by chunk";
				case CHAR:
					return "No charset detection. Casts bytes direct to chars (dangerous) - reading by char";
				case LINE:
					return "No charset detection. Casts bytes direct to chars (dangerous) - reading by line";	
				}
				return null;
			}	
			@Override
			public CharacterReader newCharReader(EReadStyle rs, InputStream in, CharsetDeciderDecoder cdd) {
				switch(rs) {
				case CHUNK:
					return new NoCharsetByChunkReader(in);
				case CHAR:
					return new NoCharsetByCharReader(in);
				case LINE:
					return new NoCharsetByLineReader(in);	
				}
				return null;
			}
			@Override
			public CharacterReader newCharReader(EReadStyle rs, InputStream in) {
				return newCharReader(rs, in, (CharsetDeciderDecoder)null);
			}
			@Override
			public CharacterReader newCharReader(EReadStyle rs, InputStream in, CharsetRecognizer[] recogs) {
				return newCharReader(rs, in, (CharsetDeciderDecoder)null);
			}
		},
	MULTI_CHARSET {
			@Override
			public String getStyleDescription(EReadStyle rs) {
				switch(rs) {
				case CHAR:
					return "Detects a (possibly different) character set for each read - reading by char";
				case CHUNK:
					return "Detects a (possibly different) character set for each read - reading by chunk";
				case LINE:
					return "Detects a (possibly different) character set for each read - reading by line"; // TODO clarify different charset each line
				}
				return null;
			}	
			@Override
			public CharacterReader newCharReader(EReadStyle rs, InputStream in, CharsetDeciderDecoder cdd) {
				switch(rs) {
				case CHAR:
					return new MultiCharsetByCharReader(in, cdd);
				case CHUNK:
					return new MultiCharsetByChunkReader(in, cdd);
				case LINE:
					return new MultiCharsetByLineReader(in, cdd);
				}
				return null;
			}
		},
	SINGLE_CHARSET_BY_AVAILABLE_STREAM {
			@Override
			public String getStyleDescription(EReadStyle rs) {
				switch(rs) {
				case CHUNK:
					return "Detect a single (possibly different) charset using only available bytes for each read - reading by chunk";
				case CHAR:
					return "Detect a single (possibly different) charset using only available bytes for each read - reading by char";
				case LINE:
					return "Detect a single (possibly different) charset using only available bytes for each read - reading by line";
				}
				return null;
			}	
			@Override
			public CharacterReader newCharReader(EReadStyle rs, InputStream in, CharsetDeciderDecoder cdd) {
				switch(rs) {
				case CHUNK:
					return new SingleCharsetByAvailableChunkReader(in, cdd);
				case CHAR:
					return new SingleCharsetByAvailableCharReader(in, cdd);
				case LINE:
					return new SingleCharsetByAvailableLineReader(in, cdd);
				}
				return null;
			}
		},
	SINGLE_CHARSET_BY_ENTIRE_STREAM {
			@Override
			public String getStyleDescription(EReadStyle rs) {
				switch(rs) {
				case CHUNK:
					return "Reads in entire stream then detects a single charset - reading into chunks";
				case CHAR:
					return "Reads in entire stream then detects a single charset - reading one char at a time";
				case LINE:
					return "Reads in entire stream then detects a single charset - reading line by line";
				}
				return null;
			}	
			@Override
			public CharacterReader newCharReader(EReadStyle rs, InputStream in, CharsetDeciderDecoder cdd) {
				switch(rs) {
				case CHUNK:
					return new SingleCharsetByEntireStreamChunkReader(in, cdd);
				case CHAR:
					return new SingleCharsetByEntireStreamCharReader(in, cdd);
				case LINE:
					return new SingleCharsetByEntireStreamLineReader(in, cdd);
				}
				return null;
			}
		}
	;
	public abstract String getStyleDescription(EReadStyle rs);
	public abstract CharacterReader newCharReader(EReadStyle rs, InputStream in, CharsetDeciderDecoder cdd);
	
	public CharacterReader newCharReader(EReadStyle rs, InputStream in) {
		return newCharReader(rs, in, CharsetDeciderDecoder.EXPRESS_RECOGNIZERS);
	}
	
	public CharacterReader newCharReader(EReadStyle rs, InputStream in, CharsetRecognizer[] recogs) {
		return newCharReader(rs, in, new DefaultCharsetDeciderDecoder(recogs));
	}
	
	public static char[] toCharsSingleCharset(byte[] bytes, int off, int len) {
		return null; // TODO
	}
	public static String toStringSingleCharset(byte[] bytes, int off, int len) {
		return null; // TODO
	}
	public static char[] toChars(byte[] bytes, int off, int len, Charset cs) {
		return null; // TODO
	}
	public static String toString(byte[] bytes, int off, int len, Charset cs) {
		return null; // TODO
	}
	public static char[] toCharsSingleCharset(byte[] bytes) {
		return toCharsSingleCharset(bytes, 0, bytes.length);
	}
	public static String toStringSingleCharset(byte[] bytes) {
		return toStringSingleCharset(bytes, 0, bytes.length);
	}
	public static char[] toChars(byte[] bytes, Charset cs) {
		return toChars(bytes, 0, bytes.length, cs);
	}
	public static String toString(byte[] bytes, Charset cs) {
		return toString(bytes, 0, bytes.length, cs);
	}
} // end public enum ECharsetDetectionStyle
