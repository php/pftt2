
def describe() {
	"run each test 1000 times"
}

def processConsoleOptions(List options) {
	options.add("-run_test_times_all")
	options.add("1000")
}
