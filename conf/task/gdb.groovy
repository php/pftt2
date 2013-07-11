
def describe() {
	"Run with GDB"
}

def processConsoleOptions(List options) {
	options.add("-debugger")options.add("gdb")
}
