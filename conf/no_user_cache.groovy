
def describe() {
	"Disables using any User object Cache (disables wincache and apcu)"
}

def scenarios() {
	new NoUserCacheScenario()
}
