
def describe() {
	"Load Appleseed Application"
}

def scenarios() {
	new AppleseedScenario()
}

def getUITestPack() {
	return null; // TODO
}

class AppleseedPhpUnitTestPack extends PhpUnitSourceTestPack {
	
	@Override
	public String getNameAndVersionString() {
		return "Appleseed";
	}
	
	@Override
	protected String getSourceRoot(AHost host) {
		return host.getPfttDir()+"/cache/working/Appleseed";
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
	return new AppleseedPhpUnitTestPack();
}
