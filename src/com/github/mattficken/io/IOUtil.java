package com.github.mattficken.io;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.github.mattficken.io.ByLineReader;

public final class IOUtil {
	
	public static InputStream ensureMarkSupported(InputStream in) throws IOException {
		if (in.markSupported())
			return in;
		return new ByteArrayInputStream(toOUT(in).toByteArray());
	}
	
	public static byte[] toBytes(InputStream in) throws IOException {
		return toOUT(in).toByteArray();
	}
	
	public static String toString(InputStream in) throws IOException {
		return toOUT(in).toString();
	}
	
	public static void copy(InputStream in, OutputStream out) throws IOException {
		byte[] buf = new byte[1024];
		int len;
		while ( ( len = in.read(buf) ) != -1 )
			out.write(buf, 0, len);
	}
	
	private static ByteArrayOutputStream toOUT(InputStream in) throws IOException {
		in = ensureBuffered(in);
		ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
		copy(in, out);
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

	public static String toString(ByLineReader reader) throws IOException {
		StringBuilder sb = new StringBuilder(4096);
		String line;
		while (reader.hasMoreLines()) {
			line = reader.readLine();
			if (line==null)
				break;
			sb.append(line);
			sb.append('\n');
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
