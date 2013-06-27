
def describe() {
	"Uses DFS remote file system to store PHP scripts instead of the local file system"
}

def scenarios() {
	evaluate(new File("$__DIR__/CommonConfig.groovy"));
	
	new SMBDFSScenario(CommonConfig.getFileServerHost())
}
