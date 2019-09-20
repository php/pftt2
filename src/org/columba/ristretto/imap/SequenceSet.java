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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A set of message indices or UIDs.
 * 
 * @author tstich
 */
public class SequenceSet {
    
    private final static SequenceSet all;
    
	static {
		all = new SequenceSet();
		all.addAll();
	}

    private List set;
    
    /**
     * Constructs the SequenceSet.
     * 
     */
    public SequenceSet() {
        set = new LinkedList();
    }
    
    /**
     * Constructs the SequenceSet.
     * 
     * @param a
     */
    public SequenceSet(int a) {
        this();
        add(a);
    }

    /**
     * Constructs the SequenceSet.
     * 
     * @param a
     * @param b
     */
    public SequenceSet(int a, int b) {
        this();
        add(a,b);
    }

    /**
     * Constructs the SequenceSet.
     * 
     * @param s
     */
    public SequenceSet(Integer[] s) {
        this();
        for( int i=0; i<s.length; i++) {
            add( s[i].intValue());
        }
    }
    

    /**
     * Constructs the SequenceSet.
     * 
     * @param s
     * @param offset
     * @param length
     */
    public SequenceSet(int[] s, int offset, int length) {
        this();
        for( int i=offset; i<offset + length; i++) {
            add( s[i]);
        }
    }
    
    /**
     * Constructs the SequenceSet.
     * 
     * @param l
     */
    public SequenceSet(List l) {
    	this();
    	Iterator it = l.iterator();
    	while( it.hasNext()) {
    		add(((Integer)it.next()).intValue());
    	}
    }

    /**
     * The static set that stands for all messages in the mailbox.
     * 
     * @return the sequenceset that represents all messages
     */
    public static SequenceSet getAll() {        
        return all;
    }
    
    /**
     * Add the index/UID to the set.
     * 
     * @param a index/uid
     */
    public void add(int a) {
        set.add(new SequenceEntry(a));
    }
    
    /**
     * Add the range of a:b to the set.
     * 
     * @param a start index/uid
     * @param b end index/uid
     */
    public void add(int a, int b) {
        set.add(new SequenceEntry(a, b));
    }

    /**
     * Add a open range (a:*) to the set.
     * 
     * @param a start index/UID
     */
    public void addOpenRange(int a) {
    	set.add(new SequenceEntry(a, SequenceEntry.STAR));
    }
    
    /**
     * Add all messages to the set.
     * 
     */
    public void addAll() {
        set.add(new SequenceEntry(1,SequenceEntry.STAR));
    }
    
    /**
     * Compress the set to the shortest possible
     * representation. 
     * 
     */
    public void pack() {
    	if( set.size() <= 1 ) return;
        Collections.sort( set );
        Iterator it = set.iterator();
        
        SequenceEntry a = (SequenceEntry) it.next();
        SequenceEntry b;
        
        while( it.hasNext() ) {
            b = (SequenceEntry) it.next();
            if( a.canMergeWith(b)) {
                a.merge(b);
                it.remove();
            } else {
                a = b;
            }
        }
    }
    

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        pack();
        StringBuffer result = new StringBuffer();
        Iterator it = set.iterator();
        result.append( it.next().toString());
        while( it.hasNext() ) {
            result.append(',');
            result.append(it.next().toString());
        }
        return result.toString();
    }

	/**
	 * Computes the length of this set. This method will work only
	 * on SequenceSets that represent indices!
	 * 
	 * @param exists the number of exisiting messages in the mailbox
	 * @return the length of the SequenceSet
	 */
	public int getLength(int exists) {
		int length = 0;
		
        Iterator it = set.iterator();
        while( it.hasNext() ) {
        	SequenceEntry entry = (SequenceEntry) it.next();
        	
        	switch( entry.getType() ) {
        		case SequenceEntry.ALL : {
        			return exists;
        		}
        		
        		case SequenceEntry.OPEN_RANGE : {
        			length += exists - entry.getA() + 1;
        			break;
        		}
        		
        		case SequenceEntry.RANGE : {
        			length += entry.getB() - entry.getA() + 1;
        			break;
        		}
        		
        		case SequenceEntry.SINGLE : {
        			length ++;
        			break;
        		}
        	}
        }		
		
		return length;
	}
    
	/**
	 * Converts the SequenceSet to an array of integers. This method will work only
	 * on SequenceSets that represent indices!
	 * 
	 * @param exists exists the number of exisiting messages in the mailbox
	 * @return the array of indices in the SequenceSet.
	 */
	public int[] toArray(int exists) {
        pack();
		int[] result = new int[getLength(exists)];
		int pos=0;
		int index=1;
		
        Iterator it = set.iterator();
        while( it.hasNext() ) {
        	SequenceEntry entry = (SequenceEntry) it.next();
        	
        	switch( entry.getType() ) {
        		case SequenceEntry.ALL : {
        			for( int i=0; i<exists; i++) {
        				result[i]=i+1;
        			}
        		}
        		
        		case SequenceEntry.OPEN_RANGE : {
        			for( index=entry.getA(); index <= exists ; index++ )  {
        				result[pos++] = index; 
        			}
        			break;
        		}
        		
        		case SequenceEntry.RANGE : {
        			for( index=entry.getA(); index <= entry.getB() ; index++ )  {
        				result[pos++] = index; 
        			}
        			break;
        		}
        		
        		case SequenceEntry.SINGLE : {
        			result[pos++] = entry.getA() == SequenceEntry.STAR ? exists: entry.getA();
        			break;
        		}
        	}
        }		
		
		
		return result;
	}
}
