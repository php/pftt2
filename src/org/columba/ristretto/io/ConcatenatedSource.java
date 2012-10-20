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
import java.util.ArrayList;

/**
 * Source that concats the underlying Sources.
 * 
 * @author tstich
 *
 */
public class ConcatenatedSource implements Source {

	ArrayList sources;
	ArrayList nextSourceBegin;
	int position;
	int length;
	int sourceIndex;
	/**
	 * Constructs an empty ConcatenatedSource.
	 */
	public ConcatenatedSource() {
		sources = new ArrayList();
		nextSourceBegin = new ArrayList();
		nextSourceBegin.add(new Integer(0));
		position = 0;
		length = 0;
		sourceIndex = 0;
	}

	/**
	 * Add the Source to the end. 
	 * 
	 * @param source
	 */
	public void addSource( Source source ) {
		sources.add( source );
		length += source.length();
		nextSourceBegin.add( new Integer(length) );
	}

	/**
	 * @see org.columba.ristretto.io.Source#fromActualPosition()
	 */
	public Source fromActualPosition() {
		ConcatenatedSource newsource = new ConcatenatedSource();
		newsource.addSource(((Source)sources.get(sourceIndex)).fromActualPosition());
		for( int i=sourceIndex+1; i<sources.size(); i++) {
			newsource.addSource((Source)sources.get(i));
		}
		return newsource;
	}

	/**
	 * @see org.columba.ristretto.io.Source#getPosition()
	 */
	public int getPosition() {
		return position;
	}

	/**
	 * @see org.columba.ristretto.io.Source#isEOF()
	 */
	public boolean isEOF() {
		return position == length;
	}

	/**
	 * @see org.columba.ristretto.io.Source#next()
	 */
	public char next() throws IOException {
		if( ((Source)sources.get(sourceIndex)).isEOF() )  {
			sourceIndex++;
		}
		
		return ((Source)sources.get(sourceIndex)).next();
	}

	/**
	 * @see org.columba.ristretto.io.Source#seek(int)
	 */
	public void seek(int arg0) throws IOException {
		// check if position is in this source
		if( !(((Integer)nextSourceBegin.get(sourceIndex)).intValue() < arg0 && arg0 < ((Integer)nextSourceBegin.get(sourceIndex+1)).intValue()) ) {
			// find the source
			sourceIndex = 0;
			while( !(((Integer)nextSourceBegin.get(sourceIndex)).intValue() <= arg0 && arg0 < ((Integer)nextSourceBegin.get(sourceIndex+1)).intValue()) ) {
				sourceIndex++;
			}
		}
		int posInSource = arg0 - ((Integer)nextSourceBegin.get(sourceIndex)).intValue();		
		position = arg0;
		
		((Source)sources.get(sourceIndex)).seek(posInSource);
	}

	/**
	 * @see java.lang.CharSequence#charAt(int)
	 */
	public char charAt(int arg0) {
		try {
			seek( arg0 );
		} catch (IOException e) {
			return (char)0;
		}
		return ((Source)sources.get(sourceIndex)).charAt(((Source)sources.get(sourceIndex)).getPosition());
	}

	/**
	 * @see java.lang.CharSequence#length()
	 */
	public int length() {
		return length;
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
	 * @see org.columba.ristretto.io.Source#subSource(int, int)
	 */
	public Source subSource(int arg0, int arg1) {
		ConcatenatedSource newsource = new ConcatenatedSource();
	
		// find the begin source
		int beginSourceIndex = 0;
		while( !(((Integer)nextSourceBegin.get(beginSourceIndex)).intValue() <= arg0 && arg0 < ((Integer)nextSourceBegin.get(beginSourceIndex+1)).intValue()) ) {
			beginSourceIndex++;
		}
	
		int beginPos = arg0 - ((Integer)nextSourceBegin.get(beginSourceIndex)).intValue();


		// find the end source
		int endSourceIndex = 0;
		while( !(((Integer)nextSourceBegin.get(endSourceIndex)).intValue() <= arg1 && arg1 < ((Integer)nextSourceBegin.get(endSourceIndex+1)).intValue()) ) {
			endSourceIndex++;
		}
	
		int endPos = arg1 - ((Integer)nextSourceBegin.get(endSourceIndex)).intValue();

		// create newsource
		if( beginSourceIndex == endSourceIndex ) {
			newsource.addSource( (Source) ((Source)sources.get(beginSourceIndex)).subSequence( beginPos, endPos ));
		} else {
			newsource.addSource( (Source) ((Source)sources.get(beginSourceIndex)).subSequence( beginPos,  ((Source)sources.get(beginSourceIndex)).length()) );
			for( int i=beginSourceIndex+1; i<endSourceIndex-1; i++) {
				newsource.addSource( (Source) sources.get(i));	
			}				
			newsource.addSource( (Source) ((Source)sources.get(endSourceIndex)).subSequence( 0,  endPos ));
		}

		return newsource;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer result = new StringBuffer(length);
		for( int i=0; i<sources.size(); i++) {
			result.append( sources.get(i).toString() );
		}
		return result.toString();
	}

	/**
	 * @see org.columba.ristretto.io.Source#close()
	 */
	public void close() throws IOException {
		for( int i=0; i<sources.size(); i++) {
			((Source)sources.get(i)).close();
		}
		sources = null;
	}

	/**
	 * @see org.columba.ristretto.io.Source#deepClose()
	 */
	public void deepClose() throws IOException {
		for( int i=0; i<sources.size(); i++) {
			((Source)sources.get(i)).deepClose();
		}
		sources = null;		
	}
}
