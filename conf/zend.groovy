
def describe() {
	"Load Zend Framework"
}

class ZendFrameworkPhpUnitTestPack extends PhpUnitSourceTestPack {
	
	@Override
	public String getNameAndVersionString() {
		return "ZendFramework";
	}
	
	@Override
	protected String getSourceRoot(AHost host) {
		return host.getPfttDir()+"/cache/working/zend";
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
	return new ZendFrameworkPhpUnitTestPack();
}
