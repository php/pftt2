
def scenarios() {
	[new DrupalScenario()]
}

def createTestPack() {
	[new DefaultPhpUnitTestPack() {
		String[][] getNonThreadSafeGroups() {
			[
					new String[]{}
				]
		}
		String getName() {
			"Drupal"
		}
	}]
}
