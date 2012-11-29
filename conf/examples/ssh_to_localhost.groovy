
def hosts() {
	def local = new SSHHost("10.200.50.195", "administrator", "password01!");
	println(local.isWindows());
[
	local
]
}
