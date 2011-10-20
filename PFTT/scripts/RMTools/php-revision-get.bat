0</* :Begin Polyglot shebang
@cscript /nologo /E:jscript %~f0 %*
@goto :EOF
:End Polyglot shebang*/0;

function getHelp() {
	return [
		'A tool for interfacing with PHP\'s RMTOOLS from the Windows',
		'command line. Helpful for automating the download of snapshots and',
		'per-revision binaries in QA and testing.',
		'', 
		'PRE-REQS:', 
		'    For extracting the .zip downloads an unzip utility must be in your PATH.', 
		'    Supported unzip utilities include: [ 7z, unzip, jzip ]', 
		'', 
		'    For downloading, the presence of `curl` in your PATH will allow ', 
		'    simultaneous downloads, and will do so with less memory overhead than the ', 
		'    built-in `MSXML2.XMLHTTP`.',
		'',
		'USAGE:',
		'    php-revision-get help',
		'        Get this help',
		'',
		'    php-revision-get info [<RESTRAINTS>] [<COMMON>]',
		'        Poll the remote JSON and return a list ',
		'        of available builds.',
		'',
		'    php-revision-get bin[ary|ies|s] [<RESTRAINTS>] [<DOWNLOAD>] [<COMMON>]',
		'        Download the specified binaries',
		'',
		'COMMON OPTIONS',
		'    --format=<json|xml>',
		'        Specify the format of output.',
		'        Note that some errors cannot be caught and output nicely,',
		'        and some errors might occur before the format gets set. Write',
		'        your consumers to catch non-expected output nicely.',
		'        Default format is json.',
		'        --xml               alias of --format=xml',
		'        --json              alias of --format=json',
		'',
		'    --base-url=<URL>',
		'        Set the base URL. Defaults to: ',
		'        ['+config.baseurl+']',
		'',
		'RESTRAINTS',
		'    This tool automatically assumes no restraints other than what you',
		'    explicitly specify. This means that it will select all available ',
		'    builds from the latest revision of each branch unless you tell it ',
		'    otherwise. Below are the available restraints to apply.',
		'',
		'    --revision=[<COMPARRISON>]<NUMBER>',
		'        Limit to a specific revision or to revisions that match a ',
		'        comparrison operator, such as "greater than", where optional ',
		'        COMPARRISON is "<" or ">". Notice that there is no space ',
		'        between the COMPARRISON operator and the NUMBER.',
		'        --newer-than=<NUMBER>  alias of --revision="><NUMBER>',
		'',
		'    --branch=<STRING>',
		'        Limit to a specific branch, where STRING is a valid branch ',
		'        built with RMTOOLS.',
		'        --5.3                 alias of --branch="5.3"',
		'        --trunk               alias of --branch="trunk"',
		'',
		'    --threadsafety=<BOOLISH>',
		'        Limit threadsafetiness to either ON or OFF',
		'        --ts                  alias of --threadsafety=ON',
		'        --nts                 alias of --threadsafety=OFF',
		'',
		'    --compiler=<STRING>',
		'        Limit to a specific compiler.',
		'        --vc6                 alias of --compiler="vc6"',
		'        --vc9                 alias of --compiler="vc9"',
		'',
		'    --architecture=<STRING>',
		'        Limit to a specific architecture',
		'        --x86                 alias of --architecture="x86"',
		'',
		'    --platform=<STRING>',
		'        Limit to a specific platform',
		'        --windows             alias of --platform="windows"',
		'',
		'    --build-type=<STRING>',
		'        Get a devel or debug package insteead of a standard build.',
		'        NOTE: Unlike other restrinats, this one comes with a default',
		'        value; to remove the restraint, use `--build-type="ALL"`.',
		'        --standard            alias of --build-type="standard"',
		'        --debug               alias of --build-type="debug"',
		'        --devel               alias of --build-type="devel"',
		'',
		'DOWNLOAD OPTIONS',
		'    When running a function that downloads builds or source snapshots,',
		'    these options help specify how to handle the output.',
		'',
		'    --output-directory=<PATH>',
		'        Specify a directory in which to save downloaded items.',
		'        If an output directory is not specified, CWD is used.',
		'        --output-dir=<PATH>   alias of --output-directory=<PATH>',
		'',
		'    --unzip[=<BOOLISH>]',
		'        Specify whether or not to unzip any downloaded .zip archives.',
		'        Default behavior is to automatically unzip.',
		'        --no-unzip            alias of --unzip="OFF"',
		'        --disable-unzip       alias of --unzip="OFF"',
		'',
		'    --cleanup[=<BOOLISH>]',
		'        Specify whether or not to remove an archive that has been ',
		'        extracted. If no value is present, TRUE is assumed.',
		'        Default behavior is to automatically cleanup.',
		'        --no-cleanup          alias of --cleanup="OFF"',
		'        --disable-cleanup     alias of --cleanup="OFF"',
		''
	].join('\n');
}

