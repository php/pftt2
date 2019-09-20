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
package org.columba.ristretto.coder;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

/**
 * FilterInputStream that decodes a bytestream into a characterstream.
 * 
 * @author Timo Stich <tstich@users.sourceforge.net>
 */
public class CharsetDecoderInputStream extends FilterInputStream {

	private Charset charset;
	private CharsetDecoder decoder;

	private ByteBuffer inBytes;
	private CharBuffer outBytes;
	
	/**
	 * Constructs a CharsetDecoderInputStream.
	 * 
	 * @param arg0 The raw bytestream
	 * @param charset The charset of the characterstream
	 */
	public CharsetDecoderInputStream(InputStream arg0, Charset charset) {
		super(arg0);
		this.charset = charset;
		decoder = charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
		
		inBytes = ByteBuffer.allocate(5);
		outBytes = CharBuffer.allocate(1);
	}
	
	/**
	 * Read one byte (two if 16-bit charset) from the stream and
	 * decode it to the charset. 
	 * 
	 * @return the decoded char
	 * @throws IOException
	 */
	private int decodeNextChar() throws IOException {
		CoderResult result;
		int read;

		
		inBytes.clear();
		inBytes.limit(1);

		read = in.read();		
		if( read == -1) return -1;
		inBytes.put( 0, (byte) read );
		
		outBytes.clear();
		result = decoder.decode(inBytes, outBytes, in.available() == 0);
		
				
		// Do we need to read a second byte?
		while( outBytes.position() == 0 ) {
			read = in.read();		
			if( read == -1) return -1;
			inBytes.limit(inBytes.limit()+1);
			inBytes.put(inBytes.limit()-1,(byte) read);

			outBytes.clear();
			result = decoder.decode(inBytes, outBytes, in.available() == 0);
		}
		
		return outBytes.position();
	}
	
	
	/**
	 * @see java.io.InputStream#read()
	 */
	public int read() throws IOException {		
		if( decodeNextChar() == -1) return -1;
		
		return outBytes.get(0);
	}
	
	/**
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	public int read(byte[] arg0, int arg1, int arg2) throws IOException {
		int next;
		for( int i=0; i<arg2; i++) {
			next = read();
			if( next == -1 ) {
				if( i == 0 ) {
					return -1;
				} else {
					return i;
				}
			}
			arg0[arg1+i] = (byte) next;
		}
		return arg2;
	}
	

}
