
def describe() {
	"Tests Release Candidate and Final builds covering more Scenario Sets than normal snapshot testing."
}

def processConsoleOptions(List options) {
	options.add("-c")
	// test local and remote file systems
	options.add("smb,dfs,deduplication,localfs")
	options.add("-c")
	// test opcache
	options.add("opcache,no_code_cache")
	options.add("-c")
	// test these SAPIs
	options.add("cli,builtin_web,apache")
	options.add("-c")
	//
	// test with these Applications (to make sure new PHP build doesn't break them)
	options.add("symfony,wordpress,mysql")
	
	// TODO iis wincacheu apcu mediawiki drupal joomla typo3 cake_php zend semantic_mediawiki
}
