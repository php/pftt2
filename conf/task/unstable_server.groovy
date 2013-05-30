
def describe() {
	"Restarts web server after each test. Slow, but works around buggy web servers that crash a lot"
}

def processConsoleOptions(List options) {
	options.add("-restart_each_test_all")
}
