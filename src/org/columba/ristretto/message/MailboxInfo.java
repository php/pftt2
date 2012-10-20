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
package org.columba.ristretto.message;

/**
 * Datastructure that stores the status
 * of a mailbox.
 * 
 * @author tstich
 *
 */
public class MailboxInfo {
    
    private String[] definedFlags;
    private int exists;
    private int recent;
    private int unseen;
    
    private int firstUnseen;
    private String[] permanentFlags;
    
    private int uidNext;
    private int uidValidity;    
    
    private boolean writeAccess;
    
    /**
     * Constructs the MailboxInfo.
     */
    public MailboxInfo() {
        exists = recent = unseen = firstUnseen = 0;
        uidNext = uidValidity = -1;
    }
    
    /**
     * Resets the MailboxInfo.
     * 
     */
    public void reset() {
        exists = recent =  unseen = firstUnseen = 0;
        uidNext = uidValidity = -1;        
    }
    
    /**
     * @return Returns the definedFlags.
     */
    public String[] getDefinedFlags() {
        return (String[]) definedFlags.clone();
    }
    /**
     * @param definedFlags The definedFlags to set.
     */
    public void setDefinedFlags(String[] definedFlags) {
        this.definedFlags = definedFlags;
    }
    /**
     * @return Returns the exists.
     */
    public int getExists() {
        return exists;
    }
    /**
     * @param exists The exists to set.
     */
    public void setExists(int exists) {
        this.exists = exists;
    }
    /**
     * @return Returns the firstUnseen.
     */
    public int getFirstUnseen() {
        return firstUnseen;
    }
    /**
     * @param firstUnseen The firstUnseen to set.
     */
    public void setFirstUnseen(int firstUnseen) {
        this.firstUnseen = firstUnseen;
    }
    /**
     * @return Returns the permanentFlags.
     */
    public String[] getPermanentFlags() {
        return (String[]) permanentFlags.clone();
    }
    /**
     * @param permanentFlags The permanentFlags to set.
     */
    public void setPermanentFlags(String[] permanentFlags) {
        this.permanentFlags = permanentFlags;
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
    public int getUidNext() {
        return uidNext;
    }
    /**
     * @param uidNext The uidNext to set.
     */
    public void setUidNext(int uidNext) {
        this.uidNext = uidNext;
    }
    /**
     * @return Returns the uidValidity.
     */
    public int getUidValidity() {
        return uidValidity;
    }
    /**
     * @param uidValidity The uidValidity to set.
     */
    public void setUidValidity(int uidValidity) {
        this.uidValidity = uidValidity;
    }
    /**
     * @return Returns the writeAccess.
     */
    public boolean isWriteAccess() {
        return writeAccess;
    }
    /**
     * @param writeAccess The writeAccess to set.
     */
    public void setWriteAccess(boolean writeAccess) {
        this.writeAccess = writeAccess;
    }
    
    /**
     * @return the predicted next UID.
     */
    public int predictNextUid() {
        return uidNext++;
    }
    
    /**
     * Increase the number of existing messages.
     */
    public void incExists() {
        exists++;
    }
    
    /**
     * Increase the number of unseen messages.
     */
    public void incUnseen() {
        unseen++;
    }
    
    /**
     * Increase the number of recent messages.
     */
    public void incRecent() {
        recent ++;
    }

    /**
     * Decrease the number of exising messages.
     */
    public void decExists() {
        exists--;
    }
    
    /**
     * Decrease the number of unseen messages.
     */
    public void decUnseen() {
        unseen--;
    }
    
    /**
     * Decrease the number of recent messages
     */
    public void decRecent() {
        recent--;
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

    /**
     * This equals method checks if the number of existing, unseen and recent
     * Messages of the both MailboxInfos are equal. Other attributes are
     * not compared.
     * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if( !(obj instanceof MailboxInfo) ) return false;
		
		MailboxInfo other = (MailboxInfo) obj;
		
		return exists == other.exists && recent == other.recent && unseen == other.unseen;
	}
}
