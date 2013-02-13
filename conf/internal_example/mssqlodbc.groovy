
def scenarios() {
	evaluate(new File("conf/internal/CommonConfig.groovy"));
	
	new MSSQLODBCScenario(CommonConfig.getDatabaseServerHost())
}