// Set up some useful constants
var FOR_READING = 1
,	FOR_WRITING = 2
,	FOR_APPENDING = 8
,	$FSO = new ActiveXObject("Scripting.FileSystemObject")
,	$WSH_SHELL = new ActiveXObject("WScript.Shell")
,	$EXEC_AND_CLOSE = function(command){
		var process = $WSH_SHELL.Exec( command );
		while( process.Status ) WScript.Sleep(50);
	}
;
function WL(line) { WScript.Stdout.WriteLine(line);return line;}

// Create an Error class that can be output with the chosen format
function FormattedError(message) {
    this.name = "Error";
    this.message = (message || "");
	this.toString = function() {
		var format = config.format || 'xml'
		,	output = [{name:this.name,message:this.message}]
		;
		return getFormatted( output, ['errors','error'] );
	}
}
FormattedError.prototype = new Error;

/** @begin inclusion of json2.js
 * Include compressed JSON stringifier and parser - 2011-02-07
 * json2.js, available from https://github.com/douglascrockford/JSON-js/blob/master/json2.js 
 * under the MIT license:
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
eval(function(p,a,c,k,e,d){e=function(c){return(c<a?'':e(parseInt(c/a)))+((c=c%a)>35?String.fromCharCode(c+29):c.toString(36))};if(!''.replace(/^/,String)){while(c--){d[e(c)]=k[c]||e(c)}k=[function(e){return d[e]}];e=function(){return'\\w+'};c=1};while(c--){if(k[c]){p=p.replace(new RegExp('\\b'+e(c)+'\\b','g'),k[c])}}return p}('x h;3(!h){h={}}(5(){"1y 1W";5 f(n){7 n<10?\'0\'+n:n}3(6 1p.z.w!==\'5\'){1p.z.w=5(l){7 1a(q.17())?q.1Z()+\'-\'+f(q.1J()+1)+\'-\'+f(q.1w())+\'T\'+f(q.1C())+\':\'+f(q.1E())+\':\'+f(q.1H())+\'Z\':C};Q.z.w=1S.z.w=1O.z.w=5(l){7 q.17()}}x N=/[\\1z\\1r\\1o-\\1h\\1g\\1f\\1e\\1i-\\1j\\1n-\\1m\\1l-\\1k\\1d\\15-\\14]/g,L=/[\\\\\\"\\1A-\\1x\\1Y-\\1G\\1r\\1o-\\1h\\1g\\1f\\1e\\1i-\\1j\\1n-\\1m\\1l-\\1k\\1d\\15-\\14]/g,8,H,13={\'\\b\':\'\\\\b\',\'\\t\':\'\\\\t\',\'\\n\':\'\\\\n\',\'\\f\':\'\\\\f\',\'\\r\':\'\\\\r\',\'"\':\'\\\\"\',\'\\\\\':\'\\\\\\\\\'},m;5 O(p){L.1b=0;7 L.12(p)?\'"\'+p.E(L,5(a){x c=13[a];7 6 c===\'p\'?c:\'\\\\u\'+(\'1t\'+a.1u(0).11(16)).1s(-4)})+\'"\':\'"\'+p+\'"\'}5 D(l,A){x i,k,v,e,K=8,9,2=A[l];3(2&&6 2===\'y\'&&6 2.w===\'5\'){2=2.w(l)}3(6 m===\'5\'){2=m.M(A,l,2)}1T(6 2){J\'p\':7 O(2);J\'S\':7 1a(2)?Q(2):\'C\';J\'1V\':J\'C\':7 Q(2);J\'y\':3(!2){7\'C\'}8+=H;9=[];3(W.z.11.1X(2)===\'[y 1R]\'){e=2.e;G(i=0;i<e;i+=1){9[i]=D(i,2)||\'C\'}v=9.e===0?\'[]\':8?\'[\\n\'+8+9.P(\',\\n\'+8)+\'\\n\'+K+\']\':\'[\'+9.P(\',\')+\']\';8=K;7 v}3(m&&6 m===\'y\'){e=m.e;G(i=0;i<e;i+=1){k=m[i];3(6 k===\'p\'){v=D(k,2);3(v){9.1c(O(k)+(8?\': \':\':\')+v)}}}}U{G(k 1q 2){3(W.1v.M(2,k)){v=D(k,2);3(v){9.1c(O(k)+(8?\': \':\':\')+v)}}}}v=9.e===0?\'{}\':8?\'{\\n\'+8+9.P(\',\\n\'+8)+\'\\n\'+K+\'}\':\'{\'+9.P(\',\')+\'}\';8=K;7 v}}3(6 h.V!==\'5\'){h.V=5(2,B,I){x i;8=\'\';H=\'\';3(6 I===\'S\'){G(i=0;i<I;i+=1){H+=\' \'}}U 3(6 I===\'p\'){H=I}m=B;3(B&&6 B!==\'5\'&&(6 B!==\'y\'||6 B.e!==\'S\')){19 18 1U(\'h.V\')}7 D(\'\',{\'\':2})}}3(6 h.Y!==\'5\'){h.Y=5(o,R){x j;5 X(A,l){x k,v,2=A[l];3(2&&6 2===\'y\'){G(k 1q 2){3(W.1v.M(2,k)){v=X(2,k);3(v!==1B){2[k]=v}U{1F 2[k]}}}}7 R.M(A,l,2)}o=Q(o);N.1b=0;3(N.12(o)){o=o.E(N,5(a){7\'\\\\u\'+(\'1t\'+a.1u(0).11(16)).1s(-4)})}3(/^[\\],:{}\\s]*$/.12(o.E(/\\\\(?:["\\\\\\/1L]|u[0-1N-1M-F]{4})/g,\'@\').E(/"[^"\\\\\\n\\r]*"|1K|1I|C|-?\\d+(?:\\.\\d*)?(?:[1D][+\\-]?\\d+)?/g,\']\').E(/(?:^|:|,)(?:\\s*\\[)+/g,\'\'))){j=1Q(\'(\'+o+\')\');7 6 R===\'5\'?X({\'\':j},\'\'):j}19 18 1P(\'h.Y\')}}}());',62,124,'||value|if||function|typeof|return|gap|partial|||||length|||JSON||||key|rep||text|string|this||||||toJSON|var|object|prototype|holder|replacer|null|str|replace||for|indent|space|case|mind|escapable|call|cx|quote|join|String|reviver|number||else|stringify|Object|walk|parse|||toString|test|meta|uffff|ufff0||valueOf|new|throw|isFinite|lastIndex|push|ufeff|u17b5|u17b4|u070f|u0604|u200c|u200f|u206f|u2060|u202f|u2028|u0600|Date|in|u00ad|slice|0000|charCodeAt|hasOwnProperty|getUTCDate|x1f|use|u0000|x00|undefined|getUTCHours|eE|getUTCMinutes|delete|x9f|getUTCSeconds|false|getUTCMonth|true|bfnrt|fA|9a|Boolean|SyntaxError|eval|Array|Number|switch|Error|boolean|strict|apply|x7f|getUTCFullYear'.split('|'),0,{}));
/** @end inclusion of js2.js */


