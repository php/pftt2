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
 * Contributor(s): Voxmobili SA
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
 * 
 * Holds the information of the UIDPLUS response.
 * 
 * @author tstich
 *
 */
public class CopyInfo 
{
	private long uidValidity;
	private SequenceSet srcUids;
	private SequenceSet destUids;

	/**
	 * Constructs the CopyInfo.
	 * 
	 * @param uidValidity
	 * @param srcUids
	 * @param destUids
	 */
	public CopyInfo(long uidValidity, SequenceSet srcUids, SequenceSet destUids) {
		this.uidValidity = uidValidity;
		this.srcUids = srcUids;
		this.destUids = destUids;
	}
	/**
	 * @return Returns the destUids.
	 */
	public SequenceSet getDestUids() {
		return destUids;
	}
	/**
	 * @param destUids The destUids to set.
	 */
	public void setDestUids(SequenceSet destUids) {
		this.destUids = destUids;
	}
	/**
	 * @return Returns the srcUids.
	 */
	public SequenceSet getSrcUids() {
		return srcUids;
	}
	/**
	 * @param srcUids The srcUids to set.
	 */
	public void setSrcUids(SequenceSet srcUids) {
		this.srcUids = srcUids;
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
}
