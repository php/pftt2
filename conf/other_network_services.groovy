
def scenarios() {
[
	// streams
	new FTPScenario(),
	new HTTPScenario(),
	// web services
	new SOAPScenario(),
	new XMLRPCScenario()
]
}
