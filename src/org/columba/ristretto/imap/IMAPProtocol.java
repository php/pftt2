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
package org.columba.ristretto.imap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;

import org.columba.ristretto.auth.AuthenticationException;
import org.columba.ristretto.auth.AuthenticationFactory;
import org.columba.ristretto.auth.AuthenticationMechanism;
import org.columba.ristretto.auth.AuthenticationServer;
import org.columba.ristretto.auth.NoSuchAuthenticationException;
import org.columba.ristretto.coder.Base64;
import org.columba.ristretto.config.RistrettoConfig;
import org.columba.ristretto.imap.parser.AppendInfoParser;
import org.columba.ristretto.imap.parser.CopyInfoParser;
import org.columba.ristretto.imap.parser.FlagsParser;
import org.columba.ristretto.imap.parser.IMAPHeaderParser;
import org.columba.ristretto.imap.parser.ListInfoParser;
import org.columba.ristretto.imap.parser.MailboxInfoParser;
import org.columba.ristretto.imap.parser.MailboxStatusParser;
import org.columba.ristretto.imap.parser.MimeTreeParser;
import org.columba.ristretto.imap.parser.NamespaceParser;
import org.columba.ristretto.imap.parser.NumberListParser;
import org.columba.ristretto.imap.parser.QuotaInfoParser;
import org.columba.ristretto.imap.parser.StringListParser;
import org.columba.ristretto.imap.parser.UIDParser;
import org.columba.ristretto.log.LogInputStream;
import org.columba.ristretto.log.LogOutputStream;
import org.columba.ristretto.log.RistrettoLogger;
import org.columba.ristretto.message.MailboxInfo;
import org.columba.ristretto.message.MimeTree;
import org.columba.ristretto.parser.ParserException;
import org.columba.ristretto.ssl.RistrettoSSLSocketFactory;

/**
 * IMAP Protocol implementation.
 * 
 * @author Timo Stich <tstich@users.sourceforge.net>
 */
public class IMAPProtocol implements AuthenticationServer {
	/**
	 * @deprecated Use NOT_CONNECTED instead
	 * 
	 */	
	public static final int LOGOUT = 0;
	
	/**
	 * State of the Protocol.
	 * 
	 */
	public static final int NOT_CONNECTED = 0;
	
	/**
	 * State of the Protocol.
	 * 
	 */
	public static final int NON_AUTHENTICATED = 1;

	/**
	 * State of the Protocol.
	 * 
	 */
	public static final int AUTHENTICATED = 2;

	/**
	 * State of the Protocol.
	 * 
	 */
	public static final int SELECTED = 3;

	/**
	 * The default IMAP port is 143.
	 */
	public static final int DEFAULT_PORT = 143;

	/**
	 * The default IMAPS port is 993.
	 */
	public static final int DEFAULT_SSL_PORT = 993;

	private TagFactory tagFactory;

	protected String host;

	protected int port;

	private IMAPInputStream in;

	private OutputStream out;

	private Socket socket;

	private int state;

	private String selectedMailbox;

	private Object lock;

	private ArrayList listeners;

	/**
	 * Constructs the IMAPProtocol2.
	 * 
	 * @param host
	 *            Address of the IMAP Server
	 * @param port
	 *            Port to connect to
	 */
	public IMAPProtocol(String host, int port) {
		this.host = host;
		this.port = port;

		tagFactory = new TagFactory();

		state = NOT_CONNECTED;
		listeners = new ArrayList();
	}

	/**
	 * Opens a socket to the IMAP server.
	 * 
	 * @throws IOException
	 * @throws IMAPException
	 */
	public void openPort() throws IOException, IMAPException {
		socket = new Socket(host, port);
		socket.setSoTimeout(RistrettoConfig.getInstance().getTimeout());

		createStreams();

		IMAPResponse response;
		try {
			response = in.readResponse();
		} catch (IOException e) {
			state = NOT_CONNECTED;
			throw e;
		}
		// Answer can be OK, PRE-AUTH or BYE

		if (response.isBYE())
			throw new IMAPException(response);

		if (response.isOK())
			state = NON_AUTHENTICATED;
		else
			// must be a PRE-AUTH response -> we are already authenticated
			state = AUTHENTICATED;
	}

	/**
	 * Opens a SSL socket to the IMAP server.
	 * 
	 * 
	 * @throws IOException
	 * @throws SSLException
	 * @throws IMAPException
	 */	
	public void openSSLPort() throws IOException, SSLException, IMAPException {
		socket = RistrettoSSLSocketFactory.getInstance().createSocket(host,
				port);
		socket.setSoTimeout(RistrettoConfig.getInstance().getTimeout());

		// handshake (which cyper algorithms are used?)
		((SSLSocket) socket).startHandshake();

		createStreams();

		IMAPResponse response;
		try {
			response = in.readResponse();
		} catch (IOException e) {
			state = NOT_CONNECTED;
			throw e;
		}
		// Answer can be OK, PRE-AUTH or BYE

		if (response.isBYE())
			throw new IMAPException(response);

		if (response.isOK())
			state = NON_AUTHENTICATED;
		else
			// must be a PRE-AUTH response -> we are already authenticated
			state = AUTHENTICATED;
	}

	// Commands in ALL STATES

	/**
	 * Gets the capabilities of the IMAP server. 
	 *
	 * @return the capabilities of the server 
	 * @throws IOException 
	 * @throws IMAPException 
	 * 
	 */
	public String[] capability() throws IOException, IMAPException {
		checkState(NON_AUTHENTICATED);
		IMAPCommand command = new IMAPCommand(tagFactory.nextTag(),
				"CAPABILITY");

		IMAPResponse[] responses = communicate(command);
		ArrayList result = new ArrayList();
		for (int i = 0; i < responses.length - 1; i++) {
			if (responses[i].getResponseSubType().equals("CAPABILITY")) {
				result.addAll(Arrays.asList(StringListParser.parse(responses[0]
						.getResponseMessage())));
			} else {
				handleResponse(responses[i]);
			}
		}

		// Check last response for command success
		if (!responses[responses.length - 1].isOK())
			throwException(responses[responses.length - 1]);

		return (String[]) result.toArray(new String[0]);
	}

	/**
	 * Sends the NOOP command to the server. This can be used to poll for new
	 * messages. To do this register as an #IMAPListener with this protocol.
	 * 
	 * @throws IOException
	 * @throws IMAPException
	 */
	public void noop() throws IOException, IMAPException {
		checkState(NON_AUTHENTICATED);

		IMAPCommand command = new IMAPCommand(tagFactory.nextTag(), "NOOP");
		IMAPResponse[] responses = communicate(command);
		// handle any responses that do not directly
		// relate to the command
		for (int i = 0; i < responses.length - 1; i++) {
			handleResponse(responses[i]);
		}

		// Check last response for command success
		if (!responses[responses.length - 1].isOK())
			throwException(responses[responses.length - 1]);
	}

