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
package org.columba.ristretto.parser;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.columba.ristretto.io.Source;
import org.columba.ristretto.message.Header;

/**
 * Parser for headers as defined in RFC 2822 and RFC 2045.
 * 
 * @author Timo Stich <tstich@users.sourceforge.net>
 */
public class HeaderParser {
	
	private static final Pattern linePattern = Pattern.compile("([^\r\n]*)(\r?\n)|(\r\n?)");
	private static final Pattern keyValuePattern = Pattern.compile("([^:\\s]+): (.*)");
	private static final Pattern lineWrapPattern = Pattern.compile("\\s(.*)");
	
	private HeaderParser() {
	}
	/**
	 * Parses the headers of a RFC 2822 or 2045 compliant message. The parser
	 * starts at the actual position of the Source. After the parsing process
	 * the Source is positioned after the headers.
	 * 
	 * @see BodyParser
	 * 
	 * @param source
	 * @return the Header
	 * @throws ParserException
	 */
	public static Header parse( Source source ) throws ParserException {
		Header header = new Header();
		Matcher lineMatcher = linePattern.matcher(source);
		Matcher keyValueMatcher = keyValuePattern.matcher("");
		Matcher lineWrapMatcher = lineWrapPattern.matcher(""); 
		
		String lastKey = null;
		String lastValue = null;
		
		while(lineMatcher.find()) {
			// Is this the end of the header?
			if( lineMatcher.group(1) == null) {
				// Store a previously found key value pair
				if( lastValue != null) {
					header.append(lastKey, lastValue);
				}
				
				// we are done
				return header;
			}
			
			// Is this the end of the header?
			if( lineMatcher.group(1).equals("")) {
				// Store a previously found key value pair
				if( lastValue != null) {
					header.append(lastKey, lastValue);
				}
				
				// Go to the end of the header
				try {
				    source.seek(lineMatcher.end());
				} catch (IOException e) {
				    e.printStackTrace();
				}
				
				// we are done
				return header;
			}
			
			// Check first for the most possible type:
			// a simple key value line
			keyValueMatcher.reset(lineMatcher.group(1));
			if( keyValueMatcher.matches()) {
				// Store a previously found key value pair
				if( lastValue != null) {
					header.append(lastKey, lastValue);
				}
				
				// This is a normal key value headerline
				lastKey = normalizeKey( keyValueMatcher.group(1) );
				lastValue = keyValueMatcher.group(2);
			} else {
				// Do we have a folding WS?
				lineWrapMatcher.reset(lineMatcher.group(1));
				if( lineWrapMatcher.matches()) {
					if(lastValue != null) {
						// Append this to the last value
						lastValue += lineWrapMatcher.group(1);
					}
				}
				
			}
		}

		// We detected no end of header but ran out of lines		
		// Store a previously found key value pair
		if( lastValue != null) {
			header.append(lastKey, lastValue);
		}
		try {
            // Seek source to its end
            source.seek(source.length()-1);
        } catch (IOException e) {
            e.printStackTrace();
        }
		
		return header;
	}
	
	/**
	 * Formats the keys of the headers as defined in RFC 2822 and 2045.
	 * This is used to e.g. transform "from" -> "From".  
	 * 
	 * @param key unformated key.
	 * @return correctly formated key.
	 */
	public static String normalizeKey( String key ) {
		StringBuffer normalizedKey = new StringBuffer( key.length() );
		char last = key.charAt(0);		
		char act;
		normalizedKey.append(Character.toUpperCase(key.charAt(0)));

		for( int i=1; i<key.length(); i++) {
			act = key.charAt(i);
			if( last == '-' || (( last == 'I' || last == 'i') && act == 'd') ) {
				normalizedKey.append(Character.toUpperCase(act));
			} else {
				normalizedKey.append(Character.toLowerCase(act) );
			}
			last = act;
		}
		
		return normalizedKey.toString();
	}
}
