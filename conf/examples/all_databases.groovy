
def scenarios() {
[
	new MSAccessScenario(),
	new MSSQLODBCScenario(),
	new MSSQLScenario(),
	new MySQLScenario(),
	new PostgresSQLScenario(),
	new SQLite3Scenario()
]
}
