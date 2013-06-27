
def scenarios() {
	evaluate(new File("$__DIR__/CommonConfig.groovy"));
	
	new LDAPScenario(CommonConfig.getLDAPServerHost())
}
