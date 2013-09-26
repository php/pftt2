
def describe() {
	"""Uses MSSQL Server 2008r2"""
}

def scenarios() {
	new MSSQLScenario(
			MSSQLScenario.EMSSQLVersion.DRIVER10,
			'10.200.48.76',
			// note: this login MUST have authorization to create databases, drop those
			//       databases and perform all operations within those databases
			'pftt',
			'password01!'
		)
}
