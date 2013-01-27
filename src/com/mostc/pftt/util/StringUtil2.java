package com.mostc.pftt.util;

import java.io.IOException;
import java.io.Writer;

import com.github.mattficken.io.IOUtil;

/**
 * 
 * @author Matt Ficken
 *
 */

public final class StringUtil2 {
	
	public static String ensurePhpTags(String code) {
		if (code.startsWith("<?php") || code.startsWith("<?")) {
			if(code.endsWith("?>")) {
				return code;
			} else {
				return code + "\r\n?>";
			}	
		} else {
			return "<?php \r\n "+code+"\r\n?>";
		}
	}

	/** replacement for StringWriter that will silent ignore write requests if
	 * too many chars have been written.
	 * 
	 * This avoids OutOfMemoryErrors.
	 * 
	 */
	public static class LengthLimitStringWriter extends Writer {
		protected final StringBuilder sb;
		protected final int max;
		
		/**
		 * 
		 * @param cap - initial capacity
		 * @param max - maximum capacity - will not exceed this
		 */
		public LengthLimitStringWriter(int cap, int max) {
			this.sb = new StringBuilder(cap);
			this.max = max;
		}
		
		public LengthLimitStringWriter() {
			this(512, IOUtil.QUARTER_MEGABYTE);
		}

		@Override
		public void close() throws IOException {
			
		}

		@Override
		public void flush() throws IOException {
			
		}

		@Override
		public void write(char[] chars, int off, int len) throws IOException {
			len = Math.min(len, max - sb.length());
			if (len>0)
				sb.append(chars, off, len);
		}
		
		@Override
		public String toString() {
			return sb.toString();
		}
		
	} // end public static class LengthLimitStringWriter
	
	private StringUtil2() {}
	
} // end public final class StringUtil2
