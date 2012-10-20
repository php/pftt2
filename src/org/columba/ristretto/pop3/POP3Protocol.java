/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Ristretto Mail API.
 *
 * The Initial Developers of the Original Code are
 * Timo Stich and Frederik Dietz.
 * Portions created by the Initial Developers are Copyright (C) 2004
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */
package org.columba.ristretto.pop3;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;

import org.columba.ristretto.auth.AuthenticationException;
import org.columba.ristretto.auth.AuthenticationFactory;
import org.columba.ristretto.auth.AuthenticationMechanism;
import org.columba.ristretto.auth.AuthenticationServer;
import org.columba.ristretto.auth.NoSuchAuthenticationException;
import org.columba.ristretto.coder.Base64;
import org.columba.ristretto.concurrency.Mutex;
import org.columba.ristretto.config.RistrettoConfig;
import org.columba.ristretto.io.Source;
import org.columba.ristretto.log.LogInputStream;
import org.columba.ristretto.log.LogOutputStream;
import org.columba.ristretto.log.RistrettoLogger;
import org.columba.ristretto.parser.ParserException;
import org.columba.ristretto.pop3.parser.ScanListParser;
import org.columba.ristretto.pop3.parser.UIDListParser;
import org.columba.ristretto.ssl.RistrettoSSLSocketFactory;

/**
 * Implementation of the client side POP3 Protocol.
 * 
 * @author Timo Stich <tstich@users.sourceforge.net>
 */
public class POP3Protocol implements AuthenticationServer {

    /** JDK 1.4+ logging framework logger, used for logging. */
    private static final Logger LOG = Logger.getLogger("org.columba.ristretto.pop3.protocol");


    /**
     * The default port of POP3 is 110.
     */
	public static final int DEFAULT_PORT = 110;
	/**
	 * The default port of POP3S is 995.
	 */
	public static final int DEFAULT_SSL_PORT = 995;

	/**
	 * @deprecated Use NOT_CONNECTED instead
	 */
	public static final int CONNECTION_CLOSED = 0;

	/**
	 * Protcol state.
	 */
	public static final int NOT_CONNECTED = 0;	
	/**
	 * Protcol state.
	 */
	public static final int AUTHORIZATION = 1;
	/**
	 * Protcol state.
	 */
	public static final int TRANSACTION = 2;

	private static final Pattern timestampPattern =
		Pattern.compile("(<[^>]*>)");

	private static final Pattern linePattern = Pattern.compile("(([^\r\n]+)\r?\n?)");

	private static final Pattern statPattern = Pattern.compile("(\\d+) (\\d+)");
	

	// Attributes for ProgressObservable & Cancelable
	private String servername;
	private int port;
	private Socket socket;
	private POP3InputStream in;
	private OutputStream out;
	private int state;

	private String timestamp;

	private Mutex mutex;
	
	/**
	 * Constructs the POP3Protocol.
	 * 
	 * @param servername the server to connect to 
	 * @param port the port to connect to
	 */
	public POP3Protocol(String servername, int port) {
		this.port = port;
		this.servername = servername;

		mutex = new Mutex();
		
		state = NOT_CONNECTED;
	}

	/**
	 * Constructs the POP3Protocol.
	 *  
	 * @param servername the server to connect to 
	 */
	public POP3Protocol(String servername) {
		this(servername, DEFAULT_PORT);
	}

	/**
	 * Opens a connection to the POP3 server.
	 * 
	 * @throws IOException
	 * @throws POP3Exception
	 */
	public void openPort() throws IOException, POP3Exception {
		socket = new Socket(servername, port);

		socket.setSoTimeout(RistrettoConfig.getInstance().getTimeout());
		
		createStreams();

		POP3Response response = in.readSingleLineResponse();

		// try to find a timestamp
		Matcher matcher = timestampPattern.matcher(response.getMessage());
		if (matcher.find()) {
			timestamp = matcher.group(1);
		}
		
		if( response.isOK() ) {
			state = AUTHORIZATION;
		}
	}
	
