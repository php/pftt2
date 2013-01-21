package com.mostc.pftt.model.custom;

/** Tests setting owner using PHP's chown() function and verifying it
 * with PHP's fileowner() function.
 * 
 * @see http://php.net/manual/en/function.chown.php
 * @see http://php.net/manual/en/function.fileowner.php
 * 
 * @author Matt Ficken
 *
 */

class ChownFileOwnerTest extends ACLTest {

	@Override
	public String getName() {
		return "chown-fileowner";
	}
	
}
