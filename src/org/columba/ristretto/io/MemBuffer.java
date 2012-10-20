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

import org.columba.ristretto.concurrency.Mutex;

/**
 * A flexible memory buffer.
 * 
 * @author tstich
 */
public class MemBuffer {
	private final static int INCREMENT = 1024;
	
    private byte[] buffer;
	
    private int len;
	
    private Mutex mutex;
    
    /**
     * Constructs the memory buffer.
     */
    public MemBuffer() {
    	this(INCREMENT);
    }
    
	/**
	 * Constructs the memory buffer with
	 * the given initial size. 
	 * @param size the initial size
	 */
	public MemBuffer(int size) {
		buffer = new byte[size];
		
		len = 0;
		
		mutex = new Mutex();
	}
	
	/**
	 * Append the value to the buffer.
	 * 
	 * @param value
	 */
	public void append(int value) {
		mutex.lock();
		if( len == buffer.length) growBuffer(buffer.length + INCREMENT);
		
		buffer[len++] = (byte) value;
		mutex.release();
	}
	
	/**
	 * Append the byte array to the end of the buffer.
	 * 
	 * @param array
	 * @param offset
	 * @param length
	 */
	public void append(byte[] array, int offset, int length) {
		mutex.lock();
		int available = buffer.length - len;
		if( length > available) growBuffer(buffer.length  + ( length - available > INCREMENT ? length : INCREMENT));
		System.arraycopy(array, offset, buffer, len, length);
		len += length;
		mutex.release();
	}
	
	/**
	 * Append the byte array to the end of the buffer.
	 * 
	 * @param array
	 */
	public void append(byte[] array) {
		append(array, 0, array.length);
	}
	
	/**
	 * Get the value at the given position.
	 * 
	 * @param pos
	 * @return the value at the position
	 */
	public int get(int pos) {
		if( pos > len ) throw new ArrayIndexOutOfBoundsException(pos);
		
		return buffer[pos];
	}
	
	/**
	 * Fill the array with data from the buffer starting
	 * at position pos.
	 * 
	 * @param pos
	 * @param array
	 * @return the number of bytes copied to the array.
	 */
	public int get(int pos, byte[] array) {		
		return get(pos, array, 0, array.length);
	}
	
	/**
	 * Fill the array with data from the buffer starting
	 * at position pos.
	 * 
	 * @param pos
	 * @param array
	 * @param offset
	 * @param length
	 * @return the number of bytes copied to the array.
	 */
	public int get(int pos, byte[] array, int offset, int length) {
		mutex.lock();
		int result = length > (len-pos) ? (len-pos) : length;
		if( result < 0 ) {
			mutex.release();
			throw new ArrayIndexOutOfBoundsException(pos);
		}
		
		System.arraycopy(buffer, pos, array, offset, result);
		mutex.release();
		
		return result;
	}

	/**
	 * 
	 * 
	 * @return returns the size of the buffer.
	 */
	public int size() {
		mutex.lock();
		int result = len;
		mutex.release();
		return result;
	}
	
	   /**
	    * Grows the buffer to fit the given size.
	    * 
	 * @param newSize
	 */
	private void growBuffer(int newSize) {
		byte[] newBuffer = new byte[newSize];
		System.arraycopy(buffer,0,newBuffer,0,buffer.length);
		buffer = newBuffer;		
	}
	

}
