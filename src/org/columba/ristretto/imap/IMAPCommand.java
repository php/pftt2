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
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;

import org.columba.ristretto.io.StreamUtils;

/**
 * Command which is send to the IMAP Server. IMAPCommand is used by the
 * IMAPProtocol to send commands. Every command has a unique tag that identifies
 * it and the answer from the server.
 * 
 * <br>
 * <b>RFC(s): </b> 3105
 * 
 * @author Timo Stich <tstich@users.sourceforge.net>
 */
public class IMAPCommand {
	private String tag;

	private String command;

	private Object[] parameters;

	private boolean lastWasLiteral;

	private Charset charset;

	/**
	 * Constructs the IMAPCommand.
	 * @param tag 
	 * @param command
	 * @param parameters
	 */
	public IMAPCommand(String tag, String command, Object[] parameters) {
		this(tag, command, parameters, Charset.forName("ISO-8859-1"));
	}

	/**
	 * Constructs the IMAPCommand.
	 * @param tag 
	 * 
	 * @param command
	 */
	public IMAPCommand(String tag, String command) {
		this(tag, command, null);
	}

	/**
	 * Writes the IMAPCommand to an OutputStream. The given InputStream is also
	 * needed since the response of the IMAP server is needed to continue the
	 * command in case of a literal send.
	 * 
	 * @param in
	 * @param out
	 * @throws IMAPException
	 * @throws IOException
	 */
	public void writeToStream(IMAPInputStream in, OutputStream out)
			throws IMAPException, IOException {
		Object arg;
		out.write(tag.getBytes("US-ASCII"));
		out.write(' ');
		out.write(command.getBytes("US-ASCII"));
		// write arguments
		if (parameters != null) {
			Iterator it = Arrays.asList(parameters).iterator();
			while (it.hasNext()) {
				out.write(' ');
				arg = it.next();
				if (arg instanceof String) {
					writeString((String) arg, in, out);
				} else if (arg instanceof String[]) {
					writeStringArray((String[]) arg, in, out);
				} else if (arg instanceof InputStream) {
					writeInputStream((InputStream) arg, in, out);
				} else if (arg instanceof SearchKey) {
					writeSearchKey((SearchKey) arg, in, out);
				} else if (arg instanceof byte[]) {
					writeByteArray(((byte[]) arg), in, out);
				} else if (arg instanceof Integer[]) {
					writeAddress(((Integer[]) arg), in, out);
				} else if (arg instanceof char[]) {
					writeCharArray(((char[]) arg), in, out);
				} else if (arg instanceof Section) {
					writeSection((Section) arg, in, out);
				} else {
					writeCharArray(arg.toString().toCharArray(), in, out);
				}
			}
		}

		out.write('\r');
		out.write('\n');
		out.flush();
	}

	private void writeSearchKey(SearchKey key, IMAPInputStream in,
			OutputStream out) throws IOException, IMAPException {
		String[] strings = key.toStringArray();

		if (strings.length > 1) {
			out.write('(');
			out.write(strings[0].getBytes(charset.name()));
			for (int i = 1; i < strings.length; i++) {
				out.write(' ');
				writeString(strings[i], in, out);
			}
			out.write(')');
		} else if (strings.length == 1) {
			writeString(strings[0], in, out);
		}

	}

	private void writeSection(Section section, IMAPInputStream in,
			OutputStream out) throws IMAPException, IOException {
		Object arg;
		out.write('(');
		out.write(section.getType().getBytes("US-ASCII"));
		out.write('[');

		Iterator it = Arrays.asList(section.getParams()).iterator();
		while (it.hasNext()) {
			arg = it.next();
			if (arg instanceof String) {
				writeString((String) arg, in, out);
				if (it.hasNext())
					out.write(' ');
			} else if (arg instanceof String[]) {
				writeStringArray((String[]) arg, in, out);
			} else if (arg instanceof Integer[]) {
				writeAddress((Integer[]) arg, in, out);
			} else {
				writeCharArray(arg.toString().toCharArray(), in, out);
			}
		}

		out.write(']');
		out.write(')');
		lastWasLiteral = false;
	}

	/**
	 * @param cs
	 * @param in
	 * @param out
	 */
	private void writeCharArray(char[] cs, IMAPInputStream in, OutputStream out)
			throws IMAPException, IOException {
		for (int i = 0; i < cs.length; i++) {
			out.write(cs[i]);
		}
	}

	/**
	 * @param integers
	 * @param in
	 * @param out
	 */
	private void writeAddress(Integer[] integers, IMAPInputStream in,
			OutputStream out) throws IMAPException, IOException {

		out.write(new Integer(integers[0].intValue() + 1).toString().getBytes(
				charset.name()));
		for (int i = 1; i < integers.length; i++) {
			out.write('.');
			out.write(new Integer(integers[i].intValue() + 1).toString()
					.getBytes(charset.name()));
		}

		lastWasLiteral = false;
	}

