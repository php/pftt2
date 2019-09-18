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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

/**
 * Attributes are used to store additional attributes of a message.
 * The information is stored in a Hashtable using key-value pairs.
 * <p>
 * Keys are Strings, values can be any Object.
 *
 * @author Timo Stich <tstich@users.sourceforge.net>
 */
public class Attributes implements Saveable, Cloneable {

    private Hashtable attributes;

    /**
     * Creates an empty Attributes object. 
     *
     */
    public Attributes() {
        attributes = new Hashtable();
    }

    /**
     * Creates an Attributes object and loads attributes from the given InputStream.
     * 
     * @param in the InputSream to load from.
     * @throws IOException thrown if there was a problem reading from the InputStream.
     */
    public Attributes(ObjectInputStream in) throws IOException {
        load(in);
    }

    /**
     * Sets the attribute key-value pair.
     * @param key the key
     * @param value the value
     */
    public void put(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * Returns the attribute value for the specified key.
     * s
     * @param key the attribute key.
     * @return the value for the key.
     */
    public Object get(String key) {
        return attributes.get(key);
    }


    /** {@inheritDoc} */
    public final void load(ObjectInputStream in) throws IOException {
        int size = in.readInt();
        attributes = new Hashtable(size);
        for (int i = 0; i < size; i++) {
            try {
                attributes.put(in.readUTF(), in.readObject());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /** {@inheritDoc} */
    public final void save(ObjectOutputStream out) throws IOException {
        out.writeInt(attributes.size());
        Iterator keys = attributes.keySet().iterator();

        while (keys.hasNext()) {
            String key = (String) keys.next();
            Object value = attributes.get(key);
            out.writeUTF(key);
            out.writeObject(value);
        }
    }

    /**
     * Returns the number of attribute pairs.
     * @return the number of attribute pairs.
     */
    public int count() {
        return attributes.size();
    }

    /** {@inheritDoc} */
    public Object clone() {
        Attributes clone;
        try {
            clone = (Attributes) super.clone();
            clone.attributes = (Hashtable) attributes.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        boolean isEqual = false;
        if ((obj != null) && (obj instanceof Attributes)) {
            Attributes other = (Attributes) obj;
            isEqual = attributes.equals(other.attributes);
        }
        return isEqual;
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return attributes.hashCode();
    }

    /** {@inheritDoc} */
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Attributes[");
        Set set = attributes.keySet();
        for (Iterator iterator = set.iterator(); iterator.hasNext();) {
            String key = (String) iterator.next();
            buffer.append(key);
            buffer.append("=");
            buffer.append(attributes.get(key));
            if (iterator.hasNext()) {
                buffer.append(", ");
            }
        }
        buffer.append("]");
        return buffer.toString();
    }
}
