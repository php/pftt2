package com.mostc.pftt.model.performance;

import com.mostc.pftt.host.AHost;

// XXX some requests may be returned a shorter page (ex: error message, but not detected because
//      the status code is 200... this can happen if its an application not web server error)
//    -this will affect results
//       -ex: database connection couldn't be made 
//    -download page 1 time first, to get the correct size
//       -then compare the size when running wcat or after wcat finished using the average size
//    -this is actually RyanB's technique which he found necessary to get accurate results
abstract class WCATPerformanceTest extends PerformanceTest {

	protected abstract String writeWCATSettings(String scenario_file, int virtual_clients);
	protected abstract String writeWCATScenario();
	
	String set_file, scenario_file
	void runTest() {
		
	}
	
	void prepareWCAT(AHost host) {
		set_file = host.mktempname(getClass(), ".wcat");
		
		scenario_file = host.mktempname(getClass(), ".wcat");
		
		writeWCATScenario();
		
		writeWCATSettings(scenario_file, 4); // TODO specify virtual clients (4, 8, 16, or 32)
		
		// TODO write strings to files
	}
	
	void cleanupWCAT() {
		
	}
	
	void runWCAT(AHost host) {
/*
// hostnames to run wcat clients on
// $CLIENTS = "php-load01,php-load02"
// $SETFILE = writeWCATSettings
// $OUT_FILE = temp file to write summary to
// hostname of target web server
// $SERVER = "php-web01"
// $_
//
		c:\wcat\wcat.wsf -terminate -run -v $_ -clients $CLIENTS -f $SETFILE -s $SERVER -o $OUT_FILE
*/
	}
	
