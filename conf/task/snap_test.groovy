
def describe() {
	"Test a few Scenario Sets against PHP snapshot builds"
}

def processConsoleOptions(List options) {
	options.add("-c")
	// test these SAPIs
	// TODO builtin_web only without opcache, apcu, wincacheu
	options.add("apache,cli") // TODO iis
	options.add("-c")
	// test with and without opcache
	options.add("opcache,no_code_cache")
	options.add("-c")
	// test with these Applications (to make sure new PHP build doesn't break them)
	options.add("symfony,wordpress,mysql")
	
	// TODO wincacheu apcu mysql mssql postgresql sqlite3 curl ftp xmlrpc soap
	// TODO mediawiki drupal joomla typo3 cake_php zend semantic_mediawiki tiki fengoffice phpbb squirelmail
}
