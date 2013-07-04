
def describe() {
	"Don't run builtin_web"
}

def notScenarios() {
	['Local-FileSystem_Builtin-Web', 'SMB-DFS_Builtin-Web', 'SMB-CA_Builtin-Web', 'SMB-Basic_Builtin-Web', 'SMB-Deduplication_Builtin-Web',
		'Opcache_Local-FileSystem_Builtin-Web', 'Opcache_SMB-DFS_Builtin-Web', 'Opcache_SMB-CA_Builtin-Web', 'Opcache_SMB-Basic_Builtin-Web', 'Opcache_SMB-Deduplication_Builtin-Web']
}
