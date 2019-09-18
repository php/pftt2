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
package org.columba.ristretto.pop3;

import org.columba.ristretto.io.Source;

/**
 * Representation of a response from a POP3 server.
 * 
 * @author tstich
 *
 */
public class POP3Response {

	/**
	 * Response type.
	 */
	public static final int OK = 0;
	/**
	 * Response type.
	 */
	public static final int ERR = -1;
	
	private int type;
	private String message;
	private Source data;
	private CharSequence source;
	
	/**
	 * Constructs the POP3Response.
	 * 
	 * @param source
	 */
	public POP3Response(CharSequence source) {
		this.source = source;
	};
	
	

	/**
	 * @return the data
	 */
	public Source getData() {
		return data;
	}

	/**
	 * Set the data. 
	 * 
	 * @param data
	 */
	public void setData(Source data) {
		this.data = data;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Set the message.
	 * 
	 * @param message
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * @return the type
	 */
	public int getType() {
		return type;
	}

	/**
	 * Set the type.
	 * 
	 * @param type
	 */
	public void setType(int type) {
		this.type = type;
	}
	
	/**
	 * @return <code>true</true> if the type is OK
	 */
	public boolean isOK() {
		return type == OK;
	}
	
	/**
	 * @return <code>true</true> if the type is ERR
	 */
	public boolean isERR() {
		return type == ERR;
	}

	/**
	 * @return the source 
	 */
	public CharSequence getSource() {
		return source;
	}

	/**
	 * Set the source.
	 * 
	 * @param source
	 */
	public void setSource(CharSequence source) {
		this.source = source;
	}

}
