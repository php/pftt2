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
package org.columba.ristretto.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream that wraps the MemBuffer.
 * 
 * @author tstich
 */
public class MemBufferInputStream extends InputStream {

	private MemBuffer buffer;
	
	private int pos;
	
	/**
	 * Creates the MemBufferInputStream.
	 * 
	 * @param buffer
	 */
	public MemBufferInputStream(MemBuffer buffer) {
		this.buffer = buffer;
	}

	/**
	 * @see java.io.InputStream#read()
	 */
	public int read() throws IOException {
		if( buffer.size() == pos) return -1;
		else return buffer.get(pos++);
	}

	/**
	 * @see java.io.InputStream#available()
	 */
	public int available() throws IOException {
		return buffer.size() - pos;
	}
	/**
	 * @see java.io.InputStream#close()
	 */
	public void close() throws IOException {
		buffer = null;
	}
	/**
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	public int read(byte[] b, int off, int len) throws IOException {
		int result = buffer.get(pos, b, off, len);
		pos += result;
		if( result == 0 ) return -1;
		else return result;
	}
	/**
	 * @see java.io.InputStream#skip(long)
	 */
	public long skip(long n) throws IOException {
		return pos += n;
	}
	/**
	 * @return Returns the buffer.
	 */
	public MemBuffer getBuffer() {
		return buffer;
	}
}
