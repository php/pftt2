
def scenarios() {
	evaluate(new File("conf/internal/CommonConfig.groovy"));
	
	new PostgresSQLScenario(CommonConfig.getDatabaseServerHost())
}
