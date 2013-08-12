package com.mostc.pftt.model.core;

/** A PhpIni that can not be edited.
 * 
 * Attempts to edit (such as calling #putSingle) are silently ignored. No exceptions are thrown.
 * 
 * @author Matt Ficken
 *
 */

public class ReadOnlyPhpIni extends PhpIni {
	@Override
	public void putSingle(String directive, String value) {
		
	}
	@Override
	public void putMulti(String directive, String value) {
		
	}
	@Override
	public void replaceAll(PhpIni ini) {
		
	}
	@Override
	public void remove(String directive) {
		
	}
}
