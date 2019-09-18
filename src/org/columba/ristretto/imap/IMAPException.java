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

/**
 * Thrown whenever anything unexpected in the communication
 * with the IMAP server happend. 
 * 
 * @author tstich
 *
 */
public class IMAPException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	IMAPResponse response;
	
	/**
	 * Constructs the IMAPException.
	 *
	 */
	public IMAPException() {
	}
	/**
	 * Constructs the IMAPException.
	 * 
	 * @param arg0
	 * @param arg1
	 */
	public IMAPException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}
	
	/**
	 * Constructs the IMAPException.
	 * 
	 * @param s
	 */
    public IMAPException( String s )
    {
        super(s );
    }
    
    /**
     * Constructs the IMAPException.
     * 
     * @param response
     */
    public IMAPException( IMAPResponse response )
    {
    	super(response.getResponseMessage());
    	this.response = response;
    }

    /**
     * Constructs the IMAPException.
     * 
     * @param cause
     */
    public IMAPException(Throwable cause) {
        super(cause);
    }

    /**
     * Gets the response of the IMAP server that caused
     * the exception. If no response is available <code>
     * null</code> is returned.
     * 
	 * @return the response that caused the exception.
	 */
	public IMAPResponse getResponse() {
		return response;
	}
	/**
	 * @see java.lang.Throwable#getMessage()
	 */
	public String getMessage() {
		if( response != null) return response.getResponseMessage();
		else return super.getMessage();
	}
}