package com.mostc.pftt.model.custom;

/** Tests setting ACL permutations using PHP's chmod() function and testing
 * them using PHP's fileperms() function.
 * 
 * @see http://php.net/manual/en/function.fileperms.php
 * @see http://php.net/manual/en/function.chmod.php
 * 
 * @author Matt Ficken
 *
 */

class ChmodFilePermsTest extends ACLTest {

	@Override
	public String getName() {
		return "chmod-fileperms";
	}
	
}
