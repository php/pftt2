
def describe() {
	"Run 1 test at a time, to purely function test the code not stress it at all (slow)"
}

def processConsoleOptions(List options) {
	options.add("-thread_count")
	options.add("1")
}
