
class PhpBuild
  include TestBenchFactor
  include PhpIni::Inheritable

  def self.get_set(*globs)
    set = Class.new(TypedArray( self )){include TestBenchFactorArray}.new
      
    globs.each do |glob|
      Dir.glob( glob ) do |php|
        next if php.end_with? '.zip'
        set << PhpBuild.new( php )
      end
    end
    
    unless set.empty?
      return set
    end

    # check for a Release or Release_TS folder. if present, they are the PHP build to run    
    globs.each do |glob|
      if File.exist?(glob+'/Release')
        set << PhpBuild.new(glob+'/Release')
      elsif File.exist?(glob+'/Release_TS')
        set << PhpBuild.new(glob+'/Release_TS')
      end
    end
    
    set
  end

  def initialize path, hsh={}
    @path = path
    
    # LATER get actual SDK info
    @sdk_info = {
      :major => 6, # may be using Windows SDK 7. should use SDK 6 with PHP though!!
      :minor => 0,
      :product => 'Windows SDK'
    }
    
    determine_properties_and_requirements
  end
  attr_reader :path, :sdk_info

  def [](k)
    properties.merge(requirements)[k]
  end

  def describe(*args)
    return File.basename(path) if args.empty?
    args.map do |prop|
      case prop
      when :threadsafety then ( properties[:threadsafe] ? 'ts' : nil )
      else properties[prop]
      end
    end.compact.join('-')
  end
  
  def to_s
    File.basename(path)
  end

  protected

  # gets the info about this php build
  def determine_properties_and_requirements
    base_name = File.basename(path)
    
    if path.include?'branches'
      # check for an 'svn' build
      
      # for platform/build information, assume that this was built on localhost
      requirement :platform => $is_windows ? :windows : :posix
        
      parts = path.split(Regexp.new('[/|\\\\]'))
      if parts.length < 2
        raise 'Unsupported PHP Build Type(Info Format:('
      elsif parts[parts.length-1] == 'Release' or parts[parts.length-1] == 'Release_TS'
        base_name = parts[parts.length-2]
        
        property :threadsafe => parts[parts.length-1] == 'Release_TS' ? 'ts' : 'nts'
      else
        property :threadsafe => Dir.exists(File.join(path, 'Release_TS')) ? 'ts' : 'nts'
      end
        
      if base_name == 'PHP_5_4'
        property :php_branch => 'PHP_5_4'
        property :php_version_major => '5'
        property :php_version_minor => '4'
      elsif base_name == 'PHP_5_3'
        property :php_branch => 'PHP_5_3'
        property :php_version_major => '5'
        property :php_version_minor => '3'
        # LATER fix branch parsing support so it'll work for any version(not just 5_4 and 5_3)
      else
        raise 'Unsupported PHP Build Type(Info Format:)'
      end
          
      # LATER get svn revision (include in :version too)
      property :revision => 'r'
      
      property :type => :svn
      if $is_windows
        property :compiler => 'vc9'
      #elsif RbConfig::CONFIG['host_os'] =~ /mingw/
      #  property :compiler => 'mingw' # unsupported compiler
      else
        property :compiler => 'gcc'
      end 
      
    elsif path.include?'trunk'
      raise 'Running SVN build from trunk is not supported!'
      # LATER support for getting info from trunk build
    else
      begin
        parts = base_name.split('-')
    
        requirement :platform => !parts.select{|i| i =~/(Win32|windows)/ }.empty? ? :windows : :posix

        branchinfo = parts.select{|i| i =~ /(?:[0-9]\.[0-9]+|trunk)/ }.first

        bs = branchinfo.split('.')
        property :php_branch => bs.first(2).join('_')
        property :php_version_major => bs.first.to_i
        property :php_version_minor => bs[1].to_i
        property :threadsafe => parts.select{|i| i == 'nts' }.empty?
        property :revision => (parts.select{|i| i =~ /r[0-9]+/ }).first
      
        property :type => case
        when branchinfo =~ /svn/         then :svn
        when branchinfo =~ /RC/          then :release_candidate
        when property(:revision).nil?    then :snap
        when branchinfo =~ /alpha|beta/  then :prerelease
        else :release
        end
        property :compiler => (parts.select{|i| i =~ /vc[0-9]+/i }).first.upcase

      rescue
        raise 'Unsupported PHP Build Type(Info Format)'
      end
    end
    
    property :version => [
        branchinfo,
        property(:revision)
      ].compact.join('-')
    
    self
  end

  ini <<-INI
    ;date.timezone is not in the defaults from run-tests.php,
    ;but 5.3 test cases require this to be set, and doing so 
    ;seems to eliminate some failures
    date.timezone=UTC
  INI
end
