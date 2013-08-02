
def describe() {
	"Enables Wincache User Cache (can use this with Opcache) to cache only files (not user objects)"
}

def scenarios() {
	new WinCacheU_OnlyFileCacheScenario()
}
