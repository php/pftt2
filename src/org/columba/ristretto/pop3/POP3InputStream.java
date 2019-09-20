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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.columba.ristretto.concurrency.Mutex;
import org.columba.ristretto.io.ConnectionDroppedException;
import org.columba.ristretto.io.TempSourceFactory;
import org.columba.ristretto.parser.ParserException;
import org.columba.ristretto.pop3.parser.POP3ResponseParser;

/**
 * FilterInputStream that capsules the InputStream from the
 * socket with a POP3 server.
 * 
 * 
 * @author Timo Stich <tstich@users.sourceforge.net>
 */
public class POP3InputStream extends FilterInputStream {

	private static final int RETR_BUFFER_SIZE = 1024;
	private StringBuffer lineBuffer;
	private static final Pattern sizePattern = Pattern.compile("(\\d+) octets");

	/**
	 * Constructs a POP3InputStream.
	 * 
	 * @param in the socket InputStream
	 */
	public POP3InputStream(InputStream in) {
		super(in);

		lineBuffer = new StringBuffer();
	}

	/**
	 * Reads and parses a single line reponse from a POP3 server.
	 * 
	 * @return the parsed POP3Response
	 * @throws IOException
	 * @throws POP3Exception
	 */
	public POP3Response readSingleLineResponse()
		throws IOException, POP3Exception {
		readLineInBuffer();

		try {
			POP3Response response = POP3ResponseParser.parse(lineBuffer);

			return response;
		} catch (ParserException e) {
			throw new POP3Exception(e.getMessage());
		}
	}
	
	/**
	 * Uses the POP3DownloadThread to asynchronously download mails from the POP3 server.
	 * 
	 * @param size estimated size of the mail or -1 if unknown.
	 * @param mutex Mutex of the socket which is released after the download finished.
	 * @return the InputStream of the mail
	 * @throws IOException
	 */
	public InputStream asyncDownload(int size, Mutex mutex) throws IOException {
		return POP3DownloadThread.asyncDownload( new POP3MultiLineStream(this, size), size, mutex);
	}

	/**
	 * Blocking download of mails from the POP3 server.
	 * 
	 * @param size
	 * @return the InputStream of the mail
	 * @throws IOException
	 */
	public InputStream syncDownload(int size) throws IOException {
		return new POP3MultiLineStream(this, size);
		
	}

	
	/**
	 * Reads and parses a multiline POP3 response.
	 * 
	 * @return the parsed POP3Response.
	 * @throws IOException
	 * @throws POP3Exception
	 */
	public POP3Response readMultiLineResponse()
		throws IOException, POP3Exception {
		POP3Response response = readSingleLineResponse();

		// Read data only if this response was +OK
		if (response.isOK()) {
			int size = -1;
			// try to find out the size of the stream
			Matcher matcher = sizePattern.matcher(response.getMessage());
			if( matcher.find() ) {
				size = Integer.parseInt(matcher.group(1));
			}

			POP3MultiLineStream multiLineStream = new POP3MultiLineStream(this, size);
			response.setData(TempSourceFactory.createTempSource(multiLineStream, -1));			
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
		lineBuffer.append((char)read);

		// read the LF
		read = in.read();
		if( read != '\n') throw new ConnectionDroppedException();
		lineBuffer.append((char) read);
	}

}
