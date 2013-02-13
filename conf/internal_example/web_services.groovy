
def scenarios() {
	evaluate(new File("conf/internal/CommonConfig.groovy"));
	
	[
		new XMLRPCScenario(CommonConfig.getXMLRPCServerHost()),
		new SOAPScenario(CommonConfig.getSOAPServerHost())
		]
}
