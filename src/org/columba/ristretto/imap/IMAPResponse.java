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

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.columba.ristretto.io.CharSequenceSource;
import org.columba.ristretto.io.Source;

/**
 * This class represents a response from an IMAP server.
 * 
 * @author Frederik Dietz <fdietz>, Timo Stich <tstich@users.sourceforge.net>
 */
public class IMAPResponse {
	
	private static final Pattern literalPattern = Pattern.compile("^\\{(\\d+)\\}$");
	private static final Matcher literalMatcher = literalPattern.matcher("");	

	protected List literals;

	protected int preNumber;

	protected String tag;

	protected String source;

	protected String responseMessage;

	protected int responseType;

	protected String responseSubType;

	protected ResponseTextCode responseTextCode;
	
	/**
	 * Constant for responsetype STATUS. Returned by {@link #getResponseType()}.
	 */
	public static final int RESPONSE_STATUS = 0;

	/**
	 * Constant for responsetype MAILBOX_DATA. Returned by {@link #getResponseType()}.
	 */
	public static final int RESPONSE_MAILBOX_DATA = 1;

	/**
	 * Constant for responsetype MESSAGE_DATA. Returned by {@link #getResponseType()}.
	 */
	public static final int RESPONSE_MESSAGE_DATA = 2;

	/**
	 * Constant for responsetype CONTINUATION. Returned by {@link #getResponseType()}.
	 */
	public static final int RESPONSE_CONTINUATION = 3;

	/**
	 * Constructs an IMAPResponse with the given source.
	 * 
	 * @param source
	 */
	public IMAPResponse(String source) {
		this.source = source;
		
		literals = new LinkedList();
		preNumber = -1;
	}

	/**
	 * Returns the tag of the response.
	 * If the reponse has no tag null is returned.
	 * 
	 * @see #isTagged()
	 * 
	 * @return the tag of the reponse or null if not present.
	 */
	public String getTag() {
		return tag;
	}


	
	/**
	 * Gets the source of the response.
	 * 
	 * @return the response as String
	 */
	public String getSource() {
		StringBuffer cleanedup = new StringBuffer(source.length());
		literalMatcher.reset(source);
		
		while (literalMatcher.find()) {
			literalMatcher.appendReplacement(cleanedup, getData(literalMatcher.group()).toString());
		 }
		 literalMatcher.appendTail(cleanedup);
		
		return cleanedup.toString();
	}


	
	/**
	 * Checks if the response is tagged.
	 * 
	 * @return true if the response is tagged, else false
	 */
	public boolean isTagged() {
		return tag != null;
	}

	/**
	 * Checks if the response subtye is "OK".
	 * <p>Convienice method, uses {@link #getResponseSubType() }. 
	 * 
	 * @return true if the response subtye is "OK", else false
	 */
	public boolean isOK() {
		return responseSubType.equals("OK");
	}

	/**
	 * Checks if the response subtye is "NO".
	 * <p>Convienice method, uses {@link #getResponseSubType() }. 
	 * 
	 * @return true if the response subtye is "NO", else false
	 */
	public boolean isNO() {
		return responseSubType.equals("NO");
	}

	/**
	 * Checks if the response subtye is "BYE".
	 * <p>Convienice method, uses {@link #getResponseSubType() }. 
	 * 
	 * @return true if the response subtye is "BYE", else false
	 */
	public boolean isBYE() {
		return responseSubType.equals("BYE");
	}

	/**
	 * Checks if the response subtye is "BAD".
	 * <p>Convienice method, uses {@link #getResponseSubType() }. 
	 * 
	 * @return true if the response subtye is "BAD", else false
	 */
	public boolean isBAD() {
		return responseSubType.equals("BAD");
	}

	/**
	 * Sets the tag.
	 * 
	 * @param string
	 */
	public void setTag(String string) {
		tag = string;
	}

	/**
	 * Gets the message.
	 * <p>
	 * Returns the message from the server response. This can be a human-readable
	 * message that might be used to show to the user if an error occured.
	 * For example if the answer is "A044 BAD No such command as "BLURDYBLOOP"" the
	 * response message is: "No such command as "BLURDYBLOOP"". <p>
	 * This can also be the data if the reponse is of type message or mailbox-data.
	 * For example if the answer is "* LIST (\Noselect) "/" ~/Mail/foo" the message
	 * is "(\Noselect) "/" ~/Mail/foo".
	 * <p>
	 * <b>Note:</b> If the message contains literals you can access them via {@link #getLiteral(int)}. 
	 * 
	 * @return the message of a response
	 */
	public String getResponseMessage() {
		return responseMessage;
	}

