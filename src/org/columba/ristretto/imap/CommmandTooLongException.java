//The contents of this file are subject to the Mozilla Public License Version 1.1
//(the "License"); you may not use this file except in compliance with the 
//License. You may obtain a copy of the License at http://www.mozilla.org/MPL/
//
//Software distributed under the License is distributed on an "AS IS" basis,
//WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License 
//for the specific language governing rights and
//limitations under the License.
//
//The Original Code is "The Columba Project"
//
//The Initial Developers of the Original Code are Frederik Dietz and Timo Stich.
//Portions created by Frederik Dietz and Timo Stich are Copyright (C) 2003. 
//
//All Rights Reserved.

package org.columba.ristretto.imap;

/**
 * Thrown when a the length of the command is longer than 1000 charcters.
 * Split the command and reissue in smaller packets.
 * 
 * @author tstich
 */
public class CommmandTooLongException extends IMAPException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	IMAPCommand command;
	
	/**
	 * Constructs the CommmandTooLongException.
	 * 
	 * @param command
	 */
	public CommmandTooLongException(IMAPCommand command) {
		super();
		this.command = command;
	}
	/**
	 * Constructs the CommmandTooLongException.
	 * 
	 * 
	 */
	public CommmandTooLongException() {
		super();
	}

	/**
	 * Constructs the CommmandTooLongException.
	 * 
	 * @param arg0
	 * @param arg1
	 */
	public CommmandTooLongException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	/**
	 * Constructs the CommmandTooLongException.
	 * 
	 * @param s
	 */
	public CommmandTooLongException(String s) {
		super(s);
	}

	/**
	 * Constructs the CommmandTooLongException.
	 * 
	 * @param response
	 */
	public CommmandTooLongException(IMAPResponse response) {
		super(response);
	}

	/**
	 * Constructs the CommmandTooLongException.
	 * 
	 * @param cause
	 */
	public CommmandTooLongException(Throwable cause) {
		super(cause);
	}

	/**
	 * Gets the command that caused the error.
	 * 
	 * @return the issued command
	 */
	public IMAPCommand getCommand() {
		return command;
	}
}
