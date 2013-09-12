
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
	options.add("opcache,no_code_cache,not_opcache_builtin_web")
	//options.add("-c")
	// test with WinCacheU:
	//   users probably won't run wincacheu_only_file in production (skip)
	//   users probably won't run wincacheu* with no_code_cache (skip)
	//   no_user_cache => test without wincacheu, enables scenariosets that wincacheu doesn't support
	//options.add("no_user_cache,not_wincacheu_no_code_cache,wincacheu_only_user,not_wincacheu_builtin_web,wincacheu_file_and_user")
	options.add("-c")
	// test with these Applications (to make sure new PHP build doesn't break them)
	options.add("joomla,symfony,wordpress")
	
	options.add("-c")
	// load up mysql and http(curl) scenarios
	options.add("local_mysql,http")
	
	// run each scenarioset/test-pack for up to 1 hour
	options.add("-max_run_time_millis")
	options.add(""+(1*60*60*1000))
	
	// TODO apcu mssql postgresql sqlite3 ftp xmlrpc soap
	// TODO mediawiki drupal typo3 cake_php zend semantic_mediawiki tiki fengoffice phpbb squirelmail
}
