
def describe() {
	"Test a few Scenario Sets against PHP snapshot builds"
}

def processConsoleOptions(List options) {
	// don't specify a file system here(fe `localfs`) so user can do `snap_test` only on `dfs` etc... if they want
	//
	options.add("-c")
	// test these SAPIs
	options.add("apache,cli,builtin_web")
	options.add("-c")
	// test with and without opcache
	options.add("opcache,no_code_cache,not_opcache_cli,not_opcache_builtin_web")
	options.add("-c")
	// test with these Applications (to make sure new PHP build doesn't break them)
	options.add("symfony,wordpress,local_mysql")
	
	options.add("-c")
	// load up mysql and http(curl) scenarios
	options.add("local_mysql,http")
	
	// TODO wincacheu apcu mssql postgresql sqlite3 ftp xmlrpc soap
	// TODO mediawiki drupal joomla typo3 cake_php zend semantic_mediawiki tiki fengoffice phpbb squirelmail
}
