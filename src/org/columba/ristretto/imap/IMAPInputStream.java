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

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.columba.ristretto.concurrency.Mutex;
import org.columba.ristretto.imap.parser.IMAPResponseParser;
import org.columba.ristretto.imap.parser.MessageAttributeParser;
import org.columba.ristretto.io.AsyncInputStream;
import org.columba.ristretto.io.ConnectionDroppedException;
import org.columba.ristretto.io.TempSourceFactory;
import org.columba.ristretto.message.Attributes;
import org.columba.ristretto.parser.ParserException;

/**
 * 
 * InputStream to read IMAPReponses from an IMAP server. It handles all IMAP
 * specific features like literals in an transparent way.
 * 
 * @author tstich <tstich@users.sourceforge.net>
 */
public class IMAPInputStream extends FilterInputStream {

	private static final Pattern literalPattern = Pattern
			.compile("\\{(\\d+)\\}$");

	private IMAPProtocol protocol;

	private Matcher literalMatcher;

	private Mutex mutex;

	private StringBuffer lineBuffer;

	private LinkedList observerList;

	/**
	 * Constructs the IMAPInputStream.
	 * 
	 * @param in
	 *            the socket InputStream.
	 * @param protocol
	 *            the associated protocol class.
	 */
	public IMAPInputStream(InputStream in, IMAPProtocol protocol) {
		super(in);
		literalMatcher = literalPattern.matcher("");
		lineBuffer = new StringBuffer();

		observerList = new LinkedList();
		mutex = new Mutex();

		this.protocol = protocol;
	}

	/**
	 * Reads a body part from the asynchronously from the InputStream. This is
	 * done using the IMAPDownloadThread.
	 * 
	 * @see IMAPDownloadThread
	 * 
	 * @return the InputStream from the downloaded part
	 * @throws IOException
	 * @throws IMAPException
	 */
	public InputStream readBodyNonBlocking() throws IOException, IMAPException {
		mutex.lock();
		boolean dontrelease = false;
		try {
			// do basic parsing to find out what type the answer is
			IMAPResponse response;
			try {
				readLineInBuffer();
				response = IMAPResponseParser.parse(lineBuffer);
			} catch (ParserException e) {
				throw new IMAPException(e);
			}

			// Error Response
			if (response.isTagged() && (response.isBAD() || response.isNO())) {
				mutex.release();
				throw new IMAPException(response);
			}

			// Some unsolicited Responses which must be processed
			while (!response.isTagged()
					&& !response.getResponseSubType().equals("FETCH")
					&& response.getResponseMessage().indexOf("BODY") == -1) {
				protocol.handleResponse(response);

				try {
					readLineInBuffer();
					response = IMAPResponseParser.parse(lineBuffer);
				} catch (ParserException e) {
					mutex.release();
					throw new IMAPException(e);
				}
			}

			// Error Response
			if (response.isTagged() && (response.isBAD() || response.isNO())) {
				mutex.release();
				throw new IMAPException(response);
			}

			// is there a literal in the answer
			literalMatcher.reset(response.getResponseMessage());

			// This is what we care about
			if (literalMatcher.find()) {

				// read literal from inputstream
				int size = Integer.parseInt(literalMatcher.group(1));

				AsyncInputStream result = IMAPDownloadThread.asyncDownload(
						this, size, mutex);
				dontrelease = true;
				return result;
			} else {
				// first parse the message attributes
				Attributes attributes = MessageAttributeParser.parse(response
						.getResponseMessage());
				String body = (String) attributes.get("BODY");
				if (body == null) {
					mutex.release();
					return new ByteArrayInputStream(new byte[0]);
				}

				if (body.length() > 0 && body.charAt(0) == '\"') {
					body = body.substring(1, body.length() - 1);
				}

				try {
					// Read OK Response
					readLineInBuffer();
				} catch (IOException e1) {
					mutex.release();
					throw e1;
				}

				return new ByteArrayInputStream(body != null ? body
						.getBytes("US-ASCII") : new byte[0]);
			}
		} finally {
			if (dontrelease == false) {
				mutex.release();
			}
		}
	}

	/**
	 * Reads a response from the InputStream.
	 * 
	 * @return the read reponse
	 * @throws IOException
	 * @throws IMAPException
	 */
	public IMAPResponse readResponse() throws IOException, IMAPException {
		IMAPResponse response;
		mutex.lock();
		try {
			// do basic parsing to find out what type the answer is

			try {
				readLineInBuffer();
				response = IMAPResponseParser.parse(lineBuffer);
			} catch (ParserException e) {
				throw new IMAPException(e);
			}
			// is there a literal in the answer
			literalMatcher.reset(response.getResponseMessage());
			int literalIndex = 0;
			while (literalMatcher.find()) {
				// read literal from inputstream
				int literalSize = Integer.parseInt(literalMatcher.group(1));

				// Cleanup literals to make the {N} to increase for every
				// literal
				response.setResponseMessage(response.getResponseMessage()
						.substring(0, literalMatcher.start())
						+ '{' + (literalIndex++) + '}');

				// assign literal to response
				response.addLiteral(TempSourceFactory.createTempSource(this,
						literalSize));

				// read rest in response and remove the trailing CRLF
				readLineInBuffer();
				String restresponse = lineBuffer.toString();
				restresponse = restresponse.substring(0,
						restresponse.length() - 2);
				response.appendResponseText(restresponse);

				// Could there be another Literal?
				if (restresponse.length() > 3) {
					literalMatcher.reset(response.getResponseMessage());
				}
			}

		} finally {
			mutex.release();
		}
		return response;
	}

	private void readLineInBuffer() throws IOException {
		// Clear the buffer
		lineBuffer.delete(0, lineBuffer.length());

		int read = in.read();
		// read until CRLF
		while (read != '\r' && read != -1) {
			lineBuffer.append((char) read);
			read = in.read();
		}
		lineBuffer.append((char) read);

		// read the LF
		read = in.read();
		if (read != '\n')
			throw new ConnectionDroppedException();
		lineBuffer.append((char) read);
	}

	/**
	 * Checks if the is a unread response waiting to be read. This can e.g.
	 * occur if the IMAP server sends a BYE response before closing the socket
	 * because of a idle timeout.
	 * 
	 * @return <code>true</code> there is response waiting to be read.
	 * @throws IOException
	 */
	public boolean hasUnsolicitedReponse() throws IOException {
		boolean result;

		mutex.lock();
		try {
			result = in.available() > 0;
		} finally {
			mutex.release();
		}
		return result;
	}
}
