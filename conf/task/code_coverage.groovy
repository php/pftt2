
def describe() {
	"Monitor and report PHP code coverage using Xdebug (Dynamic Run-Time Analysis)."
}

def scenarios() {
	// @see PhpUnitTemplate#renderTemplate
	//     -it will now collect the code coverage data
	//     -and the test case runner will provide it to the test-pack which will store it
	//
	new XDebugScenario()
}