	/**
	 * Opens a connection to the POP3 server using a SSL socket.
	 * This is also known as the POPs protocol.
	 * 
	 * @throws IOException
	 * @throws SSLException
	 * @throws POP3Exception
	 */
	public void openSSLPort() throws IOException, SSLException, POP3Exception {
		socket = RistrettoSSLSocketFactory.getInstance().createSocket(servername,
				port);
		socket.setSoTimeout(RistrettoConfig.getInstance().getTimeout());

		// handshake (which cyper algorithms are used?)
		((SSLSocket) socket).startHandshake();
		
		createStreams();
		
		POP3Response response = in.readSingleLineResponse();

		// try to find a timestamp
		Matcher matcher = timestampPattern.matcher(response.getMessage());
		if (matcher.find()) {
			timestamp = matcher.group(1);
		}
		
		if( response.isOK() ) {
			state = AUTHORIZATION;
		}
	}	
	
	/**
	 * Switches to a SSL Socket using the TLS extension. 
	 * 
	 * @throws IOException
	 * @throws SSLException
	 * @throws POP3Exception
	 */
	public void startTLS() throws IOException, SSLException, POP3Exception {
		sendCommand("STLS", null);
		
		POP3Response response = readSingleLineResponse();
		if( response.isERR() ) throw new CommandNotSupportedException("STLS");
		
		socket = RistrettoSSLSocketFactory.getInstance().createSocket(socket, servername, port, true);
		
		// handshake (which cyper algorithms are used?)
		((SSLSocket) socket).startHandshake();
		
		createStreams();
	}

	protected void sendCommand(String command, String[] parameters)
		throws IOException {
		try {
			// write the command
			out.write(command.getBytes());

			// write optional parameters
			if (parameters != null) {
				for (int i = 0; i < parameters.length; i++) {
					out.write(' ');
					out.write(parameters[i].getBytes());
				}
			}
			
			// write CRLF
			out.write('\r');
			out.write('\n');

			// flush the stream
			out.flush();
		} catch (IOException e) {
			state = NOT_CONNECTED;
			throw e;
		}
	}
	
	protected POP3Response readSingleLineResponse() throws IOException, POP3Exception {
		try {
			return in.readSingleLineResponse();
		} catch (IOException e) {
			// Connection was closed
			state = NOT_CONNECTED;
			throw e;
		}
	}

	protected POP3Response readMultiLineResponse() throws IOException, POP3Exception {
		try {
			return in.readMultiLineResponse();
		} catch (IOException e) {
			// Connection was closed
			state = NOT_CONNECTED;
			throw e;
		}
	}


	private void checkState(int state) throws POP3Exception {
		if( this.state != state ) {
			throw new POP3Exception("Wrong state: Should be "+ state + "but is in " + this.state);
		}
	}

	
	/**
	 * Checks if the POP3 server supports APOP authentication.
	 * 
	 * @see #apop(String, char[])
	 * 
	 * @return true if the server supports APOP
	 * @throws POP3Exception
	 */
	public boolean isApopSupported() throws POP3Exception {
		checkState(AUTHORIZATION);

		return( timestamp != null);
	}
	
