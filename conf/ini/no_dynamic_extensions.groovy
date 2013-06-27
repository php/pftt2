
def describe() {
	"configures php.ini to not load any dynamic extensions (PHP and Zend)"
}

def prepareINI(PhpIni ini) {
	ini.remove(PhpIni.EXTENSION);
	ini.remove(PhpIni.EXTENSION_DIR);
	ini.remove(PhpIni.ZEND_EXTENSION);
}
