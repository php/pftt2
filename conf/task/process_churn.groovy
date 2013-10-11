
def describe() {
	"""Runs 24 threads per CPU to create a lot of `process churn` (lots of processes starting up and terminating at the same time... this will stress shared components like Opcache and the Kernel)"""
}

def processConsoleOptions(List options) {
	options.add("-thread_count");
	options.add(""+( 24 * LocalHost.getInstance().getCPUCount()));
}