	/**
	 * @param bs
	 * @param in
	 * @param out
	 */
	private void writeByteArray(byte[] bs, IMAPInputStream in, OutputStream out)
			throws IMAPException, IOException {
		out.write('{');
		out.write(Integer.toString(bs.length).getBytes(charset.name()));
		out.write('}');
		out.write('\r');
		out.write('\n');
		out.flush();
		IMAPResponse response = in.readResponse();
		if (response.getResponseType() != IMAPResponse.RESPONSE_CONTINUATION)
			throw new IMAPException(response);

		out.write(bs);
		out.flush();
		lastWasLiteral = true;
	}

	/**
	 * @param stream
	 * @param out
	 */
	private void writeInputStream(InputStream stream, IMAPInputStream in,
			OutputStream out) throws IMAPException, IOException {
		int size = 1000;

		out.write('{');
		out
				.write(Integer.toString(stream.available()).getBytes(
						charset.name()));
		//out.write(Integer.toString(size).getBytes("US-ASCII"));
		out.write('}');
		out.write('\r');
		out.write('\n');
		out.flush();

		IMAPResponse response = in.readResponse();
		if (response.getResponseType() != IMAPResponse.RESPONSE_CONTINUATION)
			throw new IMAPException(response);

		// Copy the stream to the outputstream
		StreamUtils.streamCopy(stream, out, 1400);

		//debug
		/*
		 * for(int i=0;i <size; i++) { out.write(Integer.toString(i %
		 * 10).getBytes()); } out.flush();
		 */
		lastWasLiteral = true;
	}

	/**
	 * @param strings
	 * @param out
	 */
	private void writeStringArray(String[] strings, IMAPInputStream in,
			OutputStream out) throws IMAPException, IOException {
		out.write('(');
		if (strings.length > 0) {
			out.write(strings[0].getBytes(charset.name()));
			for (int i = 1; i < strings.length; i++) {
				out.write(' ');
				writeString(strings[i], in, out);
			}
		}
		out.write(')');
		lastWasLiteral = false;
	}

	/**
	 * @param sequence
	 */
	private void writeString(String sequence, IMAPInputStream in,
			OutputStream out) throws IMAPException, IOException {
		// check if the argument is 7-bit, " and \ safe
		boolean plainSafe = true;
		boolean quote = sequence.length() == 0;

		int i = 0;
		while (i < sequence.length() && plainSafe) {
			plainSafe &= sequence.charAt(i) < 128;
			plainSafe &= sequence.charAt(i) != '\"';
			plainSafe &= sequence.charAt(i) != '\\';
			plainSafe &= sequence.charAt(i) != '\0';
			plainSafe &= sequence.charAt(i) != '\r';
			plainSafe &= sequence.charAt(i) != '\n';

			quote |= sequence.charAt(i) == ' ';
			quote |= sequence.charAt(i) == '(';
			quote |= sequence.charAt(i) == ')';
			quote |= sequence.charAt(i) == '{';
			quote |= sequence.charAt(i) == '%';
			quote |= sequence.charAt(i) == '*';
			quote |= sequence.charAt(i) == ']';
			//quote |= sequence.charAt(i) == '/';

			i++;
		}
		// write as literal if not plain safe else just write the bytes
		if (plainSafe) {
			if (quote) {
				out.write('\"');
			}
			out.write(sequence.getBytes(charset.name()));
			if (quote) {
				out.write('\"');
			}
			lastWasLiteral = false;
		} else {
			writeByteArray(sequence.getBytes(charset.name()), in, out);
		}
	}

	/**
	 * Gets the tag that identifies the command.
	 * 
	 * @return Returns the tag.
	 */
	public String getTag() {
		return tag;
	}

	/**
	 * Constructs the IMAPCommand.java.
	 * 
	 * @param tag
	 * @param command
	 * @param parameters
	 * @param charset
	 */
	public IMAPCommand(String tag, String command, Object[] parameters,
			Charset charset) {
		this.tag = tag;
		this.command = command;
		this.parameters = parameters;
		this.charset = charset;
	}

	/**
	 * Estimates the length of the Command without literal sizes. This is used
	 * to ensure that the total length is less than 1000 octets as proposed in
	 * RFC 2683.
	 * 
	 * @return the estimated total length of the command.
	 */
	public int estimateLength() {
		// Tag + WS + command
		int estimatedLength = command.length() + tag.length() + 1;

		// Sum the estimated length for each parameter
		if (parameters != null) {
			for (int i = 0; i < parameters.length; i++) {
				// estimated the length of the parameter
				if (parameters[i] instanceof char[]) {
					estimatedLength += ((char[]) parameters[i]).length;
				}

				else if (parameters[i] instanceof String) {
					estimatedLength += ((String) parameters[i]).length();
				}

				else if (parameters[i] instanceof String[]) {
					String[] strings = (String[]) parameters[i];
					for (int j = 0; j < strings.length; j++) {
						estimatedLength += strings[j].length();
					}
				}

				else if (parameters[i] instanceof SearchKey) {
					String[] strings = ((SearchKey) parameters[i])
							.toStringArray();
					for (int j = 0; j < strings.length; j++) {
						estimatedLength += strings[j].length();
					}
				}

				else {
					// default length
					estimatedLength += 3;
				}

				// this is for space, parenthesis, etc.
				estimatedLength++;
			}
		}

		return estimatedLength;
	}
}