
def describe() {
	"Run only 1 test at a time per CPU"
}

def processConsoleOptions(List options) {
	options.add("-thread_count");
	options.add(""+( 1 * new LocalHost().getCPUCount()));
}