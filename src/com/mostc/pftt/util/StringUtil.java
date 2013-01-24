package com.mostc.pftt.util;

import java.io.IOException;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.mattficken.io.IOUtil;
import com.mostc.pftt.util.apache.regexp.RE;
import com.mostc.pftt.util.apache.regexp.RECompiler;
import com.mostc.pftt.util.apache.regexp.REProgram;

/** String utility functions
 * 
 * @author Matt Ficken
 * 
 */

public final class StringUtil {
	public static final String EMPTY = "";
	
	public static String chomp(String in) {
		if (in.endsWith("\r\n"))
			return in.substring(0, in.length()-2);
		else if (in.endsWith("\n"))
			return in.substring(0, in.length()-1);
		else
			return in;
	}
	
	public static String unquote(String in) {
		if (isEmpty(in))
			return in;
		while (in.startsWith("\"") || in.startsWith("'"))
			in = in.substring(1);
		while (in.endsWith("\"") || in.endsWith("'"))
			in = in.substring(0, in.length()-1);
		return in;
	}
	
	public static boolean startsWithCS(String a, String b) {
		return a==null||b==null? false : a.startsWith(b);
	}
	
	public static boolean startsWithIC(String a, String b) {
		if (a==null||b==null)
			return false;
		else if (a.startsWith(b))
			return true;
		else
			return a.toLowerCase().startsWith(b.toLowerCase());
	}
	
	public static boolean endsWithCS(String a, String b) {
		return a==null||b==null? false : a.endsWith(b);
	}
	
	public static boolean endsWithIC(String a, String b) {
		if (a==null||b==null)
			return false;
		else if (a.endsWith(b))
			return true;
		else
			return a.toLowerCase().endsWith(b.toLowerCase());
	}
		
	public static String toString(char c) {
		return new String(new char[]{c});
	}

	public static Object padFirst(String string, int len, char c) {
		if (string.length()>=len)
			return string;
		StringBuffer sb = new StringBuffer();
		while (sb.length()+string.length() < len)
			sb.append(c);
		sb.append(string);
		return sb.toString();
	}
	
	public static final String[] EMPTY_ARRAY = new String[]{};
	public static String[] splitLines(String str) {
		if (str==null)
			return EMPTY_ARRAY;
		else
			return PATTERN_RN.split(str);
	}
	
	public static String[] splitEqualsSign(String str) {
		if (str==null)
			return EMPTY_ARRAY;
		
		return PATTERN_EQ.split(str);
	}
	
	public static String[] splitWhitespace(String str) {
		if (str==null)
			return EMPTY_ARRAY;
		
		return PATTERN_WS.split(str);
	}
	
	static final Pattern PATTERN_WS = Pattern.compile("[\\w]+");
	static final Pattern PATTERN_R_N = Pattern.compile("\\r\\n");
	static final Pattern PATTERN_R = Pattern.compile("\\r");
	static final Pattern PATTERN_RN = Pattern.compile("[\\r]?\\n");
	static final Pattern PATTERN_EQ = Pattern.compile("\\=");
	public static String normalizeLineEnding(String str) {
		if (str==null)
			return str;
		 
		str = replaceAll(PATTERN_R_N, "\n", str);
		str = replaceAll(PATTERN_R, EMPTY, str);
		return str;
	}
	public static String removeLineEnding(String str) {
		if (str==null)
			return str;
		
		str = replaceAll(PATTERN_R_N, EMPTY, str);
		str = replaceAll(PATTERN_R, EMPTY, str);
		return str;
	}
	
	public static String replaceAll(Pattern pat, String rep, String str) {
		return pat.matcher(str).replaceAll(rep);
	}
	
	/** ensures returned string starts and end with "
	 * 
	 * @see #ensureApos
	 * @param str
	 * @return
	 */
	public static String ensureQuoted(String str) {
		if (!str.startsWith("\""))
			str = "\"" + str;
		if (!str.endsWith("\""))
			str = str + "\"";
		return str;
	}
	
	/** ensures returned string starts and end with '
	 * 
	 * @see #ensureQuoted
	 * @param str
	 * @return
	 */
	public static String ensureApos(String str) {
		if (!str.startsWith("'"))
			str = "'" + str;
		if (!str.endsWith("'"))
			str = str + "'";
		return str;
	}
	
	public static String makeRegularExpressionSafe(String str) {
		return makeRegularExpressionSafe(str, "/");
	}

	public static String makeRegularExpressionSafe(String str, String delim) {
		//Pattern.quote(str)
		StringBuilder sb = new StringBuilder(str.length());
		char c;
		for ( int i =0 ; i < str.length () ; i++ ) {
			c = str.charAt(i);
			switch (c) {
			case '(':
			case ')':
			case '[':
			case ']':
			case '{':
			case '}':
			case '.':
			case '?':
			case '*': 
			case '^':
			case '|':
			case '\\':
			case '$':
			case '+':
				sb.append("\\");
			}
			sb.append(c);
		}
		return sb.toString();
	}

