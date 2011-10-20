
# NOTE: you can add a --PFTT-- section to your PHPT file and store options. those options
#       will be read into the corresponding PhptTestCase instance
# NOTE: check for IS_PFTT environment variable in a PHPT test to tell if its being run
#       by PFTT instead of run-tests.php (make test)

class PhptTestCase

  include TestBenchFactor

  @@supported_sections = {
    :required => [
      [:file,:fileeof,:file_external],
      [:expect,:expectf,:expectregex],
      [:test]
    ],
    :optional => [
      #[:post,:post_raw],
      [:credit,:credits],
      [:ini],
      [:skipif],
      [:description],
      # LATER implement support for these other sections
      [:clean],
      [:request],
      [:post], [:post_raw], [:gzip_post], [:deflate_post], [:get],
      [:cookie],
      [:stdin],
      [:headers],
      [:cgi],
      # LATER support for expected failure
      [:xfail],
      [:expectheaders],
      [:args], [:env],
      # LATER I think pdo_mysql tests depend on this section (to run pdo tests)
      [:redirecttest]
    ]
  }

  attr_reader :phpt_path, :dir

  def initialize( dir, path, set=nil )
    if !File.exists?( path ) 
      raise 'File not found: ['+path+']'
    end
    @dir = dir
    @phpt_path = path
    @set = set
  end
  attr_accessor :scn_list
  attr_reader :set

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

  def raw(deploy_dir)
    @raw ||= IO.read(File.join(deploy_dir, full_name))
  end

  def parse!(deploy_dir)
    reset!
    @result_tester = nil
    section = :none
    raw(deploy_dir).lines do |line|
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

  protected

  def reset!
    @parts = {}
    @ini = nil
    @options = nil
  end

  private

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
    
    # search each directory for PHPT files
    paths.map{|path|
      Dir.glob( File.join( path, '**/*.phpt' ) ).each do |files|
        if test_names != nil and test_names.length > 0
          test_names.each{|test_name|
            if files.is_a?(Array)
              files.each{|file|
                if file.include?(test_name)
                  add_selected_file(path, file)
                end
              }
            elsif files.include?(test_name)
              add_selected_file(path, files)
            end
          }
        else
          add_selected_file(path, files)
        end
      end
    } 
    @selected = nil
    self
  end
  
  class DuplicateTestError < StandardError
  end
  
  private
  
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
    @selected.push(file)
    push(PhptTestCase.new( dir, File.join(dir, file), self ))
  end

end
