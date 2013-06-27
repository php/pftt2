
def describe() {
	"""(Windows only) Opcache, without a startup process, needed to always keep a 
handle to the SharedMemoryArea open. High process-churn can crash Opcache because all the processes with a SharedMemoryArea handle may close at the same time."""
}

def scenarios() {
	new OpcacheNoStartupProcessScenario();
}
