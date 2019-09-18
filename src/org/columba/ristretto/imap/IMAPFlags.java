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
import java.io.ObjectInputStream;

import org.columba.ristretto.message.Flags;

/**
 * Extends the Flags with JUNK and UID from IMAP.
 * 
 * <br>
 * <b>RFC(s):</b> 3105
 * 
 * @author Timo Stich <tstich@users.sourceforge.net>
 */
public class IMAPFlags extends Flags {
	
    /**
     * Flag used on some IMAP servers for SPAM messages.
     * 
     */
    public static final short JUNK = 0x0100;
    
	private Object uid;
	private int index;
	

	/**
	 * Constructs the IMAPFlags.
	 * 
	 */
	public IMAPFlags() {
		super();
	}

	/**
	 * Constructs a new IMAPFlags Object with the given flags
	 * 
	 * @param flags
	 */
	public IMAPFlags(short flags) {
		super(flags);
	}

	/**
	 * Contructs a new IMAPFlags Object that can be read from the ObjectInputStream
	 * 
	 * @param in The ObjectInputStream to read from
	 * @throws IOException
	 */
	public IMAPFlags(ObjectInputStream in) throws IOException {
		super(in);
	}

	/**
	 * Gets the UID. 
	 * 
	 * @return the UID or <code>null</code> if no UID was associated.
	 */
	public Object getUid() {
		return uid;
	}

	/**
	 * Sets the UID.
	 * 
	 * @param object The UID from the imap-server
	 */
	public void setUid(Object object) {
		uid = object;
	}

	
	/**
	 * Sets the JUNK flag.
	 * 
	 * @param b the value of the JUNK flag.
	 */
	public void setJunk(boolean b) {
		set(JUNK,b);
	}
	
    /**
     * Gets the JUNK flag.
     * 
     * @return the value of the JUNK flag.
     */
    public boolean getJunk() {
        return get(JUNK);
    }
	
	
    /**
     * @see org.columba.ristretto.message.Flags#toString()
     */
    public String toString() {
    	boolean first = true;
    	StringBuffer result = new StringBuffer("(");
    	
        if( getAnswered()) {
            result.append("\\Answered");
            first = false;
        }
        
        if( getFlagged()) {
        	if( !first ) result.append(" ");
        	first = false;
            result.append("\\Flagged");
        }
        
        if( getDeleted()) {
        	if( !first ) result.append(" ");
        	first = false;
            result.append("\\Deleted");
        }

        if( getSeen()) {
        	if( !first ) result.append(" ");
        	first = false;
            result.append("\\Seen");
        }
        
        if( getDraft()) {
        	if( !first ) result.append(" ");
        	first = false;
            result.append("\\Draft");
        }
        
        /* Recent is only allowed when fetching thus we should never render it
        if( getRecent()) {
            result.append(" \\Recent");
        }
        */
        
        // JUNK is only allowed in a STORE command !
        if( this.get(JUNK)) {
        	if( !first ) result.append(" ");
        	first = false;
            result.append("JUNK");
        }
        
        result.append(")");
        
        return result.toString();

    }
	/**
	 * @return Returns the index.
	 */
	public int getIndex() {
		return index;
	}
	/**
	 * @param index The index to set.
	 */
	public void setIndex(int index) {
		this.index = index;
	}
}
