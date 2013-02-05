package com.mostc.pftt.model.core;

/** A PhpIni that can not be edited.
 * 
 * Attempts to edit (such as calling #putSingle) are silently ignored. No exceptions are thrown.
 * 
 * @author Matt Ficken
 *
 */

public class ReadOnlyPhpIni extends PhpIni {
	public void putSingle(String directive, String value) {
		
	}
	public void putMulti(String directive, String value) {
		
	}
	public void replaceAll(PhpIni ini) {
		
	}
	public void remove(String directive) {
		
	}
}
