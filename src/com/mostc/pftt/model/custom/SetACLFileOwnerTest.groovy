package com.mostc.pftt.model.custom;

/** Tests ACL permutations by applying them with SetACL and testing them
 * with PHP's fileowner() function.
 * 
 * @see http://php.net/manual/en/function.fileowner.php
 * 
 * @author Matt Ficken
 *
 */

class SetACLFileOwnerTest extends ACLTest {

	@Override
	public String getName() {
		return "setacl-fileowner";
	}
	
}
