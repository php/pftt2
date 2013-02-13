
def scenarios() {
	evaluate(new File("conf/internal/CommonConfig.groovy"));
	
	new IMAPScenario(CommonConfig.getIMAPServerHost())
}
