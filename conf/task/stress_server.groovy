
def describe() {
	"Never restarts web servers. Adds extra stress to each web server. Will produce invalid PHPT test results (because no INI configuration changes will occur)."
}

def processConsoleOptions(List options) {
	options.add("-no_restart_all")
	options.add("-randomize_order")
}
