
def describe() {
	"Load SDK for Windows Azure (azure-sdk-for-php)"	
}

def scenarios() {
	new AzureSDKScenario()
}

class AzureSDKPhpUnitTestPack extends PhpUnitSourceTestPack {
	
	@Override
	public String getNameAndVersionString() {
		return "Azure-SDK";
	}
	
	@Override
	protected String getSourceRoot(AHost host) {
		return host.getPfttDir()+"/cache/working/azure-sdk-for-php";
	}
	
	@Override
	public boolean isDevelopment() {
		return true;
	}
	
	@Override
	protected boolean openAfterInstall(ConsoleManager cm, AHost host) throws Exception {
		addPhpUnitDist(getRoot()+"/tests/unit", getRoot()+"/tests/WindowsAzureTests.php");
		addPhpUnitDist(getRoot()+"/tests/functional", getRoot()+"/tests/WindowsAzureTests.php");
		
		return true;
	}

} // end class AzureSDKPhpUnitTestPack

def getPhpUnitSourceTestPack() {
	return new AzureSDKPhpUnitTestPack();
}
