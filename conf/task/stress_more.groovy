
def describe() {
	"run each test 10000 times with 64 threads"
}

def processConsoleOptions(List options) {
	options.add("-no_thread_safety")
	options.add("-thread_count")
	options.add(Integer.toString(32 * LocalHost.getInstance().getCPUCount()))
	options.add("-run_test_times_all")
	options.add("10000")
}
