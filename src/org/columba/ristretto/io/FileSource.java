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

import java.io.File;
import java.io.IOException;

/**
 * Source that wraps a File.
 * 
 * @author tstich
 *
 */
public class FileSource implements Source {

	private int start, end;
	private int pos;
	
	private FileSourceModel model;


	/**
	 * Constructs the FileSource.
	 * 
	 * @param file
	 * @throws IOException
	 */
	public FileSource( File file ) throws IOException {
		model = new FileSourceModel( file );
		model.incReferences();
		start = 0;
		end = model.length();
		pos = 0;
	}
	
	protected FileSource( FileSource parent ) {
		this.model = parent.model;
		this.model.incReferences();
		this.start = parent.start;
		this.end = parent.end;
		this.pos = 0;
	}

	/**
	 * @see org.columba.ristretto.io.Source#fromActualPosition()
	 */
	public Source fromActualPosition() {
		FileSource subsource = new FileSource( this );
		subsource.start = start + pos;

		return subsource;
	}

	/**
	 * @see org.columba.ristretto.io.Source#getPosition()
	 */
	public int getPosition() {
		return pos;
	}

	/**
	 * @see org.columba.ristretto.io.Source#seek(int)
	 */
	public void seek(int position) throws IOException {
		pos = position;
	}

	/**
	 * @see org.columba.ristretto.io.Source#next()
	 */
	public char next() throws IOException {
		return model.get(start + pos++);
	}

	/**
	 * @see java.lang.CharSequence#length()
	 */
	public int length() {
		return end - start;
	}

	/**
	 * @see java.lang.CharSequence#charAt(int)
	 */
	public char charAt(int arg0){
		try {
			return model.get(start + arg0);
		} catch (IOException e) {
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * @see org.columba.ristretto.io.Source#subSource(int, int)
	 */
	public Source subSource(int arg0, int arg1) {
		FileSource subsource = new FileSource( this );
		subsource.start = start + arg0;
		subsource.end = start + arg1;
		return subsource;
	}

	/**
	 * @see java.lang.CharSequence#subSequence(int, int)
	 */
	public CharSequence subSequence(int arg0, int arg1) {
		if( arg1 - arg0 < 1024) {
			StringBuffer result = new StringBuffer( arg1- arg0);
			for( int i=arg0; i<arg1; i++ ) {
				result.append(charAt(i));
			}
			return result;
		} else {
			return subSource( arg0, arg1);
		}
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer(length());
		for( int i=0; i < length(); i++) {
			buffer.append(charAt(i));
		}
		return buffer.toString();
	}



	/**
	 * @see org.columba.ristretto.io.Source#isEOF()
	 */
	public boolean isEOF() {
		return pos == end;
	}
	
	

	/**
	 * @see java.lang.Object#finalize()
	 */
	protected void finalize() throws Throwable {
		super.finalize();		
		if( model != null ) {
		    model.decReferences();
		    model = null;
		}
	}
	/**
	 * @see org.columba.ristretto.io.Source#close()
	 */
	public void close() throws IOException {
		if( model != null ) {
		    model.decReferences();
		    model = null;
		}
	}

	/**
	 * @see org.columba.ristretto.io.Source#deepClose()
	 */
	public void deepClose() throws IOException {
	    if ( model != null)
	        model.close();
		model = null;
	}
	
	/**
	 * Get the File.
	 * 
	 * @return file
	 */
	public File getFile() {
		return model.getFile();
	}
}
