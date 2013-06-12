
def describe() {
	"Load Semantic-MediaWiki Application"
}

def scenarios() {
	new SemanticMediaWikiScenario()
}

def getUITestPack() {
	return null; // TODO
}


class SemanticMediaWikiPhpUnitTestPack extends PhpUnitSourceTestPack {
	
	@Override
	public String getNameAndVersionString() {
		return "SemanticMediaWiki";
	}
	
	@Override
	protected String getSourceRoot(AHost host) {
		return host.getPfttDir()+"/cache/working/semantic_mediawiki";
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
	return new SemanticMediaWikiPhpUnitTestPack();
}

