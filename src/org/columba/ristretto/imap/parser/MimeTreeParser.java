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
package org.columba.ristretto.imap.parser;

import org.columba.ristretto.imap.IMAPResponse;
import org.columba.ristretto.message.Attributes;
import org.columba.ristretto.message.Header;
import org.columba.ristretto.message.LocalMimePart;
import org.columba.ristretto.message.Message;
import org.columba.ristretto.message.MessageMimePart;
import org.columba.ristretto.message.MimeHeader;
import org.columba.ristretto.message.MimePart;
import org.columba.ristretto.message.MimeTree;
import org.columba.ristretto.message.MimeType;
import org.columba.ristretto.message.StreamableMimePart;
import org.columba.ristretto.parser.ParserException;

/**
 * Parser for the BodyStructure of a message.
 * 
 * @author Timo Stich <tstich@users.sourceforge.net>
 */
public class MimeTreeParser {

	/**
	 * Parse the Bodystructure of the IMAPResponse.
	 * 
	 * @param response
	 * @return the MimeTree representing the Bodystructure.
	 * @throws ParserException
	 */
	public static MimeTree parse(IMAPResponse response) throws ParserException {
		String message = response.getResponseMessage();
			
		// first parse the message attributes
		Attributes attributes =
			MessageAttributeParser.parse(message);

		String bodystructure = (String) attributes.get("BODYSTRUCTURE");
		if( bodystructure == null ) throw new ParserException(message);
		
		MimeTree mptree = 
			parseBodyStructure(response , bodystructure);

		return mptree;
	}

	private static MimeTree parseBodyStructure(
		IMAPResponse response,
		String input) throws ParserException  {
		int openParenthesis = input.indexOf("(");

		String bodystructure =
			input.substring(
				openParenthesis + 1,
				ParenthesisParser.getClosingPos(
					input,
					openParenthesis));

		return new MimeTree(parseBS(response, bodystructure));
	}

	protected static MimePart parseBS(IMAPResponse response, String input) throws ParserException {
		MimePart result;

		if (input.charAt(0) == '(') {
			result = new MimePart();

			BodystructureTokenizer tokenizer =
				new BodystructureTokenizer(response, input);
			BodystructureTokenizer subtokenizer;

			Item nextItem = tokenizer.getNextItem();
			Item valueItem;

			while (nextItem != null) {
				if (nextItem.getType() != Item.PARENTHESIS)
					break;

				result.addChild(
					parseBS(response, (String) nextItem.getValue()));
				nextItem = tokenizer.getNextItem();
			}

			// Parse the Rest of the Multipart

			// Subtype / nextItem is already filled from break in while-block
			result.getHeader().setMimeType(
				new MimeType(
					"multipart",
					((String) nextItem.getValue()).toLowerCase()));

			// Search for any ContentParameters
			nextItem = tokenizer.getNextItem();
			if (nextItem.getType() == Item.PARENTHESIS) {
				subtokenizer =
					new BodystructureTokenizer(
						response,
						(String) nextItem.getValue());

				nextItem = subtokenizer.getNextItem();
				while (nextItem != null) {
					valueItem = subtokenizer.getNextItem();

					result.getHeader().putContentParameter(
						((String) nextItem.getValue()).toLowerCase(),
						(String) valueItem.getValue());

					nextItem = subtokenizer.getNextItem();
				}
			}

			// ID
			nextItem = tokenizer.getNextItem();
			if (nextItem == null)
				return result;
			if (nextItem.getType() == Item.STRING)
				result.getHeader().setContentID(
					((String) nextItem.getValue()).toLowerCase());

			// Description
			nextItem = tokenizer.getNextItem();
			if (nextItem == null)
				return result;
			if (nextItem.getType() == Item.STRING)
				result.getHeader().setContentDescription(
					((String) nextItem.getValue()).toLowerCase());

		} else {

			result = parseMimeStructure(response, input);
		}

		return result;
	}

