
def scenarios() {
	evaluate(new File("$__DIR__/CommonConfig.groovy"));
	
	new MySQLScenario(CommonConfig.getDatabaseServerHost())
}
