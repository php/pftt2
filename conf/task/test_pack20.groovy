
def describe() {
	"run test-pack 20 times"
}

def processConsoleOptions(List options) {
	options.add("-run_test_pack")
	options.add("20")
}
