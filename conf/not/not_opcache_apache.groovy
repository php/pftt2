
def describe() {
	"Don't run Opcache with Apache"
}

def notScenarios() {
	['Opcache_Local-FileSystem_Apache-ModPHP', 'Opcache_SMB-DFS_Apache-ModPHP', 'Opcache_SMB-CA_Apache-ModPHP', 'Opcache_SMB-Basic_Apache-ModPHP', 'Opcache_SMB-Deduplication_Apache-ModPHP']
}
