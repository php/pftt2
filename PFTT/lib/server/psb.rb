
require 'webrick'

module Server
  class PSB < WEBrick::HTTPServlet::AbstractServlet

    def do_fbc_report(response, base, test)
      # flip base & test around to make sure base is always older than test
    
      report = Report::Comparison::ByPlatform::FuncBuildComparison.new()
      report.html_print
    end

    def do_fbc_list(response, base=nil)
      # list all functional test runs (if base!=nil, then this is to choose second run)
    
      response.body += <<-TABLE
<table>
<tr>
<td>Run</td>
<td>Start Time</td>
<td>Hosts</td>
<td>Builds</td>
</tr>
</table>
TABLE
    end
  
    def do_GET(request, response)
      response.status = 200
      response['Content-Type'] = 'text/html'
      
      do_header(response)
      if request.query['report'] == 'fbc'
        response.body += "<p>FBC reporting not yet implemented</p>"
# TODO       if request.query['base']
#          if request.query['test']
#            do_fbc_report(response, request.query['base'], request.query['test'])
#          else
#            # show the func run list again, but
#            # have the user pick the second run to compare to
#            do_fbc_list(response, request.query['base'])
#          end
#        else
#          do_fbc_list(response)
#        end
      elsif request.query['report'] == 'pbc'
        # LATER PSB support for PBC reports
        response.body += "<p>PBC reporting not yet implemented</p>"
      elsif request.query['report'] == 'fuzz'
        response.body += "<p>Fuzz reporting not yet implemented</p>"
      elsif request.query['report'] == 'net_view'
        # LATER PSB support for net_view reports
        response.body += "<p>Network report not yet implemented</p>"
      else
        do_home(response) 
      end
      do_footer(response)
    end

    def do_home(response)
      response.body += <<-HOME
<h3>To start, choose Functional or Performance reporting</h3>
<br/>
<h2>PFTT Features</h2>
<ul>
<li>Track changes <b>over time</b> with PSB</li>
<li>Makes testing many OS/Versions <b>quick and convenient</b>, instead of laborious and tedious (helps to avoid bugs like PHP #57578). </li>
<li>Can test across many OS/Versions much earlier and more often <b>decreasing the net cost to achieve and maintain full functionality</b> on an OS/Version</li>
<li>PHP-Core, application devs, analysts and reviewers get a <b>consistent, reproducable system to compare</b> PHP-Core and Apps accross multiple OSs, Versions and Hosting Platforms.</li>
<li>Test hosts only need a network connection and SSH support. PFTT does the rest! </li>
<li>Automating entire test series and <b>repetitive tasks related to manual testing</b>, PFTT provides Faster, efficient and more predictable test cycles!</li>
<li>Configures multiple middlewares, scenarios and PHP builds (TS and NTS) for <b>easy to reproduce setups</b> (auto documents test setups for transparency too!)</li>
<li>Manage revisions and create PHP builds</li>      
</ul>
<br/>
<h2>Terminology</h2>
<ul>
<li><strong>PSB</strong> - PFTT Statisics Browser</li>
<li><strong>FBC</strong> Report - Functional Build Comparison Report
<ul>
<li><strong>FBC-D</strong> Report - For Daily use. One scenario set tested.</li>
<li><strong>FBC-S</strong> Report - For Stable release. All scenarios tested.</li>
</ul>
</li>
<li><strong>PBC</strong> Report - Performance Build Comparison Report
<ul>
<li><strong>PBC-D</strong> Report - For Daily use. One scenario set tested.</li>
<li><strong>PBC-S</strong> Report - For Stable release. All scenarios tested.</li>
</ul>
<li><strong>PFTT</strong>
<ul>
<li><strong>PFTT Server</strong> - Coordinates Clients access to Hosts</li>
<li><strong>PFTT Client</strong> - Executes tests on hosts.</li>
<li><strong>PFTT Host</strong> - Machines that are tested.</li>
</ul>
<li><strong>FGR</strong> Report - Functional Group Run Report (combines multiple FCR reports)</li>
<li><strong>FCR</strong> Report - Functional Combination (Host/Build/Middleware) Run Report</li>
<li><strong>PGR</strong> Report - Performance Group Run Report (combines multiple PCR reports)</li>
<li><strong>PCR</strong> Report - Performance Combination (Host/Build/Middlware) Run Report</li>
<li><strong>FUZ</strong> Report - Fuzz Report</li>
<li><strong>PSC</strong> Protocol - PFTT SSH Communications Protocol enables PFTT clients and hosts (so hosts only need base OS and ssh to be used with PFTT)</li> 
</ul>
<br/>
<p><a href="https://github.com/OSTC/PFTT2" target="_blank"><font color="blue">PFTT Project</font></a></p>
<br/>
HOME
    end

    def do_header(response)
      response.body = <<-HEAD
<html>
<head>
<title>PFTT Statisics Browser</title>
</head>
<body>
<h1>PSB: PFTT Statistics Browser</h1>

HEAD
      do_toolbar(response)
    end
  
    def do_footer(response)
      do_toolbar(response)
      response.body += <<-FOOT
      
</body>
</html>
FOOT
    end
    
    def do_toolbar(response)
      response.body += '<table>      
<tr>
<td><a href="/"><font color="blue">Home</font></a></td>
<td><a href="/?report=fbc"><font color="blue">Functional (FBC)</font></a></td>
<td><a href="/?report=pbc"><font color="blue">Performance (PBC)</font></a></td>
<td><a href="/?report=fuzz"><font color="blue">Fuzz</font></a></td>
<td><a href="/?report=net_view"><font color="blue">Network</font></a></td>
<td><a href="/"><font color="blue">Install PFTT Client</font></a></td>
</tr>
</table>'
    end

  end # end class PSB
end # end module Web
