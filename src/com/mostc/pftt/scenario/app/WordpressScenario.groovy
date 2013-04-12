package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.MySQLScenario;
import com.mostc.pftt.scenario.ScenarioSet;

/** WordPress is a free and open source blogging tool and a content management system (CMS) 
 * based on PHP and MySQL. It has many features including a plug-in architecture and a
 * template system. 
 * 
 * @see http://wordpress.org/
 * 
 */

public class WordpressScenario extends ZipDbApplication {
	
	@Override
	public String getName() {
		return "Wordpress";
	}

	@Override
	public boolean isImplemented() {
		return true;
	}

	@Override
	protected String getZipAppFileName() {
		return "wordpress-3.5.1.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		MySQLScenario mysql = requireMySQLScenario(cm, scenario_set);
		if (mysql==null)
			return false;
		
		def wp_config_str = """
		<?php
define('DB_NAME', '$mysql.db_name');
define('FORCE_SSL', false);
function is_ssl() {
	return false;
}
define('DB_USER', '$mysql.user');
define('DB_PASSWORD', '$mysql.password');
define('DB_HOST', '$mysql.host');
define('DB_CHARSET', 'utf8');
define('DB_COLLATE', '');
define('AUTH_KEY',         'put your unique phrase here');
define('SECURE_AUTH_KEY',  'put your unique phrase here');
define('LOGGED_IN_KEY',    'put your unique phrase here');
define('NONCE_KEY',        'put your unique phrase here');
define('AUTH_SALT',        'put your unique phrase here');
define('SECURE_AUTH_SALT', 'put your unique phrase here');
define('LOGGED_IN_SALT',   'put your unique phrase here');
define('NONCE_SALT',       'put your unique phrase here');
$table_prefix  = 'wp_';
define('WPLANG', '');
define('WP_DEBUG', false);
if ( !defined('ABSPATH') )
	define('ABSPATH', dirname(__FILE__) . '/');

require_once(ABSPATH . 'wp-settings.php');
		"""
		
		host.saveTextFile(app_dir+"/wp-config.php", wp_config_str);

		return true;
	}

}
