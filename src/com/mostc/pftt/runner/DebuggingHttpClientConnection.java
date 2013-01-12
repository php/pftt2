package com.mostc.pftt.runner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.impl.io.AbstractSessionInputBuffer;
import org.apache.http.impl.io.AbstractSessionOutputBuffer;
import org.apache.http.io.EofSensor;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.io.SessionOutputBuffer;
import org.apache.http.params.HttpParams;

public class DebuggingHttpClientConnection extends DefaultHttpClientConnection {
	protected final ByteArrayOutputStream response, request;
	
	public DebuggingHttpClientConnection(ByteArrayOutputStream request, ByteArrayOutputStream response) {
		this.request = request;
		this.response = response;
	}
	
	@Override
	protected SessionInputBuffer createSessionInputBuffer(final Socket socket, int buffersize, final HttpParams params) throws IOException {
		InputStream in = socket.getInputStream();
		DebuggingInputStream din = new DebuggingInputStream(response, in);
		
		return new DebugSocketInputBuffer(socket, din, buffersize, params);
	}
	
	@Override
	protected SessionOutputBuffer createSessionOutputBuffer(final Socket socket, int buffersize, final HttpParams params) throws IOException {
		OutputStream out = socket.getOutputStream();
		DebuggingOutputStream dout = new DebuggingOutputStream(request, out);
		
		return new DebugSocketOutputBuffer(socket, dout, buffersize, params);
	}
	
	protected static class DebuggingInputStream extends InputStream {
		protected final ByteArrayOutputStream bin;
		protected final InputStream real;

		public DebuggingInputStream(ByteArrayOutputStream bin, InputStream real) {
			this.bin = bin;
			this.real = real;
		}
		
		@Override
		public int read(byte[] buf, int off, int len) throws IOException {
			len = real.read(buf, off, len);
			bin.write(buf, off, len);
			return len;
		}
		
		@Override
		public int read(byte[] buf) throws IOException {
			int len = real.read(buf);
			bin.write(buf, 0, len);
			return len;
		}
		
		@Override
		public int read() throws IOException {
			int i = real.read();
			bin.write(i);
			return i;
		}
		
		@Override
		public void close() throws IOException {
			bin.close();
			real.close();
		}
		
	} // end protected static class DebuggingInputStream
	
	protected static class DebuggingOutputStream extends OutputStream {
		protected final ByteArrayOutputStream bout;
		protected final OutputStream real;

		public DebuggingOutputStream(ByteArrayOutputStream bout, OutputStream real) {
			this.bout = bout;
			this.real = real;
		}
		
		@Override
		public void write(byte[] buf, int off, int len) throws IOException {
			real.write(buf, off, len);
			bout.write(buf, off, len);
		}
		
		@Override
		public void write(byte[] buf) throws IOException {
			real.write(buf);
			bout.write(buf);
		}
		
		@Override
		public void write(int b) throws IOException {
			real.write(b);
			bout.write(b);
		}
		
		@Override
		public void close() throws IOException {
			real.close();
			bout.close();
		}
		
	} // end protected static class DebuggingOutputStream
	
	protected static class DebugSocketInputBuffer extends AbstractSessionInputBuffer implements EofSensor {
		private final Socket socket;
		private boolean eof;

		public DebugSocketInputBuffer(final Socket socket, final DebuggingInputStream din, int buffersize, final HttpParams params) throws IOException {
			super();
			if (socket == null) {
				throw new IllegalArgumentException("Socket may not be null");
			}
			this.socket = socket;
			this.eof = false;
			if (buffersize < 0) {
				buffersize = socket.getReceiveBufferSize();
			}
			if (buffersize < 1024) {
				buffersize = 1024;
			}
			init(din, buffersize, params);
		}

		@Override
		protected int fillBuffer() throws IOException {
			int i = super.fillBuffer();
			this.eof = i == -1;
			return i;
		}
		
		public boolean isDataAvailable(int timeout) throws IOException {
			boolean result = hasBufferedData();
			if (!result) {
				int oldtimeout = this.socket.getSoTimeout();
				try {
					this.socket.setSoTimeout(timeout);
					fillBuffer();
					result = hasBufferedData();
				} catch (SocketTimeoutException ex) {
					throw ex;
				} finally {
					socket.setSoTimeout(oldtimeout);
				}
			}
			return result;
		}
		
		public boolean isEof() {
			return this.eof;
		}

	} // end protected static class DebugSocketInputBuffer
	
	protected static class DebugSocketOutputBuffer extends AbstractSessionOutputBuffer {

		public DebugSocketOutputBuffer(final Socket socket, final DebuggingOutputStream dout, int buffersize, final HttpParams params) throws IOException {
			super();
			if (socket == null) {
				throw new IllegalArgumentException("Socket may not be null");
			}
			if (buffersize < 0) {
				buffersize = socket.getSendBufferSize();
			}
			if (buffersize < 1024) {
				buffersize = 1024;
			}
			init(dout, buffersize, params);
		}

	} // end protected static class DebugSocketOutputBuffer
	
} // end public class DebuggingHttpClientConnection
