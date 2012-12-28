package com.mostc.pftt.runner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.params.HttpParams;

public class PhptDebuggingSocketFactory extends PlainSocketFactory {
	
	@Override
	public Socket createSocket(final HttpParams params) {
		return debugWrap(super.createSocket(params));
	}
	
	@Override
	public Socket createSocket() {
		return debugWrap(super.createSocket());
	}
	
	protected Socket debugWrap(final Socket sock) {
		return new PhptDebugSocket(sock);
	}
	
	public static class PhptDebugSocket extends Socket {
		protected final ByteArrayOutputStream in, out;
		protected final Socket sock;
		
		public PhptDebugSocket(final Socket sock) {
			this.sock = sock;
			
			in = new ByteArrayOutputStream();
			out = new ByteArrayOutputStream(); // TODO cap
		}
		
		@Override
		public void connect(SocketAddress addr) throws IOException {
			sock.connect(addr);
		}
		@Override
		public void connect(SocketAddress addr, int timeout) throws IOException {
			sock.connect(addr, timeout);
		}
		@Override
		public void bind(SocketAddress addr) throws IOException {
			sock.bind(addr);
		}
		@Override
		public InetAddress getInetAddress() {
			return sock.getInetAddress();
		}
		@Override
		public InetAddress getLocalAddress() {
			return sock.getLocalAddress();
		}
		@Override
		public int getPort() {
			return sock.getPort();
		}
		@Override
		public int getLocalPort() {
			return sock.getLocalPort();
		}
		@Override
		public SocketAddress getRemoteSocketAddress() {
			return sock.getRemoteSocketAddress();
		}
		@Override
		public SocketAddress getLocalSocketAddress() {
			return sock.getLocalSocketAddress();
		}
		@Override
		public SocketChannel getChannel() {
			throw new IllegalStateException("don't get here... will miss debugging output");
		}
		@Override
		public InputStream getInputStream() throws IOException {
			return new DebugInputStream(in, sock.getInputStream());
		}
		@Override
		public OutputStream getOutputStream() throws IOException {
			return new DebugOutputStream(out, sock.getOutputStream());
		}
		@Override
		public void setTcpNoDelay(boolean on) throws SocketException {
			sock.setTcpNoDelay(on);
		}
		@Override
		public boolean getTcpNoDelay() throws SocketException {
			return sock.getTcpNoDelay();
		}
		@Override
		public void setSoLinger(boolean on, int linger) throws SocketException {
			sock.setSoLinger(on, linger);
		}
		@Override
		public int getSoLinger() throws SocketException {
			return sock.getSoLinger();
		}
		@Override
		public void sendUrgentData(int data) throws IOException {
			sock.sendUrgentData(data);
		}
		@Override
		public void setOOBInline(boolean on) throws SocketException {
			sock.setOOBInline(on);
		}
		@Override
		public boolean getOOBInline() throws SocketException {
			return sock.getOOBInline();
		}
		@Override
		public void setSoTimeout(int timeout) throws SocketException {
			sock.setSoTimeout(timeout);
		}
		@Override
		public int getSoTimeout() throws SocketException {
			return sock.getSoTimeout();
		}
		@Override
		public void setSendBufferSize(int size) throws SocketException {
			sock.setSendBufferSize(size);
		}
		@Override
		public int getSendBufferSize() throws SocketException {
			return sock.getSendBufferSize();
		}
		@Override
		public void setReceiveBufferSize(int size) throws SocketException {
			sock.setReceiveBufferSize(size);
		}
		@Override
		public int getReceiveBufferSize() throws SocketException {
			return sock.getReceiveBufferSize();
		}
		@Override
		public void setKeepAlive(boolean on) throws SocketException {
			sock.setKeepAlive(on);
		}
		@Override
		public boolean getKeepAlive() throws SocketException {
			return sock.getKeepAlive();
		}
		@Override
		public void setTrafficClass(int clazz) throws SocketException {
			sock.setTrafficClass(clazz);
		}
		@Override
		public int getTrafficClass() throws SocketException {
			return sock.getTrafficClass();
		}
		@Override
		public void setReuseAddress(boolean on) throws SocketException {
			sock.setReuseAddress(on);
		}
		@Override
		public boolean getReuseAddress() throws SocketException {
			return sock.getReuseAddress();
		}
		@Override
		public void close() throws IOException {
			sock.close();
		}
		@Override
		public void shutdownInput() throws IOException {
			sock.shutdownInput();
		}
		@Override
		public void shutdownOutput() throws IOException {
			sock.shutdownOutput();
		}
		@Override
		public boolean isConnected() {
			return sock.isConnected();
		}
		@Override
		public boolean isBound() {
			return sock.isBound();
		}
		@Override
		public boolean isClosed() {
			return sock.isClosed();
		}
		@Override
		public boolean isInputShutdown() {
			return sock.isInputShutdown();
		}
		@Override
		public boolean isOutputShutdown() {
			return sock.isOutputShutdown();
		}
		protected static class DebugInputStream extends InputStream {
			protected final ByteArrayOutputStream in;
			protected final InputStream impl;

			public DebugInputStream(ByteArrayOutputStream in, InputStream impl) {
				this.in = in;
				this.impl = impl;
			}

			@Override
			public int read(byte[] buf) throws IOException {
				int len = impl.read(buf);
				
				if (len>-1)
					in.write(buf, 0, len);
				
				return len;
			}
			@Override
			public int read(byte[] buf, int off, int len) throws IOException {
				len = impl.read(buf, off, len);
				
				if (len>-1)
					in.write(buf, off, len);
				
				return len;
			}
			@Override
			public long skip(long a) throws IOException {
				return impl.skip(a);
			}
			@Override
			public int available() throws IOException {
				return impl.available();
			}
			@Override
			public void close() throws IOException {
				impl.close();
			}
			@Override
			public void mark(int a) {
				impl.mark(a);
			}
			@Override
			public void reset() throws IOException {
				impl.reset();
			}
			@Override
			public boolean markSupported() {
				return impl.markSupported();
			}

			@Override
			public int read() throws IOException {
				int b = impl.read();
				
				in.write(b);
				
				return b;
			}
			
		} // end protected static class DebugInputStream
		protected static class DebugOutputStream extends OutputStream {
			protected final ByteArrayOutputStream out;
			protected final OutputStream impl;

			public DebugOutputStream(ByteArrayOutputStream out, OutputStream impl) {
				this.out = out;
				this.impl = impl;
			}
			
			@Override
			public void write(byte[] buf) throws IOException {
				out.write(buf);
				
				impl.write(buf);
			}
			@Override
			public void write(byte[] buf, int off, int len) throws IOException {
				out.write(buf, off, len);
				
				impl.write(buf, off, len);
			}
			@Override
			public void flush() throws IOException {
				impl.flush();
			}
			@Override
			public void close() throws IOException {
				impl.close();
			}
			@Override
			public void write(int b) throws IOException {
				out.write(b);
				
				impl.write(b);
			}
			
		} // end protected static class DebugOutputStream
	} // end public static class PhptDebugSocket
	
} // end public class PhptDebuggingSocketFactory
