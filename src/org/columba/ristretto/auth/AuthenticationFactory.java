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
package org.columba.ristretto.auth;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.columba.ristretto.auth.mechanism.CramMD5Mechanism;
import org.columba.ristretto.auth.mechanism.DigestMD5Mechanism;
import org.columba.ristretto.auth.mechanism.LoginMechanism;
import org.columba.ristretto.auth.mechanism.PlainMechanism;
import org.columba.ristretto.auth.mechanism.SaslWrapper;


/**
 * Factory for the AuthentictionMechanism of the SASL
 * authentication process. New AuthenticationMechanisms
 * can be implemented and registered with this Factory.
 * <br>
 * Ristretto supports PLAIN, LOGIN and DIGEST-MD5.
 * <br>
 * <b>RFC(s)</b> 2222
 * 
 * @author Timo Stich <tstich@users.sourceforge.net>
 */
public class AuthenticationFactory {

    /** JDK 1.4+ logging framework logger, used for logging. */
    private static final Logger LOG = Logger.getLogger("org.columba.ristretto.auth");

	
    private static final Pattern authTokenizerPattern = Pattern.compile("\\b([^\\s]+)\\b");
    
	private Map authTable;
	private static AuthenticationFactory myInstance;

	private AuthenticationFactory() {
		authTable = new Hashtable();
		
		// add Factory implemented AuthenticationMechanisms
		addAuthentication("PLAIN", PlainMechanism.class);
		addAuthentication("LOGIN", LoginMechanism.class);
		
		if( SaslWrapper.available() ) {
			addAuthentication("DIGEST-MD5", DigestMD5Mechanism.class);
			addAuthentication("CRAM-MD5", CramMD5Mechanism.class);
		}
	}
	
	/**
	 * Gets the singleton instance of the AuthenticationFactory.
	 * 
	 * @return the singleton instance
	 */
	public static AuthenticationFactory getInstance() {
		if( myInstance == null ) {
			myInstance = new AuthenticationFactory();
		}
		
		return myInstance;
	} 

	/**
	 * Adds a new AuthenticationMechanism to the Factory.
	 * 
	 * @param name the SASL registered name of the mechanism
	 * @param auth the implementation of the SASL mechanism
	 */
	public void addAuthentication(String name, Class auth) {
		authTable.put(name, auth);
	}

	/**
	 * Gets a new instance of the AuthenticationMechanism
	 * which implements the specified SASL mechanism. 
	 * 
	 * @param name the SASL registered name of the mechanism
	 * @return a new instance of an AuthenticationMechanism
	 * @throws NoSuchAuthenticationException if no implementation of the specified
	 * mechanism can be found 
	 */
	public AuthenticationMechanism getAuthentication(String name) throws NoSuchAuthenticationException {		
		AuthenticationMechanism auth;
        if( !authTable.containsKey(name) ) throw new NoSuchAuthenticationException( name );
		
		try {
            auth = (AuthenticationMechanism) ((Class)authTable.get(name)).newInstance();
        } catch (InstantiationException e) {
           throw new NoSuchAuthenticationException(e);
        } catch (IllegalAccessException e) {
            throw new NoSuchAuthenticationException(e);
        }
		
		return auth;
	}

    /**
     * Checks if the specified mechanism is
     * supported by the Factory.
     * 
     * @param mechanism the SASL name of the mechanism
     * @return true if an implementation of the mechanism
     * is registered with this Factory.
     */
    public boolean isSupported(String mechanism) {
        return authTable.get(mechanism) != null;
    }
    
    /**
     * Gets a List of the supported Mechanisms.
     * 
     * @return a list of the SASL registered names
     * of the supported mechanisms. 
     */
    public List getSupportedMechanisms() {
    	List list = new LinkedList();
    	Set keys = authTable.keySet();
    	Iterator it = keys.iterator();

    	while(it.hasNext()) {
    		list.add(it.next());
    	}
    	
    	return list;
    }

    /**
     * Gets a List of the supported Mechanisms from server and client.
     * 
     * @param authCapa the CAPA response from a server.
     * @return a list of the SASL registered names
     * of the supported mechanisms. 
     */
    public List getSupportedMechanisms(String authCapa) {
    	List list = new LinkedList();
    	
        Matcher matcher = authTokenizerPattern.matcher( authCapa );
        
        // First token is AUTH
        matcher.find();
        
        // Search for a supported Authentication by both
        // client and server
        while( matcher.find() ) {
            if( isSupported( matcher.group(1) )) {
                list.add( matcher.group(1) );
            }
        }
    	
    	return list;
    }

    /**
     * Gets the securest supported SASL mechanism of the
     * specified. The assumption hereby is that the
     * given SASL mechanims are sorted securest first.
     * <br><br>
     * <b>Note:</b> This method can be convieniently used with the
     * capability reponses from a POP3, IMAP or SMTP
     * server.
     * <br><br>
     * <b>Example:</b>
     * <br><code>getSecurestMethod("DIGEST-MD5 LOGIN PLAIN")
     * <br>returns "DIGEST-MD5".</code>
     * 
     * @param authCapability a whitespace separated list of SASL mechanisms
     * @return the first supported SASL mechanism
     * @throws NoSuchAuthenticationException 
     */
    public String getSecurestMethod(String authCapability) throws NoSuchAuthenticationException {
        Matcher matcher = authTokenizerPattern.matcher( authCapability );
        
        // First token is AUTH
        matcher.find();
        
        // Search for a supported Authentication and
        // return the first == securest Method found
        while( matcher.find() ) {
            if( isSupported( matcher.group(1) )) {
                return matcher.group(1);
            }
        }
        
        throw new NoSuchAuthenticationException( authCapability );
    }

}