	/**
	 * Sends the APOP authentication command.
	 * 
	 * @param user username
	 * @param secret the shared secret (e.g. the password)
	 * @throws IOException
	 * @throws POP3Exception 
	 */
	public void apop(String user, char[] secret)
		throws IOException, POP3Exception {
		if (timestamp == null) {
			throw new CommandNotSupportedException("No timestamp from server - APOP not possible");
		}
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(timestamp.getBytes());
			if (secret == null) 
				secret = new char[0];
			byte[] digest = md.digest(new String(secret).getBytes("ISO-8859-1"));
			
			mutex.lock();
			sendCommand("APOP", new String[] { user, digestToString(digest)});
			
			POP3Response response = readSingleLineResponse(); 
			if (!response.isOK()) {
				throw new POP3Exception(response);
			}
			state = TRANSACTION;
		} catch (NoSuchAlgorithmException e) {
			throw new POP3Exception("Installed JRE doesn't support MD5 - APOP not possible");
		} finally {
			mutex.release();
		}
	}
	
    /**
     * Authenticates a user. This is done with the Authentication
     * mechanisms provided by the @link{org.columba.ristretto.auth.AuthenticationFactory}.
     * 
     * 
     * @param algorithm the algorithm used to authenticate the user (e.g. PLAIN, DIGEST-MD5)
     * @param user the user name
     * @param password the password
     * @throws IOException
     * @throws POP3Exception
     * @throws AuthenticationException
     */
	public void auth(String algorithm, String user, char[] password) throws IOException, POP3Exception, AuthenticationException {		
		
		mutex.lock();
		try {
			AuthenticationMechanism auth = AuthenticationFactory.getInstance().getAuthentication(algorithm);
			sendCommand("AUTH", new String[] { algorithm } );
			
			auth.authenticate(this, user, password);
		} catch (NoSuchAuthenticationException e) {
			throw new POP3Exception( e );
		} catch (AuthenticationException e) {
			throw e;
		} finally {
			mutex.release();
		}
		
		POP3Response response = readSingleLineResponse();
		mutex.release();
		
		if (!response.isOK()) {
			throw new POP3Exception(response);
		}
		state = TRANSACTION;
	}
	/**
	 * 
	 * Helper method for APOP authentication
	 * 
	 * @param digest
	 * @return the digest String
	 */
	private String digestToString(byte[] digest) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < 16; ++i) {
			if ((digest[i] & 0xFF) < 0x10) {
				sb.append("0");
			}
			sb.append(Integer.toHexString(digest[i] & 0xFF));
		}
		return sb.toString();
	}

	
	/**
	 * Authenticates a user with the simple USER PASS commands.
	 * 
	 * @param usr the username
	 * @param pass the password
	 * @throws IOException
	 * @throws POP3Exception
	 */
	public void userPass(String usr, char[] pass) throws IOException, POP3Exception {
		POP3Response response;
		mutex.lock();
		sendCommand("USER", new String[] { usr });
		try {
			response = readSingleLineResponse();
		
			if (response.isOK()) {
				sendCommand("PASS", new String[] { new String(pass) });
			
				response = readSingleLineResponse();
			}
		} 
		
		
		finally {
			mutex.release();
		}
		
		if (!response.isOK()) {
			throw new POP3Exception(response);
		}

		state = TRANSACTION;
	}

	/**
	 * Sends the STAT command to get the status of the mailbox.
	 * 
	 * @return Array of number of messages and total size.
	 * @throws IOException
	 * @throws POP3Exception
	 */
	public int[] stat() throws IOException, POP3Exception {
		checkState(TRANSACTION);		
		POP3Response response;

		mutex.lock();
		sendCommand("STAT", null);
		try {
			response = readSingleLineResponse();
		} finally {
			mutex.release();
		}
		
		if (response.isOK()) {
			Matcher matcher = statPattern.matcher(response.getMessage());
			if( matcher.find() ) {
				return new int[] { Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)) };
			}
		}

		throw new POP3Exception(response);
	}

	/**
	 * Sends the LIST command to get informations about the mails in the mailbox.
	 * 
	 * @return ScanList entries whith the information about index and size of the mails in the mailbox.
	 * @throws IOException
	 * @throws POP3Exception
	 */
	public ScanListEntry[] list() throws IOException, POP3Exception {
		checkState(TRANSACTION);
		POP3Response response;
		LinkedList list = new LinkedList();
		
		mutex.lock();
		sendCommand("LIST", null);
		try {
			response = readMultiLineResponse();
		} finally {
			mutex.release();
		}
		
		if( response.isOK() ) {
			Source source = response.getData();
			Matcher matcher = linePattern.matcher(source);

			while( matcher.find()) {
				try {
					list.add(ScanListParser.parse(matcher.group(1)));
				} catch (ParserException e) {
					// This line couldn't be parsed
					// throw no exception but simply drop it
				    LOG.severe(e.getMessage());
				}
			}
			
		}
		
		return (ScanListEntry[]) list.toArray(new ScanListEntry[] {});
	}
	
	
	/**
	 * Sends the LIST command with a index of a mail as parameter.
	 * 
	 * @param messageindex the index of the mail in the mailbox
	 * @return the index and size of the mail.
	 * @throws IOException
	 * @throws POP3Exception
	 */
	public ScanListEntry list(int messageindex) throws IOException, POP3Exception {
		checkState(TRANSACTION);
		POP3Response response;
		
		sendCommand("LIST", new String[] {Integer.toString(messageindex)});
		response = readSingleLineResponse();
		if( response.isOK() ) {
			try {
				return ScanListParser.parse( response.getMessage() );
			} catch (ParserException e) {
				throw new POP3Exception( e.getMessage() );
			}
		}
		
		throw new POP3Exception(response);
	}

	/**
	 * Asynchronously download the message from the
	 * server. This means the call to the method
	 * returns instantly while the message is downloaded 
	 * by the POP3DownloadThread. 
	 * 
	 * @param messageindex
	 * @return the message
	 * @throws IOException
	 * @throws POP3Exception
	 */
	public InputStream retr(int messageindex) throws IOException, POP3Exception {
		checkState(TRANSACTION);
		
		ScanListEntry info = list( messageindex );
		
		return retr( messageindex, info.getSize());
	}
	
	
		
	/**
	 * Asynchronously download the message from the
	 * server. This means the call to the method
	 * returns instantly while the message is downloaded 
	 * by the POP3DownloadThread. 
	 * 
	 * @param messageindex
	 * @param size 
	 * @return the message
	 * @throws IOException
	 * @throws POP3Exception
	 */
	public InputStream retr(int messageindex, int size) throws IOException, POP3Exception {
		checkState(TRANSACTION);
		POP3Response response;
		
		mutex.lock();
		sendCommand("RETR", new String[] {Integer.toString(messageindex)});
		response = readSingleLineResponse();
		if( response.isERR() ) {
			throw new POP3Exception( response );
		}
		
		return in.asyncDownload(size, mutex);
	}
	
	
	/**
	 * Synchronously download the message from the
	 * server. This means the call to the method
	 * blocks until the complete message is downloaded
	 * from the server.
	 * 
	 * @param messageindex
	 * @param size 
	 * @return the message
	 * @throws IOException
	 * @throws POP3Exception
	 */
	public InputStream sretr(int messageindex, int size) throws IOException, POP3Exception {
		checkState(TRANSACTION);
		POP3Response response;
		
		
		sendCommand("RETR", new String[] {Integer.toString(messageindex)});
		response = readSingleLineResponse();
		if( response.isERR() ) {
			throw new POP3Exception( response );
		}
		
		return in.syncDownload(size); //no mutex usage
	}
	
	
	
	/**
	 * Delete the message from the server.
	 * 
	 * @param messageindex
	 * @return <code>true</code> if the command succeeded
	 * @throws IOException
	 * @throws POP3Exception
	 */
	public boolean dele(int messageindex) throws IOException, POP3Exception {
		checkState(TRANSACTION);
		POP3Response response;
		
		try {
			mutex.lock();
			
			sendCommand("DELE", new String[] {Integer.toString(messageindex)});
			response = readSingleLineResponse();
		} finally {
			mutex.release();
		}
		
		return response.isOK();				
	}
	
	/**
	 * Send a NOOP command.
	 * 
	 * @throws IOException
	 * @throws POP3Exception
	 */
	public void noop() throws IOException, POP3Exception {
		checkState(TRANSACTION);
		POP3Response response;
		
		try {
			mutex.lock();
			
			sendCommand("NOOP", null );
			response = readSingleLineResponse();
		} finally {
			mutex.release();
		}
	
		
		if( !response.isOK() ) throw new POP3Exception( response);		
	}
	
	/**
	 * Send a RSET command to the server.
	 * 
	 * @throws IOException
	 * @throws POP3Exception
	 */
	public void rset() throws IOException, POP3Exception {
		checkState(TRANSACTION);
		POP3Response response;
		
		try {
			mutex.lock();
			
			sendCommand("RSET", null );
			response = readSingleLineResponse();
		} finally {
			mutex.release();
		}
		
		
		if( !response.isOK() ) throw new POP3Exception( response);		
	}
	
	/**
	 * Quit the connection to the server.
	 * 
	 * @throws IOException
	 * @throws POP3Exception
	 */
	public void quit() throws IOException, POP3Exception {
		try {
			mutex.lock();
			sendCommand("QUIT", null );
		} finally {
			mutex.release();
			socket.close();
		
			in = null;
			out = null;
			socket = null;
		
			state = NOT_CONNECTED;

		}
	}

	/**
	 * Send the TOP command to get the fist n lines
	 * of the message. Not supported by every server.
	 * 
	 * 
	 * @param messageindex
	 * @param numberOfLines
	 * @return the top of the message
	 * @throws IOException
	 * @throws POP3Exception
	 */
	public Source top(int messageindex, int numberOfLines) throws IOException, POP3Exception {
		checkState(TRANSACTION);
		POP3Response response;
		try {
			mutex.lock();
			sendCommand("TOP", new String[] {Integer.toString(messageindex), Integer.toString(numberOfLines)});
			response = readMultiLineResponse();

		} finally {
			mutex.release();
		}
		
		return response.getData();		
	}

	/**
	 * Fetch the UID of the messages.
	 * 
	 * @param messageindex
	 * @return the UidListEntry
	 * @throws IOException
	 * @throws POP3Exception
	 */
	public UidListEntry uidl(int messageindex) throws IOException, POP3Exception {
		checkState(TRANSACTION);
		POP3Response response;
		
		try {
			mutex.lock();
			sendCommand("UIDL", new String[] {Integer.toString(messageindex) } );
			response = readSingleLineResponse();
		} finally {
			mutex.release();
		}
	
		
		if( response.isOK() ) {
			try {
				return UIDListParser.parse( response.getMessage() );				
			} catch (ParserException e) {
				throw new POP3Exception( e.getMessage() );
			}
		}
		
		throw new POP3Exception(response);
	}
	
	/**
	 * Fetch the UIDs of all messages.
	 * 
	 * @return the UidListEntries
	 * @throws IOException
	 * @throws POP3Exception
	 */
	public UidListEntry[] uidl() throws IOException, POP3Exception {
		checkState(TRANSACTION);
		POP3Response response;
		LinkedList list = new LinkedList();
		
		try {
			mutex.lock();
			sendCommand("UIDL", null);
			response = readMultiLineResponse();
		} finally {
			mutex.release();
		}
		
		if( response.isOK() ) {
			Source source = response.getData();
			Matcher matcher = linePattern.matcher(source);

			while( matcher.find()) {
				try {
					list.add(UIDListParser.parse(matcher.group(1)));
				} catch (ParserException e) {
					// This line couldn't be parsed
				    LOG.severe(e.getMessage());
				}
			}
			
		} else throw new CommandNotSupportedException("UIDL");
		
		return (UidListEntry[]) list.toArray(new UidListEntry[] {});
	}

	/**
	 * Get the capabilities of the server.
	 * 
	 * @return the capablities
	 * @throws IOException
	 * @throws POP3Exception
	 */
	public String[] capa() throws IOException, POP3Exception {
		POP3Response response;
		LinkedList list = new LinkedList();
		
		try {
			mutex.lock();
			sendCommand("CAPA", null);
			response = readMultiLineResponse();
		} finally {
			mutex.release();
		}
		
		if( response.isOK() ) {
			Source source = response.getData();
			Matcher matcher = linePattern.matcher(source);

			while( matcher.find()) {
				list.add(matcher.group(2)); 
			}
			
		} else {
			throw new CommandNotSupportedException("CAPA");
		}
		
		return (String[]) list.toArray(new String[] {});
	}
	
	

	/**
	 * @see org.columba.ristretto.auth.AuthenticationServer#authSend(byte[])
	 */
	public void authSend(byte[] call) throws IOException {
		out.write(Base64.encode(ByteBuffer.wrap(call), false).toString()
				.getBytes("Us-ASCII"));
		out.write('\r');
		out.write('\n');
	    out.flush();
	}

	/**
	 * @see org.columba.ristretto.auth.AuthenticationServer#authReceive()
	 */
	public byte[] authReceive() throws AuthenticationException, IOException {
	    try {
                POP3Response response = in.readSingleLineResponse();
                if(response.isOK()) {
                	if( response.getMessage() != null) {
                		return Base64.decodeToArray(response.getMessage());
                	} else {
                		return new byte[0];
                	}
                }
                throw new AuthenticationException(new POP3Exception(response));
            } catch (POP3Exception e) {
                throw new AuthenticationException( e );
            } 
	}

	/**
	 * @return the actual state
	 */
	public int getState() {
		return state;
	}
	
	private void createStreams() throws IOException {
	    if( RistrettoLogger.logStream != null ) {        
	        in =
	            new POP3InputStream( new LogInputStream(
	                    socket.getInputStream(),RistrettoLogger.logStream ));
	        out = new LogOutputStream( socket.getOutputStream(),RistrettoLogger.logStream) ;
	    } else {
	        in =
	            new POP3InputStream( 
	                    socket.getInputStream());
	        out = socket.getOutputStream();
	        
	    }
	}
    /**
     * @see org.columba.ristretto.auth.AuthenticationServer#getHostName()
     */
    public String getHostName() {
        return servername;
    }
    
    /**
     * @see org.columba.ristretto.auth.AuthenticationServer#getService()
     */
    public String getService() {
        return "pop3";
    }
    
	/**
	 * Drops the connection.
	 * 
	 * @throws IOException
	 *  
	 */
	public void dropConnection() throws IOException {
		if (state != NOT_CONNECTED) {
			state = NOT_CONNECTED;

			socket.close();
			in = null;
			out = null;
			socket = null;
			
			mutex.release();
		}
	}
    
}
