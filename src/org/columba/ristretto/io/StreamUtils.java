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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.logging.Logger;

/**
 * Utility methods to copy, clone and convert
 * a stream to a String.
 * 
 * @author tstich
 *
 */
public class StreamUtils {
    /** JDK 1.4+ logging framework logger, used for logging. */
    private static final Logger LOG = Logger
            .getLogger("org.columba.ristretto.message.io");
	
	
	private static final int BUFFERSIZE = 8000;

	/**
	 * Copies all bytes from an InputStream to an OutputStream. The Bufsize should be 8000 bytes or 16000 bytes. This
	 * is platform dependend. A higher number of bytes to read on block, blocks the operation for a greater time. 
	 * @param _isInput InputStream from wihch the bytes are to copied.
	 * @param _osOutput OutputStream in which the bytes are copied.
	 * @param _iBufSize The Buffer size. How many bytes on block are should be copied.
	 * @return Number of bytes which are copied.
	 * @throws IOException 
	 */
	public static long streamCopy(InputStream _isInput, OutputStream _osOutput, int _iBufSize) throws IOException {
		byte[] _aBuffer = new byte[_iBufSize];
		int _iBytesRead;
		long _lBytesCopied = 0;
		while ((_iBytesRead = _isInput.read(_aBuffer)) > 0 ) {
			_osOutput.write(_aBuffer, 0, _iBytesRead);
			_lBytesCopied += _iBytesRead;
			_osOutput.flush();
		}
		LOG.finer("Copied " + _lBytesCopied + "bytes");
		
		return _lBytesCopied;
	}

	/**
	 * Copies all bytes from an InputStream to an OutputStream. The Bufsize is set to 8000 bytes.  
	 * @param _isInput InputStream from wihch the bytes are to copied.
	 * @param _osOutput OutputStream in which the bytes are copied.
	 * @return Number of bytes which are copied.
	 * @throws IOException 
	 */
	public static long streamCopy(InputStream _isInput, OutputStream _osOutput ) throws IOException {
		return streamCopy( _isInput, _osOutput, BUFFERSIZE );
	}

	
	/**
	 * Reads a InputStream into a StringBuffer.
	 * This method is 8bit safe.
	 * 
	 * @param in the InputStream to read from
	 * @return the interpreted InputStream
	 * @throws IOException
	 */
	public static StringBuffer readInString( InputStream in ) throws IOException {
    	StringBuffer result = new StringBuffer(in.available());
        int read = in.read();

        while (read > 0) {
            result.append((char) read);
            read = in.read();
        }

        in.close();
        
        return result;
	}
	
	/** Copies all bytes from the given InputStream in a intern ByteArrayOutputStream and returnes a new InputStream
	 * with all bytes from the ByteArrayOutputStream. The data are real copied so this methods "clones" the given
	 * Inputstream and gives back a new InputStream with same Data.
	 * @param from InputStream from which all datas are to copy
	 * @return a new InputStream with all data from the given InputStream
	 * @throws IOException
	 */
	public static InputStream streamClone(InputStream from) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		 streamCopy(from, out);
		 return new ByteArrayInputStream(out.toByteArray()); 
	}

}
