
def describe() {
	"Uses SMB remote file system to store PHP scripts instead of the local file system"
}

def scenarios() {
	evaluate(new File("conf/internal/CommonConfig.groovy"));
	
	new SMBBasicScenario(CommonConfig.getFileServerHost())
}
