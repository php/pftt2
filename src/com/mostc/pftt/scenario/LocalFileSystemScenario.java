package com.mostc.pftt.scenario;

/** Placeholder scenario for storing a PHP build and its test pack on the local file system as opposed to remotely somewhere.
 * 
 * @author Matt Ficken
 *
 */

public class LocalFileSystemScenario extends AbstractFileSystemScenario {

	@Override
	public String getName() {
		return "Local-FileSystem";
	}
	
	@Override
	public boolean isImplemented() {
		return true;
	}

}
