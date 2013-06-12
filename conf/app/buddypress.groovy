
def describe() {
	"Load Buddypress Application"
}

def scenarios() {
	new BuddypressScenario();
}

class BuddypressPhpUnitTestPack extends PhpUnitSourceTestPack {
	
	@Override
	public String getNameAndVersionString() {
		return "Buddypress";
	}
	
	@Override
	protected String getSourceRoot(AHost host) {
		return host.getPfttDir()+"/cache/working/buddypress";
	}
	
	@Override
	public boolean isDevelopment() {
		return true;
	}
	 
	@Override
	protected boolean openAfterInstall(ConsoleManager cm, AHost host) throws Exception {
		// TODO implement
		return false;
	}
	
}

def getPhpUnitSourceTestPack() {
	return new BuddypressPhpUnitTestPack();
}
