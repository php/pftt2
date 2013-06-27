
def scenarios() {
	evaluate(new File("$__DIR__/CommonConfig.groovy"));
	
	[
		new XMLRPCScenario(CommonConfig.getXMLRPCServerHost()),
		new SOAPScenario(CommonConfig.getSOAPServerHost())
		]
}
