
def scenarios() {
	evaluate(new File("conf/internal/CommonConfig.groovy"));
	
	new SMBDeduplicationScenario(CommonConfig.getFileServerHost(), CommonConfig.getFileServerVolume())
}
