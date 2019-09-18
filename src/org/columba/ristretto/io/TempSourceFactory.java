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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Factory for creating temporary sources.
 * A decision for a file or memory data backend
 * is made given the size of the temporary source.
 * 
 * @author tstich
 *
 */
public class TempSourceFactory {

	/**
	 * Upper size bound for creating a memory source. 
	 */
	public static int MAX_IN_RAM_SIZE = 51200; // default is 50kB
	protected static String TEMP_PREFIX = "ristretto_";
	protected static String TEMP_POSTFIX = "tmp";
	protected static int BUFFER_SIZE = 51200;

	private static int counter = 0;

	/**
	 * Creates a temporary Source.
	 * 
	 * @param in
	 * @param size the size of the temp source or -1 if unknown
	 * @return the temporary source
	 * @throws IOException
	 */
	public static Source createTempSource(
		InputStream in,
		int size)
		throws IOException {

		if(size != -1 && (size < MAX_IN_RAM_SIZE
			|| in.available() < MAX_IN_RAM_SIZE) ) {

			// read in RAM
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			copyStream(in, out, size);

			out.close();

			return new ByteBufferSource(out.toByteArray());
		} else {
			File tempFile = createTempFile();

			OutputStream out = new FileOutputStream(tempFile);

			copyStream(in, out, size);

			out.close();
			
			// assign literal to response
			return new FileSource(tempFile);
		}

	}

	/**
	 * Creates a temporary Source.
	 * 
	 * @param in
	 * @return the temporary source
	 * @throws IOException
	 */
	public static Source createTempSource(
		InputStream in)
		throws IOException {			
		return  TempSourceFactory.createTempSource(in, -1);
	}
	
	
	/**
	 * Check if a memory or file source will be created.
	 * 
	 * @param size
	 * @return <code>true</code> if a temporary source will be used
	 */
	public static boolean useMemoryTemp(int size) {
	    return size < MAX_IN_RAM_SIZE;
	}
	
	/**
	 * Create a temporary file that will be deleted
	 * on exit of the Virtual Machine.
	 * 
	 * @return the temporary file
	 * @throws IOException
	 */
	public static File createTempFile() throws IOException {
        // read in tempfile
        File tempFile =
        	File.createTempFile(TEMP_PREFIX + nextID(), TEMP_POSTFIX);
        tempFile.deleteOnExit();
        return tempFile;
    }

    private static synchronized int nextID() {
		return counter++;
	}

	private static int copyStream(
		InputStream in,
		OutputStream out,
		int size)
		throws IOException {
		byte[] buffer = new byte[BUFFER_SIZE];

		int copied = 0;
		int actSize;

		if (size != -1) {

			while (size - copied > 0) {
				actSize =
					in.read(
						buffer,
						0,
						size - copied >= BUFFER_SIZE
							? BUFFER_SIZE
							: size - copied);
				if (actSize == -1)
					break;
				out.write(buffer, 0, actSize);
				copied += actSize;
			}
		} else {
			while (true) {
				actSize =
					in.read(buffer);
				if (actSize == -1)
					break;
				out.write(buffer, 0, actSize);
				copied += actSize;
			}
		}

		return copied;
	}

}
