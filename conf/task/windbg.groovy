
def describe() {
	"Run with WinDebug"
}

def processConsoleOptions(List options) {
	options.add("-windbg")options.add("valgrind")
}
