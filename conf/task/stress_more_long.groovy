
def describe() {
	"run each test 100000 times with 64 threads"
}

def processConsoleOptions(List options) {
	options.add("-no_thread_safety")
	options.add("-thread_count")
	options.add("64")
	options.add("-run_test_times_all")
	options.add("100000")
	options.add("-randomize_order")
}
