
def describe() {
	"Enables Wincache User Cache (can use this with Opcache) to cache files and objects to accelerate PHP applications"
}

def scenarios() {
	new WinCacheUScenario()
}
