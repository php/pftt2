
def scenarios() {
	evaluate(new File("conf/internal/CommonConfig.groovy"));
	
	new FTPScenario(CommonConfig.getFTPServerHost())
}
