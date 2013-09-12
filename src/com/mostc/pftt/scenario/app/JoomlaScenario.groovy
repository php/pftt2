package com.mostc.pftt.scenario.app;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.ActiveTestPack;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.scenario.ScenarioSet;

/** Joomla is a free and open source content management system (CMS) for publishing content on the
 * World Wide Web and intranets and a model–view–controller (MVC) Web application framework that can
 * also be used independently.
 * 
 * @see http://www.joomla.org/
 * 
 */

public class JoomlaScenario extends ZipDbApplication {

	@Override
	public String getName() {
		return "Joomla";
	}

	@Override
	public boolean isImplemented() {
		return true;
	}

	@Override
	protected String getZipAppFileName() {
		return "Joomla_3.0.2-Stable-Full_Package.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		
		/*host.open("joomla/joomla.xml");
		def xml = """
		  <?xml version="1.0" encoding="UTF-8" ?> 
		  - <extension version="3.0" type="file" method="upgrade">
		    <name>files_joomla</name> 
		    <author>Joomla! Project</author> 
		    <authorEmail>admin@joomla.org</authorEmail> 
		    <authorUrl>www.joomla.org</authorUrl> 
		    <copyright>(C) 2005 - 2012 Open Source Matters. All rights reserved</copyright> 
		    <license>GNU General Public License version 2 or later; see LICENSE.txt</license> 
		    <version>3.0.2</version> 
		    <creationDate>November 2012</creationDate> 
		    <description>FILES_JOOMLA_XML_DESCRIPTION</description> 
		    <scriptfile>administrator/components/com_admin/script.php</scriptfile> 
		  - <update>
		  - <!--  Runs on update; New in 1.7 
		    --> 
		  - <schemas>
		    <schemapath type="mysql">administrator/components/com_admin/sql/updates/mysql</schemapath> 
		    <schemapath type="sqlsrv">administrator/components/com_admin/sql/updates/sqlsrv</schemapath> 
		    <schemapath type="sqlazure">administrator/components/com_admin/sql/updates/sqlazure</schemapath> 
		    <schemapath type="postgresql">administrator/components/com_admin/sql/updates/postgresql</schemapath> 
		    </schemas>
		    </update>
		  - <fileset>
		  - <files>
		    <folder>administrator</folder> 
		    <folder>cache</folder> 
		    <folder>cli</folder> 
		    <folder>components</folder> 
		    <folder>images</folder> 
		    <folder>includes</folder> 
		    <folder>language</folder> 
		    <folder>layouts</folder> 
		    <folder>libraries</folder> 
		    <folder>logs</folder> 
		    <folder>media</folder> 
		    <folder>modules</folder> 
		    <folder>plugins</folder> 
		    <folder>templates</folder> 
		    <folder>tmp</folder> 
		    <file>htaccess.txt</file> 
		    <file>web.config.txt</file> 
		    <file>LICENSE.txt</file> 
		    <file>README.txt</file> 
		    <file>index.php</file> 
		    </files>
		    </fileset>
		  - <updateservers>
		    <server type="collection">http://update.joomla.org/core/list.xml</server> 
		    <server type="collection">http://update.joomla.org/jed/list.xml</server> 
		    </updateservers>
		    </extension>
		"""*/

		return true;
	}

}
