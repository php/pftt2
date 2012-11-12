
def scenarios() {
	SSHHost win8_server = new SSHHost("192.168.1.1", "administrator", "password01!");
[
	new SMBBasicScenario(win8_server),
	new SMBDeduplicationScenario(win8_server, "E:"),
	/* XXX new SMBDFSScenario(),
	new SMBCAScenario(),*/
	// probably don't need to test branch cache, but including it for completeness
	//new SMBBranchCacheScenario()
]
}
