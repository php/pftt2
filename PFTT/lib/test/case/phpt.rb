
# NOTE: you can add a --PFTT-- section to your PHPT file and store options. those options
#       will be read into the corresponding Test::Case::Phpt instance
# NOTE: check for IS_PFTT environment variable in a PHPT test to tell if its being run
#       by PFTT instead of run-tests.php (make test)

module Test
  module Case
class Phpt

  include Test::Factor

  # PHPT files may have many sections. some sections are required, some are optional.
  # 
  # see http://qa.php.net/phpt_details.php for info about all the sections
  @@supported_sections = {
    :required => [
      [:file,:fileeof,:file_external],
      [:expect,:expectf,:expectregex],
      [:test]
    ],
    :optional => [
      [:credit,:credits],
      [:comment],
      [:ini],
      [:skipif],
      [:description],
      [:clean],
      [:redirecttest],
      [:xfail],
      # CLI middleware
      [:stdin],
      [:args], [:env],
      # HTTP middlewares (iis, apache)
      [:request],
      [:post], [:post_raw], [:gzip_post], [:deflate_post], [:get],
      [:cookie],
      #    --headers-- would require using server-tests.php (instead of run-tests.php), but PFTT provides the capabilities of both
      [:headers], 
      [:cgi],
      [:expectheaders]
      
    ]
  }

  attr_reader :phpt_path, :dir

  def initialize( dir, path, set=nil )
    unless $hosted_int
      if path.starts_with?('G:/abc')
        path = path['G:/abc'.length+1...path.length]
        # TODO TUE  
      end
#      unless path.starts_with?('C:/abc')
#        path = 'C:/abc/'+path
#      end
      
    end
# TODO TUE   if !File.exists?( path ) 
#      raise 'File not found: ['+path+']'
#    end
    @dir = dir
    @phpt_path = path
    @set = set
    @env = {} # enable mw_redirecttest to pass ENV vars to be returned by cli_env
    @parts = {}
  end
  attr_accessor :scn_list
  attr_reader :set, :env
  
  def path(type)
    # TODO
    # :http  :local_working :remote_working
  end
  
  def to_xml()
    xml = {
      '@dir' => dir,
      '@path' => @phpt_path
    }
    # don't need to include the phpt sections, the client will already have it
    # this saves a lot of resources
    #
    # TODO client needs to read this data from memory, not file!!
#    xml['part'] = []
 #   parts.each do |key, content|
  #    xml['part'].push({'@part_name'=>key, 'text'=>content})
   # end
    return xml
  end
   
  def self.from_xml(xml)
    dir = xml['@dir']
    unless dir.is_a?(String)
      dir = dir.to_s
    end
    dir.gsub!('G:/', 'C:/') # TODO TUE
    test_case = Test::Case::Phpt.new(dir, xml['@path'])
#    xml['part'].each do |part|
 #     part_name = part['@part_name']
  #    text = part['text']
  #      
      # match symbol to string
        # TODO .flatten
   #   @@supported_sections.flatten.each do |part_sym|
    #    if part_sym.to_s == part_name
