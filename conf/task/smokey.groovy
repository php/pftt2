
def describe() {
	"""Runs some of the core, application or UI tests on a build, instead of all of them. 
More thorough than the regular smoke tests that PFTT runs, but not as time consuming as running all the tests.
Helps find bad builds that the regular smoke tests won't detect."""
}

def processConsoleOptions(List options) {
	options.add("-run_count");
	options.add("1000");
}
