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

import org.columba.ristretto.message.MailboxInfo;

/**
 * Holds the informations about the status of the mailbox.
 * 
 * @author tstich
 */
public class MailboxStatus {
    private String name;
    private int messages;
    private int unseen;
    private int recent;
    private long uidValidity;
    private long uidNext;
    
    /**
     * Constructs the MailboxStatus.
     * 
     */
    public MailboxStatus() {
        messages = unseen = recent = -1;
        uidValidity = uidNext = -1;
    }
    
    /**
     * Constructs the MailboxStatus.
     * 
     * @param info the MailboxInfo to convert
     */
    public MailboxStatus(MailboxInfo info) {
    	messages = info.getExists();
    	recent = info.getRecent();
    	uidNext = info.getUidNext();
    	uidValidity = info.getUidValidity();
    	
    	// Info is not in the mailboxinfo
    	unseen = -1;
    }

    /**
     * @return Returns the messages.
     */
    public int getMessages() {
        return messages;
    }
    /**
     * @param messages The messages to set.
     */
    public void setMessages(int messages) {
        this.messages = messages;
    }
    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }
    /**
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * @return Returns the recent.
     */
    public int getRecent() {
        return recent;
    }
    /**
     * @param recent The recent to set.
     */
    public void setRecent(int recent) {
        this.recent = recent;
    }
    /**
     * @return Returns the uidNext.
     */
    public long getUidNext() {
        return uidNext;
    }
    /**
     * @param uidNext The uidNext to set.
     */
    public void setUidNext(long uidNext) {
        this.uidNext = uidNext;
    }
    /**
     * @return Returns the uidValidity.
     */
    public long getUidValidity() {
        return uidValidity;
    }
    /**
     * @param uidValidity The uidValidity to set.
     */
    public void setUidValidity(long uidValidity) {
        this.uidValidity = uidValidity;
    }
    /**
     * @return Returns the unseen.
     */
    public int getUnseen() {
        return unseen;
    }
    /**
     * @param unseen The unseen to set.
     */
    public void setUnseen(int unseen) {
        this.unseen = unseen;
    }
}
