
def createTestPack() {
	[new DefaultPhpUnitTestPack() {
		String[][] getNonThreadSafeGroups() {
			[
					new String[]{}
				]
		}
		String getName() {
			"Symfony"
		}
	}]
}