#          test_case.parts[part_sym] = text
 #         break
 #       end
  #    end
   # end
    return test_case
  end
  
  def http_headers(mw_cli)
    # support for --HEADERS-- section
    #
    # http://qa.php.net/phpt_details.php#headers_section
    headers_str = parts[:headers]
      
    headers_str, err, status = mw_cli.php!('-r "echo '+headers_str+';"');
    if status != 0
      puts headers_str
      puts err
      return {}
    end
    
    headers = {}
    env_str.split('\n').each do |line|
      i = line.index('=')
      if i==-1
        next # shouldn't happen
      end
      hdr_name = line[0..i]
      hdr_value = line[i+1..line.length]
                
      headers[hdr_name] = hdr_value
    end
    return headers
  end
  
  def mw_redirecttest(mw_cli)
    # support for --REDIRECTTEST-- section
    #
    # return array(
    #  'ENV' => array(
    #      'PDOTEST_DSN' => 'sqlite2::memory:'
    #    ),
    #  'TESTS' => 'ext/pdo/tests'
    #  );
    #
    # see http://qa.php.net/phpt_details.php#redirecttest_section
    #
    redirecttest_str = parts[:redirecttest]
    
    # run a quick php script through PHP to print out first the 'ENV' then the 'TEST' values  
    env_str, err, status = mw_cli.php!('-r "$r = '+redirecttest_str+'; foreach($r[\'ENV\'] as $name, $value) {echo "$name=$value\n";}');
    if status != 0
      puts env_str
      puts err
      return []
    end
    #
    
    #
    test_list_str, err, status = mw_cli.php!('-r "$r = '+redirecttest_str+'; foreach($r[\'TEST\'] as $test_name) {echo "$test_name\n";}');
    if status != 0
      puts test_list_str
      puts err
      return []
    end
    #
    
    env = {}
    env_str.split('\n').each do |line|
      i = line.index('=')
      if i==-1
        next # shouldn't happen
      end
      env_name = line[0..i]
      env_value = line[i+1..line.length]
            
      env[env_name] = env_value
    end
    
    test_list = []
    test_list_str.split('\n').each do |line|
      line.chomp!
      
      test_case = Test::Case::Phpt.new(@dir, line, @set)
      # copy the ENV section to this test_case. will be accessible by #cli_env
      test_case.env = env 
      test_list.push(test_case)
    end
      
    test_list
  end
  
  def cli_env(cwd, filepath)
    # provides support for --ENV-- section, primarily for the CLI middleware,
    # and for getting the ENV variables provided in a --REDIRECTTEST-- section (that redirected to this test)
    #
    # see http://qa.php.net/phpt_details.php#env_section
    #
    # looks like its meant for PHPTs that were originally meant to be run through server-tests.php not run-tests.php
    unless parts.has_key?(:env)
      return {}
    end
    
    lines = parts[:env].split('\\n')
      
    env = @env
      
    lines.each do |line|
      line.chomp!
      
      # the line may use one of the following variables, which we need to substitue for now
      
      #$filename - full native path to file, will become PATH_TRANSLATED
      line.gsub!('\$filename', filepath)
      #$filepath - =dirname($filename)
      line.gsub!('\$filepath', filepath)
      #$scriptname - this is what will become SCRIPT_NAME unless you override it
      line.gsub!('\$scriptname', filepath)
      #$this->conf - all server-tests configuration vars
      line.gsub!('\$cwd', cwd)
      # even though http://qa.php.net/phpt_details.php#env_section lists other variables like $docroot
      #  server-tests.php doesn't seem to use support them at all (server-tests.php only seems to support these 4)
      
      i = line.index('=')
      unless i
        @borked_reasons||=[]
        @borked_reasons << 'malformed environment variable line (PHPT ENV section)'
        next
      end
      env_name = line[0..i]
      env_value = line[i+1..line.length]
      
      env[env_name] = env_value
    end
    
    env
  end
  
  def http_request
    # provides support for --REQUEST-- section
    #
    # see http://qa.php.net/phpt_details.php#request_section
    #
    # Valid settings for this section include:
    #
    # SCRIPT_NAME - The inital part of the request url
    # PATH_INFO - The pathinfo part of a request url
    # FRAGMENT - The fragment section of a url (after #)
    # QUERY_STRING - The query part of a url (after ?)
    #
    unless parts.has_key?(:request)
      return nil
    end
    
    # parse the request to get the different parts of the request
    parts = {}    
    lines = parts[:request].split('\\n')
    lines.each do |line|
      line.chomp!
      
      i = line.index('=')
      if i==-1
        @borked_reasons||=[]
        @borked_reasons << 'malformed environment variable line(PHPT REQUEST section)'
        next
      end
      part_name = line[0..i]
      part_value = line[i+1..line.length]
            
      parts[part_name.upcase] = part_value
    end
    #
    #
    
    if parts.has_key?('PATH_INFO')
      url = parts['PATH_INFO']
    elsif parts.has_key?('SCRIPT_NAME')
      url = parts['SCRIPT_NAME']
    else
      @borked_reasons||=[]
      @borked_reasons << 'PHPT REQUEST section missing both PATH_INFO and SCRIPT_NAME'
      return ''
    end
    if parts.has_key?('QUERY_STRING')
      url += '?' + parts['QUERY_STRING']
    end
    if parts.has_key?('FRAGMENT')
      url += '#' + parts['FRAGMENT']
    end
    
    return url
  end
  
  def compatible?(host, middleware, php, scn_set)
    # if the --cgi-- section is present, this requires(is only compatible if) the middleware is http
    not parts.has_key(:cgi) or middleware.instance_of?(Middleware::Http)
  end
    
  
  def name
    @name ||= File.basename @phpt_path, '.phpt'
  end
  
  def full_name_wo_phpt
    fn = full_name
    if fn.ends_with?('.phpt')
      fn = fn[0..fn.length-'.phpt'.length-1]
    end
    return fn
  end
  
  def full_name
    test_case_name = @phpt_path
    if test_case_name.starts_with?(@dir)
      test_case_name = test_case_name[@dir.length+1, test_case_name.length]
    end
    return test_case_name
  end
  
  def ext_name
    test_case_name = full_name
                
    test_module = test_case_name
    # turn test case names like ext/date/tests/bug12345 into ext/date
    if test_module.starts_with?('ext/')
      i = test_module.index('/', 'ext/'.length+1)
            
      if i > 0
        test_module = test_module[0, i]
      end
    end
    return TypedToken::StringToken::ExtName.new(test_module)
  end

  def relative_path
    full_name # LATER
  end

  def description
    @phpt_path
  end

  def expectation
    if @expectation
      return @expectation
    end
    p = parts()
    if p.has_key?(:expect)
      @expectation_type = :expect
    elsif p.has_key?(:expectf)
      @expectation_type = :expectf
    elsif p.has_key?(:expectregex)
      @expectation_type = :expectregex
    else
      return nil
    end
    # TODO include :expectheaders here too??
    @expectation = {:type => @expectation_type, :content => p[@expectation_type]}
    return @expectation
  end

  def options
    require 'yaml'
    if @options.nil?
      @options = YAML::load parts[:pftt] if parts[:pftt]
      @options ||= {}
    end
    @options
  end

  def ini
    @ini ||= PhpIni.new parts[:ini]
  end

  def bork_reasons
    borked?
    @bork_reasons
  end

  def borked?
    unless @bork_reasons
      @bork_reasons = []
      (@@supported_sections.values.flatten(1)).each do |group|
        if (parts.keys & group).length > 1
          @bork_reasons << 'duplicate sections:'+group.to_s
        end
      end
      @@supported_sections[:required].each do |group|
        if (parts.keys & group).length < 1;
          @bork_reasons << 'missing required section:'+group.to_s
        end
      end
      counte = (parts.has_key?(:expect)) ? 1 : 0
      counte += (parts.has_key?(:expectf)) ? 1 : 0
      counte += (parts.has_key?(:expectregex)) ? 1 : 0
      if counte > 1
        @bork_reasons << 'can only have one EXPECT or EXPECTF or EXPECTREGEX section, not '+counte.to_s
      end
      counth = (parts.has_key?(:get)) ? 1 : 0
      counth += (parts.has_key?(:post)) ? 1 : 0
      counth += (parts.has_key?(:post_raw)) ? 1 : 0
      counth += (parts.has_key?(:gzip_post)) ? 1 : 0
      counth += (parts.has_key?(:deflate_post)) ? 1 : 0
      if counth > 1
        @bork_reasons << 'can only have one GET, POST, POST_RAW, GZIP_POST or DEFLATE_POST section, not '+counth.to_s
      end
    end
    !@bork_reasons.length.zero?
  end

  def unsupported?
    # are any sections in parts not present in @supported_sections
    !unsupported_sections.length.zero?
  end

  def unsupported_sections
    @unsupported_sections ||= (parts.keys - @@supported_sections.values.flatten)
  end

  def []( section )
    return parts[section] || nil
  end

  def has_section? section
    return parts.has_key?(section) # LATER
  end

  def save_section( section, path, extension=section.to_s )
    fullpath = File.join( path, "#{name}.#{extension}")
    File.open fullpath, 'w' do |file|
      file.write self[section]
    end
    fullpath
  end

  def extension
    {
      :file => 'php',
      :skipif => 'skipif.php',
      :clean => 'clean.php'
    }
  end

  def files
    # supporting files:
    case
    when nil 
    # scenario 1: options[:support_files]

    # scenario 2: folder with same name as this test case

    # scenario 3: all folders & files alongside this and all files that are not phpt files
    else
      base = File.dirname( @phpt_path )
      @files ||= Dir.glob( File.join(base,'*') ).map do |file|
        next nil if ['..','.'].include? file
        next nil if file.end_with? '.phpt'
        file
      end.compact
    end
  end

  def pass?
    raise 'Result not attached' if @result_tester.nil?
    @result_tester.pass?
  end

  def inspect
    parts.inspect
  end

  def raw()
    @raw ||= IO.read(File.join(@dir, full_name))
  end

  def parse!()
    reset!
    @result_tester = nil
    section = :none
    raw().lines do |line|
      if line =~ /^--(?<section>[A-Z_]+)--/
        section = Regexp.last_match[:section].downcase.to_sym
        @parts[section]=''
      else
        @parts[section]||=''
        @parts[section] += parse_line line, File.dirname( @phpt_path )
      end
    end

    if @parts.has_key? :fileeof
      @parts[:file]=@parts.delete(:fileeof).gsub(/\r?\n\Z/,'')
    elsif @parts.has_key? :file_external
      context = File.dirname( @phpt_path )
      external_file = File.absolute_path( @parts.delete(:file_external).gsub(/\r?\n\Z/,''), context )
      # TODO TUE c:/abc/ 
      
      # TODO TUE TEMP 
      external_file = (($phpt_path+'/'+external_file).gsub('C:/php-sdk/0/PFTT2/PFTT', ''))
      
      @parts[:file]= IO.read( external_file ).lines do |line|
        parse_line line, context
      end
    end
    @parts[:file].gsub!(%Q{\r\n},%Q{\n}) unless @parts[:file].nil?
  end
  
  def parts
    parse! if @parts.empty?
    @parts
  end

  protected

  def reset!
    @parts = {}
    @ini = nil
    @options = nil
  end

  def parse_line( line, context )
    return line unless line =~ /^\#\!?include (?<script>.*)/
    script = File.expand_path Regexp.last_match[:script].chomp, context
    expanded = ''
    IO.read(script).lines do |line|
      expanded += parse_line line, File.dir_name(script)
    end
    expanded
  end
  
