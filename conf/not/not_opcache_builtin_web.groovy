
def describe() {
	"Don't run Opcache with builtin_web"
}

def notScenarios() {
	['Opcache_Local-FileSystem_Builtin-Web', 'Opcache_SMB-DFS_Builtin-Web', 'Opcache_SMB-CA_Builtin-Web', 'Opcache_SMB-Basic_Builtin-Web', 'Opcache_SMB-Deduplication_Builtin-Web']
}