	private static Header parseEnvelope(
		IMAPResponse response,
		String envelope) throws ParserException {

		Header result = new Header();

		BodystructureTokenizer tokenizer =
			new BodystructureTokenizer(response, envelope);
		Item nextItem;

		// Date
		nextItem = tokenizer.getNextItem();
		if (nextItem.getType() == Item.STRING)
			result.set("Date", (String) nextItem.getValue());

		// Subject
		nextItem = tokenizer.getNextItem();
		if (nextItem.getType() == Item.STRING)
			result.set("Subject", (String) nextItem.getValue());

		// From
		nextItem = tokenizer.getNextItem();
		if (nextItem.getType() == Item.PARENTHESIS);

		// Sender
		nextItem = tokenizer.getNextItem();
		if (nextItem.getType() == Item.PARENTHESIS);

		// Reply-To
		nextItem = tokenizer.getNextItem();
		if (nextItem.getType() == Item.PARENTHESIS);

		// To
		nextItem = tokenizer.getNextItem();
		if (nextItem.getType() == Item.PARENTHESIS);

		// Cc
		nextItem = tokenizer.getNextItem();
		if (nextItem.getType() == Item.PARENTHESIS);

		// Bcc
		nextItem = tokenizer.getNextItem();
		if (nextItem.getType() == Item.PARENTHESIS);

		// In-Reply-To
		nextItem = tokenizer.getNextItem();
		if (nextItem.getType() == Item.STRING)
			result.set("In-Reply-To", (String) nextItem.getValue());

		// Message-ID
		nextItem = tokenizer.getNextItem();
		if (nextItem.getType() == Item.STRING)
			result.set("Message-ID", (String) nextItem.getValue());

		return result;
	}

	private static MimePart parseMimeStructure (
		IMAPResponse response,
		String structure) throws ParserException  {
		MimeHeader header = new MimeHeader();
		BodystructureTokenizer tokenizer =
			new BodystructureTokenizer(response, structure);
		BodystructureTokenizer subtokenizer;
		Item nextItem, valueItem;
		MimeType type = null;
		StreamableMimePart result = null;

		String contentType = "text";
		// Content-Type
		nextItem = tokenizer.getNextItem();
		if (nextItem.getType() == Item.STRING)
			contentType = ((String) nextItem.getValue()).toLowerCase();

		// ContentSubtype
		nextItem = tokenizer.getNextItem();
		if (nextItem.getType() == Item.STRING) {
			type =
				new MimeType(
					contentType,
					((String) nextItem.getValue()).toLowerCase());
			header.setMimeType(type);
		}

		// are there some Content Parameters ?
		nextItem = tokenizer.getNextItem();
		if (nextItem.getType() == Item.PARENTHESIS) {
			subtokenizer =
				new BodystructureTokenizer(
					response,
					(String) nextItem.getValue());

			nextItem = subtokenizer.getNextItem();
			while (nextItem != null) {
				valueItem = subtokenizer.getNextItem();

				header.putContentParameter(
					((String) nextItem.getValue()).toLowerCase(),
					(String) valueItem.getValue());

				nextItem = subtokenizer.getNextItem();
			}
		}

		// ID
		nextItem = tokenizer.getNextItem();
		if (nextItem.getType() == Item.STRING)
			header.setContentID(((String) nextItem.getValue()).toLowerCase());

		// Description
		nextItem = tokenizer.getNextItem();
		if (nextItem.getType() == Item.STRING)
			header.setContentDescription(
				((String) nextItem.getValue()).toLowerCase());

		// Encoding
		nextItem = tokenizer.getNextItem();
		if (nextItem.getType() == Item.STRING)
			header.setContentTransferEncoding(
				((String) nextItem.getValue()).toLowerCase());

		// Size
		int size = 0;
		nextItem = tokenizer.getNextItem();
		if (nextItem.getType() == Item.NUMBER)
			size = ((Integer) nextItem.getValue()).intValue();

		// Is this a Message/RFC822 Part ?
		if (type.getType().equals("message")
			&& type.getSubtype().equals("rfc822")) {
			result = new MessageMimePart(header);

			Message subMessage = new Message();

			// Envelope

			nextItem = tokenizer.getNextItem();
			if (nextItem.getType() == Item.PARENTHESIS) {
				subMessage.setHeader(
					parseEnvelope(response, (String) nextItem.getValue()));
			}

			// Bodystrucuture of Sub-Message

			nextItem = tokenizer.getNextItem();
			if (nextItem.getType() == Item.PARENTHESIS) {
				MimePart subStructure =
					parseBS(response, (String) nextItem.getValue());
				subStructure.setParent(result);
				subMessage.setMimePartTree(new MimeTree(subStructure));
			}

			((MessageMimePart) result).setMessage(subMessage);

			// Number of lines
			nextItem = tokenizer.getNextItem();
		}
		// Is this a Text - Part ?
		else if (type.getType().equals("text")) {
			result = new LocalMimePart(header);

			// Number of lines
			nextItem = tokenizer.getNextItem();
			// DONT CARE
		} else {
			result = new LocalMimePart(header);
		}

		// Set size
		result.setSize(size);

		// Are there Extensions ?
		// MD5
		nextItem = tokenizer.getNextItem();
		if (nextItem == null)
			return new MimePart(header);
		// DONT CARE

		// are there some Disposition Parameters ?
		nextItem = tokenizer.getNextItem();
		if (nextItem == null)
			return new MimePart(header);

		if (nextItem.getType() == Item.STRING) {
			header.setContentDisposition(
				((String) nextItem.getValue()).toLowerCase());
		} else if (nextItem.getType() == Item.PARENTHESIS) {
			subtokenizer =
				new BodystructureTokenizer(
					response,
					(String) nextItem.getValue());

			nextItem = subtokenizer.getNextItem();
			header.setContentDisposition(
				((String) nextItem.getValue()).toLowerCase());

			nextItem = subtokenizer.getNextItem();
			// Are there Disposition Parameters?
			if (nextItem.getType() == Item.PARENTHESIS) {
				subtokenizer =
					new BodystructureTokenizer(
						response,
						(String) nextItem.getValue());

				nextItem = subtokenizer.getNextItem();

				while (nextItem != null) {
					valueItem = subtokenizer.getNextItem();

					header.putDispositionParameter(
						((String) nextItem.getValue()).toLowerCase(),
						(String) valueItem.getValue());

					nextItem = subtokenizer.getNextItem();
				}
			}
		}

		// WE DO NOT GATHER FURTHER INFORMATION

		return result;
	}

}

