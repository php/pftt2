
def scenarios() {
	evaluate(new File("conf/internal/CommonConfig.groovy"));
	
	new LDAPScenario(CommonConfig.getLDAPServerHost())
}
