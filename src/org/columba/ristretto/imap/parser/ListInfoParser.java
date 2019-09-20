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
import org.columba.ristretto.imap.ListInfo;
import org.columba.ristretto.imap.MailboxNameUTF7Converter;
import org.columba.ristretto.parser.ParserException;

/**
 * @author frd
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ListInfoParser {

	// example : * LIST () NIL testbox\r\n
		private static final Pattern listResponsePattern =
			Pattern.compile("^\\(([^)]*)\\) " + // read parameters in $1
							"((\"([^\"]+)\")|(NIL)) " + // read separator in $4
							"\"?([^\"]*)\"?$"); //read name or literal in $6

	private static final Pattern parameterPattern =
		Pattern.compile(
			"((\\\\Noinferiors)|(\\\\Noselect)|(\\\\Marked)|(\\\\UnMarked)) ?",
			Pattern.CASE_INSENSITIVE);

	/**
	 * Parse the ListInfo of the IMAP response.
	 * 
	 * @param response
	 * @return the ListInfo
	 * @throws ParserException
	 */
	public static ListInfo parse(IMAPResponse response)
		throws ParserException {
		Matcher matcher =
			listResponsePattern.matcher(response.getResponseMessage());
		if (matcher.matches()) {
			String parameterString = matcher.group(1);
			int parameters = 0;
			if (parameterString != null) {
				parameters = parseParameters(parameterString);
			}
			String delimiter = matcher.group(4); // this might also be null
			String name = MailboxNameUTF7Converter.decode(matcher.group(6));
		
			name = response.getData(name).toString();

			ListInfo result = new ListInfo(name, delimiter, parameters);

			return result;
		}

		throw new ParserException(
			"Invalid List/Lsub response : " + response.getSource());
	}

	private static int parseParameters(String parameterString) {
		Matcher matcher = parameterPattern.matcher(parameterString);
		int result = 0;

		while (matcher.find()) {
			// Noinferiors
			if (matcher.group(2) != null) {
				result |= ListInfo.NOINFERIORS;
				continue;
			}
			// Noselect
			if (matcher.group(3) != null) {
				result |= ListInfo.NOSELECT;
				continue;
			}
			// Marked
			if (matcher.group(4) != null) {
				result |= ListInfo.MARKED;
				continue;
			}
			// Unmarked
			if (matcher.group(5) != null) {
				result |= ListInfo.UNMARKED;
				continue;
			}
		}

		return result;
	}

}
