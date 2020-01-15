
def describe() {
	"Load `Hello World` Application"
}

def scenarios() {
	new HelloWorldScenario()
}

class HelloWorldPhpUnitTestPack extends PhpUnitSourceTestPack {
	
	@Override
	public String getNameAndVersionString() {
		return "HelloWorld-Tests";
	}
	
	@Override
	protected String getSourceRoot(ConsoleManager cm, AHost host) {
		return host.getPfttDir()+"/cache/helloworld";
	}
	
	@Override
	public boolean isDevelopment() {
		return false;
	}
	
	@Override
	public boolean isFileNameATest(String file_name) {
		return file_name.endsWith("Test.php");
	}
	
	protected void readTestFile(final int max_read_count, String rel_test_file_name, String abs_test_file_name, PhpUnitDist php_unit_dist, List<PhpUnitTestCase> test_cases, File file) throws IOException {
		super.readTestFile(max_read_count, rel_test_file_name, abs_test_file_name, php_unit_dist, test_cases, file);
	}
	
	@Override
	protected boolean openAfterInstall(ConsoleManager cm, AHost host) throws Exception {
		// Adds the test directory, other parameter is empty since there is no bootstrap file
		addPhpUnitDist(getRoot()+"/tests", "");
		return true;
	} // end public boolean openAfterInstall
} 

def getPhpUnitSourceTestPack() {
	return new HelloWorldPhpUnitTestPack();
}