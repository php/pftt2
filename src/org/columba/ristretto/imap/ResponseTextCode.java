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
 * The text code of the IMAP response.
 * 
 * @author tstich
 */
public class ResponseTextCode {
	/**
	 * Textcode type.
	 */
    public static final int UNSPECIFIED = -1;
    /**
     * 	Textcode type.
     */
    public static final int ALERT = 0;
	/**
	 * Textcode type.
	 */
    public static final int BADCHARSET = 1;
	/**
	 * Textcode type.
	 */
    public static final int CAPABILITY = 2;
	/**
	 * Textcode type.
	 */
    public static final int PARSE = 3;
	/**
	 * Textcode type.
	 */
    public static final int PERMANENTFLAGS = 4;
	/**
	 * Textcode type.
	 */
    public static final int READ_ONLY = 5;
	/**
	 * Textcode type.
	 */
    public static final int READ_WRITE = 6;
	/**
	 * Textcode type.
	 */
    public static final int TRYCREATE = 7;
	/**
	 * Textcode type.
	 */
    public static final int UIDVALIDITY = 8;
	/**
	 * Textcode type.
	 */
    public static final int UIDNEXT = 9;
	/**
	 * Textcode type.
	 */
    public static final int UNSEEN = 10;
    
    private int type;
    private int longValue;
    private String stringValue;
    private String[] stringArrayValue;
    
    
    
    /**
     * @return Returns the intValue.
     */
    public int getIntValue() {
        return longValue;
    }
    /**
     * @param intValue The intValue to set.
     */
    public void setIntValue(int intValue) {
        this.longValue = intValue;
    }
    /**
     * @return Returns the stringArrayValue.
     */
    public String[] getStringArrayValue() {
        return (String[]) stringArrayValue.clone();
    }
    /**
     * @param stringArrayValue The stringArrayValue to set.
     */
    public void setStringArrayValue(String[] stringArrayValue) {
        this.stringArrayValue = stringArrayValue;
    }
    /**
     * @return Returns the stringValue.
     */
    public String getStringValue() {
        return stringValue;
    }
    /**
     * @param stringValue The stringValue to set.
     */
    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }
    /**
     * @return Returns the type.
     */
    public int getType() {
        return type;
    }
    /**
     * @param type The type to set.
     */
    public void setType(int type) {
        this.type = type;
    }
}
