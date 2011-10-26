
# NOTE: you can add a --PFTT-- section to your PHPT file and store options. those options
#       will be read into the corresponding PhptTestCase instance
# NOTE: check for IS_PFTT environment variable in a PHPT test to tell if its being run
#       by PFTT instead of run-tests.php (make test)

class PhptTestCase

  include TestBenchFactor

  # PHPT files may have many sections. some sections are required, some are optional.
  #
  # see http://qa.php.net/phpt_details.php for info about all the sections
  @@supported_sections = {
    :required => [
      [:file,:fileeof,:file_external],
      [:expect,:expectf,:expectregex,:expectheaders],
      [:test]
    ],
    :optional => [
      [:credit,:credits],
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

  attr_reader :phpt_path, :dir, :parts

  def initialize( dir, path, set=nil )
    if !File.exists?( path ) 
      raise 'File not found: ['+path+']'
    end
    @dir = dir
    @phpt_path = path
    @set = set
    @env = {} # enable mw_redirecttest to pass ENV vars to be returned by cli_env
  end
  attr_accessor :scn_list
  attr_reader :set, :env
  
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
      
      test_case = PhptTestCase.new(@dir, line, @set)
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
      if i==-1
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
    @expectation_type ||= (parts.keys & [:expect,:expectf,:expectregex]).first
    @expectation ||= {:type => @expectation_type, :content => self[@expectation_type]}
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
# TODO     counte = (parts.has_key?(:expect)) ? 1 : 0
#      counte += (parts.has_key?(:expectf)) ? 1 : 0
#      counte += (parts.has_key?(:expectregex)) ? 1 : 0
#      if counte > 0
#        @bork_reasons << 'can only have one EXPECT or EXPECTF or EXPECTREGEX section, not '+counte.to_s
#      end
#      counth = (parts.has_key?(:get)) ? 1 : 0
#      counth = (parts.has_key?(:post)) ? 1 : 0
#      counth = (parts.has_key?(:post_raw)) ? 1 : 0
#      counth = (parts.has_key?(:gzip_post)) ? 1 : 0
#      counth = (parts.has_key?(:deflate_post)) ? 1 : 0
#      if counth > 0
#        @bork_reasons << 'can only have one GET, POST, POST_RAW, GZIP_POST or DEFLATE_POST section, not '+counth.to_s
#      end
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
    return parts.has_key?(section)
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
    @raw ||= IO.read(File.join('c:/php-sdk/svn/branches/php_5_4/', full_name)) # TODO phptdir
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
        @parts[section] += parse_line line, File.dirname( @phpt_path )
      end
    end

    if @parts.has_key? :fileeof
      @parts[:file]=@parts.delete(:fileeof).gsub(/\r?\n\Z/,'')
    elsif @parts.has_key? :file_external
      context = File.dirname( @phpt_path )
      external_file = File.absolute_path( @parts.delete(:file_external).gsub(/\r?\n\Z/,''), context ) 
      @parts[:file]= IO.read( external_file ).lines do |line|
        parse_line line, context
      end
    end
    @parts[:file].gsub!(%Q{\r\n},%Q{\n}) unless @parts[:file].nil?
  end

  # TODO protected

  def reset!
    @parts = {}
    @ini = nil
    @options = nil
  end

  # TODO private

  def parse_line( line, context )
    return line unless line =~ /^\#\!?include (?<script>.*)/
    script = File.expand_path Regexp.last_match[:script].chomp, context
    expanded = ''
    IO.read(script).lines do |line|
      expanded += parse_line line, File.dir_name(script)
    end
    expanded
  end

  def parts
    parse! unless @parts
    @parts
  end
end

def PhptTestCase::Error
  def initialize(message=nil)
    @message = message
  end

  def to_s
    @message
  end
end

class PhptTestCase::Array < TypedArray(PhptTestCase)
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
        #tests_names = ['math/abs'] # TODO
    # search each directory for PHPT files
    paths.map do |path|
      Dir.glob( File.join( path, '**/*.phpt' ) ).each do |files|
        if test_names != nil and test_names.length > 0
          test_names.each do |test_name|
            # internally, ruby always uses / for filenames (check for / not \)
            test_name.gsub!('\\', '/')
#            puts files.inspect
#            puts test_name.inspect
            if files.is_a?(Array)
              files.each do |file|
                if file.include?(test_name)
                  add_selected_file(path, file)
                end
              end
            elsif files.include?(test_name)
              add_selected_file(path, files)
            end
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
  
  # TODO private
  
  def add_selected_file(dir, file)
    #puts file
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
    @selected.push(file)
    push(PhptTestCase.new( dir, File.join(dir, file), self ))
  end

end