class BodystructureTokenizer {

	private String s;
	private Interval i;
	private IMAPResponse response;

	/**
	 * Constructs the BodystructureTokenizer.
	 * 
	 * @param response
	 * @param s
	 */
	public BodystructureTokenizer(IMAPResponse response, String s) {
		this.s = s;
		this.response = response;
		i = new Interval();
	}

	/**
	 * Gets the next item of the structure.
	 * 
	 * @return the next Item
	 * @throws ParserException
	 */
	public Item getNextItem() throws ParserException {
		Item result = new Item();

		// Search for next Item
		i.a = i.b + 1;
		// ..but Check Bounds!!
		if (i.a >= s.length())
			return null;
		while (s.charAt(i.a) == ' ') {
			i.a++;
			if (i.a >= s.length())
				return null;
		}

		// Quoted

		if (s.charAt(i.a) == '\"') {
			i.b = s.indexOf("\"", i.a + 1);

			result.setType(Item.STRING);
			result.setValue(s.substring(i.a + 1, i.b));
		}

		// Literal

		else if (s.charAt(i.a) == '{') {
			i.b = s.indexOf("}", i.a);

			result.setType(Item.STRING);
			result.setValue(response.getData(s.substring(i.a, i.b+1)).toString());
		}

		// Parenthesized

		else if (s.charAt(i.a) == '(') {
			i.b = ParenthesisParser.getClosingPos(s, i.a);

			result.setType(Item.PARENTHESIS);
			result.setValue(s.substring(i.a + 1, i.b));
		}

		// NIL or Number

		else {
			i.b = s.indexOf(" ", i.a + 1);
			if (i.b == -1)
				i.b = s.length();

			String item = s.substring(i.a, i.b);
			i.b--;

			if (item.equals("NIL")) {
				result.setType(Item.NIL);
			} else {
				result.setValue(new Integer(item));
				result.setType(Item.NUMBER);
			}
		}

		return result;
	}
}

class Item {
	/**
	 * Itemtype
	 */
	public static final int STRING = 0;
	/**
	 * Itemtype
	 */
	public static final int PARENTHESIS = 1;
	/**
	 * Itemtype
	 */
	public static final int NIL = 2;
	/**
	 * Itemtype
	 */
	public static final int NUMBER = 3;

	private Object value;
	private int type;
	/**
	 * Returns the type.
	 * 
	 * @return int
	 */
	public int getType() {
		return type;
	}

	/**
	 * Returns the value.
	 * 
	 * @return Object
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Sets the type.
	 * 
	 * @param type
	 *            The type to set
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * Sets the value.
	 * 
	 * @param value
	 *            The value to set
	 */
	public void setValue(Object value) {
		this.value = value;
	}

}

class Interval {
	/**
	 * The start position.
	 */
	public int a;
	/**
	 * The end position.
	 */
	public int b;

	/**
	 * Constructs the Interval.
	 * 
	 * @param a
	 * @param b
	 */
	public Interval(int a, int b) {
		this.a = a;
		this.b = b;
	}

	/**
	 * Constructs the Interval.
	 */
	public Interval() {
		a = -1;
		b = -1;
	}

	/**
	 * Reset the Interval.
	 */
	public void reset() {
		a = -1;
		b = -2;
	}
}
