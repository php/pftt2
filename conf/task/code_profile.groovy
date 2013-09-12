
def describe() {
	"""Profile code to get run-times and find slow code points using Xdebug (Dynamic Run-Time Analysis)

Stores *.cachegrind file in result-pack for each test-pack and scenario set.

ANSWERS How fast/slow?
"""
}

def scenarios() {
	new XDebugProfilingScenario()
}
