package com.mostc.pftt.model.custom;

/** Tests ACL permutations by applying them with SetACL and testing with PHP's
 * fileperms() function.
 * 
 * @see http://php.net/manual/en/function.fileperms.php
 * 
 * @author Matt Ficken
 *
 */

class SetACLFilePermsTest extends ACLTest {

	@Override
	public String getName() {
		return "setacl-fileperms";
	}
	
}
