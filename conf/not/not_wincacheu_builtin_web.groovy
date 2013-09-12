
def describe() {
	"Don't run WinCacheU with Builtin-Web"
}

def notScenarios() {
	[
		'Local-FileSystem_MySQL-5.6_Builtin-Web_WinCacheU-File-And-User',
		'Local-FileSystem_MySQL-5.6_Builtin-Web_WinCacheU-Only-File',
		'Local-FileSystem_MySQL-5.6_Builtin-Web_WinCacheU-Only-User',
		'SMB-DFS_MySQL-5.6_Builtin-Web_WinCacheU-File-And-User',
		'SMB-DFS_MySQL-5.6_Builtin-Web_WinCacheU-Only-File',
		'SMB-DFS_MySQL-5.6_Builtin-Web_WinCacheU-Only-User',
		'SMB-Basic_MySQL-5.6_Builtin-Web_WinCacheU-File-And-User',
		'SMB-Basic_MySQL-5.6_Builtin-Web_WinCacheU-Only-File',
		'SMB-Basic_MySQL-5.6_Builtin-Web_WinCacheU-Only-User',
		'SMB-Deduplication_MySQL-5.6_Builtin-Web_WinCacheU-File-And-User',
		'SMB-Deduplication_MySQL-5.6_Builtin-Web_WinCacheU-Only-File',
		'SMB-Deduplication_MySQL-5.6_Builtin-Web_WinCacheU-Only-User',
		'Local-FileSystem_Builtin-Web_WinCacheU-File-And-User',
		'Local-FileSystem_Builtin-Web_WinCacheU-Only-File',
		'Local-FileSystem_Builtin-Web_WinCacheU-Only-User',
		'SMB-DFS_Builtin-Web_WinCacheU-File-And-User',
		'SMB-DFS_Builtin-Web_WinCacheU-Only-File',
		'SMB-DFS_Builtin-Web_WinCacheU-Only-User',
		'SMB-Basic_Builtin-Web_WinCacheU-File-And-User',
		'SMB-Basic_Builtin-Web_WinCacheU-Only-File',
		'SMB-Basic_Builtin-Web_WinCacheU-Only-User',
		'SMB-Deduplication_Builtin-Web_WinCacheU-File-And-User',
		'SMB-Deduplication_Builtin-Web_WinCacheU-Only-File',
		'SMB-Deduplication_Builtin-Web_WinCacheU-Only-User'
	]
}
