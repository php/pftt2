
def describe() {
	"""Suspends test processes when they are first started before the test is run. This lets you check out the process first.
The process will be resumed and the test run, automatically (the One Minute timeout does not start until the process is resumed).
	"""
}

def processConsoleOptions(List options) {
	// suspend for 30 seconds first
	options.add("-suspend_seconds");
	options.add("30")
}
