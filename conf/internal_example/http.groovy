
def scenarios() {
	evaluate(new File("conf/internal/CommonConfig.groovy"));
	
	new HTTPScenario(CommonConfig.getHTTPServerHost())
}