end

class Test::Case::Phpt::Array < TypedArray(Test::Case::Phpt)
  attr_reader :path
  def initialize ( phpt_dir, test_names=[] )
    phpt_dir.convert_path! # critical - don't mix \ with / (used later)
    
    @selected = []
                  
    # remove duplicate directories from the list of dirs to check
    paths = []
    if phpt_dir.is_a?(String)
      paths = [phpt_dir]
    else
      phpt_dir.map{|path|
        path = File.absolute_path(path)
        unless paths.include?(path)
          paths.push(path)
        end
      }
    end
        
    # search each directory for PHPT files
    paths.map do |path|
      Dir.glob( File.join( path, '**/*.phpt' ) ) do |files|
        if test_names != nil and test_names.length > 0
          test_names.each do |test_name|
            add_selected_file(path, files)
          end
        else
          add_selected_file(path, files)
        end
      end
    end
    @selected = nil
    self
  end
  
  class DuplicateTestError < StandardError
  end
  
  def ext
    # TODO list extensions included in test case array
  end
  
  protected
  
  def add_selected_file(dir, file)
    file = File.absolute_path(file)
    if file.starts_with?(dir)
      file = file[dir.length+1...file.length]
    end
    if @selected.include?(file)
      # since there are no duplicate directories, we know that two different directories
      # have the same test (error out to avoid confusion)
      #
      raise DuplicateTestError
    end
    # don't look in folders named 'Release' or 'Release_TS' because those folders
    # may be inside the php dir
    # and Release or Release_TS may contain duplicate PHPTs!
    if file.starts_with?('Release') or file.starts_with?('Release_TS')
      return
    end 
    # TODO skip these slow things
    #if file.include?('/windows_acl') #or file.include?('oci8') or file.include?('sql') or file.include?('/pdo') or file.include?('/soap')
     # return
    #els
    if file.include?('/http/')
      # assume this passes TODO TUE
      return
    end    
    @selected.push(file)
    push(Test::Case::Phpt.new( dir, File.join(dir, file), self ))
  end
  
  def can_ini_set?(directive_name)
    # returns if directive_name can be used with php's ini_set() function
    # directive_name is either a String or an Array of Strings
    #
    if directive_name.is_a?(Array)
      directive_name.each do |n|
        if @@user_ini_directives.include?(n)
          return true
        end
      end
      return false
    end
  
    # if its not on this list, assume it can't be set by an ini_set() call
    @@user_ini_directives.include?(directive_name)
  end

