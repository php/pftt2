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
import java.util.logging.Logger;

import org.columba.ristretto.io.Source;


/**
 * Container for a header and a mimetree, representing a message.
 *
 *
 * @author Timo Stich <tstich@users.sourceforge.net>
 */
public class Message {

    /** JDK 1.4+ logging framework logger, used for logging. */
    private static final Logger LOG = Logger.getLogger("org.columba.ristretto.message");


	protected Header header;
	protected MimeTree mimePartCollection;
	protected Source source;
	
	
	/**
	 * Constructs an empty Message.
	 */
	public Message() {
	}

	/**
	 * Get the ith MimePart.
	 * 
	 * @param i 
	 * @return the ith MimePart
	 */
	public MimePart getMimePart(int i) {
		return mimePartCollection.get(i);

	}

	/**
	 * 
	 * 
	 * @return the number of available MimeParts
	 */
	public int getMimePartCount() {
		if (mimePartCollection != null)
			return mimePartCollection.count();
		else
			return 0;
	}

	/**
	 * @return the MimeTree of the message.
	 */
	public MimeTree getMimePartTree() {
		return mimePartCollection;
	}

	/**
	 * Set the MimeTree of the message.
	 * 
	 * @param ac
	 */
	public void setMimePartTree(MimeTree ac) {
		mimePartCollection = ac;
	}

	/**
	 * 
	 * 
	 * @return the Header of the message
	 */
	public Header getHeader() {
		return header;
	}

	/**
	 * Sets the Header of the message.
	 * 
	 * @param h
	 */
	public void setHeader(Header h) {
		this.header =  h;
	}

	/**
	 * @return the Source of the message.
	 */
	public Source getSource() {
		return source;
	}

	/**
	 * Set the Source of the message.
	 * 
	 * @param source
	 */
	public void setSource(Source source) {
		this.source = source;
	}

	/**
	 * Make a Source#deepClose() on the associated
	 * Source. This ensures that all resources allocated
	 * with this message are released.
	 */
	public void close() {
		try {
			source.deepClose();
		} catch (IOException e) {
		    LOG.warning( e.getLocalizedMessage() );
		}
	}
	
	/**
	 * @see java.lang.Object#finalize()
	 */
	protected void finalize() throws Throwable {
		super.finalize();
		source.deepClose();
	}
}