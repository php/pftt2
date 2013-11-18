
def describe() {
	"run each test 1000 times with 16 threads"
}

def processConsoleOptions(List options) {
	options.add("-thread_count")
	options.add(Integer.toString(16 * LocalHost.getInstance().getCPUCount()))
	options.add("-run_test_times_all")
	options.add("1000")	
}
