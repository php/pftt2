
def scenarios() {
	evaluate(new File("conf/internal/CommonConfig.groovy"));
	
	new SMBDFSScenario(CommonConfig.getFileServerHost())
}
