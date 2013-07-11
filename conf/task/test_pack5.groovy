
def describe() {
	"run test-pack 5 times"
}

def processConsoleOptions(List options) {
	options.add("-run_test_pack")
	options.add("5")
}
