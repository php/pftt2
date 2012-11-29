
This is the default location to store PFTT configuration files.

You provide them to PFTT like so:
pftt -config <file1>,<file2, etc...> <command>

PFTT search conf, conf/internal and conf/examples. If your config file is in another folder,
you must specify the path to it.

This folder has these 3 sub-folders
 * apps - configurations for Application testing (you may store app configs anywhere, this is just the default location)
 * examples - example complex configuration files (network services - SMB, Databases, etc...)
 * internal - store your internal use only configuration files (usually network services, addresses
 and credentials you don't want to share. the internal folder is ignored by git)

Examples:

Test all PHPTs on Apache
pftt -config apache phpt_all

Setup a MySQL server and put info in conf/internal/mysql.groovy.
Then, this will install & setup Drupal, IIS and MySQL (on Windows).
pftt -config drupal,iis,mysql setup

 
Config files are executable code written in Groovy.

You may define these functions:
hosts() - define remote hosts (SSH) to test against. 
		You can add 1 host for each OS/Version you support, thereby covering all OS/Versions you support in
		a single test pass.
scenarios() - provide individual Scenarios. PFTT will calculate all the permuations of these to generate ScenarioSets for you
scenario_sets() - provide whole Scenario Sets
configure_smtp() - configure an SMTPProtocol to define where to email reports to
configure_ftp() - configure an FTPClient to define a location to upload telemetry archives to
