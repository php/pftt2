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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * InputStream that logs the data that flows through it.
 * 
 * @author tstich
 *
 */
public class LogInputStream extends FilterInputStream {

    private static final byte[] PREFIX_STRING = { 'S', ':', ' ' };

    private static final int LINEEND = 0;
    private static final int IN_LINE = 1;

    private static final int MAX_LENGTH = 100 - PREFIX_STRING.length;

    private int state;
    private int line_length;

    private OutputStream logOutputStream;

    /**
     * Constructs the LogInputStream.java.
     * 
     * @param arg0
     * @param log
     *            The LogOutputStream
     */
    public LogInputStream(InputStream arg0, OutputStream log) {
        super(arg0);
        this.logOutputStream = log;

        state = LINEEND;

        line_length = 0;
    }

    /**
     * Constructs the LogInputStream.java. This constructor sets the
     * LogOutputStream to System.out.
     * 
     * @param arg0
     */
    public LogInputStream(InputStream arg0) {
        this(arg0, System.out);
    }

    /**
     * @see java.io.InputStream#read()
     */
    public int read() throws IOException {
        int read = in.read();

        if( read == -1 ) return -1;
        
        switch (state) {
            case (LINEEND) :
                {
                    line_length = 0;
                    state = IN_LINE;

                    // if something follows first print the prefix
                    logOutputStream.write(PREFIX_STRING);

                    break;
                }

            case (IN_LINE) :
                {
                    line_length++;

                    // check for line ends
                    if (read == '\n') {
                        state = LINEEND;
                    } else if (line_length == MAX_LENGTH) {
                        // 	check for long lines
                        line_length = 0;
                        logOutputStream.write('\\');
                        logOutputStream.write('\n');
                        logOutputStream.write(PREFIX_STRING);
                    }

                    break;
                }
        }

        logOutputStream.write(read);

        return read;
    }

    /**
     * @see java.io.InputStream#read(byte[], int, int)
     */
    public int read(byte[] arg0, int arg1, int arg2) throws IOException {
        int next;
        int i = 0;

        for (; i < arg2; i++) {
            next = read();
            if (next == -1) {
                break;
            }
            arg0[arg1 + i] = (byte) next;
        }
        
        if( i == 0) return -1;
        else return i;
    }

    /**
     * @return Returns the logOutputStream.
     */
    public OutputStream getLogOutputStream() {
        return logOutputStream;
    }

    /**
     * @param logOutputStream
     *            The logOutputStream to set.
     */
    public void setLogOutputStream(OutputStream logOutputStream) {
        this.logOutputStream = logOutputStream;
    }

}
