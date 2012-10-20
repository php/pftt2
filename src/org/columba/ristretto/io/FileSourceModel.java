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
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Timer;
import java.util.TimerTask;

/**
 * FileSourceModel of a FileSource. Works together with
 * the FileSource to efficently wrap a File
 * in a FileSource.
 * 
 * 
 * @author tstich
 *
 */
public class FileSourceModel {

	private File file;
	private FileChannel channel;
	private ByteBuffer buffer;
	private int bufferStart;
	private int references;
	
	private CloseChannelTimerTask closeTask;

	// Share one timer between all instances
	private static Timer timer = new Timer();
	
	private static final int BUFFERSIZE = 61440; //byte
	private static final long CLOSE_DELAY = 2000; //ms

	
	/**
	 * Constructs the FileSourceModel.
	 * 
	 * @param file
	 * @throws IOException
	 */
	public FileSourceModel( File file ) throws IOException {
		this.file = file;
		
		buffer = ByteBuffer.allocate( BUFFERSIZE );

		bufferStart = 0;
		references = 0;
		
		updateBufferFromFile();
	}

	/**
	 * Get the character at the given position.
	 * 
	 * @param pos
	 * @return the character at this position
	 * @throws IOException
	 */
	public char get( int pos ) throws IOException {
		// Must be read from channel or is it in the buffers
		if( (pos < bufferStart)  ) {
			while( pos < bufferStart ) {
				bufferStart -= (BUFFERSIZE / 2);
			}
			
			updateBufferFromFile();
		} else 
		if (pos >= bufferStart + BUFFERSIZE ) {
			while( pos >= bufferStart + BUFFERSIZE ) {
				bufferStart += (BUFFERSIZE / 2);
			}
			updateBufferFromFile();
		}
		
		// this is a trick to avoid interpreting
		// the byte as a signed value
		byte value = buffer.get( pos - bufferStart );
		int trueValue = (value & 0x080) + (value & 0x07F);
		
		return (char) trueValue; 
	}
	
	/**
	 * @throws IOException
	 */
	private void updateBufferFromFile() throws IOException {
		if( channel == null ) {
			channel = new RandomAccessFile( file, "r" ).getChannel();
		}

		buffer.clear();
		channel.read(buffer, bufferStart );
		
		// The channel will be closed after 2 secs
		// idle
		restartCloseTimer();
	}

	private void restartCloseTimer() {
		if( closeTask != null ) closeTask.cancel();
		closeTask = new CloseChannelTimerTask(this);
		timer.schedule(closeTask, CLOSE_DELAY);
	}

	/**
	 * Get the lenght of the File.
	 * 
	 * @return the length
	 * @throws IOException
	 */
	public int length() throws IOException {
		return (int) channel.size();
	}
	
	/**
	 * Another FileSource is dependent on this
	 * model.
	 * 
	 */
	public void incReferences() {
		references++;
	}
	
	/**
	 * A dependant FileSource is closed.
	 * If none are left the FileSourceModel
	 * can be closed and the File resources
	 * can be released.
	 */
	public void decReferences() {
		references--;
		if( references <= 0 ) {
			try {
				close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Closes the FileSourceModel.
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		if( channel != null) {
			channel.close();
			channel = null;
		}
	}

	/**
	 * @return Returns the file.
	 */
	public File getFile() {
		return file;
	}

}


class CloseChannelTimerTask extends TimerTask {
	
	FileSourceModel model;
	
	
	/**
	 * Constructs the CloseChannelTimerTask
	 * 
	 * @param model
	 */
	public CloseChannelTimerTask(FileSourceModel model) {
		super();
		this.model = model;
	}
	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		try {
			model.close();
		} catch (IOException e) {
		}
	}
}