	/**
	 * Sets the message.
	 * 
	 * @see #getResponseMessage()
	 * 
	 * @param responseText
	 */
	public void setResponseMessage(String responseText) {
		this.responseMessage = responseText;
	}

	/**
	 * Gets the response type of this imap response.
	 * <p>
	 * There are four types of responses:
	 * <li> RESPONSE_STATUS . Example: * OK IMAP4rev1 server ready
	 * <li> RESPONSE_MAILBOX_DATA . Example : * LSUB () "." #news.comp.mail.misc
	 * <li> RESPONSE_MESSAGE_DATA . Example : * 23 FETCH (FLAGS (\Seen) RFC822.SIZE 44827)
	 * <li> RESPONSE_CONTINUATION . Example : + Ready for additional command text
	 * 
	 * @return type of the response
	 */
	public int getResponseType() {
		return responseType;
	}

	/**
	 * Sets the response type of this imap response.
	 * 
	 * @see #getResponseType()
	 * 
	 * @param responseType
	 */
	public void setResponseType(int responseType) {
		this.responseType = responseType;
	}

	/**
	 * Gets the reponse subtype.
	 * <p>
	 * Example: For "* LIST (\Noselect) "/" ~/Mail/foo" the reponse subtype is LIST.
	 * 
	 * @return the response subtype
	 */
	public String getResponseSubType() {
		return responseSubType;
	}

	/**
	 * Sets the reponse subtype.
	 * <p>
	 * Example: For "* LIST (\Noselect) "/" ~/Mail/foo" the reponse subtype is LIST.
	 * 
	 * @param responseSubType
	 */
	public void setResponseSubType(String responseSubType) {
		this.responseSubType = responseSubType;
	}

	/**
	 * Gets the reponse text code.
	 * <p>
	 * The reponse text code is optional extra information to the response. It is
	 * null if no response text is present.<p>
	 * Example: For "A142 OK [READ-WRITE] SELECT completed" the text code is "READ-WRTE". 
	 * 
	 * @return the response text code
	 */
	public ResponseTextCode getResponseTextCode() {
		return responseTextCode;
	}

	/**
	 * Sets the reponse text code.
	 * 
	 * @see #getResponseTextCode()
	 * 
	 * @param responseTextCode
	 */
	public void setResponseTextCode(ResponseTextCode responseTextCode) {
		this.responseTextCode = responseTextCode;
	}

	/**
	 * @param restresponse
	 */
	public void appendResponseText(String restresponse) {
		source += restresponse;
		responseMessage += restresponse;
	}

	/**
	 * Gets the Literal with a given index. 
	 * @param index 
	 * 
	 * @return the Source of the Literal
	 */
	public Source getLiteral(int index) {
		return (Source) literals.get(index);
	}

	/**
	 * Add a literal to the response.
	 * 
	 * @param literal
	 */
	public void addLiteral(Source literal) {
		literals.add(literal);
	}

	/**
	 * Gets the pre number.
	 * <p>
	 * Is can be any number that comes in a server response before a command
	 * specification.<p>
	 * Example: For "* 44 EXPUNGE" the pre number is 44. 
	 * 
	 * @return the pre number of the response or -1 if not present.
	 */
	public int getPreNumber() {
		return preNumber;
	}

	/**
	 * Sets the pre number.
	 * 
	 * @param preNumber
	 */
	public void setPreNumber(int preNumber) {
		this.preNumber = preNumber;
	}
	
	/**
	 * Gets the data.
	 * <p> Use this method to get the data if this can be quoted or a literal.
	 * For example if the input is {0} the first literal is returned. If the
	 * input is "bla" then bla is returned.
	 * 
	 * @param data
	 * @return the Source of the data
	 */
	public Source getData(CharSequence data) {
		if( data.length() == 0 ) return new CharSequenceSource(data);
		literalMatcher.reset(data);
		if( literalMatcher.matches() ) {
			return getLiteral(Integer.parseInt(literalMatcher.group(1)));
		} else {
			// remove ""
			if( data.charAt(0) == '"') {
				return new CharSequenceSource( data.subSequence(1,data.length()-1) );	
			} else {
				return new CharSequenceSource( data );
			}
			
		}
	}

}
