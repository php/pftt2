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
 * This class represents a listinfo like returned from a LSUB or LIST response.
 * <p>
 * For further information especially on the parameters see RFC 3501.
 * 
 * @author Timo Stich <tstich@users.sourceforge.net>
 */
public class ListInfo implements Comparable {
	
	/**
	 * ListInfo parameter. There are no inferiors for this mailbox.
	 */
	public static final int NOINFERIORS = 1;

	/**
	 * ListInfo parameter. The SELECT command on this mailbox is forbidden.
	 */
	public static final int NOSELECT = 2;

	/**
	 * ListInfo parameter. This mailbox is marked.
	 */
	public static final int MARKED = 4;

	/**
	 * ListInfo parameter. This mailbox is unmarked.
	 */
	public static final int UNMARKED = 8;

	protected String mailboxName;
	protected String delimiter;
	protected int parameters;

	/**
	 * Contstructs a ListInfo.
	 * 
	 * @param name of the mailbox
	 * @param delimiter the delimiter of the imap server
	 * @param parameters 
	 */
	public ListInfo(String name, String delimiter, int parameters) {
		this.mailboxName = name;
		this.delimiter = delimiter;
		this.parameters = parameters;
	}
	
	/**
	 * Gets the name of the mailbox.
	 * 
	 * @return name of the mailbox.
	 */
	public String getName() {
		return mailboxName;
	}
	
	/**
	 * Checks if the parameter NOSELECT is not set.
	 * 
	 * @return boolean true if NOSELECT is not set, else false.
	 */
	public boolean isSelectable() {
		return !getParameter(NOSELECT);
	}

	/**
	 * Gets the delimiter of the imap server.
	 * For example '.' or '/'.
	 * 
	 * @return the delimiter
	 */
	public String getDelimiter() {
		return delimiter;
	}
	
	/**
	 * Check if this parameter is set.
	 * 
	 * @param parameter 
	 * @return true if the parameter is set, else false.
	 */
	public boolean getParameter(int parameter) {
		return (parameters & parameter) > 0;
	}

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object arg0) {
        return mailboxName.compareTo(((ListInfo)arg0).getName());
    }

}