	/**
	 * @param responses
	 * @throws IMAPException
	 */
	private void throwException(IMAPResponse response) throws IMAPException {
		if (response.isBYE()) {
			handleResponse(response);
		} else {
			throw new IMAPException(response);
		}
	}

	/**
	 * Close the socket to the server.
	 * 
	 * @throws IOException
	 * @throws IMAPException
	 */
	public void logout() throws IOException, IMAPException {
		checkState(NON_AUTHENTICATED);
		IMAPCommand command = new IMAPCommand(tagFactory.nextTag(), "LOGOUT");

		IMAPResponse[] responses = communicate(command);
		// handle any responses that do not directly
		// relate to the command
		for (int i = 0; i < responses.length; i++) {
			if (!responses[i].isBYE()) {
				handleResponse(responses[i]);
			}
		}

		state = NOT_CONNECTED;
		// Close the streams
		in.close();
		in = null;
		out.close();
		out = null;
	}

	// Commands in NON-AUTHENTICATES state
	/**
	 * Sends the TLS command and switches to a SSL encrypted connection. <br>
	 * Protocol has to be in NON_AUTHENTICATED state.
	 * 
	 * @see #getState()
	 * 
	 * @throws IOException
	 * @throws SSLException
	 * @throws IMAPException
	 */
	public void startTLS() throws IOException, SSLException, IMAPException {
		checkState(NON_AUTHENTICATED);

		IMAPCommand command = new IMAPCommand(tagFactory.nextTag(), "STARTTLS");
		IMAPResponse[] responses = communicate(command);
		// handle any responses that do not directly
		// relate to the command
		for (int i = 0; i < responses.length - 1; i++) {
			handleResponse(responses[i]);
		}

		// Check last response for command success
		if (!responses[responses.length - 1].isOK())
			throwException(responses[responses.length - 1]);

		socket = RistrettoSSLSocketFactory.getInstance().createSocket(socket,
				host, port, true);

		// handshake (which cyper algorithms are used?)
		((SSLSocket) socket).startHandshake();

		createStreams();
	}

	/**
	 * Authenticate with the specified SASL method. The supported
	 * methods from the server can be found out with the #capability()
	 * command.
	 * <br>Protocol has to be in NON_AUTHENTICATED state.
	 * @see #getState()
	 * 
	 * @param method the SASL method
	 * @param user
	 * @param password
	 * @throws IOException
	 * @throws IMAPException
	 * @throws AuthenticationException
	 */
	public void authenticate(String method, String user, char[] password)
			throws IOException, IMAPException, AuthenticationException {
		checkState(NON_AUTHENTICATED);
		String tag = tagFactory.nextTag();

		try {
			AuthenticationMechanism auth = AuthenticationFactory.getInstance()
					.getAuthentication(method);
			IMAPCommand command = new IMAPCommand(tag, "AUTHENTICATE",
					new Object[] { method });
			
		
			try{
				command.writeToStream(in, out);
			} catch (IOException e) {
				state = NOT_CONNECTED;
				throw e;
			}

			auth.authenticate(this, user, password);
		} catch (NoSuchAuthenticationException e) {
			throw new IMAPException(e);
		}
		
		IMAPResponse response;
		
		try {
			response = in.readResponse();
		} catch (SocketException e) {
			state = NOT_CONNECTED;
			throw e;
		}

		// handle any responses that do not directly
		// relate to the command
		while (!response.isTagged()) {
			handleResponse(response);
			try {
				response = in.readResponse();
			} catch (SocketException e) {
				state = NOT_CONNECTED;
				throw e;
			}
		}

		if (!response.isOK())
			throw new IMAPException(response);
		state = AUTHENTICATED;
	}

	
	/**
	 * Sends the LOGIN command to authenticate with
	 * the server.
	 * <br>Protocol has to be in NON_AUTHENTICATED state.
	 * @see #getState()
	 * 
	 * @param user
	 * @param password
	 * @throws IOException
	 * @throws IMAPException
	 */
	public void login(String user, char[] password) throws IOException,
			IMAPException {
		checkState(NON_AUTHENTICATED);
		IMAPCommand command = new IMAPCommand(tagFactory.nextTag(), "LOGIN",
				new Object[] { user, new String(password) });
		
		IMAPResponse[] responses = communicate(command);

		// handle any responses that do not directly
		// relate to the command
		for (int i = 0; i < responses.length - 1; i++) {
			handleResponse(responses[i]);
		}

		// Check last response for command success
		if (!responses[responses.length - 1].isOK())
			throwException(responses[responses.length - 1]);
		state = AUTHENTICATED;
	}

	// Commands in AUTHENTICATED state

	/**
	 * Select a mailbox. To get a list of all subscribed
	 * mailboxes use the #lsub(String, String) command.
	 *
	 * <br>Protocol has to be in AUTHENTICATED state.
	 * @see #getState()
	 * 
	 * @param mailbox the mailbox to select
	 * @return the MailboxInfo of the selected mailbox
	 * @throws IOException
	 * @throws IMAPException
	 */
	public MailboxInfo select(String mailbox) throws IOException, IMAPException {
		return selectCore(mailbox, "SELECT");
	}

	private MailboxInfo selectCore(String mailbox, String s)
			throws IMAPException, IOException {
		checkState(AUTHENTICATED);
		IMAPCommand command = new IMAPCommand(tagFactory.nextTag(), s,
				new Object[] { MailboxNameUTF7Converter.encode(mailbox) });

		IMAPResponse[] responses = communicate(command);

		MailboxInfo result = new MailboxInfo();

		for (int i = 0; i < responses.length - 1; i++) {
			if (responses[i].getResponseType() == IMAPResponse.RESPONSE_MAILBOX_DATA
					|| responses[i].isOK()) {
				try {
					result = MailboxInfoParser.parse(responses[i], result);
				} catch (ParserException e1) {
					throw new IMAPException(e1);
				}
			} else {
				handleResponse(responses[i]);
			}
		}

		// Check last response for command success
		if (!responses[responses.length - 1].isOK()) {
			state = AUTHENTICATED;
			throwException(responses[responses.length - 1]);
		}

		// Parse the last response to find
		try {
			result = MailboxInfoParser.parse(responses[responses.length - 1],
					result);
		} catch (ParserException e1) {
			throw new IMAPException(e1);
		}

		state = SELECTED;
		selectedMailbox = mailbox;

		return result;
	}

	/**
	 * Sends the EXAMINE command to get the mailbox info. 
	 * <br>Protocol has to be in AUTHENTICATED state.
	 * @see #getState()
	 * 
	 * @param mailbox
	 * @return the MailboxInfo of the examined mailbox.
	 * @throws IOException
	 * @throws IMAPException
	 */
	public MailboxInfo examine(String mailbox) throws IOException,
			IMAPException {
		return selectCore(mailbox, "EXAMINE");
	}

