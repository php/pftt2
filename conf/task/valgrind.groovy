
def describe() {
	"Run with Valgrind"
}

def processConsoleOptions(List options) {
	options.add("-debugger")options.add("valgrind")
}