function arrayOfObjectsToXml( objs, clues ) {
	var dom = new ActiveXObject('msxml2.DomDocument.6.0');
	dom.appendChild( dom.createProcessingInstruction("xml","version='1.0'") );
	var root = dom.createElement(clues[0]);
	dom.appendChild(root);

	for( var i = 0; i < objs.length; i++ ) {
		var branch = dom.createElement(clues[1]);
		for( var k in objs[i] ) {
			var attr = dom.createAttribute( k );
			attr.value = ( typeof(objs[i][k])=='boolean' ? ( objs[i][k] ? 1 : 0 ) : objs[i][k] );
			branch.setAttributeNode( attr );
		}
		root.appendChild( branch );
	}
	
	return dom.xml;
}
/**
 * Write a comment to output in the configured format, such that syntax is not broken.
 */
function writeComment( str ) {
	if( typeof( str ) != 'string' ) str = ''+str;
	switch( config.format ) {
		case 'xml':
			WL( '<!'+'--'+str.replace(/-->/g, '\-\-\>/') + '--'+'>' );
			break;
		case 'json':
			WL( '/'+'*'+str.replace(/\*\//g, '\*\/') + '*'+'/' );
			break;
		default:
			throw new Error('format ['+config.format+'] not recognized.\n'+var_dump(obj));
	}
}

function getFormatted( obj, clues ){
	switch( config.format ) {
		case 'xml':
			return arrayOfObjectsToXml( obj, clues );
		case 'json':
			return JSON.stringify( obj );
		default:
			throw new Error('format ['+config.format+'] not recognized.\n'+var_dump(obj));
	}
}
function writeFormatted( obj, clues ) {
	WL( getFormatted( obj, clues ) );
}

function which( exeFullName, additionalPaths ) {
	additionalPaths = additionalPaths || [];
    if( $FSO.FileExists( exeFullName ) )
        return exeFullName;
	
	for( var each in paths = ( $WSH_SHELL.Environment( 'Process' )('PATH').split(';').concat( additionalPaths ) ) ) {
        var testPath = $FSO.BuildPath(paths[each], exeFullName );
        if ($FSO.FileExists(testPath)) {
            return $FSO.GetAbsolutePathName(testPath);
        }
    }
    return false;
}

var Utility = (function(){
	function key_match( matcher, key ){
		if( matcher instanceof Array ){
			return ( key_match( matcher[0], test ) ? matcher[1] : false)
		} else if( matcher instanceof RegExp ){
			return (
				matcher.test( key ) ?
				key :
				false
			)
		} else if( typeof matcher === "function" ) {
			return matcher( key );
		} else {
			return matcher == key;
		}
	}

	function filter_object_properties( obj, filtertype, filter ){
		var ret = {}
		for( var property in obj ){
			if( !obj.hasOwnProperty( property ) ) continue;
			for( var i = 0; i< filter.length; i++ ){
				var match = key_match( filter[i], property )
				if(match) break;
			}
			if( (filtertype=='only') && match ) ret[match] = obj[property];
			else if ( (filtertype=='except') && !match ) ret[property] = obj[property];
			else if ( (filtertype=='filter') ) ret[ match || property ] = obj[property];
		}
		if(obj.prototype) ret.prototype = obj.prototype
		
		return ret;
	}

	function filter_objects_in_an_array( objArr, filtertype, filter ){
		var ret = [];
		for( var i = 0; i < objArr.length; i++)
			ret.push( filter_object_properties( objArr[i], filtertype, filter ) )
		return ret;
	}

	function filter_objects_out_of_array_by_properties( objArr, propname, propval, func ){
		func = func || function(a,b){return (a==b);};
		var retArr =[];
		for( var i = 0; i < objArr.length; i++ ){
			if( propval instanceof Comparrison || propval instanceof RegExp){
				if( !propval.test( objArr[i][propname] ) ) continue;
			} else if( objArr[i][propname] != propval ) {
				continue;
			}
			retArr.push( objArr[i] );
		}
		return retArr;
	}

	return {
		FilterObjectProperties: filter_object_properties,
		FilterObjectPropertiesInArray: filter_objects_in_an_array,
		FilterArrayOfObjectsByProperties: filter_objects_out_of_array_by_properties
	}
})();

var Comparrison = function( operator, value ){
	this.value = value;
	this.operator = operator;
	this.toString = function(){
		return '[Comparrison: '+this.operator+' '+this.value+']';
	}
	this.test = new Function('a','return (a '+this.operator+' this.value);')
}

var PhpRevisionInfo = (function(){

	var build_type_map = {
		standard: { bool:'has_php_pkg',   string:'php'            },
		debug:    { bool:'has_debug_pkg', string:'php-debug-pack' },
		devel:    { bool:'has_devel_pkg', string:'php-devel-pack' }
	}

	function retrieve_data( url ){
		var HTTP = new ActiveXObject('MSXML2.XMLHTTP');
		// append a timestamp to the query string to avoid caching.
		url = url + ( ( url.indexOf('?')==-1 ) ? '?' : '&' ) + 'now=' + (new Date()).valueOf()
		
		try {
			HTTP.open( 'GET', url, false );
			HTTP.send();
		} catch (e) {
			throw new FormattedError( 'URL cannot be reached: [' + url + ']' );
		}
		if( HTTP.Status != 200 )
			throw new FormattedError( [
							'Server responded, but could not return requested URL:'
							,'['+ HTTP.Status + '][' + url +']'
							, HTTP.ResponseText
							].join('\n')
						);
		try {
			return JSON.parse(HTTP.ResponseText);
		} catch(e) {
			throw new FormattedError( 'Could not parse JSON returned.' )
		}
	}

	function filter_object_properties( obj, params ){
		var ret = {}
		if(obj.prototype) ret.prototype = obj.prototype
		for( var property in obj ){
			if( !obj.hasOwnProperty( property ) ) continue;


		}
	}

	function get_branches( baseurl ){
		return [
			{name:'5.3'},
			{name:'trunk'}
		]
	}

	function get_buildnames( baseurl, branch ){
		buildnames = retrieve_data( [ baseurl, 'php-'+branch, 'php-'+branch+'.json' ].join('/') ).builds;
		var ret = []
		while( buildnames.length ){
			var build = {
				name: buildnames.shift()
			}
			buildsplit = build.name.split('-');
			build.threadsafe = /^z?ts$/i.test(buildsplit[0])
			build.platform = buildsplit[1]
			build.compiler = buildsplit[2]
			build.architecture = buildsplit[3]
			ret.push( build );
		}
		return ret;
	}

	function get_build_revisions( baseurl, branch ){
		return Utility.FilterObjectProperties( 
			retrieve_data( [ baseurl, 'php-'+branch, 'php-'+branch+'.json' ].join('/') ),
			'only',
			[ /^revision/ ]
		);
	}

	function get_build_status( baseurl, branch, revision, buildname ){
		return(
			retrieve_data( [ baseurl, 'php-'+branch, 'r'+revision, buildname+'.json' ].join('/') )
		);
	}

	function get_all( baseurl, restraints ){
		var branches = ( 'branch' in restraints ? [{name:restraints.branch}] : get_branches( baseurl ) )
		,	revision_info = []
		;
		for( var br = 0; br < branches.length; br++ ){
			var revision
			,	buildnames = get_buildnames( baseurl, branches[br].name )
			;

			if( 'revision' in restraints && restraints.revision.operator == '==' ){
				revision = restraints.revision.value;
			} else { 
				revision = get_build_revisions( baseurl, branches[br].name ).revision_last;
			}

			for( var bn = 0; bn < buildnames.length; bn++ ){
				for( var build_type in build_type_map ){
					if( ('build_type' in restraints) && build_type != restraints.build_type ) continue;
					var build_status = get_build_status( baseurl, branches[br].name, revision, buildnames[bn].name )
					var build = {
						name: buildnames[bn].name,
						build_type: build_type,
						revision: revision,
						branch: branches[br].name,
						built: build_status.has_php_pkg,
						stats: build_status.stats,
						threadsafe: buildnames[bn].threadsafe,
						platform: buildnames[bn].platform,
						compiler: buildnames[bn].compiler,
						architecture: buildnames[bn].architecture
					}
					if( build_status[build_type_map[build.build_type].bool] ){
						build.url = [ 
							baseurl, 
							'php-'+build.branch, 
							'r'+build.revision, 
							[ 
								build_type_map[build.build_type].string, 
								build.branch, 
								build.name, 
								'r'+revision 
							].join('-')+'.zip'
						].join('/');
					}
					build.snapshot = [
							baseurl, 
							'php-'+build.branch, 
							'r'+build.revision, 
							[ 'php-'+build.branch, 'src', 'r'+revision ].join('-')+'.zip'
						].join('/');
					revision_info.push( build );
				}
			}
		}
		return revision_info;
	}
		
	return {
		GetBranches: get_branches,
		GetBuildNames: get_buildnames,
		GetBuildRevisions: get_build_revisions,
		GetBuildStatus: get_build_status,
		Get: get_all
	}
})()


var DownloadObjects = (function(){
	function mark_started( download, base_dir ){
		download.local_archive = $FSO.GetAbsolutePathName($FSO.BuildPath( base_dir, download.url.split(/[\\\/]/g).slice(-1) ));
		download.downloading = true;
		download.done = false;
	}
	function mark_complete( download ){
		download.downloading = false;
		download.done = true;
	}
	function mark_duplicate( download ){
		download.duplicate = true;
	}
	function unmark( download ){
		delete download.downloading;
		delete download.done;
		delete download.duplicate;
	}

	function save_via_curl( downloads, base_dir ){
		var start_time = (new Date()).valueOf()
		,	progress = {}
		;

		if(!$FSO.FolderExists( base_dir ))
			$EXEC_AND_CLOSE('cmd /C "mkdir "'+base_dir+'""');

		for( var i=0; i < downloads.length; i++ ){
			var download = downloads[i];
			if( download.url in progress ){
				mark_duplicate( download );
				continue;
			} 
			progress[download.url]=true;
			mark_started( download, base_dir )
			download.worker = $WSH_SHELL.Exec( 
				'curl -q --fail --retry 3 -o "'+download.local_archive+'" "'+download.url
			);
		}

		function at_least_one_download_in_progress(){
			for( var i=0; i < downloads.length; i++ ){
				if( 'downloading' in downloads[i] && downloads[i].downloading ) return true;
			}
			return false;
		}

		while( at_least_one_download_in_progress() && ( (new Date()).valueOf() < start_time + ( 3*60*1000 ) ) ){
			for( var i=0; i < downloads.length; i++ ){
				var download = downloads[i];
				if( download.done || download.duplicate ) {
					continue;
				}
				if( download.worker.Status ){
					mark_complete( download );
					delete download.worker;
					continue;
				}
			}
			WScript.Sleep( 500 );
		}

		for( var i=0; i < downloads.length; i++ ){
			unmark( downloads[i] );	
		}
		return downloads;
	}

	function save_via_xmlhttp( downloads, base_dir ){ // a fallback for if CURL is not in the path.
		writeComment('WARNING: curl.exe not found. '+
			'\n\tUsing MSXML2.XMLHTTP instead, which cannot handle concurrent connections '+
			'\n\tand has significantly higher memory overhead. To use curl, ensure that it '+
			'\n\tis in your %PATH%.'+
			'\n'
		);
		var transport = new ActiveXObject('MSXML2.XMLHTTP')
		;
		
		if(!$FSO.FolderExists( base_dir ))
			$EXEC_AND_CLOSE('cmd /C "mkdir "'+base_dir+'""');
			
		for( var i = 0; i < downloads.length; i++ ){
			mark_started( downloads[i], base_dir );
			with ( transport ) {
				try {
					open( 'GET', downloads[i].url, false );
					send();
				} catch (e) {
					throw new FormattedError( 'Address cannot be reached: [' + downloads[i].url + ']' );
				}
				if( Status != 200 )
					throw new FormattedError([	'Server responded, but could not return requested URL:'
									,	'['+ Status + '][' + downloads[i].url +']'
									,	ResponseText ].join('\n') );
			}
				
			var stream = new ActiveXObject('ADODB.Stream');
			with( stream ) {
				Type = 1; // Binary
				Mode = 3;
				open();
				Write( transport.responseBody );
				Position = 0;
				SaveToFile( downloads[i].local_archive, 2 );
				close();
			}
			mark_complete( downloads[i] );
		}
		for( var i = 0; i < downloads.length; i++ ){
			unmark( downloads[i] );
		}
		return downloads;
	}
	
	return ( which( 'curl.exe' ) ? save_via_curl : save_via_xmlhttp );
})();


var UnarchiveDownloadedObjects = (function() {
	var workers = [];

	var helpers = {
		'7z.exe': 	'x "{SOURCE}" -o"{DESTINATION}" -y',
		'zip.exe': 	'-o "{SOURCE}" -d "{DESTINATION}"',
		'jzip.exe': 'xf "{SOURCE}" -C "{DESTINATION}"'
	}

	function get_helper(){
		var u = [];
		for( var utility in helpers ){
			u.push( utility );
			if( which( utility ) ) return { exe: utility, command: helpers[utility] };
		}
		throw new FormattedError('Could not find valid unzip utility. \nSupported utilities are: ['+u+']');
	}

	function extract( source, destination ){
		var helper = get_helper();
		var command = helper.exe + ' ' +
			helper.command.replace( '{SOURCE}', source ).replace('{DESTINATION}',destination);
		//writeComment( command );
		return $WSH_SHELL.Exec( command );
	}

	function main( objects, andCleanup ){
		var archives = []
		//,	workers = []
		;
		for( var i = 0; i < objects.length; i++ ){
			objects[i].local = objects[i].local_archive.split('.').slice(0,-1).join('.');
			archives.push( { 
				archive : objects[i].local_archive,
				folder : objects[i].local,
				object: objects[i] 
			} );
		}

		while( archives.length || workers.length ){
			if( workers.length < 4 && archives.length ){
				writeComment( 'Starting unarchive worker. ('+(workers.length+1)+ ' running, '+ (archives.length-1) + ' remaining)' );
				var item = archives.shift();
				var worker = {
					start:(new Date()).valueOf(),
					process:extract( item.archive, item.folder ),
					object: item.object
				}
				workers.push( worker );
			}
			for( var i = 0; i < workers.length; i++ ){
				if( workers[i].process.Status ){
					workers.splice(i--,1);
				} else if( ( (new Date()).valueOf() > workers[i].start + 20000 ) ) {
					workers[i].object.error = true
					workers[i].object.error_message = 'Extraction of zip timed out.';
					workers.splice(i--,1);
				}
			}
			WScript.Sleep(500);
		}

		if( andCleanup || andCleanup === undefined ){
			WScript.Sleep(1000);
			for( var i = 0; i < objects.length; i++ ){
				if( objects[i].error ) continue;
				$FSO.DeleteFile(objects[i].local_archive);
				delete objects[i].local_archive
			}
		}

		return objects;
	}

	main.GetHelper = get_helper;

	return main;
})();


/**
 * CONFIG DEFAULTS
 */
var config = {
	baseurl: 'http://windows.php.net/downloads/snaps',
	format:	'json',
	query_only: false,
	download: {
			toDir: null,
			extract:true,
			cleanup:true
		},
	restraints: {
		build_type:'standard'
	}
}

function getBoolishValue( str ) {
	switch( str.toUpperCase() ) {
		case 'ON':
		case 'TRUE':
		case 'ALL':
		case 'ENABLED':
			return true;
		case 'OFF':
		case 'FALSE':
		case 'NONE':
		case 'DISABLED':
			return false;
		case 'NULL':
		case '':
			return null;
	}
	return str.replace(/^(['"])(.*)\1$/,'$2');
}

function setConfigWithToken( token ) {
	var rawArgument = token[0]
	,	key = token[1].toLowerCase()
	,	val = token[2]
	,	switchStr = key.match(/^((enable)|(no|disable))-/)
	;
	
	if( switchStr ){
		key = key.slice( switchStr[0].length );
		if( switchStr[3] ) val = !val;
	}
	//writeComment( 'Configure:['+key+':'+val+']' );
	switch( key ){
		case 'help':
			WL( getHelp() );
			WScript.Quit(0);
	
	// COMMON OPTIONS
		// Output options
		case 'format':
			if( typeof val == 'boolean' ) throw new FormattedError('\nFormat specification invalid: ' + rawArgument );
			config.format = val;
			break;
		case 'json':
		case 'xml':
			config.format = key;
			break;
	
		// Base Url
		case 'base-url':
			if( typeof val != 'string' ) throw new FormattedError('\nUrl specification invalid: ' + rawArgument );
			config.baseUrl = val;
			break;
	
	// RESTRAINTS
		// Compiler Switches
		case 'compiler':
			if( typeof val == 'boolean' ) throw new FormattedError('\nCompiler specification invalid: ' + rawArgument );
			config.restraints.compiler = val;
			break;
		case 'vc9':
		case 'vc6':
			config.restraints.compiler = key;
			break;
			
		// Threadsafety switches
		case 'threadsafety':
		case 'threadsafe':
			if( typeof val == 'boolean' ) config.restraints.threadsafe = val;
			else config.restraints.threadsafe = ( /z?ts/.test(val) );
			break;
		case 'nts':
		case 'ts':
		case 'zts': // Zend Thread Safety: a remnant from when there were multiple TS?
			config.restraints.threadsafe = ( /^z?ts$/i.test(key) );
			break;
		
		// Platform
		case 'platform':
			config.restraints.platform = val;
			break;
		
		// Architecture
		case 'architecture':
			config.restraints.platform = val;
			break;
		
		// Branch
		case '5.3':
		case 'trunk':
			val = key;
		case 'branch':
			config.restraints.branch = val;
			break;

		// Build type
		case 'build-type':
			if( typeof(val)=='boolean' && val ){
				delete config.restraints.build_type;
				break;
			}
			config.restraints.build_type = val;
			break;
		case 'standard':
		case 'devel':
		case 'debug':
			config.restraints.build_type = key;
			break;

		// Revision switches
		case 'newer-than':
		case 'since':
			val = 'gt'+val;
		case 'revision':
			if( !val ) throw new FormattedError( '\nInvalid flag use. --' + key + ' requires a numeric argument' );
			if( val.slice(0,2) == 'gt' ){
				var operator = '>' ;
				val = val.slice(2);
			}else if( val.slice(0,2) == 'lt' ){
				var operator = '<' ;
				val = val.slice(2);
			}else{
				var operator = '==';
			}
			config.revision = parseInt( val, 10 );
			config.restraints.revision = new Comparrison( operator, parseInt( val, 10 ) );
			break;
		
			
	// DOWNLOAD OPTIONS
		// Extracting & Cleanup
		case 'unzip':
			key = 'extract';
		case 'extract':
		case 'cleanup':
			config.download[key] = val;
			break;

		// Output directory
		case 'output-dir':
		case 'output-directory':
			config.download.toDir = val;
			break;
		
		case 'query-only':
			config.query_only = val;
			break;

		default:
			throw new FormattedError('\nUnrecognized argument: '+rawArgument );
	}
}

(function main(){ 
	try {
		var objArgs = WScript.Arguments
		,	argsObj = []
		;

		for( var i = 0; i < objArgs.length; i++ ) {
			if( matches = objArgs(i).match(/--([a-zA-Z0-9\-]+)(=(.*))?/) ) {
				setConfigWithToken( [ 
					matches[0], 
					matches[1], 
					getBoolishValue( matches[2] ? matches[3] : 'true' ) 
				] );
			} else {
				argsObj.push( objArgs(i) );
			}
		}
		
		
		function ApplyConfig( arrayOfBranches ){
			for( var restraint in config.restraints ){
				arrayOfBranches = Utility.FilterArrayOfObjectsByProperties( 
						arrayOfBranches, 
						restraint, 
						config.restraints[restraint]
				);
			}
			return arrayOfBranches;
		}

		// php-revision-get bin[ary|aries|s] [branch] [flags]
		// php-revision-get snap[shot] [branch] [flags]
		// php-revision-get branches|branch-list
		// php-revision-get help
		switch( action = argsObj[0] ) {
			case 'help': 
			case '/?':
				WL( getHelp() );
				break;
			case 'verify-install':
				UnarchiveDownloadedObjects.GetHelper();
				writeFormatted([{'type':'success','valid':true}],['statuses','status']);
				break;
			case 'branches':
			case 'branch-list':
				var relInfo = PhpRevisionInfo.GetBranches( config.baseurl )
				,	branches = []
				;
				writeFormatted( 
					relInfo,
					['branches','branch']
				);
				break;
			case 'info':
				config.query_only = true;
			case 'binary':
			case 'bin':
			case 'binaries':
			case 'bins':
				var builds = ApplyConfig(PhpRevisionInfo.Get( config.baseurl, config.restraints ))
				;
				if( ! config.query_only ){
					writeComment('Downloading...');
					DownloadObjects( builds, ( config.download.toDir || '.' ) );
					writeComment('Done.');

					if( config.download.extract ){
						writeComment('Extracting...');
						UnarchiveDownloadedObjects( builds, config.download.cleanup );
						writeComment('Done.');
					}
				}
				writeFormatted( 
					builds,
					['builds','build']
				);
				break;
			default:
				if( action )
					WL( 'Unknown command "'+action+'".' )
				WL( getHelp() )
				WScript.Quit();
		}
	} catch (e) {
		if( e instanceof FormattedError ) {
			WL( e.toString() );
		} else {
			throw e;
		}
	}
})();