# the INI directives that can be set using ini_set()
# see http://us2.php.net/manual/en/ini.list.php
# see http://us2.php.net/manual/en/configuration.changes.modes.php
#
# includes any/all PHP_INI_ALL or PHP_INI_USER
# but not PHP_INI_PERDIR or PHP_INI_SYSTEM
@@user_ini_directives = [
'apd.bitmask', 'apd.dumpdir', 'apd.statement_tracing',
#
'arg_separator', 'arg_separator.output', 
'asp_tags', 
#
'assert.active', 'assert.bail', 'assert.callback', 'assert.quiet_eval', 'assert.warning',
#   
'async_send', 
'auto_detect_line_endings', 
'axis2.client_home', 'axis2.enable_exception', 'axis2.enable_trace', 'axis2.log_path',
#   
'bcmath.scale',   
'bcompiler.enabled', 
'birdstep.max_links', 
'blenc.key_file',
#   
'cgi.nph', 'cgi.rfc2616_headers',
# 
'child_terminate',
# 
'cli.pager', 'cli.prompt',
# 
'coin_acceptor.autoreset', 'coin_acceptor.auto_initialize', 'coin_acceptor.auto_reset', 'coin_acceptor.command_function', 
'coin_acceptor.delay', 'coin_acceptor.delay_coins', 'coin_acceptor.delay_prom', 'coin_acceptor.device', 
'coin_acceptor.lock_on_close', 'coin_acceptor.start_unlocked',
#
'com.autoregister_casesensitive', 'com.autoregister_typelib', 'com.autoregister_verbose', 'com.code_page',
# 
'daffodildb.default_host', 'daffodildb.default_password', 'daffodildb.default_socket', 'daffodildb.default_user',   
'daffodildb.port',
#
'date.default_latitude', 'date.default_longitude', 'date.sunrise_zenith', 'date.sunset_zenith',  'date.timezone',
# 
'dba.default_handler',
# 
'default_charset', 'default_mimetype', 'default_socket_timeout', 'define_syslog_variables', 'detect_unicode', 
'display_errors', 'display_startup_errors', 'docref_ext', 'docref_root', 'engine', 
'error_append_string', 'error_log', 'error_prepend_string', 'error_reporting',   
#
'etpan.default.charset', 'etpan.default.protocol',
#   
'exif.decode_jis_intel', 'exif.decode_jis_motorola', 'exif.decode_unicode_intel', 'exif.decode_unicode_motorola', 
'exif.encode_jis', 'exif.encode_unicode',
# 
'expect.logfile', 'expect.loguser', 'expect.timeout',
#    
'fbsql.batchsize', 
#
'from',
#   
'gd.jpeg_ignore_warning',
# 
'geoip.custom_directory', 'geoip.database_standard',
# 
'gpc_order', 
#
'highlight.bg', 'highlight.comment', 'highlight.default', 'highlight.html', 'highlight.keyword', 'highlight.string',
#   
'http.allowed_methods', 'http.allowed_methods_log', 'http.cache_log', 'http.composite_log', 'http.etag.mode', 
'http.etag_mode', 'http.force_exit', 'http.log.allowed_methods', 'http.log.cache', 'http.log.composite', 
'http.log.not_found', 'http.log.redirect', 'http.ob_deflate_flags', 'http.ob_inflate_flags', 'http.only_exceptions', 
'http.persistent.handles.ident', 'http.redirect_log', 'http.request.methods.allowed', 'http.send.deflate.start_flags', 
'http.send.inflate.start_flags', 'http.send.not_found_404',
#  
'ibase.dateformat', 'ibase.default_charset', 'ibase.default_password', 'ibase.default_user', 'ibase.timeformat',   
'ibase.timestampformat',
#   
'ibm_db2.binmode',
#   
'iconv.input_encoding', 'iconv.internal_encoding', 'iconv.output_encoding',
# 
'ifx.blobinfile', 'ifx.byteasvarchar', 'ifx.charasvarchar', 'ifx.nullformat', 'ifx.textasvarchar',
# 
'ignore_repeated_errors', 'ignore_repeated_source', 'ignore_user_abort',
#   
'imlib2.font_cache_max_size', 'imlib2.font_path',
#   
'include_path',   
'ingres.array_index_start', 'ingres.blob_segment_length', 'ingres.cursor_mode', 'ingres.default_database', 
'ingres.default_password', 'ingres.default_user', 'ingres.report_db_warnings', 'ingres.timeout', 'ingres.trace_connect',
# 
'ircg.control_user', 'ircg.keep_alive_interval', 'ircg.max_format_message_sets', 'ircg.shared_mem_size', 'ircg.work_dir',
# 
'last_modified', 'ldap.base_dn', 'log.dbm_dir', 'log_errors', 'log_errors_max_len', 'magic_quotes_runtime', 
'magic_quotes_sybase', 'mail.force_extra_parameters', 'mail.log', 'mailparse.def_charset', 
#
'maxdb.default_db', 'maxdb.default_host', 'maxdb.default_pw', 'maxdb.default_user', 'maxdb.long_readlen',
#   
'max_execution_time',   
#
'mbstring.detect_order', 'mbstring.encoding_translation', 'mbstring.http_input', 'mbstring.http_output', 
'mbstring.internal_encoding', 'mbstring.language', 'mbstring.script_encoding', 'mbstring.strict_detection', 
'mbstring.substitute_character', 
#
'mcrypt.algorithms_dir', 'mcrypt.modes_dir',
# 
'memcache.allow_failover', 'memcache.chunk_size', 'memcache.default_port', 'memcache.hash_function', 
'memcache.hash_strategy', 'memcache.max_failover_attempts',
#
'memory_limit',
#
'msql.allow_persistent', 'msql.max_links', 'msql.max_persistent', 'mssql.batchsize', 'mssql.charset', 
'mssql.compatability_mode', 'mssql.connect_timeout', 'mssql.datetimeconvert', 'mssql.max_procs', 
'mssql.min_error_severity', 'mssql.min_message_severity', 'mssql.textlimit', 'mssql.textsize', 'mssql.timeout', 
#
'mysql.default_host', 'mysql.default_password', 'mysql.default_port', 'mysql.default_socket', 'mysql.default_user',   
'mysql.trace_mode', 'mysqli.default_host', 'mysqli.default_port', 'mysqli.default_pw', 'mysqli.default_socket', 
'mysqli.default_user',
# 
'namazu.debugmode', 'namazu.lang', 'namazu.loggingmode', 'namazu.sortmethod', 'namazu.sortorder',
#   
'nsapi.read_timeout',
#     
'odbc.defaultbinmode', 'odbc.defaultlrl', 'odbc.default_db', 'odbc.default_pw', 'odbc.default_user',
'odbtp.datetime_format', 'odbtp.detach_default_queries', 'odbtp.guid_format', 'odbtp.interface_file', 'odbtp.truncation_errors',
#
'opendirectory.default_separator', 'opendirectory.max_refs', 'opendirectory.separator',
# 
'oracle.allow_persistent', 'oracle.max_links', 'oracle.max_persistent',
# 
'pam.servicename',
#   
'pcre.backtrack_limit', 'pcre.recursion_limit',
#
'pdo_odbc.connection_pooling',
# 
'pfpro.defaulthost', 'pfpro.defaultport', 'pfpro.defaulttimeout', 'pfpro.proxyaddress', 
'pfpro.proxylogon', 'pfpro.proxypassword','pfpro.proxyport', 
#
'pgsql.ignore_notice', 'pgsql.log_notice', 
#
'phar.extract_list', 'phar.readonly', 'phar.require_hash',
#   
'precision', 'printer.default_printer', 'python.append_path', 'python.prepend_path',
'report_memleaks', 'report_zend_debug', 'sendmail_from', 'serialize_precision',
#  
'session.auto_start', 'session.cache_expire', 'session.cache_limiter', 'session.cookie_domain', 
'session.cookie_httponly', 'session.cookie_lifetime', 'session.cookie_path', 'session.cookie_secure', 
'session.entropy_file', 'session.entropy_length', 'session.gc_divisor', 'session.gc_maxlifetime',   
'session.gc_probability', 'session.hash_bits_per_character', 'session.hash_function', 'session.name',   
'session.referer_check', 'session.save_handler', 'session.save_path', 'session.serialize_handler',   
'session.use_cookies', 'session.use_only_cookies',
#   
'simple_cvs.authMethod', 'simple_cvs.compressionLevel', 'simple_cvs.cvsRoot', 'simple_cvs.host',   
'simple_cvs.moduleName', 'simple_cvs.userName', 'simple_cvs.workingDir',
#   
'SMTP', 'smtp_port',
# 
'soap.wsdl_cache', 'soap.wsdl_cache_dir', 'soap.wsdl_cache_enabled', 'soap.wsdl_cache_limit', 'soap.wsdl_cache_ttl',
# 
'sqlite.assoc_case',
#  
'sybase.hostname', 'sybase.interface_file', 'sybase.login_timeout', 'sybase.min_client_severity', 'sybase.min_error_severity',   
'sybase.min_message_severity', 'sybase.min_server_severity', 'sybase.timeout', 'sybct.deadlock_retry_count', 
'sybct.hostname', 'sybct.login_timeout',
#
'sysvshm.init_mem', 'track_errors', 'track_vars', 'unserialize_callback_func', 'uploadprogress.file.filename_template',   
'url_rewriter.tags', 'user_agent', 
#
'valkyrie.auto_validate', 'valkyrie.config_path',
#   
'variables_order', 'velocis.max_links', 'xbithack', 
#
'xdebug.auto_profile', 'xdebug.auto_profile_mode', 'xdebug.auto_trace', 'xdebug.collect_includes', 'xdebug.collect_params',   
'xdebug.collect_return', 'xdebug.collect_vars', 'xdebug.default_enable', 'xdebug.dump.COOKIE', 'xdebug.dump.ENV NULL',
'xdebug.dump.FILES', 'xdebug.dump.GET', 'xdebug.dump.POST', 'xdebug.dump.REQUEST', 'xdebug.dump.SERVER',   
'xdebug.dump.SESSION', 'xdebug.dump_globals', 'xdebug.dump_once', 'xdebug.dump_undefined', 'xdebug.idekey', 
'xdebug.manual_url', 'xdebug.max_nesting_level', 'xdebug.remote_autostart', 'xdebug.remote_enable', 'xdebug.remote_handler',   
'xdebug.remote_host', 'xdebug.remote_log', 'xdebug.remote_mode', 'xdebug.remote_port', 'xdebug.show_exception_trace', 
'xdebug.show_local_vars', 'xdebug.show_mem_delta', 'xdebug.trace_format', 'xdebug.trace_options', 'xdebug.trace_output_dir', 
'xdebug.trace_output_name', 'xdebug.var_display_max_children', 'xdebug.var_display_max_data', 'xdebug.var_display_max_depth',
# 
'xmlrpc_error_number',
# 
'xmms.path', 'xmms.session',
#   
'y2k_compliance',
#   
'yami.response.timeout',
# 
'yaz.keepalive', 'yaz.log_file', 'yaz.log_mask', 'yaz.max_links',
# 
'zend.enable_gc', 'zend.ze1_compatibility_mode', 
#
'zlib.output_compression', 'zlib.output_compression_level', 'zlib.output_handler'
]
  
  

end # class Phpt
end # module Case
end # module Test
