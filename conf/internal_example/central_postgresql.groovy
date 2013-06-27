
def scenarios() {
	evaluate(new File("$__DIR__/CommonConfig.groovy"));
	
	new PostgresSQLScenario(CommonConfig.getDatabaseServerHost())
}
