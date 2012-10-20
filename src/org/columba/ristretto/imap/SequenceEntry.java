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
 * A part of the sequence set.
 * 
 * @author tstich
 */
public class SequenceEntry implements Comparable {
    /**
     * Entry type.
     */
	public static final int SINGLE = 0;
    /**
     * Entry type.
     */
    public static final int RANGE = 1;
    /**
     * Entry type.
     */
    public static final int OPEN_RANGE = 2;
    /**
     * Entry type.
     */
    public static final int ALL = 3;
    
    /**
     * Entry type.
     */
    public static final int STAR = -1;
    
    
    //NOTE: for the types SINGLE and OPEN_RANGE
    //"a" and "b" must have the same value !
    private int a;
    private int b;
    private int type;
    
    /**
     * Constructs the SequenceEntry.
     * @param a
     */
    public SequenceEntry(int a) {
        this.a = a;
        this.b = a;
        type = SINGLE;
    }
    
    /**
     * Constructs the SequenceEntry.
     * 
     * @param a
     * @param b
     */
    public SequenceEntry(int a, int b) {
    	if( a == b ) {
            this.a = a;
            this.b = a;
            type = SINGLE;
            
            return;
    	}
    	
    	if( a == STAR || b == STAR) {
    		type = OPEN_RANGE;
    		this.a = Math.max(a,b);
    		this.b = this.a;
    		
    		if( this.a == 1) {
    			type = ALL;
    		}
    	} else {
    		this.a = Math.min(a,b);
    		this.b = Math.max(a,b);
    		
    		type = RANGE;
    	}
    }
    
    /**
     * Check if this can be merged in one.
     * 
     * @param s
     * @return <code>true</code> if the SequenceEntries can be merged.
     */
    public boolean canMergeWith(SequenceEntry s ) {
        if( s.type == ALL || this.type == ALL ) return true;
        
        // Handle open ranges
        if( this.type == OPEN_RANGE && (s.b >= this.b || s.b == STAR)) return true;
        if( s.type == OPEN_RANGE && (this.b >= s.b || this.b == STAR)) return true;        
        
        // otherwise if there is a MAX/STAR in one of the ranges
        // merging is not possible
        if( s.b == STAR || this.b == STAR ) return false;
        
        // included/overlaps/connects with in Range
        if( this.type == RANGE && ( this.a <= s.a && this.b >= s.a-1 )) return true;
        if( this.type == RANGE && ( this.a <= s.b-1 && this.b >= s.b )) return true;
        if( s.type == RANGE && ( s.a <= this.a && s.b >= this.a-1 )) return true;
        if( s.type == RANGE && ( s.a <= this.b-1 && s.b >= this.b )) return true;
        
        return (this.a - s.a) * (this.a - s.a) <= 1 || (this.b - s.b) * (this.b - s.b) <= 1;        
    }
    
    /**
     * Merge the given Sequencentry with this entry. There is
     * no further check if this is possible so use #canMergeWith(SequenceSet)
     * to test if it is possible!
     * 
     * @param s
     */
    public void merge(SequenceEntry s) {
        if( s.type == ALL || this.type == ALL ) {
            this.type = ALL;
            return;
        }
        
        if( this.type == OPEN_RANGE && s.b >= this.a - 1) {
            this.type = OPEN_RANGE;
            this.a = this.b = Math.min(s.a,this.a);
            if( this.a == 1) type = ALL;
        }
        
        if( s.type == OPEN_RANGE && this.b >= s.a - 1) {
            this.type = OPEN_RANGE;
            this.a = this.b = Math.min(s.a,this.a);
            if( this.a == 1) type = ALL;
            return;
        }
        
        if( this.type == RANGE && ( this.a <= s.a || this.b >= s.b )) {
            this.a = s.a<this.a?s.a:this.a;
            this.b = s.b>this.b?s.b:this.b;
            return;
        }

        if( s.type == RANGE && ( s.a <= this.a || s.b >= this.b )) {
            this.type = RANGE;
            this.a = s.a<this.a?s.a:this.a;
            this.b = s.b>this.b?s.b:this.b;
            return;
        }
        
        this.type = RANGE;
        this.a = s.a<this.a?s.a:this.a;
        this.b = s.b>this.b?s.b:this.b;
    }
    
    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object o) {
        SequenceEntry s = (SequenceEntry) o;
        
        // ALL is the smallest value
        // this is better for pack
        if( type == ALL ) return -1;
        if( s.type == ALL ) return 1;
        
        // Sort open ranges to the borders
        //if( type == LEFT_OPEN_RANGE || s.type == RIGHT_OPEN_RANGE) return -1;
        //if( type == RIGHT_OPEN_RANGE || s.type == LEFT_OPEN_RANGE) return 1;
        
        // now both types can only be RANGE or SINGLE
        // check if one is a STAR
        if( s.a == STAR ) return -1;
        if( a == STAR ) return 1;
        
        // -> we only have to deal with "a"
        
        return a<s.a?-1:1;
    }
    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        switch( type ) {
            case ALL : return "1:*";
            
            case SINGLE : {
            	if( a == STAR ) {
            		return "*";
            	} else {
            		return Integer.toString(a);
            	}
            }
            
            case RANGE : return Integer.toString(a) + ':' + Integer.toString(b);
            
            case OPEN_RANGE : return Integer.toString(a) + ":*";        
        }
        
        return null;
    }
	/**
	 * @return Returns the a.
	 */
	public int getA() {
		return a;
	}
	/**
	 * @return Returns the b.
	 */
	public int getB() {
		return b;
	}
	/**
	 * @return Returns the type.
	 */
	public int getType() {
		return type;
	}
}
