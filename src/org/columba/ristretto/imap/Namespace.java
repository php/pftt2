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
 * Representation of a Namespace as specified in RFC 2342 IMAP Namespace.
 * 
 */
public class Namespace {

	private String prefix;
	private String delimiter;
	private String extensionName;
	private String[] extensionParameter;
	
	/**
	 * @param prefix
	 * @param delimiter
	 */
	public Namespace(String prefix, String delimiter) {
		this.prefix = prefix;
		this.delimiter = delimiter;
	}
	
	/**
	 * Checks if the namespace is NIL.
	 * 
	 * @return true if the namespace is NIL.
	 */
	public boolean isNil() {
		return prefix == null;
	}
	
	/**
	 * @return Returns the delimiter.
	 */
	public String getDelimiter() {
		return delimiter;
	}
	/**
	 * @param delimiter The delimiter to set.
	 */
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}
	/**
	 * @return Returns the extensionName.
	 */
	public String getExtensionName() {
		return extensionName;
	}
	/**
	 * @param extensionName The extensionName to set.
	 */
	public void setExtensionName(String extensionName) {
		this.extensionName = extensionName;
	}
	/**
	 * @return Returns the extensionParameter.
	 */
	public String[] getExtensionParameter() {
		return extensionParameter;
	}
	/**
	 * @param extensionParameter The extensionParameter to set.
	 */
	public void setExtensionParameter(String[] extensionParameter) {
		this.extensionParameter = extensionParameter;
	}
	/**
	 * @return Returns the prefix.
	 */
	public String getPrefix() {
		return prefix;
	}
	/**
	 * @param prefix The prefix to set.
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
}
