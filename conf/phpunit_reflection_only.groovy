
// PhpUnit uses only reflection to reference test methods
// OpCache may optimize out those methods so tests never get executed (they all PASS, even ones that should be FAILURE or ERROR)
// PFTT normally avoids this problem, however if you want to test if OpCache would do this, use this scenario/config
// (this is the opposite of phpunit_inline_references conf)
def scenarios() {
	return new PhpUnitReflectionOnlyScenario();
}

def describe() {
	"""Forces using only Reflection to reference test methods. This may cause Opcache to optimize out some test methods."""
}