	void readWCATSummary() {
/*
#%powershell1.0%
#
# File: summarize-results.ps1
# Description: Summarize the results output by wcat_run.ps1 and output them into an HTML-formatted file.
#
# Example Usage:
# c:\> summarize-results.ps1 -PHP1 5.3.8 -PHP2 5.4.0B2 -VIRTUAL "8,16,32"
#

Param($PHP1="", $PHP2="", $VIRTUAL="8,16,32")

Set-Location c:\wcat
$results = 'c:/wcat/results/'
$errlog = ""
$VIRTUAL = $VIRTUAL.split(',')

Function initvars  {
	$script:appnames = @("Helloworld", "Drupal", "Mediawiki", "Wordpress", "Joomla")
	$script:data = @{}

	Foreach ($app in $appnames) {
		$script:data.add($app, "")
		$script:data[$app] = @{	"IIS" = 
								@{	"nocache" = 
									@{	"php1" =
										@{	"tps$($VIRTUAL[0])" = [int]0;
											"tps$($VIRTUAL[1])" = [int]0;
											"tps$($VIRTUAL[2])" = [int]0;
											"err$($VIRTUAL[0])" = [int]0;
											"err$($VIRTUAL[1])" = [int]0;
											"err$($VIRTUAL[2])" = [int]0;
										};
									"php2" =
										@{	"tps$($VIRTUAL[0])" = [int]0;
											"tps$($VIRTUAL[1])" = [int]0;
											"tps$($VIRTUAL[2])" = [int]0;
											"err$($VIRTUAL[0])" = [int]0;
											"err$($VIRTUAL[1])" = [int]0;
											"err$($VIRTUAL[2])" = [int]0;
										}
									};
									"cache" = 
									@{	"php1" =
										@{	"tps$($VIRTUAL[0])" = [int]0;
											"tps$($VIRTUAL[1])" = [int]0;
											"tps$($VIRTUAL[2])" = [int]0;
											"err$($VIRTUAL[0])" = [int]0;
											"err$($VIRTUAL[1])" = [int]0;
											"err$($VIRTUAL[2])" = [int]0;
										};
									"php2" =
										@{	"tps$($VIRTUAL[0])" = [int]0;
											"tps$($VIRTUAL[1])" = [int]0;
											"tps$($VIRTUAL[2])" = [int]0;
											"err$($VIRTUAL[0])" = [int]0;
											"err$($VIRTUAL[1])" = [int]0;
											"err$($VIRTUAL[2])" = [int]0;
										}
									}
								};
								"Apache" = 
								@{	"nocache" = 
									@{	"php1" =
										@{	"tps$($VIRTUAL[0])" = [int]0;
											"tps$($VIRTUAL[1])" = [int]0;
											"tps$($VIRTUAL[2])" = [int]0;
											"err$($VIRTUAL[0])" = [int]0;
											"err$($VIRTUAL[1])" = [int]0;
											"err$($VIRTUAL[2])" = [int]0;
											};
										"php2" =
											@{	"tps$($VIRTUAL[0])" = [int]0;
												"tps$($VIRTUAL[1])" = [int]0;
												"tps$($VIRTUAL[2])" = [int]0;
												"err$($VIRTUAL[0])" = [int]0;
												"err$($VIRTUAL[1])" = [int]0;
												"err$($VIRTUAL[2])" = [int]0;
											}
									};
									
									"cachenoigbinary" =
									@{	"php1" =
											@{	"tps$($VIRTUAL[0])" = [int]0;
												"tps$($VIRTUAL[1])" = [int]0;
												"tps$($VIRTUAL[2])" = [int]0;
												"err$($VIRTUAL[0])" = [int]0;
												"err$($VIRTUAL[1])" = [int]0;
												"err$($VIRTUAL[2])" = [int]0;
											};
										"php2" =
											@{	"tps$($VIRTUAL[0])" = [int]0;
												"tps$($VIRTUAL[1])" = [int]0;
												"tps$($VIRTUAL[2])" = [int]0;
												"err$($VIRTUAL[0])" = [int]0;
												"err$($VIRTUAL[1])" = [int]0;
												"err$($VIRTUAL[2])" = [int]0;
											}
									};
									
									"cachewithigbinary" =
									@{	"php1" =
											@{	"tps$($VIRTUAL[0])" = [int]0;
												"tps$($VIRTUAL[1])" = [int]0;
												"tps$($VIRTUAL[2])" = [int]0;
												"err$($VIRTUAL[0])" = [int]0;
												"err$($VIRTUAL[1])" = [int]0;
												"err$($VIRTUAL[2])" = [int]0;
											};
										"php2" =
											@{	"tps$($VIRTUAL[0])" = [int]0;
												"tps$($VIRTUAL[1])" = [int]0;
												"tps$($VIRTUAL[2])" = [int]0;
												"err$($VIRTUAL[0])" = [int]0;
												"err$($VIRTUAL[1])" = [int]0;
												"err$($VIRTUAL[2])" = [int]0;
											}
									}
								}
							}
	}  ## End Foreach
}  ## End Function


## Initialize hash table
## $data[App_Name][Apache|IIS][cache|nocache|cachenoigbinary|cachewithigbinary][php1|php2][tps8|tps16|tps32]
initvars

Get-ChildItem -recurse $results | Where-Object { $_.Name -match '\.dat' } | ForEach-Object  {

	## Determine web server, cache/nocache and application from file name
	## i.e. Apache-PHP5.4.0B2-Apache-Cache-Drupal.summary.dat
	$dname = $_.Name.split("-")
	$websvr = $dname[0]
	$phpver = $dname[1] -ireplace "PHP", ""
	$cache = $dname[3].tolower()
	$appname = $dname[4] -ireplace "\.\w+", ""

	if ( $phpver -eq $PHP1 )  {  $phpver = "php1"  }
	elseif ( $phpver -eq $PHP2 )  {  $phpver = "php2"  }
	else  {  continue  }

	$contents = (get-content $_.FullName)
	Foreach ( $line in $contents )  {
		if ( $line -match "\.xml" )  {
			##																		  tps,  kcpt, bpt,   cpu, err
			## i.e. php-web01-Apache-2clnt-08vrtu-PHP5.4.0B2-Apache-Cache-Drupal.xml, 40.7, 0.0,  10670, 0.0, 0
			$line = $line.split(",")
			$tps = $line[1].trim()
			$err = $line[5].trim()
			$virt = $line[0].split("-")
			$virt = $virt[4]
			$virt = $virt -ireplace "vrtu", ""
			$virt = $virt -ireplace "^0", ""

			## write-output "$appname $websvr $cache $phpver tps$virt"
			$data[$appname][$websvr][$cache][$phpver]["tps$virt"] = $tps
			$data[$appname][$websvr][$cache][$phpver]["err$virt"] = $err
			if ( $err -gt 0 )  {
				$logfile = "c:/wcat/autocat-log.txt"
				$msg = (get-date -format "yyyy-MM-dd HH:mm:ss")+" $appname, $websvr, $cache, $dname[1]`: $err"
				$msg | Out-File -Encoding ASCII -Append $logfile
				$errlog += "$appname, $websvr, $cache, "+$dname[1]+": $err <br/>`n"
			}
		}
	}
}


## Finally, output the results template
. ".\results-template.ps1"


		
 */
	}
	
}
