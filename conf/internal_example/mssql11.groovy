
def describe() {
	"""Uses MSSQL Server 2012 or 2012sp1"""
}

def scenarios() {
	new MSSQLScenario(
			MSSQLScenario.EMSSQLVersion.DRIVER11,
			'10.200.48.76',
			// note: this login MUST have authorization to create databases, drop those
			//       databases and perform all operations within those databases
			'pftt',
			'password01!'
		)
}
