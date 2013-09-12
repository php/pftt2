
def describe() {
	"Don't run Opcache with builtin_web"
}

def notScenarios() {
	[
			'Opcache_Local-FileSystem_Builtin-Web', 
			'Opcache_SMB-DFS_Builtin-Web', 
			'Opcache_SMB-CA_Builtin-Web', 
			'Opcache_SMB-Basic_Builtin-Web', 
			'Opcache_SMB-Deduplication_Builtin-Web',
			
			'Opcache_Local-FileSystem_Builtin-Web_WinCacheU-File-And-User',
			'Opcache_SMB-DFS_Builtin-Web_WinCacheU-File-And-User',
			'Opcache_SMB-CA_Builtin-Web_WinCacheU-File-And-User',
			'Opcache_SMB-Basic_Builtin-Web_WinCacheU-File-And-User',
			'Opcache_SMB-Deduplication_Builtin-Web_WinCacheU-File-And-User',
			
			'Opcache_Local-FileSystem_Builtin-Web_WinCacheU-Only-User', 
			'Opcache_SMB-DFS_Builtin-Web_WinCacheU-Only-User', 
			'Opcache_SMB-CA_Builtin-Web_WinCacheU-Only-User', 
			'Opcache_SMB-Basic_Builtin-Web_WinCacheU-Only-User', 
			'Opcache_SMB-Deduplication_Builtin-Web_WinCacheU-Only-User',
			
			'Opcache_Local-FileSystem_Builtin-Web_WinCacheU-Only-File',
			'Opcache_SMB-DFS_Builtin-Web_WinCacheU-Only-File',
			'Opcache_SMB-CA_Builtin-Web_WinCacheU-Only-File',
			'Opcache_SMB-Basic_Builtin-Web_WinCacheU-Only-File',
			'Opcache_SMB-Deduplication_Builtin-Web_WinCacheU-Only-File',
			
			
			'Opcache_Local-FileSystem_MySQL-5.6_Builtin-Web',
			'Opcache_SMB-DFS_MySQL-5.6_Builtin-Web',
			'Opcache_SMB-CA_MySQL-5.6_Builtin-Web',
			'Opcache_SMB-Basic_MySQL-5.6_Builtin-Web',
			'Opcache_SMB-Deduplication_MySQL-5.6_Builtin-Web',
			
			'Opcache_Local-FileSystem_Builtin-Web_MySQL-5.6_WinCacheU-File-And-User',
			'Opcache_SMB-DFS_Builtin-Web_MySQL-5.6_WinCacheU-File-And-User',
			'Opcache_SMB-CA_Builtin-Web_MySQL-5.6_WinCacheU-File-And-User',
			'Opcache_SMB-Basic_Builtin-Web_MySQL-5.6_WinCacheU-File-And-User',
			'Opcache_SMB-Deduplication_Builtin-Web_MySQL-5.6_WinCacheU-File-And-User',
			
			'Opcache_Local-FileSystem_Builtin-Web_MySQL-5.6_WinCacheU-Only-User',
			'Opcache_SMB-DFS_Builtin-Web_MySQL-5.6_WinCacheU-Only-User',
			'Opcache_SMB-CA_Builtin-Web_MySQL-5.6_WinCacheU-Only-User',
			'Opcache_SMB-Basic_Builtin-Web_MySQL-5.6_WinCacheU-Only-User',
			'Opcache_SMB-Deduplication_Builtin-Web_MySQL-5.6_WinCacheU-Only-User',
			
			'Opcache_Local-FileSystem_Builtin-Web_MySQL-5.6_WinCacheU-Only-File',
			'Opcache_SMB-DFS_Builtin-Web_MySQL-5.6_WinCacheU-Only-File',
			'Opcache_SMB-CA_Builtin-Web_MySQL-5.6_WinCacheU-Only-File',
			'Opcache_SMB-Basic_Builtin-Web_MySQL-5.6_WinCacheU-Only-File',
			'Opcache_SMB-Deduplication_Builtin-Web_MySQL-5.6_WinCacheU-Only-File'
		]
}