	/**
	 * Creates a new mailbox. Be sure to also #subscribe(String)
	 * to the mailbox before selecting it.
	 * <br>Protocol has to be in AUTHENTICATED state.
	 * @see #getState()
	 * 
	 * @param mailbox the mailbox to create
	 * @throws IOException
	 * @throws IMAPException
	 */
	public void create(String mailbox) throws IOException, IMAPException {
		checkState(AUTHENTICATED);
		IMAPCommand command = new IMAPCommand(tagFactory.nextTag(), "CREATE",
				new Object[] { MailboxNameUTF7Converter.encode(mailbox) });

		IMAPResponse[] responses = communicate(command);
		// handle any responses that do not directly
		// relate to the command
		for (int i = 0; i < responses.length - 1; i++) {
			handleResponse(responses[i]);
		}

		// Check last response for command success
		if (!responses[responses.length - 1].isOK())
			throwException(responses[responses.length - 1]);
	}

	/**
	 * Deletes the mailbox. Most likely you also want to
	 * #unsubscribe(String).
	 * <br>Protocol has to be in AUTHENTICATED state.
	 * @see #getState()
	 * 
	 * @param mailbox the mailbox to delete
	 * @throws IOException
	 * @throws IMAPException
	 */
	public void delete(String mailbox) throws IOException, IMAPException {
		checkState(AUTHENTICATED);
		IMAPCommand command = new IMAPCommand(tagFactory.nextTag(), "DELETE",
				new Object[] { MailboxNameUTF7Converter.encode(mailbox) });

		IMAPResponse[] responses = communicate(command);
		// handle any responses that do not directly
		// relate to the command
		for (int i = 0; i < responses.length - 1; i++) {
			handleResponse(responses[i]);
		}

		// Check last response for command success
		if (!responses[responses.length - 1].isOK())
			throwException(responses[responses.length - 1]);
	}

	
	/**
	 * Gets the QuotaInfo of the mailbox. Works only if the
	 * server supports QUOTA capability.
	 * <br>Protocol has to be in AUTHENTICATED state.
	 * @see #getState()
	 * 
	 * @param mailbox
	 * @return the QutaInfo of this mailbox.
	 * @throws IOException
	 * @throws IMAPException
	 */
	public QuotaInfo getQuota(String mailbox) throws IOException, IMAPException {
		checkState(AUTHENTICATED);
		IMAPCommand command = new IMAPCommand(tagFactory.nextTag(),
				"GETQUOTAROOT", new Object[] { MailboxNameUTF7Converter
						.encode(mailbox) });

		IMAPResponse[] responses = communicate(command);
		QuotaInfo result = null;
		for (int i = 0; i < responses.length - 1; i++) {
			if (responses[i].getResponseType() == IMAPResponse.RESPONSE_MAILBOX_DATA) {
				try {
					result = QuotaInfoParser.parse(responses[i], result);
				} catch (ParserException e) {
					throw new IMAPException(e);
				}
			} else {
				handleResponse(responses[i]);
			}
		}

		// Check last response for command success
		if (!responses[responses.length - 1].isOK())
			throwException(responses[responses.length - 1]);

		return result;
	}

	/**
	 * Renames the mailbox. Most likely you also want
	 * to #subscribe(String) and #unsubscribe(String).
	 * <br>Protocol has to be in AUTHENTICATED state.
	 * @see #getState()
	 * 
	 * @param oldname
	 * @param newname
	 * @throws IOException
	 * @throws IMAPException
	 */
	public void rename(String oldname, String newname) throws IOException,
			IMAPException {
		checkState(AUTHENTICATED);
		IMAPCommand command = new IMAPCommand(tagFactory.nextTag(), "RENAME",
				new Object[] { MailboxNameUTF7Converter.encode(oldname),
						MailboxNameUTF7Converter.encode(newname) });

		IMAPResponse[] responses = communicate(command);
		// handle any responses that do not directly
		// relate to the command
		for (int i = 0; i < responses.length - 1; i++) {
			handleResponse(responses[i]);
		}

		// Check last response for command success
		if (!responses[responses.length - 1].isOK())
			throwException(responses[responses.length - 1]);
	}

	/**
	 * Subscribes to a mailbox. Only subscribed mailboxes
	 * can be selected.
	 * <br>Protocol has to be in AUTHENTICATED state.
	 * @see #getState()
	 * 
	 * @see #unsubscribe(String)
	 * @param mailbox
	 * @throws IOException
	 * @throws IMAPException
	 */
	public void subscribe(String mailbox) throws IOException, IMAPException {
		checkState(AUTHENTICATED);
		IMAPCommand command = new IMAPCommand(tagFactory.nextTag(),
				"SUBSCRIBE", new Object[] { MailboxNameUTF7Converter
						.encode(mailbox) });

		IMAPResponse[] responses = communicate(command);
		// handle any responses that do not directly
		// relate to the command
		for (int i = 0; i < responses.length - 1; i++) {
			handleResponse(responses[i]);
		}

		// Check last response for command success
		if (!responses[responses.length - 1].isOK())
			throwException(responses[responses.length - 1]);
	}

	/**
	 * Unsubscribes a subscribed mailbox.
	 * <br>Protocol has to be in AUTHENTICATED state.
	 * @see #getState()
	 * 
	 * @see #subscribe(String)
	 * 
	 * @param mailbox
	 * @throws IOException
	 * @throws IMAPException
	 */
	public void unsubscribe(String mailbox) throws IOException, IMAPException {
		checkState(AUTHENTICATED);
		IMAPCommand command = new IMAPCommand(tagFactory.nextTag(),
				"UNSUBSCRIBE", new Object[] { MailboxNameUTF7Converter
						.encode(mailbox) });

		IMAPResponse[] responses = communicate(command);
		// handle any responses that do not directly
		// relate to the command
		for (int i = 0; i < responses.length - 1; i++) {
			handleResponse(responses[i]);
		}

		// Check last response for command success
		if (!responses[responses.length - 1].isOK())
			throwException(responses[responses.length - 1]);
	}

	
	/**
	 * Lists available mailboxes on the server.
	 * You have to subscribe to a mailbox before you can
	 * select it.
	 * <br>Protocol has to be in AUTHENTICATED state.
	 * @see #getState()
	 * 
	 * 
	 * @param reference
	 * @param mailbox
	 * @return the ListInfo of the listed mailboxes
	 * @throws IOException
	 * @throws IMAPException
	 */
	public ListInfo[] list(String reference, String mailbox)
			throws IOException, IMAPException {
		return listCore(reference, mailbox, "LIST");
	}

