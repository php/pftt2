import com.mostc.pftt.host.*;

static def getFileServerHost() {
	String addr = new LocalHost().getAddress()
	// autodetect which network this is running on
	if (addr.startsWith("192."))
		// home network
		return new SSHHost("J-2012sp0", "administrator", "password01!");
	else
		// MOSTC
		return new SSHHost("10.200.51.177", "administrator", "password01!");
}

static def getFileServerVolume() {
	String addr = new LocalHost().getAddress()
	// autodetect which network this is running on
	if (addr.startsWith("192."))
		return "B:";
	else
		return "F:";
}

// database
static def getDatabaseServerHost() {
	String addr = new LocalHost().getAddress()
	// autodetect which network this is running on
	if (addr.startsWith("192."))
		// home network
		return new SSHHost("J-2012sp0", "administrator", "password01!");
	else
		// MOSTC
		return new SSHHost("10.200.51.177", "administrator", "password01!");
}

// ftp
static def getFTPServerHost() {
	String addr = new LocalHost().getAddress()
	// autodetect which network this is running on
	if (addr.startsWith("192."))
		// home network
		return new SSHHost("J-2012sp0", "administrator", "password01!");
	else
		// MOSTC
		return new SSHHost("10.200.51.177", "administrator", "password01!");
}

// http
static def getHTTPServerHost() {
	String addr = new LocalHost().getAddress()
	// autodetect which network this is running on
	if (addr.startsWith("192."))
		// home network
		return new SSHHost("J-2012sp0", "administrator", "password01!");
	else
		// MOSTC
		return new SSHHost("10.200.51.177", "administrator", "password01!");
}

// soap
static def getSOAPServerHost() {
	String addr = new LocalHost().getAddress()
	// autodetect which network this is running on
	if (addr.startsWith("192."))
		// home network
		return new SSHHost("J-2012sp0", "administrator", "password01!");
	else
		// MOSTC
		return new SSHHost("10.200.51.177", "administrator", "password01!");
}

// xmlrpc
static def getXMLRPCServerHost() {
	String addr = new LocalHost().getAddress()
	// autodetect which network this is running on
	if (addr.startsWith("192."))
		// home network
		return new SSHHost("J-2012sp0", "administrator", "password01!");
	else
		// MOSTC
		return new SSHHost("10.200.51.177", "administrator", "password01!");
}

// imap (email)
static def getIMAPServerHost() {
	String addr = new LocalHost().getAddress()
	// autodetect which network this is running on
	if (addr.startsWith("192."))
		// home network
		return new SSHHost("J-2012sp0", "administrator", "password01!");
	else
		// MOSTC
		return new SSHHost("10.200.51.177", "administrator", "password01!");
}

// ldap (directory)
static def getLDAPServerHost() {
	String addr = new LocalHost().getAddress()
	// autodetect which network this is running on
	if (addr.startsWith("192."))
		// home network
		return new SSHHost("J-2012sp0", "administrator", "password01!");
	else
		// MOSTC
		return new SSHHost("10.200.51.177", "administrator", "password01!");
}

getBinding().setVariable("CommonConfig", new CommonConfig());


