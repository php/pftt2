package com.mostc.pftt.model.app;

/** Pack of PHPUnit tests provided by a PHP application/framework.
 * 
 * Used against PHP Core to find if a PHP build will cause a regression (new failure) in a PHP application/framework.
 * 
 * @author Matt Ficken
 *
 */

public abstract class PhpUnitAppTestPack extends PhpAppTestPack {

	/** loads a PhpUnitAppTestPack from the test pack path, deciding which PhpUnitAppTestPack 
	 * subclass to use (ex: JoomlaTestPack)
	 * 
	 * @see JoomlaTestPack
	 * @param test_pack_path
	 * @return
	 */
	public static PhpUnitAppTestPack load(String test_pack_path) {
		return new JoomlaTestPack(); // TODO
	}
	
}