	/**
	 * @param reference
	 * @param mailbox
	 * @return @throws
	 *         IMAPException
	 * @throws IOException
	 */
	private ListInfo[] listCore(String reference, String mailbox, String s)
			throws IMAPException, IOException {
		checkState(AUTHENTICATED);
		IMAPCommand command = new IMAPCommand(tagFactory.nextTag(), s,
				new Object[] { MailboxNameUTF7Converter.encode(reference),
						MailboxNameUTF7Converter.encode(mailbox) });

		IMAPResponse[] responses = communicate(command);
		// Check last response for command success
		if (!responses[responses.length - 1].isOK())
			throwException(responses[responses.length - 1]);

		List result = new ArrayList();

		for (int i = 0; i < responses.length - 1; i++) {
			if (responses[i].getResponseSubType().equals(s)) {
				try {
					result.add(ListInfoParser.parse(responses[i]));
				} catch (ParserException e) {
					throw new IMAPException(e);
				}
			} else {
				handleResponse(responses[i]);
			}
		}

		return (ListInfo[]) result.toArray(new ListInfo[0]);
	}

	/**
	 * Lists all subscribed mailboxes.
	 * <br>Protocol has to be in AUTHENTICATED state.
	 * @see #getState()
	 * 
	 * @param reference
	 * @param mailbox
	 * @return the ListInfo of the subscribed mailboxes
	 * @throws IOException
	 * @throws IMAPException
	 */
	public ListInfo[] lsub(String reference, String mailbox)
			throws IOException, IMAPException {
		return listCore(reference, mailbox, "LSUB");
	}

	/**
	 * Sends the STATUS command to get the status
	 * of a mailbox.
	 * <br>Protocol has to be in AUTHENTICATED state.
	 * @see #getState()
	 * 
	 * @param mailbox
	 * @param statusItems
	 * @return the MailboxStatus of the mailbox
	 * @throws IOException
	 * @throws IMAPException
	 */
	public MailboxStatus status(String mailbox, String[] statusItems)
			throws IOException, IMAPException {
		checkState(AUTHENTICATED);

		IMAPCommand command = new IMAPCommand(tagFactory.nextTag(), "STATUS",
				new Object[] { MailboxNameUTF7Converter.encode(mailbox),
						statusItems });

		IMAPResponse[] responses = communicate(command);

		MailboxStatus result = null;

		// handle any responses that do not directly
		// relate to the command
		for (int i = 0; i < responses.length - 1; i++) {
			if (responses[i].getResponseSubType().equals("STATUS")) {
				try {
					result = MailboxStatusParser.parse(responses[i]);
				} catch (ParserException e) {
					throw new IMAPException(responses[i]);
				}
			} else {
				handleResponse(responses[i]);
			}
		}
		// Check last response for command success
		if (!responses[responses.length - 1].isOK())
			throwException(responses[responses.length - 1]);

		return result;
	}

	/**
	 * @param mailbox
	 * @param message
	 * @param optargs
	 *            Can be IMAPFlags and IMAPDate. If you use the IMAPFlags be
	 *            sure that only the System Flags are set. Otherwise a error
	 *            from the Server will occur.
	 * 
	 * If the server has the UIDPLUS (RFC2359) capability the new UID of the
	 * appended message is returned.
	 * <br>Protocol has to be in AUTENTICATED state.
	 * @see #getState()

	 * @return the AppendInfo if UIDPLUS is supported, <code>null</code> otherwise
	 * 
	 * @throws IOException
	 * @throws IMAPException
	 */
	public AppendInfo append(String mailbox, InputStream message,
			Object[] optargs) throws IOException, IMAPException {
		checkState(AUTHENTICATED);

		List args = new LinkedList();
		args.add(MailboxNameUTF7Converter.encode(mailbox));
		if (optargs != null) {
			for (int i = 0; i < optargs.length; i++) {
				args.add(optargs[i]);
			}
		}
		args.add(message);

		IMAPCommand command = new IMAPCommand(tagFactory.nextTag(), "APPEND",
				args.toArray());
		IMAPResponse[] responses = communicate(command);
		// handle any responses that do not directly
		// relate to the command
		for (int i = 0; i < responses.length - 1; i++) {
			handleResponse(responses[i]);
		}
		// Check last response for command success
		if (!responses[responses.length - 1].isOK())
			throwException(responses[responses.length - 1]);

		AppendInfo result = null;
		if (responses[responses.length - 1].getResponseTextCode() != null) {
			try {
				result = AppendInfoParser
						.parse(responses[responses.length - 1]);
			} catch (ParserException e1) {
				throw new IMAPException(e1);
			}
		}

		return result;
	}

	/**
	 * @deprecated Use #append(String, InputStream, Object[]) instead
	 * 
	 * @param mailbox
	 * @param message
	 * @param optargs
	 * 
	 * add APPENDUID response code returned when UIDPLUS capability is present.
	 * @return the AppendInfo
	 * 
	 * @throws IOException
	 * @throws IMAPException
	 */
	public AppendInfo appendPlus(String mailbox, InputStream message,
			Object[] optargs) throws IOException, IMAPException {
		return append(mailbox, message, optargs);
	}

	/**
	 * @param mailbox
	 * @param message
	 * 
	 * If the server has the UIDPLUS (RFC2359) capability the new UID of the
	 * appended message is returned.
	 * <br>Protocol has to be in AUTHENTICATED state.
	 * @see #getState()
	 * @return the AppendInfo if UIDPLUS is supported, <code>null</code> otherwise
	 * 
	 * @throws IOException
	 * @throws IMAPException
	 */
	public AppendInfo append(String mailbox, InputStream message)
			throws IOException, IMAPException {
		return append(mailbox, message, null);
	}

	// Selected State

	
	
	/**
	 * Sends the CHECK command to the server.
	 * 
	 * <br>Protocol has to be in SELECTED state.
	 * @see #getState()
	 * @throws IOException
	 * @throws IMAPException
	 */
	public void check() throws IOException, IMAPException {
		checkState(SELECTED);
		IMAPCommand command = new IMAPCommand(tagFactory.nextTag(), "CHECK");
		IMAPResponse[] responses = communicate(command);
		// handle any responses that do not directly
		// relate to the command
		for (int i = 0; i < responses.length - 1; i++) {
			handleResponse(responses[i]);
		}
		// Check last response for command success
		if (!responses[responses.length - 1].isOK())
			throwException(responses[responses.length - 1]);
	}

	/**
	 * Closes the selected mailbox.
	 * 
	 * <br>Protocol has to be in SELECTED state.
	 * @see #getState()
	 * 
	 * @throws IOException
	 * @throws IMAPException
	 */
	public void close() throws IOException, IMAPException {
		checkState(SELECTED);
		IMAPCommand command = new IMAPCommand(tagFactory.nextTag(), "CLOSE");
		IMAPResponse[] responses = communicate(command);
		// handle any responses that do not directly
		// relate to the command
		for (int i = 0; i < responses.length - 1; i++) {
			handleResponse(responses[i]);
		}
		// Check last response for command success
		if (!responses[responses.length - 1].isOK())
			throwException(responses[responses.length - 1]);
		state = AUTHENTICATED;
	}

