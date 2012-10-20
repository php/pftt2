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
package org.columba.ristretto.log;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * OutputStream that logs all data that goes
 * through it.
 * 
 * @author tstich
 *
 */
public class LogOutputStream extends FilterOutputStream {
    private static final byte[] PREFIX_STRING = { 'C', ':', ' ' };
    
    private static final int LINEEND = 0;
    private static final int IN_LINE = 1;
    
    private static final int MAX_LENGTH = 100 - PREFIX_STRING.length;
    
    private int state;
    private int line_length;
    
    private OutputStream logOutputStream; 
    
    /**
     * Constructs the LogOutputStream.java.
     * 
     * @param arg0
     * @param logStream Stream to log to (eg System.out)
     */
    public LogOutputStream(OutputStream arg0, OutputStream logStream) {
        super(arg0);
        this.logOutputStream =logStream;
    }
    
    /**
     * Constructs the LogOutputStream.java.
     * 
     * @param arg0
     */
    public LogOutputStream(OutputStream arg0) {
        this( arg0, System.out );
    }
    

    /**
     * @see java.io.OutputStream#write(byte[], int, int)
     */
    public void write(byte[] arg0, int arg1, int arg2) throws IOException {
        for( int i=arg1; i<arg2+arg1; i++) {
               write((int)(0x0ff & arg0[i]));
        }
    }

    /**
     * @see java.io.OutputStream#write(int)
     */
    public void write(int write) throws IOException {

        switch( state ) {
        case( LINEEND ) : {  
            line_length = 0;
            state = IN_LINE;
            
            // if something follows first print the prefix
            if( write != -1 ) {
                logOutputStream.write(PREFIX_STRING);        	        
            }   
            
            break;
        }
        
        case( IN_LINE ) : {
            line_length++;
            
            // check for line ends
            if( write == '\n' ) {
                state = LINEEND;
            } else if( line_length ==MAX_LENGTH) {        	    
                // 	check for long lines
                line_length = 0;
                logOutputStream.write('\\');
                logOutputStream.write('\n');
                logOutputStream.write( PREFIX_STRING);
            }
            
            break;
        }
        }
        
        if( write != -1 ) {
            logOutputStream.write(write);
        }
        
        out.write(write);
    }

}
