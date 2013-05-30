
def scenarios() {
	return new PhpUnitInlineReferencesScenario();
}

def describe() {
	"Uses inline references to test methods so Opcache will not optimize them out (default, see phpunit_reflection_only for opposite config)"
}
