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

import org.columba.ristretto.parser.ParserException;

/**
 * Parser for findind the closing position of a given parenthesis. 
 * 
 * @author tstich
  */
public class ParenthesisParser {
	private static final int NORMAL = 0;
	private static final int IN_QUOTES = 1;
	
	/**
	 * Finds the closing parenthesis.
	 * 
	 * @param s
	 * @param openPos
	 * @return the position of the closing parenthesis
	 * @throws ParserException
	 */
	public static int getClosingPos(CharSequence s, int openPos) throws ParserException {
		int mode = NORMAL;
		int depth = 1;
		int idx = openPos + 1;
		
		while( idx < s.length() ) {
			switch( s.charAt(idx) ) {
				case '\"' : {
					// toggle Quotes mode
					mode ^= 1;
					break;
				}
				
				case '(' : {
					if( mode == NORMAL ) {
						depth++;
					}
					break;
				}
				
				case ')' : {
					if( mode == NORMAL ) {
						depth--;
						if( depth == 0 ) return idx;
					}
					break;
				}
			}
			
			idx++;
		}
		
		throw new ParserException("No matching Parenthesis found :" + s.toString() );
	}

}
