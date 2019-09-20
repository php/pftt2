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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * The collection of namespaces defined on
 * an IMAP server.
 * 
 * @author tstich
 */
public class NamespaceCollection {

	/**
	 * Namespace type.
	 */
	public static final int PERSONAL = 0;
	/**
	 * Namespace type.
	 */
	public static final int OTHER_USER = 1;
	/**
	 * Namespace type.
	 */
	public static final int SHARED = 2;
	
	
	private List personal;
	private List otherUser;
	private List shared;
	
	/**
	 *	Constructs the NamespaceCollection. 
	 */
	public NamespaceCollection() {
		personal = new ArrayList();
		otherUser = new ArrayList();
		shared = new ArrayList();
	}
	
	/**
	 * Add a personal namespace.
	 * 
	 * @param ns 
	 */
	public void addPersonalNamespace(Namespace ns) { 
		personal.add(ns);
	}
	
	/**
	 * Add a Other-User namespace.
	 * 
	 * @param ns 
	 */
	public void addOtherUserNamespace(Namespace ns) {
		otherUser.add(ns);
	}
	
	/**
	 * Add a shared namespace.
	 * 
	 * @param ns 
	 */
	public void addSharedNamespace(Namespace ns) {
		shared.add(ns);
	}
	
	/**
	 * Gets the first personal namespaces.
	 * 
	 * @return the personal namespaces
	 */
	public Namespace getPersonalNamespace() {
		return (Namespace) personal.get(0);
	}
	
	/**
	 * Gets the ith personal namespace.
	 * 
	 * @param i 
	 * @return the ith personal namespace
	 */
	public Namespace getPersonalNamespace(int i) {
		return (Namespace) personal.get(i);
	}

	/**
	 * Gets the first other-user namespace
	 * 
	 * @return the first other-user namespace
	 */
	public Namespace getOtherUserNamespace() {
		return (Namespace) otherUser.get(0);
	}

	/**
	 * Gets the ith other-user namespace
	 * 
	 * @param i 
	 * @return the ith other-user namespace
	 */
	public Namespace getOtherUserNamespace(int i) {
		return (Namespace) otherUser.get(i);
	}

	/**
	 * Gets the first shared namespace
	 * 
	 * @return the first shared namespace
	 */
	public Namespace getSharedNamespace() {
		return (Namespace) shared.get(0);
	}
	
	/**
	 * Gets the ith shared namespace.
	 * 
	 * @param i
	 * @return the ith shared namespace
	 */
	public Namespace getSharedNamespace(int i) {
		return (Namespace) shared.get(i);
	}

	
	/**
	 * Get the Iterator over the personal namespaces.
	 * 
	 * @return the iterator over the personal namespaces
	 * 
	 */
	public Iterator getPersonalIterator() {
		return personal.iterator();
	}

	/**
	 * Gets the iterator over the other-user namespaces.
	 * 
	 * @return the iterator over the other-user namespaces.
	 */
	public Iterator getOtherUserIterator() {
		return otherUser.iterator();
	}

	/**
	 * Gets the iterator over the shared namespaces.
	 * 
	 * @return the iterator over the shared namespaces
	 */
	public Iterator getSharedIterator() {
		return shared.iterator();
	}

	/**
	 * Add a namespace of the specfied type.
	 * 
	 * @param type the type of the namespace
	 * @param ns
	 */
	public void addNamespace(int type, Namespace ns) {
		switch( type ) {
			case PERSONAL : {
				addPersonalNamespace(ns);
				break;
			}
			
			case OTHER_USER : {
				addOtherUserNamespace(ns);
				break;
			}
			
			case SHARED : {
				addSharedNamespace(ns);
				break;
			}
		}
	}
	
	/**
	 * @param namespaces
	 */
	public void addPersonalNamespace(Namespace[] namespaces) {
		personal.addAll(Arrays.asList(namespaces));
	}

	/**
	 * @param namespaces
	 */
	public void addOtherUserNamespace(Namespace[] namespaces) {
		otherUser.addAll(Arrays.asList(namespaces));
	}
	
	/**
	 * @param namespaces
	 */
	public void addSharedNamespace(Namespace[] namespaces) {
		shared.addAll(Arrays.asList(namespaces));
	}
	
	/**
	 * Gets the number of personal namespaces.
	 * 
	 * @return the number of personal namespaces
	 */
	public int getPersonalNamespaceSize() {
		return personal.size();
	}

	/**
	 * Gets the number of other-user namespaces.
	 * 
	 * @return the number of other-user namespaces
	 */
	public int getOtherUserNamespaceSize() {
		return otherUser.size();
	}

	/**
	 * Gets the number of shared namespaces.
	 * 
	 * @return the number of shared namespaces
	 */
	public int getSharedNamespaceSize() {
		return shared.size();
	}

}