	/**
	 * Expunges the selected mailbox.
	 * <br>Protocol has to be in SELECTED state.
	 * @see #getState()
	 * 
	 * @return the indices of the expunged messages
	 * @throws IOException
	 * @throws IMAPException
	 */
	public int[] expunge() throws IOException, IMAPException {
		checkState(SELECTED);
		IMAPCommand command = new IMAPCommand(tagFactory.nextTag(), "EXPUNGE");
		IMAPResponse[] responses = communicate(command);
		// Check last response for command success
		if (!responses[responses.length - 1].isOK())
			throwException(responses[responses.length - 1]);

		List expunged = new ArrayList(responses.length - 1);
		
		for (int i = 0; i < responses.length - 1; i++) {
			if (responses[i].getResponseSubType().equals("EXPUNGE")) {
				expunged.add( new Integer(responses[i].getPreNumber()) );
			} else {
				handleResponse(responses[i]);
			}
		}

		int[] expungedArray = new int[expunged.size()];
		for( int i=0; i<expunged.size(); i++) {
			expungedArray[i] = ((Integer)expunged.get(i)).intValue();
		}
		
		return expungedArray;
	}


	
	/**
	 * Search the selected mailbox.
	 * <br>Protocol has to be in SELECTED state.
	 * @see #getState()
	 * 
	 * @param charset the charset of the search parameters
	 * @param search
	 * @return the indices of the found messages
	 * @throws IOException
	 * @throws IMAPException
	 */
	public Integer[] search(Charset charset, SearchKey[] search)
			throws IOException, IMAPException {
		return searchCore("SEARCH", charset, search);
	}

	/**
	 * Search the selected mailbox.
	 * <br>Protocol has to be in SELECTED state.
	 * @see #getState()
	 * 
	 * @param charset the charset of the search parameters
	 * @param search
	 * @return the UIDs of the found messages
	 * @throws IOException
	 * @throws IMAPException
	 */
	public Integer[] uidSearch(Charset charset, SearchKey[] search)
			throws IOException, IMAPException {
		return searchCore("UID SEARCH", charset, search);
	}

	/**
	 * @param charset
	 * @param search
	 * @return @throws
	 *         IMAPException
	 * @throws IOException
	 */
	private Integer[] searchCore(String c, Charset charset, SearchKey[] search)
			throws IMAPException, IOException {
		IMAPCommand command;
		checkState(SELECTED);

		List args = new LinkedList();
		if (charset != null) {
			args.add("CHARSET");
			args.add(charset);
		}
		for (int i = 0; i < search.length; i++) {
			args.add(search[i]);
		}

		if (charset == null) {
			command = new IMAPCommand(tagFactory.nextTag(), c, args.toArray());
		} else {
			command = new IMAPCommand(tagFactory.nextTag(), c, args.toArray(),
					charset);
		}

		Integer[] result = null;
		IMAPResponse[] responses = communicate(command);
		for (int i = 0; i < responses.length - 1; i++) {
			if (responses[i].getResponseSubType().equals("SEARCH")) {
				result = NumberListParser.parse(responses[i]);
			} else {
				handleResponse(responses[i]);
			}
		}

		// Check last response for command success
		if (!responses[responses.length - 1].isOK())
			throwException(responses[responses.length - 1]);

		return result;
	}

	/**
	 * Search the selected mailbox.
	 * <br>Protocol has to be in SELECTED state.
	 * @see #getState()
	 * 
	 * @param search
	 * @return the indices of the found messages
	 * @throws IOException
	 * @throws IMAPException
	 */
	public Integer[] search(SearchKey[] search) throws IOException,
			IMAPException {
		return searchCore("SEARCH", null, search);
	}

	/**
	 * Search the selected mailbox.
	 * <br>Protocol has to be in SELECTED state.
	 * @see #getState()
	 * 
	 * @param search
	 * @return the UIDs of the found messages
	 * @throws IOException
	 * @throws IMAPException
	 */
	public Integer[] uidSearch(SearchKey[] search) throws IOException,
			IMAPException {
		return searchCore("UID SEARCH", null, search);
	}

	/**
	 * Fetch the Flags of the specified messages.
	 * 
	 * @param set the indices of the messages
	 * @return the Flags of the messages
	 * @throws IOException
	 * @throws IMAPException
	 */
	public IMAPFlags[] fetchFlags(SequenceSet set) throws IOException,
			IMAPException {
		checkState(SELECTED);

		IMAPCommand command = new IMAPCommand(tagFactory.nextTag(), "FETCH",
				new Object[] { set, new String[] { "FLAGS", "UID" } });

		IMAPResponse[] responses = communicate(command);

		ArrayList result = new ArrayList(responses.length - 1);
		for (int i = 0; i < responses.length - 1; i++) {
			if (responses[i].getResponseSubType().equals("FETCH")) {
				result.add(FlagsParser.parse(responses[i]));
			} else {
				handleResponse(responses[i]);
			}
		}

		// Check last response for command success
		if (!responses[responses.length - 1].isOK())
			throwException(responses[responses.length - 1]);

		return (IMAPFlags[]) result.toArray(new IMAPFlags[0]);
	}

	/**
	 * Fetch the Flags of the specified messages.
	 * 
	 * @param set the UIDs of the messages
	 * @return the Flags of the messages
	 * @throws IOException
	 * @throws IMAPException
	 */
	public Integer[] fetchUid(SequenceSet set) throws IOException,
			IMAPException {
		checkState(SELECTED);

		IMAPCommand command = new IMAPCommand(tagFactory.nextTag(), "FETCH",
				new Object[] { set, "UID" });

		IMAPResponse[] responses = communicate(command);

		ArrayList result = new ArrayList(responses.length - 1);

		try {
			for (int i = 0; i < responses.length - 1; i++) {
				if (responses[i].getResponseSubType().equals("FETCH")) {
					result.add(UIDParser.parse(responses[i]));
				} else {
					handleResponse(responses[i]);
				}
			}
		} catch (ParserException e) {
			throw new IMAPException(e);
		}

		// Check last response for command success
		if (!responses[responses.length - 1].isOK())
			throwException(responses[responses.length - 1]);

		return (Integer[]) result.toArray(new Integer[0]);
	}

