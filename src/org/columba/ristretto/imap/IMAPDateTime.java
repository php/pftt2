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

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * IMAP date and time representation.
 * 
 * <br>
 * <b>RFC(s):</b> 3105
 * 
 * @author Timo Stich <tstich@users.sourceforge.net>
 */
public class IMAPDateTime extends IMAPDate {

	/**
	 * @see org.columba.ristretto.imap.IMAPDate#toString()
	 */
	public String toString() {
        Calendar cal = Calendar.getInstance(tz);
        cal.setTime(date);

        StringBuffer result = new StringBuffer();
        result.append('\"');
        
        // date-day-fixed
        result.append(super.toString());
        
        result.append(' ');
        
        // time        
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        if( hour < 10 ) {
            result.append('0');
        }
        result.append(hour);
        
        result.append(':');
        int minute = cal.get(Calendar.MINUTE);
        if( minute < 10 ) {
            result.append('0');
        }
        result.append(minute);

        result.append(':');
        int second = cal.get(Calendar.SECOND);
        if( second < 10 ) {
            result.append('0');
        }
        result.append(second);
        
        result.append(' ');
        // zone
		// timezone
		int rawOffset = cal.getTimeZone().getRawOffset();
		if( rawOffset < 0 ) {		
			int hours = (-rawOffset) / 3600000; 
			int minutes = ((-rawOffset) % 3600000) / 60000;
			
			result.append( "-");
			if( hours < 10 ) {
				result.append('0');			
			}			
			result.append( hours);
			
			if( minutes < 10 ) {
				result.append('0'); 
			}
			result.append(minutes);
		} else {
			int hours = rawOffset / 3600000; 
			int minutes = (rawOffset % 3600000) / 60000;
			
			result.append( "+" );
			if( hours < 10 ) {
				result.append('0');			
			}			
			result.append( hours);
			
			if( minutes < 10 ) {
				result.append('0'); 
			}
			result.append(minutes);
		}
        
        
        result.append('\"');
        return result.toString();
    }
    /**
     * Constructs the IMAPDateTime. The date is
     * initialized with the actual date and timezone. 
     * 
     */
    public IMAPDateTime() {
        super();
    }
    /**
     * Constructs the IMAPDateTime. The timezone
     * used is the default timezone of the system. 
     * 
     * @param date the given date
     */
    public IMAPDateTime(Date date) {
        super(date);
    }
    /**
     * Constructs the IMAPDateTime.
     * 
     * @param date the given date.
     * @param tz the timezone of the given date.
     */
    public IMAPDateTime(Date date, TimeZone tz) {
        super(date, tz);
    }
}
