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

import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.columba.ristretto.imap.IMAPFlags;
import org.columba.ristretto.imap.IMAPResponse;
import org.columba.ristretto.message.Attributes;
import org.columba.ristretto.message.Flags;

/**
 * @author fdietz
 *
 * See RFC 2060 IMAP4 (http://rfc-editor.org)
 * 
 * fetch list of message flags
 * 
 * example:
 *  
 * C: A999 UID FETCH 4827313:4828442 FLAGS
 * S: * 23 FETCH (FLAGS (\Seen) UID 4827313)
 * S: * 24 FETCH (FLAGS (\Seen) UID 4827943)
 * S: * 25 FETCH (FLAGS (\Seen) UID 4828442)
 * S: A999 UID FETCH completed
 * 
 */


//7.2.6.  FLAGS Response
//
//   Contents:   flag parenthesized list
//
//	  The FLAGS response occurs as a result of a SELECT or EXAMINE
//	  command.  The flag parenthesized list identifies the flags (at a
//	  minimum, the system-defined flags) that are applicable for this
//	  mailbox.  Flags other than the system flags can also exist,
//	  depending on server implementation.
//
//	  The update from the FLAGS response MUST be recorded by the client.
//
//   Example:    S: * FLAGS (\Answered \Flagged \Deleted \Seen \Draft)
//
public class FlagsParser {


	private static final Pattern flagsPattern = Pattern.compile("(\\\\|$)?(Answered)|(Flagged)|(Deleted)|(Seen)|(Draft)|(Recent)|(Junk) ?", Pattern.CASE_INSENSITIVE);
	

	/**
	 * Parse the Flags of the IMAP response.
	 * 
	 * @param response
	 * @return the Flags.
	 */
	public static IMAPFlags parse(IMAPResponse response) {
		IMAPFlags result = new IMAPFlags();

		result.setIndex(response.getPreNumber());
		
		// first parse the message attributes
		Attributes attributes = MessageAttributeParser.parse( response.getResponseMessage() );
		
		// parse the flags
		if( attributes.get("FLAGS") != null ) {
			Matcher matcher = flagsPattern.matcher((String)attributes.get("FLAGS"));
			while( matcher.find()) {
				if( matcher.group(1) != null ) {
					result.set(Flags.ANSWERED);
				} else if( matcher.group(3) != null ) {
					result.set(Flags.FLAGGED);					
				} else if( matcher.group(4) != null ) {
					result.set(Flags.DELETED);					
				} else if( matcher.group(5) != null ) {
					result.set(Flags.SEEN);					
				} else if( matcher.group(6) != null ) {
					result.set(Flags.DRAFT);					
				} else if( matcher.group(7) != null ) {
					result.set(Flags.RECENT);					
				} else if( matcher.group(8) != null ) {
				    result.set(IMAPFlags.JUNK);					
				}
			}
		}
		
		// add uid
		if( attributes.get("UID") != null ) {
			result.setUid(new Integer((String)attributes.get("UID")));
		}
		
		return result;
	}

	/**
	 * Parse the Flags of the IMAP repsonses.
	 * 
	 * @param responses
	 * @return the Flags
	 */
	public static Flags[] parseFlags(IMAPResponse[] responses) {
		List v = new Vector();

		for (int i = 0; i < responses.length - 1; i++) {
			if (responses[i] == null)
				continue;
			
			if( responses[i].getResponseSubType().equals("FETCH")) {
				v.add( parse(responses[i]));
				// consumes this line
				responses[i] = null;
			}
		}

		Flags[] flags = new Flags[v.size()];
		((Vector)v).copyInto(flags);

		return flags;
	}

	protected static IMAPFlags parseFlagsLine(String str) {
		IMAPFlags flags = new IMAPFlags();

		if (str.indexOf("Seen") != -1) {
			//System.out.println("seen is true ");
			flags.setSeen(true);
		}
		if (str.indexOf("Answered") != -1) {
			//System.out.println("answered is true ");
			flags.setAnswered(true);
		}
		if (str.indexOf("Flagged") != -1) {
			//System.out.println("flagged is true ");
			flags.setFlagged(true);
		}
		if (str.indexOf("Deleted") != -1) {
			//System.out.println("deleted is true ");
			flags.setDeleted(true);
		}

		if (str.indexOf("Recent") != -1) {
			//System.out.println("deleted is true ");
			flags.setRecent(true);
		}

		return flags;
	}

	protected static String parseUidsLine(String data) {

      // Find the start of the UID portion. Look for UID... as the UID
      // portion isn't guarenteed to be at the beginning of the line.
      int leftIndex  = data.indexOf("UID ") + 4;
      int rightIndex = data.indexOf(" ", leftIndex);

      if(rightIndex == -1){
        // No rightIndex, therefore you went to the end of line, so just
        // return from the left index to the end of the line
        return data.substring(leftIndex);
      }else{
        // Return the sub string you found
        return data.substring(leftIndex, rightIndex);
      }
	}

}
