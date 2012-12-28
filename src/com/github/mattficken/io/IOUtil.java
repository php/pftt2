package com.github.mattficken.io;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.github.mattficken.io.ByLineReader;

public final class IOUtil {
	public static final int ONE_MEGABYTE = 1024*1024;
	public static final int HALF_MEGABYTE = 512*1024;
	public static final int QUARTER_MEGABYTE = 256*1024;
	
	public static InputStream ensureMarkSupported(InputStream in, int max_bytes) throws IOException {
		if (in.markSupported())
			return in;
		return new ByteArrayInputStream(toOUT(in, max_bytes).toByteArray());
	}
	
	public static byte[] toBytes(InputStream in, int max_bytes) throws IOException {
		return toOUT(in, max_bytes).toByteArray();
	}
	
	public static String toString(InputStream in, int max_bytes) throws IOException {
		return toOUT(in, max_bytes).toString();
	}
	
	public static int copy(InputStream in, OutputStream out, int max_bytes) throws IOException {
		byte[] buf = new byte[1024];
		int len, total_len = 0;
		while ( ( len = in.read(buf) ) != -1 ) {
			out.write(buf, 0, len);
			total_len += len;
			if (max_bytes>0 && total_len>=max_bytes)
				break;
		}
		return total_len;
	}
	
	private static ByteArrayOutputStream toOUT(InputStream in, int max_bytes) throws IOException {
		in = ensureBuffered(in);
		ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
		copy(in, out, max_bytes);
		return out;
	}

	public static BufferedInputStream ensureBuffered(InputStream in) {
		return in instanceof BufferedInputStream ? (BufferedInputStream) in : new BufferedInputStream(in);
	}
	
	public static Object getBytes(byte[] in, int in_off, int in_len) {
		if (in_off==0 && in_len==in.length)
			return in;
		
		byte[] out = new byte[in_len];
		System.arraycopy(in, in_off, out, 0, in_len);
		return out;
	}

	public static byte[] ensureLeftShifted(byte[] bytes, int off, int len) {
		if (off<1)
			return bytes;
		byte[] out = new byte[len];
		System.arraycopy(bytes, off, out, 0, len);
		return out;
	}

	public static String toString(ByLineReader reader, int max_chars) throws IOException {
		StringBuilder sb = new StringBuilder(4096);
		String line;
		while (reader.hasMoreLines()) {
			line = reader.readLine();
			if (line==null)
				break;
			sb.append(line);
			sb.append('\n');
			if (max_chars > 0 && sb.length() > max_chars)
				break;
		}
		return sb.toString();
	}
	
	/* TODO public static String toString(ByChunkReader reader) {
		
	}
	
	public static String toString(ByCharReader reader) {
		
	}
	
	public static String toString(CharacterReader reader) {
		
	}*/
	
	private IOUtil() {}
	
} // end public final class IOUtil