	private IMAPHeader[] fetchHeaderFieldsCore(String c, SequenceSet set,
			String[] fields) throws IOException, IMAPException {
		checkState(SELECTED);

		IMAPCommand command = new IMAPCommand(tagFactory.nextTag(), c,
				new Object[] {
						set,
						new Section("RFC822.SIZE BODY.PEEK", new Object[] {
								"HEADER.FIELDS", fields }) });

		IMAPResponse[] responses = communicate(command);

		ArrayList result = new ArrayList(responses.length - 1);

		try {
			for (int i = 0; i < responses.length - 1; i++) {
				if (responses[i].getResponseSubType().equals("FETCH")
						&& responses[i].getResponseMessage().indexOf("BODY") != -1) {
					result.add(IMAPHeaderParser.parse(responses[i]));
				} else {
					handleResponse(responses[i]);
				}
			}
		} catch (ParserException e) {
			throw new IMAPException(e);
		}

		// Check last response for command success
		if (!responses[responses.length - 1].isOK())
			throwException(responses[responses.length - 1]);

		return (IMAPHeader[]) result.toArray(new IMAPHeader[0]);
	}

	/**
	 * Fetch the headerfields (e.g. Subject, From, etc) of the specified messages.
	 * 
	 * @param set the indices of the messages
	 * @param fields the fields to retrieve 
	 * @return the Headers of the messages 
	 * @throws IOException
	 * @throws IMAPException
	 */
	public IMAPHeader[] fetchHeaderFields(SequenceSet set, String[] fields)
			throws IOException, IMAPException {
		return fetchHeaderFieldsCore("FETCH", set, fields);
	}

	/**
	 * Fetch the headerfields (e.g. Subject, From, etc) of the specified messages.
	 * 
	 * @param set the UIDs of the messages
	 * @param fields the fields to retrieve 
	 * @return the Headers of the messages 
	 * @throws IOException
	 * @throws IMAPException
	 */
	public IMAPHeader[] uidFetchHeaderFields(SequenceSet set, String[] fields)
			throws IOException, IMAPException {
		return fetchHeaderFieldsCore("UID FETCH", set, fields);
	}

	private IMAPHeader[] fetchHeaderCore(String c, SequenceSet set)
			throws IOException, IMAPException {
		checkState(SELECTED);

		IMAPCommand command = new IMAPCommand(tagFactory.nextTag(), c,
				new Object[] {
						set,
						new Section("RFC822.SIZE BODY.PEEK",
								new Object[] { "HEADER" }) });

		IMAPResponse[] responses = communicate(command);
		ArrayList result = new ArrayList(responses.length - 1);

		try {
			for (int i = 0; i < responses.length - 1; i++) {
				if (responses[i].getResponseSubType().equals("FETCH") && responses[i].getResponseMessage().indexOf("BODY") != -1) {
					result.add(IMAPHeaderParser.parse(responses[i]));
				} else {
					handleResponse(responses[i]);
				}
			}
		} catch (ParserException e) {
			throw new IMAPException(e);
		}

		// Check last response for command success
		if (!responses[responses.length - 1].isOK())
			throwException(responses[responses.length - 1]);

		return (IMAPHeader[]) result.toArray(new IMAPHeader[0]);
	}

	/**
	 * Fetch the complete headers of the specified messages.
	 * 
	 * @param set the indices of the messages
	 * @return the Headers of the messages
	 * @throws IOException
	 * @throws IMAPException
	 */
	public IMAPHeader[] fetchHeader(SequenceSet set) throws IOException,
			IMAPException {
		return fetchHeaderCore("FETCH", set);
	}

	/**
	 * Fetch the complete headers of the specified messages.
	 * 
	 * @param set the UIDs of the messages
	 * @return the Headers of the messages
	 * @throws IOException
	 * @throws IMAPException
	 */
	public IMAPHeader[] uidFetchHeader(SequenceSet set) throws IOException,
			IMAPException {
		return fetchHeaderCore("UID FETCH", set);
	}

	private InputStream fetchBodyCore(String c, int id, Integer[] address)
			throws IOException, IMAPException {
		checkState(SELECTED);
		IMAPCommand command = new IMAPCommand(tagFactory.nextTag(), c,
				new Object[] { Integer.toString(id),
						new Section("BODY.PEEK", new Object[] { address }) });

		try {
			command.writeToStream(in, out);
		} catch (IOException e) {
			state = NOT_CONNECTED;
			throw e;
		}

		return in.readBodyNonBlocking();
	}

	/**
	 * Fetch the specified body of the message.
	 * 
	 * @param index the index of the message
	 * @param address the address of the body
	 * @return the InputStream of the body
	 * @throws IOException
	 * @throws IMAPException
	 */
	public InputStream fetchBody(int index, Integer[] address) throws IOException,
			IMAPException {
		return fetchBodyCore("FETCH", index, address);
	}

	/**
	 * Fetch the specified body of the message.
	 * 
	 * @param uid the UID of the message
	 * @param address the address of the body
	 * @return the InputStream of the body
	 * @throws IOException
	 * @throws IMAPException
	 */
	public InputStream uidFetchBody(int uid, Integer[] address)
			throws IOException, IMAPException {
		return fetchBodyCore("UID FETCH", uid, address);
	}

	private MimeTree fetchBodystructureCore(String c, int id)
			throws IOException, IMAPException {
		checkState(SELECTED);
		IMAPCommand command = new IMAPCommand(tagFactory.nextTag(), c,
				new Object[] { Integer.toString(id), "BODYSTRUCTURE" });

		IMAPResponse[] responses = communicate(command);

		MimeTree result = null;
		for (int i = 0; i < responses.length - 1; i++) {
			if (responses[i].getResponseSubType().equals("FETCH") && responses[i].getResponseMessage().indexOf("BODYSTRUCTURE") != -1) {
				try {
					result = MimeTreeParser.parse(responses[i]);
				} catch (ParserException e) {
					throw new IMAPException(e);
				}
			} else {
				handleResponse(responses[i]);
			}

		}
		if (!responses[responses.length - 1].isOK())
			throwException(responses[responses.length - 1]);

		return result;
	}

	/**
	 * Fetch the Bodystructure of the message.
	 * 
	 * @param index the index of the message
	 * @return the Bodystructure of the message
	 * @throws IOException
	 * @throws IMAPException
	 */
	public MimeTree fetchBodystructure(int index) throws IOException,
			IMAPException {
		return fetchBodystructureCore("FETCH", index);
	}

	/**
	 * Fetch the Bodystructure of the message.
	 * 
	 * @param uid the UID of the message
	 * @return the Bodystructure of the message
	 * @throws IOException
	 * @throws IMAPException
	 */
	public MimeTree uidFetchBodystructure(int uid) throws IOException,
			IMAPException {
		return fetchBodystructureCore("UID FETCH", uid);
	}

	private InputStream fetchMessageCore(String c, int id) throws IOException,
			IMAPException {
		checkState(SELECTED);
		IMAPCommand command = new IMAPCommand(tagFactory.nextTag(), c,
				new Object[] { Integer.toString(id),
						"BODY.PEEK[]".toCharArray() });

		try {
		command.writeToStream(in, out);
		} catch (IOException e) {
			state = NOT_CONNECTED;
			throw e;
		}

		try {
			return in.readBodyNonBlocking();
		} catch (IOException e) {
			state = NOT_CONNECTED;
			throw e;
		} 
	}

