
def describe() {
	"Don't run builtin_web on remote file systems"
}

def notScenarios() {
	['SMB-DFS_Builtin-Web', 'SMB-CA_Builtin-Web', 'SMB-Basic_Builtin-Web', 'SMB-Deduplication_Builtin-Web']
}
