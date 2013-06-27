
def describe() {
	"Don't run Opcache with CLI"
}

def notScenarios() {
	['Opcache_Local-FileSystem_CLI', 'Opcache_SMB-DFS_CLI', 'Opcache_SMB-CA_CLI', 'Opcache_SMB-Basic_CLI', 'Opcache_SMB-Deduplication_CLI']
}