	static final RECompiler compiler = new RECompiler();
	public static RE compile(String needle) {
		REProgram prog = compiler.compile(needle);
		return new RE(prog);
	}
	public static RE compileQuote(String needle) {
		return compile(makeRegularExpressionSafe(needle));
	}
	public static boolean isNotEmpty(CharSequence cs) {
		return cs != null && cs.length() > 0;
	}
	
	public static boolean isNotEmpty(Object[] a) {
		return a == null ? false : a.length > 0;
	}
	
	public static boolean isEmpty(CharSequence cs) {
		return cs == null || cs.length() == 0;
	}
	
	public static boolean isEmpty(Object[] o) {
		return o == null || o.length == 0;
	}

	public static String toString(Object o) {
		return o == null ? "null" : o.toString();
	}
	
	public static String toString(Object[] o) {
		if (isEmpty(o))
			return "[]";
		
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		sb.append(toString(o[0]));
		for ( int i=1 ; i < o.length ; i++ ) {
			sb.append(',');
			sb.append(toString(o[i]));
		}
		sb.append(']');
		return sb.toString();
	}

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

	public static boolean equalsIC(String a, String b) {
		return a == null || b == null ? false : a.equalsIgnoreCase(b);
	}
	
	public static boolean equalsCS(String a, String b) {
		return a == null || b == null ? false : a.equals(b);
	}
	
	public static boolean match(Pattern pat, String text) {
		return pat.matcher(text).matches();
	}

	public static String[] getMatches(Pattern pat, String text) {
		Matcher m = pat.matcher(text);
		
		if (!m.matches())
			return null;
		
		final int c = m.groupCount();
		String[] out = new String[c];
		for ( int i=0 ; i < c ; i++ )
			out[i] = m.group(c);
		
		return out;
	}
	
	public static int hashCode(String a) {
		return a == null ? 0 : a.hashCode();
	}

	public static String join(String[] parts, String delim) {
		return join(parts, 0, parts.length, delim);
	}
	public static String join(String[] parts, int off, String delim) {
		return join(parts, off, parts.length-off, delim);
	}
	public static String join(String[] parts, int off, int len, String delim) {
		if (len<=0)
			return EMPTY;
		else if (len==1)
			return parts[off];
		
		StringBuilder sb = new StringBuilder(256);
		sb.append(parts[off]);
		off++;
		for ( int i=0 ; i < len && off < parts.length ; i++, off++ ) {
			sb.append(delim);
			sb.append(parts[off]);
		}
		return sb.toString();
	}
	
	public static boolean containsAnyCS(String str, String[] needles) {
		if (str==null||str.length()==0)
			return false;
		for ( String n : needles ) {
			if (str.contains(n))
				return true;
		}
		return false;
	}
	
	public static boolean containsAnyIC(String str, String[] needles) {
		if (str==null||str.length()==0||needles==null||needles.length==0)
			return false;
		str = str.toLowerCase();
		for ( String n : needles ) {
			if (str.contains(n))
				return true;
		}
		return false;
	}
	
	public static boolean startsWithAnyCS(String str, String[] needles) {
		if (str==null||str.length()==0)
			return false;
		for ( String n : needles ) {
			if (str.startsWith(n))
				return true;
		}
		return false;
	}
	
	public static boolean startsWithAnyIC(String str, String[] needles) {
		if (str==null||str.length()==0||needles==null||needles.length==0)
			return false;
		str = str.toLowerCase();
		for ( String n : needles ) {
			if (str.startsWith(n))
				return true;
		}
		return false;
	}
	
	public static boolean endsWithAnyCS(String str, String[] needles) {
		if (str==null||str.length()==0)
			return false;
		for ( String n : needles ) {
			if (str.endsWith(n))
				return true;
		}
		return false;
	}
	
	public static boolean endsWithAnyIC(String str, String[] needles) {
		if (str==null||str.length()==0||needles==null||needles.length==0)
			return false;
		str = str.toLowerCase();
		for ( String n : needles ) {
			if (str.endsWith(n))
				return true;
		}
		return false;
	}
	
	public static String repeat(String patt, int count) {
		StringBuilder sb = new StringBuilder(patt.length()*count);
		for ( int i=0 ; i < count ; i++ )
			sb.append(patt);
		return sb.toString();
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
	
	public static boolean isWhitespaceOrEmpty(String a) {
		if (a==null)
			return true;
		final int a_len = a.length();
		if (a_len==0)
			return true;
		for ( int i=0 ; i < a_len ; i++ ) {
			if (!Character.isLetter(a.charAt(i)))
				return false;
		}
		return true;
	}
	
	private StringUtil() {}
	
} // end public class StringUtil
