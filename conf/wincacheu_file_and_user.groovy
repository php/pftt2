
def describe() {
	"Enables Wincache User Cache (can use this with Opcache) to cache files and user objects to accelerate PHP applications"
}

def scenarios() {
	new WinCacheU_FileAndUserCacheScenario()
}
