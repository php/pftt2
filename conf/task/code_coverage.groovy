
def describe() {
	"Monitor and report PHP code coverage using Xdebug (Dynamic Run-Time Analysis)."
}

def scenarios() {
	new XDebugScenario()
}

// TODO note code coverage data stored in each test result
