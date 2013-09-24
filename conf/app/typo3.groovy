
class Typo3PhpUnitTestPack extends PhpUnitSourceTestPack {
	
	@Override
	public String getNameAndVersionString() {
		return "Typo3";
	}
	
	@Override
	protected String getSourceRoot(ConsoleManager cm, AHost host) {
		return host.getPfttDir()+"/cache/working/typo3";
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
	return new Typo3PhpUnitTestPack();
}
