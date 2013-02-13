
def scenarios() {
	evaluate(new File("conf/internal/CommonConfig.groovy"));
	
	new SMBBasicScenario(CommonConfig.getFileServerHost())
}
