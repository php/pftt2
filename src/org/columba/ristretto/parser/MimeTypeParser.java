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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.columba.ristretto.message.MimeType;

/**
 * Parser for MIME types as defined in RFC 2045.
 * 
 * @author Timo Stich <tstich@users.sourceforge.net>
 */
public class MimeTypeParser {
	
	private static final Pattern mimetypePattern = Pattern.compile("([^/\\s]+)/([^\\s;]+)");
	
	private MimeTypeParser() {
	}
	
	/**
	 * Parses a MIME type from a String. 
	 * <p>
	 * Example: text/plain 
	 * 
	 * @param mimeType the String representation of the MIME type
	 * @return the parsed MIME type
	 * @throws ParserException
	 */
	public static MimeType parse(CharSequence mimeType ) throws ParserException {
		if( mimeType == null ) throw new ParserException( "Input is null");
		Matcher matcher = mimetypePattern.matcher(mimeType);
		if( matcher.find() ) {
			return new MimeType( matcher.group(1).toLowerCase(), matcher.group(2).toLowerCase());
		} else throw new ParserException();	
	}

}
