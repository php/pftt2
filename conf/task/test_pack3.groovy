
def describe() {
	"run test-pack 3 times"
}

def processConsoleOptions(List options) {
	options.add("-run_test_pack")
	options.add("3")
}
