
class CakePhpUnitTestPack extends PhpUnitSourceTestPack {
	
	@Override
	public String getNameAndVersionString() {
		return "CakePHP";
	}
	
	@Override
	protected String getSourceRoot(AHost host) {
		return host.getPfttDir()+"/cache/working/cakephp";
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
	return new CakePhpUnitTestPack();
}
