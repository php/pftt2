
def describe() {
	"Skips testing the PHAR extension"
}

def processConsoleOptions(List options) {
	options.add("-skip_name")
	options.add("phar")
}
