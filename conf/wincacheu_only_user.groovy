
def describe() {
	"Enables Wincache User Cache (can use this with Opcache) to cache User Objects only (not files)"
}

def scenarios() {
	new WinCacheU_OnlyUserCacheScenario()
}
