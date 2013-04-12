
def scenarios() {
	// WordpressScenario looks for MySQLScenario to get database configuration
	new WordpressScenario()
}

def getUITestPack() {
	return null; // TODO
}
