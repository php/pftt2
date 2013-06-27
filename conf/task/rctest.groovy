
def describe() {
	"Tests Release Candidate and Final builds covering more Scenario Sets than normal snapshot testing."
}

def processConsoleOptions(List options) {
	options.add("-c")
	// test local and remote file systems
	// (basically snap_test * 4 file-system scenarios)
	options.add("smb,dfs,deduplication,localfs,not_remote_fs_builtin_web")
	options.add("-c")
	options.add("snap_test")
	
	// TODO iis wincacheu apcu mediawiki drupal joomla typo3 cake_php zend semantic_mediawiki
}
