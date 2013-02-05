
def scenarios() {
	[new JoomlaScenario()]
}

def createTestPack() {
	[new DefaultPhpUnitTestPack() {
		String[][] getNonThreadSafeGroups() {
			[
					new String[]{}
				]
		}
		String getName() {
			"Joomla"
		}
	}]
}
