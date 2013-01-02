package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

/** A free and open-source content management framework (CMF) written in PHP and distributed under
 * the GNU General Public License. It is used as a back-end system for at least 2.1% of
 * all websites worldwide ranging from personal blogs to corporate, political, and government
 * sites including whitehouse.gov and data.gov.uk. It is also used for knowledge management and
 * business collaboration.
 * 
 * @see https://drupal.org/
 * 
 */

public class DrupalScenario extends ZipDbApplication {

	@Override
	public String getName() {
		return "Drupal";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	protected String getZipAppFileName() {
		return "drupal-7.18.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		
		host.open("drupal/web.config");
		def xml = """
<?xml version="1.0" encoding="UTF-8"?>
		<configuration>
		  <system.webServer>
		    <!-- Don't show directory listings for URLs which map to a directory. -->
		    <directoryBrowse enabled="false" />
		    <rewrite>
		      <rules>
		        <rule name="Protect files and directories from prying eyes" stopProcessing="true">
		          <action type="CustomResponse" statusCode="403" subStatusCode="0" statusReason="Forbidden" statusDescription="Access is forbidden." />
		        </rule>
		        <rule name="Force simple error message for requests for non-existent favicon.ico" stopProcessing="true">
		          <match url="favicon\\.ico" />
		          <action type="CustomResponse" statusCode="404" subStatusCode="1" statusReason="File Not Found" statusDescription="The requested file favicon.ico was not found" />
		        </rule>
		        <!-- Rewrite URLs of the form 'x' to the form 'index.php?q=x'. -->
		        <rule name="Short URLs" stopProcessing="true">
		          <conditions>
		            <add input="{REQUEST_FILENAME}" matchType="IsFile" ignoreCase="false" negate="true" />
		            <add input="{REQUEST_FILENAME}" matchType="IsDirectory" ignoreCase="false" negate="true" />
		          </conditions>
		          <action type="Rewrite" url="index.php?q={R:1}" appendQueryString="true" />
		        </rule>
		      </rules>
		    </rewrite>

		    <httpErrors>
		      <remove statusCode="404" subStatusCode="-1" />
		      <error statusCode="404" prefixLanguageFilePath="" path="/index.php" responseMode="ExecuteURL" />
		    </httpErrors>

		    <defaultDocument>
		      <!-- Set the default document -->
		      <files>
		        <remove value="index.php" />
		        <add value="index.php" />
		      </files>
		    </defaultDocument>
		  </system.webServer>
		</configuration>
"""

		return false;
	}

}
