package com.github.mattficken.io;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		
	public static void intern(List<String> in) {
		intern(in.listIterator());
	}
	
	public static void intern(ListIterator<String> in) {
		while (in.hasNext()) {
			in.set(in.next().intern());
		}
	}
	
	public static String max(String in, int len) {
		return in.length() > len ? in.substring(0, len) : in;
	}
	
	public static String min(String in, int len) {
		return min(in, len, ' ');
	}
	
	public static String min(String in, int len, char c) {
		return padLast(in, len, c);
	}
	
	private static Random r = new Random();
	public static String randomLettersStr(int min, int max) {
		final int len = r.nextInt(max-min)+min;
		char[] chars = new char[len];
		for ( int i=0 ; i < len ; i++ ) {
			chars[i] = (char)( r.nextInt(26)+65 );
		}
		return new String(chars, 0, len);
	}
	
	public static String toTitle(String str) {
		return str.length() > 1 ? str.substring(0, 1).toUpperCase()+str.substring(1).toLowerCase() : str;
	}
	
	public static String[] splitOnUpperCase(String input) {
		LinkedList<String> out = new LinkedList<String>();
		String a = "";
		char c;
		for ( int i=0 ; i < input.length() ; i++ ) {
			c = input.charAt(i);
			if (Character.isUpperCase(c)) {
				if (a.length()>0)
					out.add(a);
				a = "";
			}
			a += c;
		}
		if (a.length()>0)
			out.add(a);
		return (String[]) out.toArray(new String[out.size()]);
	}
	
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
	
	public static String padLast(String string, int len, char c) {
		if (string.length()>=len)
			return string;
		StringBuilder sb = new StringBuilder(len);
		sb.append(string);
		while (sb.length() < len)
			sb.append(c);
		return sb.toString();
	}

	public static String padFirst(String string, int len, char c) {
		if (string.length()>=len)
			return string;
		StringBuilder sb = new StringBuilder(len);
		while (sb.length() < len)
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
	
	public static String toString(int[] o) {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		sb.append(o[0]);
		for ( int i=1 ; i < o.length ; i++ ) {
			sb.append(',');
			sb.append(o[i]);
		}
		sb.append(']');
		return sb.toString();
	}
	
	public static String toString(short[] o) {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		sb.append(o[0]);
		for ( int i=1 ; i < o.length ; i++ ) {
			sb.append(',');
			sb.append(o[i]);
		}
		sb.append(']');
		return sb.toString();
	}
	
	public static String toString(float[] o) {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		sb.append(o[0]);
		for ( int i=1 ; i < o.length ; i++ ) {
			sb.append(',');
			sb.append(o[i]);
		}
		sb.append(']');
		return sb.toString();
	}
	
	public static String toString(long[] o) {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		sb.append(o[0]);
		for ( int i=1 ; i < o.length ; i++ ) {
			sb.append(',');
			sb.append(o[i]);
		}
		sb.append(']');
		return sb.toString();
	}
	
	public static String toString(double[] o) {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		sb.append(o[0]);
		for ( int i=1 ; i < o.length ; i++ ) {
			sb.append(',');
			sb.append(o[i]);
		}
		sb.append(']');
		return sb.toString();
	}
	
	public static String toString(boolean[] o) {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		sb.append(o[0]);
		for ( int i=1 ; i < o.length ; i++ ) {
			sb.append(',');
			sb.append(o[i]);
		}
		sb.append(']');
		return sb.toString();
	}
	
	public static String toString(char[] o) {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		sb.append(o[0]);
		for ( int i=1 ; i < o.length ; i++ ) {
			sb.append(',');
			sb.append(o[i]);
		}
		sb.append(']');
		return sb.toString();
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
	
	public static String join(List<String> strings, String delim) {
		return join(strings, 0, strings.size(), delim);
	}
	
	public static String join(List<String> strings, int off, String delim) {
		return join(strings, off, strings.size() - off, delim);
	}
	
	public static String join(List<String> strings, int off, int len, String delim) {
		if (strings.size() <= 0)
			return EMPTY;
		else if (strings.size() == 1)
			return strings.get(0);
		
		StringBuilder sb = new StringBuilder(256);
		if (off==0 && !(strings instanceof ArrayList)) {
			// performance optimization primarily for LinkedLists
			Iterator<String> it = strings.iterator();
			if (it.hasNext()) {
				sb.append(it.next());
				for ( int i=1 ; i < len && it.hasNext() ; i++ ) {
					sb.append(delim);
					sb.append(it.next());
				}
			}
		} else {
			sb.append(strings.get(off));
			off++;
			for ( int i=0 ; i < len && off < strings.size() ; i++, off++ ) {
				sb.append(delim);
				sb.append(strings.get(off));
			}
		}
		return sb.toString();
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
	
	public static String toJava(CharSequence cs) {
		StringBuilder sb = new StringBuilder(cs.length());
		
		String[] lines = splitLines(cs.toString());
		
		for ( int i=0 ; i < lines.length ; i++ ) {
			sb.append("\"");
			
			sb.append(lines[i].replace("\"", "\\\""));
			
			if (i+1<lines.length)
				sb.append("\" + \n");
			else
				sb.append("\";\n");
		}
		return sb.toString();
	} // end public static String toJava
	
	public static int parseIntEx(String str) {
		return isEmpty(str) ? 0 : Integer.parseInt(str);
	}
	
	public static int parseInt(String str) {
		try {
			return parseIntEx(str);
		} catch ( NumberFormatException ex ) {
			return 0;
		}
	}
	
	/** escapes special values in string so that it can be included in Java, PHP, etc... code
	 * 
	 * @param value
	 * @return
	 */
	public static String cslashes(String value) {
		return value.replace("\\", "\\\\");
	}
	
	private StringUtil() {}
	
} // end public class StringUtil
