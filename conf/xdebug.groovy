
def describe() {
	"""Load XDebug DLL only (test load)... 
to actually use it, use a config task like `code_coverage` or `code_profile` instead."""
}

def scenarios() {
	new XDebugScenario()
}
