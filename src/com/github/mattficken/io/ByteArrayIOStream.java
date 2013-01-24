package com.github.mattficken.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** allows working with a single byte array using the OutputStream and InputStream interfaces
 * 
 * Avoids copying byte arrays like you would be doing if you used both java.io.ByteArrayOutputStream
 * and java.io.ByteArrayInputStream instead.
 * 
 * @author Matt Ficken
 *
 */

public class ByteArrayIOStream extends OutputStream {
	protected byte[] buf;
	protected int count;
	
	public ByteArrayIOStream(int count) {
		buf = new byte[count];
	}
	
	public ByteArrayIOStream() {
		this(1024);
	}

	public ByteArrayInStream getInputStream() {
		return new ByteArrayInStream();
	}
	
	@Override
	public String toString() {
		return new String(buf, 0, count);
	}
	
	public class ByteArrayInStream extends InputStream {
		protected int pos;

		@Override
		public int read() throws IOException {
			return buf[pos++];
		}
		
		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			if (pos+len>count) {
				len = count - pos;
				if (len<1)
					return len;
			}
			System.arraycopy(buf, pos, b, off, len);
			pos += len;
			return len;
		}
		
	} // end public class ByteArrayInStream
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (buf.length-count<len) {
			byte[] new_buf = new byte[count+(len*2)];
			System.arraycopy(buf, 0, new_buf, 0, count);
			buf = new_buf;
		}
		System.arraycopy(b, off, buf, count, len);
		count += len;
	}
	
	@Override
	public void write(int b) throws IOException {
		if (count+1==buf.length) {
			byte[] new_buf = new byte[count+128];
			System.arraycopy(buf, 0, new_buf, 0, count);
			buf = new_buf;
		}
		buf[count] = (byte) b;
		count++;
	}

} // end public class ByteArrayIOStream
