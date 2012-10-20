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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Flags of a message. The Flags are stored
 * as an int value.
 *
 * @author Timo Stich <tstich@users.sourceforge.net>
 */
public class Flags implements Saveable, Cloneable {

    
    /**
     * Flag
     */
    public static final short ANSWERED = 0x0001;
    /**
     * Flag
     */
    public static final short DRAFT = 0x0004;
    /**
     * Flag
     */
    public static final short FLAGGED = 0x0008;
    /**
     * Flag
     */
    public static final short RECENT = 0x0010;
    /**
     * Flag
     */
    public static final short SEEN = 0x0020;
    /**
     * Flag
     */
    public static final short DELETED = 0x0040;

    private short flags;

    /**
     * Constructs the Flags.
     */
    public Flags() {
        flags = 0;
    }

    /**
     * Constructs the Flags.
     * 
     * @param flags
     */
    public Flags(short flags) {
        this.flags = flags;
    }

    /**
     * Constructs the Flags.
     * 
     * @param in
     * @throws IOException
     */
    public Flags(ObjectInputStream in) throws IOException {
        load(in);
    }

    /**
     * Get the value of the Flag.
     * 
     * @param mask the Flag to check (eg #ANSWERED)
     * @return the value of the Flag
     */
    public boolean get(short mask) {
        return (flags & mask) > 0;
    }

    /**
     * Set the flag to the value.
     * 
     * @param mask the Flag to set (eg #ANSWERED)
     * @param value 
     */
    public void set(short mask, boolean value) {
        if (value) {
            set(mask);
        } else {
            clear(mask);
        }
    }

    /**
     * Set the Flag to <code>true</code>
     * 
     * @param mask the Flag to set (eg #ANSWERED)
     */
    public void set(short mask) {
        flags |= mask;
    }

    /**
     * Set the Flag to <code>false</code>
     * 
     * @param mask mask the Flag to clear (eg #ANSWERED)
     */
    public void clear(short mask) {
        flags &= 0x0ffffffff ^ mask;
    }

    /**
     * Toggle the Flag.
     * 
     * @param mask the Flag to toggle (eg #ANSWERED)
     */
    public void toggle(short mask) {
        flags ^= mask;
    }

    /**
     * @return the SEEN value
     */
    public boolean getSeen() {
        return get(SEEN);
    }

    /**
     * @return the RECENT value
     */
    public boolean getRecent() {
        return get(RECENT);
    }

    /**
     * @return the ANSWERED value
     */
    public boolean getAnswered() {
        return get(ANSWERED);
    }

    /**
     * @return the FLAGGED value
     */
    public boolean getFlagged() {
        return get(FLAGGED);
    }

    /**
     * @return the DELETED value
     */
    public boolean getDeleted() {
        return get(DELETED);
    }

    /**
     * @return the DRAFT value
     */
    public boolean getDraft() {
        return get(DRAFT);
    }

    /**
     * Set the SEEN Flag.
     * 
     * @param b
     */
    public void setSeen(boolean b) {
        if (b) {
            set(SEEN);
        } else {
            clear(SEEN);
        }
    }

    /**
     * Set the RECENT value.
     * 
     * @param b
     */
    public void setRecent(boolean b) {
        if (b) {
            set(RECENT);
        } else {
            clear(RECENT);
        }
    }

    /**
     * Set the ANSWERED value.
     * 
     * @param b
     */
    public void setAnswered(boolean b) {
        if (b) {
            set(ANSWERED);
        } else {
            clear(ANSWERED);
        }
    }

    /**
     * Set the FLAGGED value.
     * 
     * @param b
     */
    public void setFlagged(boolean b) {
        if (b) {
            set(FLAGGED);
        } else {
            clear(FLAGGED);
        }
    }

    /**
     * Set the DELTED value.
     * 
     * @param b
     */
    public void setDeleted(boolean b) {
        if (b) {
            set(DELETED);
        } else {
            clear(DELETED);
        }
    }

    /**
     * Set the DRAFT value.
     * 
     * @param b
     */
    public void setDraft(boolean b) {
        if (b) {
            set(DRAFT);
        } else {
            clear(DRAFT);
        }
    }

    /**
     * @see org.columba.ristretto.message.Saveable#load(java.io.ObjectInputStream)
     */
    public void load(ObjectInputStream in) throws IOException {
        flags = in.readShort();
    }

    /**
     * @see org.columba.ristretto.message.Saveable#save(java.io.ObjectOutputStream)
     */
    public void save(ObjectOutputStream out) throws IOException {
        out.writeShort(flags);
    }

    /** {@inheritDoc} */
    public Object clone() {
        Flags clone;
        try {
            clone = (Flags) super.clone();
            clone.flags = flags;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the flags as a short.
     */
    public short getFlags() {
        return flags;
    }

    /**
     * @param s the flags as a short.
     */
    public void setFlags(short s) {
        flags = s;
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        boolean isEqual = false;
        if ((obj != null) && (obj instanceof Flags)) {
            Flags other = (Flags) obj;
            isEqual = (flags == other.flags);
        }
        return isEqual;
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return flags;
    }

    /** {@inheritDoc} */
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Flags[answered=");
        buffer.append(getAnswered());
        buffer.append(", draft=");
        buffer.append(getDraft());
        buffer.append(", expunged=");
        buffer.append(getDeleted());
        buffer.append(", flagged=");
        buffer.append(getFlagged());
        buffer.append(", recent=");
        buffer.append(getRecent());
        buffer.append(", seen=");
        buffer.append(getSeen());
        buffer.append("]");
        return buffer.toString();
    }
}
