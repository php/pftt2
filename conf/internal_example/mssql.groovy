
def scenarios() {
	evaluate(new File("conf/internal/CommonConfig.groovy"));
	
	new MSSQLScenario(CommonConfig.getDatabaseServerHost())
}
