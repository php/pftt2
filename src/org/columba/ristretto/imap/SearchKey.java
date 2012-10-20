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

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * The search keys that specify a search.
 * 
 * @author tstich
 */
public class SearchKey {
	/**
	 * Searchkey type.
	 */
    public static final String ALL = "ALL";
	/**
	 * Searchkey type.
	 */
    public static final String ANSWERED = "ANSWERED";
	/**
	 * Searchkey type.
	 */
    public static final String BCC = "BCC";
	/**
	 * Searchkey type.
	 */
    public static final String BEFORE = "BEFORE";
	/**
	 * Searchkey type.
	 */
    public static final String BODY = "BODY";
	/**
	 * Searchkey type.
	 */
    public static final String CC = "CC";
	/**
	 * Searchkey type.
	 */
    public static final String DELETED = "DELETED";
	/**
	 * Searchkey type.
	 */
    public static final String FLAGGED = "FLAGGED";
	/**
	 * Searchkey type.
	 */
    public static final String FROM = "FROM";
	/**
	 * Searchkey type.
	 */
    public static final String KEYWORD = "KEYWORD";
	/**
	 * Searchkey type.
	 */
    public static final String NEW = "NEW";
	/**
	 * Searchkey type.
	 */
    public static final String OLD = "OLD";
	/**
	 * Searchkey type.
	 */
    public static final String ON = "ON";
	/**
	 * Searchkey type.
	 */
    public static final String RECENT = "RECENT";
	/**
	 * Searchkey type.
	 */
    public static final String SEEN = "SEEN";
	/**
	 * Searchkey type.
	 */
    public static final String SINCE = "SINCE";
	/**
	 * Searchkey type.
	 */
    public static final String SUBJECT = "SUBJECT";
	/**
	 * Searchkey type.
	 */
    public static final String TEXT = "TEXT";
	/**
	 * Searchkey type.
	 */
    public static final String TO = "TO";
	/**
	 * Searchkey type.
	 */
    public static final String UNANSWERED = "UNANSWERED";
	/**
	 * Searchkey type.
	 */
    public static final String UNDELETED = "UNDELETED";
	/**
	 * Searchkey type.
	 */
    public static final String UNFLAGGED = "UNFLAGGED";
	/**
	 * Searchkey type.
	 */
    public static final String UNKEYWORD = "UNKEYWORD";
	/**
	 * Searchkey type.
	 */
    public static final String UNSEEN = "UNSEEN";
	/**
	 * Searchkey type.
	 */
    public static final String DRAFT = "DRAFT";
	/**
	 * Searchkey type.
	 */
    public static final String HEADER = "HEADER";
	/**
	 * Searchkey type.
	 */
    public static final String LARGER = "LARGER";
	/**
	 * Searchkey type.
	 */
    public static final String NOT = "NOT";
	/**
	 * Searchkey type.
	 */
    public static final String OR = "OR";
	/**
	 * Searchkey type.
	 */
    public static final String SENTBEFORE = "SENTBEFORE";
	/**
	 * Searchkey type.
	 */
    public static final String SENTON = "SENTON";
	/**
	 * Searchkey type.
	 */
    public static final String SENTSINCE = "SENTSINCE";
	/**
	 * Searchkey type.
	 */
    public static final String SMALLER = "SMALLER";
	/**
	 * Searchkey type.
	 */
    public static final String UID = "UID";
	/**
	 * Searchkey type.
	 */
    public static final String UNDRAFT = "UNDRAFT";

    
    
    private List list;
    private String key;
    private Object arg;
    private Object arg2;
    
    /**
     * Constructs the SearchKey.
     * 
     * @param key
     */
    public SearchKey(String key) {
        this.key = key;
    }

    /**
     * Constructs the SearchKey.
     * 
     * @param key
     * @param arg
     */
    public SearchKey(String key, String arg) {
        this.key = key;
        this.arg = arg;
    }
    
    /**
     * Constructs the SearchKey.
     * 
     * @param key
     * @param arg
     */
    public SearchKey(String key, Integer arg) {
        this.key = key;
        this.arg = arg;
    }

    /**
     * Constructs the SearchKey.
     * 
     * @param key
     * @param arg
     */
    public SearchKey(String key, IMAPDate arg) {
        this.key = key;
        this.arg = arg;
    }
    
    /**
     * Constructs the SearchKey.
     * 
     * @param key
     * @param arg
     * @param arg2
     */
    public SearchKey(String key, String arg, String arg2) {
        this.key = key;
        this.arg = arg;
        this.arg2 = arg2;
    }

    /**
     * Constructs the SearchKey.
     * 
     * @param key
     * @param arg
     */
    public SearchKey(String key, SearchKey arg) {
        this.key = key;
        this.arg = arg;
    }

    /**
     * Constructs the SearchKey.
     * 
     * @param key
     * @param arg
     * @param arg2
     */
    public SearchKey(String key, SearchKey arg, SearchKey arg2) {
        this.key = key;
        this.arg = arg;
        this.arg2 = arg2;
    }

    /**
     * Add a Searchkey.
     * 
     * @param key
     */
    public void add( SearchKey key) {
        if( list == null ) {
            list = new LinkedList();
        }
        
        list.add(key);
    }

    /**
     * Converts the SearchKey to a StringArray.
     * This is used when sending the search to the
     * server.
     * 
     * @return the String[] representation of the SearchKey
     */
    public String[] toStringArray() {
        List result = new LinkedList();
        if( list != null ) {
           result.add("(");
        }	
        
        result.add(key);
        if( arg != null ) {
            if( arg instanceof SearchKey ) {
            	result.addAll(Arrays.asList(((SearchKey)arg).toStringArray()));
            } else {
            	result.add(arg.toString());
            }
        }        
        if( arg2 != null ) {
            if( arg2 instanceof SearchKey ) {
            	result.addAll(Arrays.asList(((SearchKey)arg2).toStringArray()));
            } else {
            	result.add(arg2.toString());
            }
        }
        if( list != null ) {
            Iterator it = list.iterator();
            Object next;
            while( it.hasNext() ) {
            	next = it.next();
            	if( next instanceof SearchKey ) {
                	result.addAll(Arrays.asList(((SearchKey)next).toStringArray()));
                } else {
                	result.add(next.toString());
                }
            }
            result.add(")");
        }
        return (String[]) result.toArray(new String[]{});
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer result = new StringBuffer();
        
        if( list != null ) {
            result.append('(');
        }
        result.append(key);
        if( arg != null ) {
            result.append(' ');
            result.append(arg.toString());
        }        
        if( arg2 != null ) {
            result.append(' ');
            result.append(arg2.toString());
        }
        if( list != null ) {
            Iterator it = list.iterator();
            while( it.hasNext() ) {
                result.append(' ');
                result.append(it.next().toString());
            }
            result.append(')');
        }
        return result.toString();
    }
    
}
