
def scenarios() {
	SSHHost win8_server = new SSHHost("192.168.1.1", "administrator", "password01!");
[
	new CSCDisableScenario(),
	new CSCEnableScenario(),
	new SMBBasicScenario(win8_server),
	new SMBDeduplicationScenario(win8_server, "E:"),
	/* XXX new SMBDFSRScenario(),
	new SMBCAScenario(),*/
]
}
