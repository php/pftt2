package com.mostc.pftt.scenario;

/** Scenario that sets up a remote FTP service and has the curl extension tested against it. (NOT IMPLEMENTED)
 * 
 * @author Matt Ficken
*
*/

public class FTPScenario extends AbstractStreamsScenario {

	@Override
	public String getName() {
		return "FTP";
	}
	
	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	public String getNameWithVersionInfo() {
		return "FTP"; // XXX -[ftp server implementation and server version]
	}

}