	/**
	 * Fetch the message source.
	 * 
	 * @param index the index of the message
	 * @return the InputStream of the message source
	 * @throws IOException
	 * @throws IMAPException
	 */
	public InputStream fetchMessage(int index) throws IOException, IMAPException {
		return fetchMessageCore("FETCH", index);
	}

	/**
	 * Fetch the message source.
	 * 
	 * @param uid the UID of the message
	 * @return the InputStream of the message source
	 * @throws IOException
	 * @throws IMAPException
	 */
	public InputStream uidFetchMessage(int uid) throws IOException,
			IMAPException {
		return fetchMessageCore("UID FETCH", uid);
	}

	private InputStream fetchMimeHeaderSourceCore(String c, int id,
			Integer[] address) throws IOException, IMAPException {
		checkState(SELECTED);
		IMAPCommand command = new IMAPCommand(tagFactory.nextTag(), c,
				new Object[] {
						Integer.toString(id),
						new Section("BODY.PEEK", new Object[] { address,
								".MIME" }) });

		try {
			command.writeToStream(in, out);
		} catch (IOException e) {
			state = NOT_CONNECTED;
			throw e;
		}

		return in.readBodyNonBlocking();
	}

	/**
	 * Fetch the source of the MIME Header of the specified MIME part.
	 * 
	 * @param index the index of the message
	 * @param address the address of the MIME part
	 * @return the InputStream of the MIME Header source
	 * @throws IOException
	 * @throws IMAPException
	 */
	public InputStream fetchMimeHeaderSource(int index, Integer[] address)
			throws IOException, IMAPException {
		return fetchMimeHeaderSourceCore("FETCH", index, address);
	}

	/**
	 * Fetch the source of the MIME Header of the specified MIME part.
	 * 
	 * @param uid the UID of the message
	 * @param address the address of the MIME part
	 * @return the InputStream of the MIME Header source
	 * @throws IOException
	 * @throws IMAPException
	 */
	public InputStream uidFetchMimeHeaderSource(int uid, Integer[] address)
			throws IOException, IMAPException {
		return fetchMimeHeaderSourceCore("UID FETCH", uid, address);
	}

	private CopyInfo copyCore(String c, SequenceSet set, String mailbox)
			throws IOException, IMAPException {
		checkState(SELECTED);

		IMAPCommand command = new IMAPCommand(tagFactory.nextTag(), c,
				new Object[] { set, MailboxNameUTF7Converter.encode(mailbox) });

		IMAPResponse[] responses = communicate(command);
		// handle any responses that do not directly
		// relate to the command
		for (int i = 0; i < responses.length - 1; i++) {
			handleResponse(responses[i]);
		}

		// Check last response for command success
		if (!responses[responses.length - 1].isOK())
			throwException(responses[responses.length - 1]);

		CopyInfo result = null;
		if (responses[responses.length - 1].getResponseTextCode() != null) {
			try {
				result = CopyInfoParser
						.parse(responses[responses.length - 1]);
			} catch (ParserException e1) {
				throw new IMAPException(e1);
			}
		}
	
		return result;
	}

	/**
	 * Copy messages from the selected mailbox to a destination mailbox.
	 * If the server has the UIDPLUS capability, the new uids can be found
	 * in the CopyInfo. 
	 * 
	 * @param set the indices of the messages to copy
	 * @param mailbox the destination mailbox
	 * @return the CopyInfo if UIDPLUS is supported, <code>null</code> else
	 * @throws IOException
	 * @throws IMAPException
	 */
	public CopyInfo copy(SequenceSet set, String mailbox) throws IOException,
			IMAPException {
		return copyCore("COPY", set, mailbox);
	}

	/**
	 * Copy messages from the selected mailbox to a destination mailbox.
	 * If the server has the UIDPLUS capability, the new uids can be found
	 * in the CopyInfo. 
	 * 
	 * @param set the UIDs of the messages to copy
	 * @param mailbox the destination mailbox
	 * @return the CopyInfo if UIDPLUS is supported, <code>null</code> else
	 * @throws IOException
	 * @throws IMAPException
	 */
public CopyInfo uidCopy(SequenceSet set, String mailbox) throws IOException,
			IMAPException {
		return copyCore("UID COPY", set, mailbox);
	}

	private IMAPFlags[] storeCore(String c, SequenceSet set, boolean type,
			IMAPFlags flags) throws IOException, IMAPException {
		checkState(SELECTED);

		String flagType = type ? "+FLAGS.SILENT" : "-FLAGS.SILENT";

		IMAPCommand command = new IMAPCommand(tagFactory.nextTag(), c,
				new Object[] { set, flagType, flags });

		IMAPResponse[] responses = communicate(command);

		ArrayList result = new ArrayList(responses.length - 1);
		for (int i = 0; i < responses.length - 1; i++) {
			if (responses[i].getResponseSubType().equals("FLAGS")) {
				result.add(FlagsParser.parse(responses[i]));
			} else if (responses[i].getResponseTextCode() != null && responses[i].getResponseTextCode().getType() == ResponseTextCode.PERMANENTFLAGS){
				//additional info -> irrelevant
			} else {
				handleResponse(responses[i]);
			}
		}

		// Check last response for command success
		if (!responses[responses.length - 1].isOK())
			throwException(responses[responses.length - 1]);

		return (IMAPFlags[]) result.toArray(new IMAPFlags[0]);
	}

	/**
	 * Store the Flags with the specified messages.
	 * 
	 * @param set the indices of the messages
	 * @param type <code>true</code> for a silent store
	 * @param flags the Flags to store
	 * @throws IOException
	 * @throws IMAPException
	 */
	public void store(SequenceSet set, boolean type, IMAPFlags flags)
			throws IOException, IMAPException {
		storeCore("STORE", set, type, flags);
	}

	/**
	 * Store the Flags with the specified messages.
	 * 
	 * @param set the UIDs of the messages
	 * @param type <code>true</code> for a silent store
	 * @param flags the Flags to store
	 * @throws IOException
	 * @throws IMAPException
	 */
	public void uidStore(SequenceSet set, boolean type, IMAPFlags flags)
			throws IOException, IMAPException {
		storeCore("UID STORE", set, type, flags);
	}

