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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.columba.ristretto.imap.IMAPResponse;
import org.columba.ristretto.parser.ParserException;

/**
 * Main parser for the reponses received
 * from the IMAP server. 
 * 
 * @author tstich
 *
 */
public class IMAPResponseParser {

	
	// Pattern to classify the response in tagged/untagged/continuation
	// and to access the text of the reponse without the trailing CRLF
	private static final Pattern response =
		Pattern.compile("^((\\*)" +		// group 2 is untagged			"|([0-9a-zA-Z]+)" + 		// group 3 is the tag			"|(\\+)) " +				// group 4 is continuation			"([^\r\n]*)\r?\n?");		// group 5 is the rest of the response without a optional CRLF

	
	// Pattern to classify a status reponse.		
	private static final Pattern resp_cond =
		Pattern.compile(
			"^(OK|BAD|NO|BYE|PREAUTH) " +	// group 1 is the status type			"(\\[(\\w+[^\\]]+)\\])?" +  // group 3 is an optional text code			
			" ?([^\r\n]*)");					// group 4 is the message


	// Pattern to classify a mailbox data response.			
	private static final Pattern mailbox_data =
		Pattern.compile(
			"^((FLAGS|LIST|LSUB|SEARCH|STATUS|CAPABILITY|NAMESPACE)" +	// group2 contains a simple command with no prenumber			"|((\\d+) " +										// group4 contains a prenumber			"(EXISTS|RECENT)))" +								// group5 the command to the prenumber			" ?([^\r\n]*)");									// group6 contains the data
			
			
	// Pattern to classify a message data response.		
	private static final Pattern message_data =
		Pattern.compile("^(\\d+) " +							//group1 contains the prenumber			"(EXPUNGE|FETCH)" +									//group2 contains the command			" ?([^\r\n]*)");									//group3 contains the data


	// Pattern to classify a continuation response.
	private static final Pattern resp_cont =
		Pattern.compile("^(\\[(\\w+[^\\]]+)\\] )?" +		// group2 contains optional text code			"([^\r\n]*)");									// group3 contains the message


	/**
	 * Parses a raw string from a IMAP Server and returns the preparsed IMAPResponse.
	 * 
	 * @param input
	 * @return the parsed Response
	 * @throws ParserException
	 */
	public static IMAPResponse parse(CharSequence input) throws ParserException {
		Matcher matcher = response.matcher(input);

		// Is this a valid response?
		if (matcher.matches()) {
			IMAPResponse result = new IMAPResponse(input.toString());

			// what kind of? (untagged)
			if (matcher.group(2) != null) {

				// test for resp-cond (BYE/OK/BAD/PREAUTH)
				Matcher respCode = resp_cond.matcher(matcher.group(5));
				if (respCode.matches()) {
					result.setResponseType(IMAPResponse.RESPONSE_STATUS);
					result.setResponseSubType(respCode.group(1));

					// do we have a responseCode?
					if (respCode.group(3) != null) {
						result.setResponseTextCode(ResponseTextCodeParser.parse(respCode.group(3)));
					}

					// set reponse message
					result.setResponseMessage(respCode.group(4));

					return result;
				}

				// test for mailbox-data (LIST|LSUB|FLAGS...)
				Matcher mailboxData = mailbox_data.matcher(matcher.group(5));
				if (mailboxData.matches()) {
					result.setResponseType(IMAPResponse.RESPONSE_MAILBOX_DATA);

					// command without or with a pre number
					if (mailboxData.group(2) != null) {
						result.setResponseSubType(mailboxData.group(1));
					} else {
						result.setPreNumber(Integer.parseInt(mailboxData.group(4)));
						result.setResponseSubType(mailboxData.group(5));
					}

					result.setResponseMessage(mailboxData.group(6));

					return result;
				}

				// test for message-data (EXPUNGE|FECTH)
				Matcher messageData = message_data.matcher(matcher.group(5));
				if (messageData.matches()) {
					result.setResponseType(IMAPResponse.RESPONSE_MESSAGE_DATA);
					
					result.setPreNumber(Integer.parseInt(messageData.group(1)));
					result.setResponseSubType(messageData.group(2));
					
					result.setResponseMessage(messageData.group(3));

					return result;
				}

				// Not any of the above subtypes
				throw new ParserException(
					"Unkown subytpe : " + result.getSource());
			}

			// what kind of? (tagged)
			if (matcher.group(3) != null) {
				result.setTag(matcher.group(3));

				// test for resp-cond (BYE/OK/BAD/PREAUTH)
				Matcher respCode = resp_cond.matcher(matcher.group(5));
				if (respCode.matches()) {
					result.setResponseType(IMAPResponse.RESPONSE_STATUS);
					result.setResponseSubType(respCode.group(1));

					// do we have a responseCode?
					if (respCode.group(3) != null) {
						result.setResponseTextCode(ResponseTextCodeParser.parse(respCode.group(3)));
					}

					// set reponse text
					result.setResponseMessage(respCode.group(4));

					return result;
				}

				// Not any of the above subtypes
				throw new ParserException(
					"Unkown subytpe :" + result.getSource());
			}

			// what kind of? (continuation)
			if (matcher.group(4) != null) {
				result.setResponseType(IMAPResponse.RESPONSE_CONTINUATION);

				Matcher respCont = resp_cont.matcher(matcher.group(5));
				if (respCont.matches()) {
					// do we have a responseCode?
					if (respCont.group(2) != null) {
						result.setResponseTextCode(ResponseTextCodeParser.parse(respCont.group(2)));
					}

					// set reponse text
					result.setResponseMessage(respCont.group(3));

					return result;
				}

			}

		}

		throw new ParserException("Not a valid IMAP response : " + input);
	}
}
