package com.mostc.pftt.model.custom;

/** Tests applying ACL permutations using PHP's chmod() function and checking
 * them using PHP's is_readable(), is_writable() and is_executable() functions.
 *
 * @see http://php.net/manual/en/function.chmod.php
 * @see http://php.net/manual/en/function.is-readable.php
 * @see http://php.net/manual/en/function.is-writable.php
 * @see http://php.net/manual/en/function.is-executable.php
 *
 * @author Matt Ficken
 *
 */

class ChmodIsRWXTest extends ACLTest {

	@Override
	public String getName() {
		return "chmod-is-rwx";
	}
	
}
/*
	for (perm:permutation()) {
		set_acl_chmod()
		
		mgr.exec("<?php
		is_writable()
		is_readable()
		is_executable()
		?>");
		
	}
*/