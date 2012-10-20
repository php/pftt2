
import com.ostc.pftt.php.app.Wordpress

// auto import UITestCase
// cr or case_runner -
// host -
// build -
// middleware -
// host_int -

install_ctx = cr.install(new Wordpress())

def user_anon_test(prefix='') {
	cr.run(new UITestCase("$prefix-View-Content")) {
		def act() {
			
		}
	})

	cr.run(new UITestCase("$prefix-Create-Story")) {
		def act() {
			
		}
	})
	
	cr.run(new UITestCase("$prefix-Post-Comments")) {
		def act() {
			
		}
	})
} // end def user_anon_test

def user_tests(user_info=null, prefix='', suffix='', xfail=false) {
	// login new user
	if (user_info) {
		user_login(user_info, prefix, suffix, xfail)
		
		// test as new user
		user_anon_test(prefix)
		
		// change settings
		cr.run(new UITestCase("$prefix-View-Settings-$suffix")) {
			def act() {
				
			}
		}, xfail)
		
		cr.run(new UITestCase("$prefix-Change-Settings-$suffix")) {
			def act() {
				
			}
		}, xfail)
	
		user_logout(user_info, prefix, xfail)
	} else {
		// report that these cases can't be tested b/c user wasn't created
		cr.bork("$prefix-Login-$suffix")
		cr.bork("$prefix-View-Settings-$suffix")
		cr.bork("$prefix-Change-Settings-$suffix")
		cr.bork("$prefix-Logout-$suffix")
	}
} // end def user_tests

def register_user(suffix='1', xfail=false) {
	// TODO
}

def admin_login(suffix='1', xfail=false, given_password=null) {
	// login as administrator
	user_login({username = install_ctx.admin_user; password = given_password==null?install_ctx.admin_password:given_password}, 'Admin', suffix, xfail)
}

def admin_logout(suffix='1') {
	// logout administrator
	user_logout('Admin', suffix)
}

def delete_user(user_info, suffix='1') {
	// TODO
}

def user_login(user_info, prefix='', suffix='', xfail=false) {
	cr.run(new UITestCase("$prefix-Login-$suffix") {
		def act() {
			go_home().find_link('Login', 'Signin').click().fill('User', user_info.username).fill('Password', user_info.password).submit()
		}
	}, xfail);
}

def user_logout(prefix='', suffix='', xfail=false) {
	// logout
	cr.run(new UITestCase("$prefix-Logout-$suffix") {
		def act() {
			// unless go_url() or go_home() called, starts with page left by previous UITestCase
			find_link('Logout').click()
		}
	}, xfail);
}

def xfail_tests(suffix='') {
	admin_login(suffix, xfail=true, given_password='wrong_password')
	
	admin_login(suffix)
	
	change_password(created_user)
	change_password(registered_user)
	
	admin_logout(suffix)
	
	user_login(created_user, xfail=true)
	user_login(registered_user, xfail=true)
}

///////// FIRST TEST CASE HERE ////////////


// test anonymously
user_anon_test('Anonymous')

// register new user
registered_user = null
cr.run(new UITestCase('Register-User')) {
	def act() {
//		registered_user = {
//			username = ''
//			password = ''
//		}	
	}
}

admin_login('1')

user_anon_test('Admin')

// find new user in user list
cr.run(new UITestCase('Admin-User-List') {
	def act() {
		
	}
});

// create user
created_user = null
cr.run(new UITestCase('Admin-Create-User')) {
	def act() {
		
	}
}

// change theme
cr.run(new UITestCase('Admin-Change-Theme') {
	def act() {
		
	}
});

admin_logout('1')

user_tests(registered_user, "Registered-User")
user_tests(created_user, "Created-User")

xfail_tests('-2')

cr.run(new UITestCase('Admin-Enable-All-Plugins') {
	def act() {
		
	}
});

user_anon_test('All-Plugins-Anonymous')

xfail_tests('All-Plugins')

admin_login('All-Plugins')
cr.run(new UITestCase('Admin-Disable-Anonymous-Access') {
	def act() {
		
	}
});
admin_logout('All-Plugins')

user_anon_test('Anonymous-All-Plugins', xfail=true)
register_user(suffix='All-Plugins', xfail=true)

admin_login('All-Plugins-2')
delete_user(created_user, suffix='1')
delete_user(registered_user, suffix='2')
admin_logout('All-Plugins-2')

user_login(created_user, 'All-Plugins', 'Old-Password', xfail=true)
user_login(registered_user, 'All-Plugins', 'Old-Password', xfail=true)