	/**
	 * Gets the defined namespaces.
	 * 
	 * @return the NamespaceCollection
	 * @throws IOException
	 * @throws IMAPException
	 */
	public NamespaceCollection namespace() throws IOException, IMAPException {
		checkState(AUTHENTICATED);
		IMAPCommand command = new IMAPCommand(tagFactory.nextTag(),
				"NAMESPACE", new Object[] {});

		IMAPResponse[] responses = communicate(command);
		// Check last response for command success
		if (!responses[responses.length - 1].isOK())
			throwException(responses[responses.length - 1]);

		NamespaceCollection result = null;

		for (int i = 0; i < responses.length - 1; i++) {
			if (responses[i].getResponseSubType().equals("NAMESPACE")) {
				try {
					responses[i].getResponseMessage();

					result = NamespaceParser.parse(responses[i]);
				} catch (ParserException e) {
					throw new IMAPException(e);
				}
			} else {
				handleResponse(responses[i]);
			}
		}

		return result;

	}

	private synchronized IMAPResponse[] communicate(IMAPCommand command)
			throws IOException, IMAPException {

		// Check for the length of the command if enabled
		if (RistrettoConfig.getInstance().isCheckCommandLineLength()
				&& command.estimateLength() > 1000) {
			throw new CommmandTooLongException(command);
		}

		try {
			command.writeToStream(in, out);
		} catch (IOException e1) {
			state = NOT_CONNECTED;
			throw e1;
		}

		List responses = new LinkedList();
		IMAPResponse response;

		try {
			response = in.readResponse();
			while (!response.isTagged() && !response.isBYE()) {
				responses.add(response);
				response = in.readResponse();
			}
		} catch (IOException e) {
			state = NOT_CONNECTED;
			throw e;
		}

		if (response.isTagged()) {
			if (!response.getTag().equals(command.getTag()))
				throw new IMAPException("Tag mismatch" + response.getSource()
						+ ". Expected " + command.getTag() + " but is "
						+ response.getTag());
		}

		responses.add(response);

		return (IMAPResponse[]) responses.toArray(new IMAPResponse[] {});
	}

	private void createStreams() throws IOException {
		if (RistrettoLogger.logStream != null) {
			in = new IMAPInputStream(new LogInputStream(
					socket.getInputStream(), RistrettoLogger.logStream), this);

			out = new LogOutputStream(socket.getOutputStream(),
					RistrettoLogger.logStream);
		} else {
			in = new IMAPInputStream(socket.getInputStream(), this);

			out = new PrintStream(socket.getOutputStream(), true);
		}
	}

	private void checkState(int state) throws IMAPException {
		if (getState() < state) {
			if (getState() == NOT_CONNECTED)
				throw new IMAPDisconnectedException();
			else
				throw new IMAPException("Wrong state for command");
		}

	}

	/**
	 * @return Returns the state.
	 */
	public int getState() {
		processUnsolicitedResponses();

		return state;
	}

	private void processUnsolicitedResponses() {
		if (state > NOT_CONNECTED) {
			try {
				while (in.hasUnsolicitedReponse()) {
					IMAPResponse response;
					try {
						response = in.readResponse();
						handleResponse(response);
					} catch (IOException e) {
						state = NOT_CONNECTED;
					}
				}
			} catch (IOException e) {

			} catch (IMAPException e) {
			}
		}
	}

	/**
	 * Adds an IMAPListener to the protocol.
	 * 
	 * @param listener
	 */
	public void addIMAPListener(IMAPListener listener) {
		listeners.add(listener);
	}

	/**
	 * Remove the listener from the protocol.
	 * 
	 * @param listener
	 */
	public void removeIMAPListener(IMAPListener listener) {
		listeners.remove(listener);
	}

	void handleResponse(IMAPResponse response) throws IMAPException {
		Iterator it;
		if (response.isBYE()) {
			state = NOT_CONNECTED;
			// Close the streams
			try {
				in.close();
				in = null;
				out.close();
				out = null;
			} catch (IOException e) {
				//should never happen
			}
			throw new IMAPDisconnectedException(response);
		}

		if (response.getResponseSubType().equals("EXISTS")) {
			// Exists
			it = listeners.iterator();
			while (it.hasNext()) {
				((IMAPListener) it.next()).existsChanged(getSelectedMailbox(),
						response.getPreNumber());
			}
		}

		if (response.getResponseSubType().equals("FLAGS")) {
			IMAPFlags changedFlags = FlagsParser.parse(response);
			if (changedFlags.getIndex() != -1) {
				// Maybe Flags update
				it = listeners.iterator();
				while (it.hasNext()) {
					((IMAPListener) it.next()).flagsChanged(
							getSelectedMailbox(), changedFlags);
				}
			}
		}

		if (response.getResponseSubType().equals("FETCH")
				&& response.getResponseMessage().indexOf("FLAGS") != -1) {
			// Flags update
			it = listeners.iterator();
			while (it.hasNext()) {
				((IMAPListener) it.next()).flagsChanged(getSelectedMailbox(),
						FlagsParser.parse(response));
			}
		}

		if (response.getResponseSubType().equals("RECENT")) {
			// Recent changed
			it = listeners.iterator();
			while (it.hasNext()) {
				((IMAPListener) it.next()).recentChanged(getSelectedMailbox(),
						response.getPreNumber());
			}
		}

		if ((response.isNO() || response.isBAD() || response.isOK())
				&& response.getResponseTextCode() != null) {
			if (response.getResponseTextCode().equals("ALERT")) {
				// Alert message
				it = listeners.iterator();
				while (it.hasNext()) {
					((IMAPListener) it.next()).alertMessage(response
							.getResponseMessage());
				}
			}

			if (response.getResponseTextCode().equals("PARSE")) {
				// Header-Parser error
				it = listeners.iterator();
				while (it.hasNext()) {
					((IMAPListener) it.next()).parseError(response
							.getResponseMessage());
				}
			}
		}
	}

	/**
	 * @see org.columba.ristretto.auth.AuthenticationServer#authReceive()
	 */
	public byte[] authReceive() throws AuthenticationException, IOException {
		try {
			IMAPResponse response;
			try {
				response = in.readResponse();
			} catch (IOException e) {
				state = NOT_CONNECTED;
				throw e;
			}
			
			if (response.getResponseType() == IMAPResponse.RESPONSE_CONTINUATION) {
				return Base64.decodeToArray(response.getResponseMessage());
			} else {
				throw new AuthenticationException(new IMAPException(response));
			}
		} catch (IMAPException e) {
			throw new AuthenticationException(e);
		}
	}

	/**
	 * @see org.columba.ristretto.auth.AuthenticationServer#authSend(byte[])
	 */
	public void authSend(byte[] call) throws IOException {
		out.write(Base64.encode(ByteBuffer.wrap(call), false).toString()
				.getBytes());
		out.write('\r');
		out.write('\n');
	}

	/**
	 * @see org.columba.ristretto.auth.AuthenticationServer#getHostName()
	 */
	public String getHostName() {
		return host;
	}

	/**
	 * @see org.columba.ristretto.auth.AuthenticationServer#getService()
	 */
	public String getService() {
		return "imap";
	}

	/**
	 * @return Returns the selectedMailbox.
	 */
	public String getSelectedMailbox() {
		return selectedMailbox;
	}
}