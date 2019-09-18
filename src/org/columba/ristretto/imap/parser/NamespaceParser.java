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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.columba.ristretto.imap.IMAPResponse;
import org.columba.ristretto.imap.Namespace;
import org.columba.ristretto.imap.NamespaceCollection;
import org.columba.ristretto.parser.ParserException;

/**
 * Parser for the Namespace response.
 * 
 * @author tstich
 */
public class NamespaceParser {

	// regexp for the namespace
	private static final Pattern namespacePattern = Pattern.compile("\\(" + // start
			// with
			// (
			"\"([^\"]*)\"" + // read the reference in "(group1)"
			"\\s" + // and now comes a WS
			"\"([^\"]*)\"" + // read the delimiter in "(group2)"
			"(\\s\"([^\"]*)\"" + // optional extensionName in group4
			"\\s\\(([^\\)]*)\\))?" + // extensionParameters in group5
			"\\)"); // end with )

	private static final Pattern quotedPattern = Pattern
			.compile("\"([^\"]*)\"\\s?"); // match a quoted string "(group1)"

	/**
	 * Parse the Namespace of the IMAP response.
	 * 
	 * @param response
	 * @return the NamespaceCollection 
	 * @throws ParserException
	 */
	public static final NamespaceCollection parse(IMAPResponse response)
			throws ParserException {
		return parse(response.getResponseMessage());
	}

	/**
	 * Parse the Namespace of the IMAP response.
	 * 
	 * @param input
	 * @return the NamespaceCollection 
	 * @throws ParserException
	 */
	public static final NamespaceCollection parse(String input)
			throws ParserException {
		NamespaceCollection result = new NamespaceCollection();

		// First we tokenize the response in the three parts for personal, other
		// users and shared
		String[] parts = tokenizeParts(input);

		// parse the three parts personal, other users and shared
		for (int i = 0; i < 3; i++) {
			String part = parts[i];

			//check for nil
			if (part.equalsIgnoreCase("nil")) {
				result.addNamespace(i, new Namespace(null, null));
			} else {
				// parse the namespace(s)
				Matcher namespaceMatcher = namespacePattern.matcher(part);

				while (namespaceMatcher.find()) {
					Namespace ns = new Namespace(namespaceMatcher.group(1),
							namespaceMatcher.group(2));

					// if there is an extension then add it to the object
					if (namespaceMatcher.group(4) != null) {
						ns.setExtensionName(namespaceMatcher.group(4));

						// parse the parameterlist
						List parameterList = new ArrayList();
						Matcher parameterMatcher = quotedPattern
								.matcher(namespaceMatcher.group(5));
						while (parameterMatcher.find()) {
							parameterList.add(parameterMatcher.group(1));
						}

						// add it as a String[] to the namespace
						ns.setExtensionParameter((String[]) parameterList
								.toArray(new String[0]));
					}

					result.addNamespace(i, ns);
				}
			}
		}

		return result;
	}

	private static String[] tokenizeParts(String input) throws ParserException {
		String[] result = new String[3];
		int start = 0;
		int end = -1;

		for (int i = 0; i < 3; i++) {
			if (input.charAt(start) == '(') {
				end = ParenthesisParser.getClosingPos(input, start);

				// strip the leading and closing parenthesis
				result[i] = input.substring(start + 1, end);
			} else {
				end = start + 2;
				result[i] = "NIL";
			}
			start = end + 1;

			// skip the WS
			if( input.length() > start && input.charAt(start) == ' ') {
				start ++;
			}
		}

		return result;
	}

}