package com.mostc.pftt.model.custom;

/** Tests applying ACL permutations using SetACL and checking them using
 * PHP's is_readable(), is_writable() and is_executable() functions.
 * 
 * @see http://php.net/manual/en/function.is-readable.php
 * @see http://php.net/manual/en/function.is-writable.php
 * @see http://php.net/manual/en/function.is-executable.php
 * 
 * @author matt
 *
 */

class SetACLIsRWXTest extends ACLTest {

	@Override
	public String getName() {
		return "setacl-isrwx";
	}
	
}

/*
for (perm:permutation()) {
		set_acl_apply()
		is_writable()
		is_readable()
		is_executable()
	}
*/
