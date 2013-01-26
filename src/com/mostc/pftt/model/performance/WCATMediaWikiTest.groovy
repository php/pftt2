package com.mostc.pftt.model.performance;

class WCATMediaWikiTest extends WCATPerformanceTest {
	
	@Override
	public String getName() {
		return "WCAT-MediaWiki";
	}
	
	@Override
	String writeWCATScenario() {
"""
scenario
{
    name    = "Mediawiki";

    warmup      = 20;
    duration    = 120;
    cooldown    = 10;

    /////////////////////////////////////////////////////////////////
    //
    // All requests inherit the settings from the default request.
    // Defaults are overridden if specified in the request itself.
    //
    /////////////////////////////////////////////////////////////////
    default
    {
        // send keep-alive header
        setheader
        {
            name    = "Connection";
            value   = "keep-alive";
        }

        // set the host header
        setheader
        {
            name    = "Host";
            value   = server();
        }

        // HTTP1.1 request
        version     = HTTP11;

        // keep the connection alive after the request
        close       = ka;
    }

    transaction
    {
        id = "Web Site Homepage";
        weight = 1;

        request
        {
			url			= "/mediawiki/index.php?title=Main_Page";
			statuscode	= 200;
        	//cookies	= false;
        }

        //
        // specifically close the connection after both files are requested
        //
        close
        {
            method      = ka;
        }
    }
}
"""
	} // end String writeWCATScenario
	
	@Override
	String writeWCATSettings(String scenario_file, int virtual_clients) {
"""
settings
{
    //--------------------------------------------------------------------------
    // General controller settings
    //
    //  clientfile     - specifies the client file, relative to working dir
    //  server         - host name of the webserver
    //  virtualclients - number of 'threads' per physical client
    //  clients        - number of physical webcat client machines
    //
    //--------------------------------------------------------------------------
    // Example:
	// clientfile	  = "test.wcat";
    // server         = "131.107.145.189,131.107.145.190";
    // clients        = 2;
    // virtualclients = 4;
	// bind = "php-load01,php-load02";
    //
    //--------------------------------------------------------------------------

    virtualclients = $virtual_clients;
    clientfile     = "$scenario_file";
	
    //--------------------------------------------------------------------------
    // Performance counters (pass '-x' option to wcctl.exe to enable)
    //
    //  interval        - polling interval in seconds (default=10)
    //  host            - host name of machine to monitor (default=webserver)
    //  counter         - path of counter to monitor
    //
    //--------------------------------------------------------------------------
    // Optional:
    //
    //   Additional machines can be monitored by adding more counters blocks.
    //
    // Example:
    //
    //   counters {
    //       host = "sqlserver";   // name of remote machine
    //       interval = 5;
    //       counter = "...";
    //   }
    //
    //--------------------------------------------------------------------------

    counters
    {
        interval = 10;

        counter = "Processor(_Total)\\% Processor Time";
        counter = "Processor(_Total)\\% Privileged Time";
        counter = "Processor(_Total)\\% User Time";
        counter = "Processor(_Total)\\Interrupts/sec";

        counter = "Memory\\Available KBytes";

        counter = "Process(w3wp)\\Working Set";

        counter = "System\\Context Switches/sec";
        counter = "System\\System Calls/sec";

        counter = "Web Service(_Total)\\Bytes Received/sec" ; 
        counter = "Web Service(_Total)\\Bytes Sent/sec" ; 
        counter = "Web Service(_Total)\\Connection Attempts/sec" ; 
        counter = "Web Service(_Total)\\Get Requests/sec" ; 
    }

    //--------------------------------------------------------------------------
    // Registry Key Monitors (pass '-x' option to wcctl.exe to enable)
    //
    //  path - registry path, relative to HKLM
    //  name - name of registry key
    //  type - type of value (REG_SZ | REG_DWORD)
    //
    //--------------------------------------------------------------------------
    // Optional:
    //
    //   Additional registry keys can be monitored on the web server by
    //   adding more registry blocks to this file.  Note that simple strings and
    //   dwords are all that webcat currently supports.
    //
    // Example:
    //
    //   registry {
    //     path = "System\\CurrentControlSet\\Services\\Tcpip\\Parameters";
    //     name = "DhcpDomain";
    //     type = REG_SZ;
    //   }
    //
    //--------------------------------------------------------------------------

    registry
    {
        path = "System\\CurrentControlSet\\Control\\FileSystem";
        name = "NtfsDisableLastAccessUpdate";
        type = REG_DWORD;
    }

    registry
    {
        path = "System\\CurrentControlSet\\Services\\Tcpip\\Parameters";
        name = "SynAttackProtect";
        type = REG_DWORD;
    }
}
"""
	} // end String writeWCATSettings
	
} // end class WCATMediaWikiTest
