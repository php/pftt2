
def describe() {
	"Run tests in random order"
}

def processConsoleOptions(List options) {
	options.add("-randomize_order")
}
