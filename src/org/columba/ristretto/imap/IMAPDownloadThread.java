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
package org.columba.ristretto.imap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import org.columba.ristretto.concurrency.Mutex;
import org.columba.ristretto.io.AsyncInputStream;
import org.columba.ristretto.io.MemBuffer;
import org.columba.ristretto.io.MemBufferInputStream;
import org.columba.ristretto.io.MemBufferOutputStream;
import org.columba.ristretto.io.TempSourceFactory;

/**
 * Thread for asynchronous download from the IMAP
 * server. It is invoked by the IMAPProtocol if
 * a asychrounous method is called.
 * 
 * @author Timo Stich <tstich@users.sourceforge.net>
 */
public class IMAPDownloadThread implements Runnable {
    private static final int BUFFER_SIZE = 1024;
    
    /** JDK 1.4+ logging framework logger, used for logging. */
    private static final Logger LOG = Logger.getLogger("org.columba.ristretto.imap.protocol");

    private AsyncInputStream partner;

    private InputStream source;
    private OutputStream out;

    private byte[] buffer = new byte[BUFFER_SIZE];

    private int size;
    private int read;

    private Mutex mutex;


    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {
        int lastRead;
		long startTime = System.currentTimeMillis();
        
        try {
            while( read < size ) {                
                lastRead = source.read(buffer,0, size - read < BUFFER_SIZE ? size - read : BUFFER_SIZE);
                out.write(buffer, 0, lastRead);
                partner.grow(lastRead);
                read += lastRead;
            }
        } catch (IOException e) {
            partner.exceptionOccured(e);
            // This unblocks the waiting reader
            partner.grow(size - read);
        }

        try {
            //now read the rest from the response
            int rest = 0;
            while( rest != '\n' && rest != -1) {
                rest = source.read();
            }

            // and the a004 OK FETCH completed
            rest = 0;
            while( rest != '\n' && rest != -1) {
                rest = source.read();
            }
        } catch (IOException e1) {
            LOG.warning( e1.getLocalizedMessage() );
        }
        
        LOG.finer("Needed " + (System.currentTimeMillis() - startTime) + " ms for downloading " + size + " bytes.");
        
        if( mutex!= null ) {
            mutex.release();
        }
    }
    /**
     * Constructs the DownloadThread.java.
     *
     * @param partner
     * @param source
     * @param size
     */
    private IMAPDownloadThread(AsyncInputStream partner, InputStream source,
            OutputStream out, int size, Mutex mutex) {
        this.partner = partner;
        this.source = source;
        this.size = size;
        this.out = out;
        this.mutex = mutex;
    }
    
    /**
     * Constructs a new IMAPDownloadThread and starts
     * the download. 
     * 
     * @param input the InputStream from the server. 
     * @param size the size of the part in bytes.
     * @param mutex that manages the access on the input.
     * @return the InputStream of the downloaded part.
     * @throws IOException
     */
    public static AsyncInputStream asyncDownload(InputStream input, int size, Mutex mutex) throws IOException {
		InputStream literalSource;
		OutputStream out;
		
		if( TempSourceFactory.useMemoryTemp(size)) {
			MemBuffer literalBuffer = new MemBuffer(size);
            
            literalSource = new MemBufferInputStream(literalBuffer);
            out = new MemBufferOutputStream(literalBuffer);
        } else {
            File tempFile = TempSourceFactory.createTempFile();
            
            // First create file that has correct size
            byte[] zeros = new byte[10000];
			out = new FileOutputStream(tempFile);
            try {
				int i;
				for( i=0; i<size; i+=10000) {
				    out.write(zeros);
				}
				out.write(zeros,0,size % 10000);
			} finally {
				out.close();
			}
            
            literalSource = new FileInputStream(tempFile);
            out = new FileOutputStream(tempFile);	            
        }

        
        AsyncInputStream asyncStream = new AsyncInputStream(literalSource, size); 
     
        IMAPDownloadThread thread = new IMAPDownloadThread( asyncStream, input, out, size, mutex );
        
        new Thread( thread ).start();
        
        return asyncStream;
    }
}
