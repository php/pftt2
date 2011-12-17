
require 'monkeypatch/string/unindent.rb'
require 'util/install.rb'

module Middleware
  class Base
    @@temp_lock = Mutex.new 
    
    include Test::Factor
    include PhpIni::Inheritable
 
#    def self.instantiable
#      All << self
#    end

    attr_accessor :docroot, :host
    attr_accessor :deployed_php
    
    def self.from_xml(xml, host, php_build, scn_set)
      case xml['@name']
      when 'CLI'
        return Middleware::Cli.new(host, php_build, scn_set)
      when 'IIS-FastCGI'
        return Middleware::Http::Iis::FastCgi.new()
      when 'IIS-FastCGI-WinCache'
        return Middleware::Http::Iis::FastCgi::Wincache.new(host, php_build, scn_set)
      when 'Apache-ModPHP'
        return Middleware::Http::Apache::ModPhp.new(host, php_build, scn_set)
      when 'Apache-ModPHP-APC'
        return Middleware::Http::Apache::ModPhp::APC.new(host, php_build, scn_set)
      when 'Apache-ModPHP-APC-IGBinary'
        return Middleware::Http::Apache::ModPhp::APC::IGBinary.new(host, php_build, scn_set)
      end
    end
    
    def to_xml
      {
        '@name' => mw_name
      }
    end     
    
    def close
      @host.close
    end
    
    def self.to_s
      mw_name
    end
    
    def to_s
      mw_name
    end
    
    def ==(o)
      return self.class == o.class
    end

    #def ini(arg=nil)
      #ret = super
      # if we're getting the whole stack, push the extensions_dir to the *top*.
      #PhpIni.new(%Q{extension_dir="#{@deployed_php}/ext"}).configure(ret) if arg.nil?
      #ret
    #end

    # now start defining our base
    def initialize( host, php_build, scenarios )
      @host = host
      @php_build = php_build
      @scenarios = scenarios
    end
    
    def describe
      @description ||= self.class.to_s.downcase.gsub('::','-')
    end
    
    def filtered_expectation(test_case, filtered_expectation)
      return filtered_expectation # see Middleware::Http#filtered_expectation
    end

    def _deploy_php_bin(ctx)
      # TODO try running php.exe -n --help to make sure it will run (VC9 runtime, etc...) 
      
      
      # ask scenarios for the folder to deploy PHP to
      deploy_to = nil
      # TODO @scenarios.map{|scn_type, scn| deploy_to||= scn.deployed_php(self) }
      
      unless deploy_to
        # fallback on storing php in a sub-folder in %SYSTEMDRIVE%/php-sdk/PFTT-PHPs or ~/php-sdk/PFTT-PHPs
        if @host.windows?(ctx)
          deploy_to = @host.systemdrive(ctx)+"/php-sdk/PFTT-PHPs"
        else
          deploy_to = '~/php-sdk/PFTT-PHPs'
        end
      end
      
      # ensure folder exists
      @host.mkdir(deploy_to, ctx)
      
      # if $force_deploy, make a new directory! otherwise, reuse existing directory (for quick manual testing can't take the time
      #          to copy everything again)
      @deployed_php ||= @php_build.path # TODO @host.join(deploy_to, ( @php_build[:version] + ((@php_build[:threadsafe])?'-TS':'-NTS') + ( $force_deploy ? '_'+String.random(4) : '' ) ) )
      
      #    
#  TODO TUE    if $force_deploy or not File.exists?(php_binary()) or File.mtime(@php_build.path) >= File.mtime(php_binary())
        unless $hosted_int
          # TODO bug - this will compress multiple copies of PHP build at same time if using multiple hosts!
        puts "PFTT:deploy: uploading... "+@deployed_php
        
        # TODO TUE
        zip_name = nil
        @@temp_lock.synchronize do 
        zip_name = package_php_build(Host::Local.new(), 'c:/php-sdk/builds/5_4/'+$php_build_path)
        end
        
          # TODO package_php_build
      host.upload_force(zip_name, host.systemdrive(ctx)+'/5.4.0beta2-NTS.7z', false, ctx) # critical: false
                
      # TODO check if build already on remote host (so compression and upload can be skipped)
      sd = host.systemdrive
          ctx = Tracing::Context::Dependency::Check.new # TODO ctx.new
      host.delete_if("#{sd}\\php-sdk\\PFTT-PHPs\\5.4.0beta2-NTS", ctx)
          
      host.exec!("#{sd}\\php-sdk\\bin\\7za.exe x -o#{sd}\\php-sdk\\PFTT-PHPs #{sd}\\5.4.0beta2-NTS.7z ", ctx, {:chdir=>"#{sd}\\", :null_output=>true})
        #@host.upload(@php_build.path,@deployed_php) 
        puts "PFTT:deploy: uploaded!"
        end
#      else
#        puts "PFTT:deploy: reusing deployed php: #{@deployed_php}"
#      end
      
      @deployed_php 
    end

    def _undeploy_php_bin(ctx)
      if $force_deploy
       @host.delete( @deployed_php )
      end
    end

    def install ctx, r=nil
      # LATER set host's clock to the same time as the client's clock
      
      _deploy_php_bin(ctx)
      apply_ini(@current_ini)
      
      unless $hosted_int
      vc9 = Util::Install::VC9.new(@host)
      vc9.ensure_installed(ctx)
      
      if @host.windows?
        # turn on file sharing... (add PHP_SDK) share
        # make it easy for user to share files with this windows machine
        # (during cleanup or analysis or triage, after tests have run)
        #
        # user can access PHP_SDK and the system drive ( \\hostname\C$ \\hostname\G$ etc...)
        #
        if @host.credentials
          @host.exec!('NET SHARE PHP_SDK='+@host.systemdrive+'\\php-sdk /Grant:"'+@host.credentials[:user]+'",Full', ctx)
        end
      end
      end
    end
    
    def create_ini(scn_set, platform)
      apply_ini(@current_ini)
      
      # ask scenarios to add anything they need to this INI
      # TODO scn_set.ini(platform, @current_ini)
                  
      @current_ini
    end
 
    def uninstall ctx, r=nil
      _undeploy_php_bin(ctx)
      unset_ini
    end

    def deploy_script( local_file )
      @deployed_scripts||=[]
      @deployed_scripts << @host.deploy( local_file, deploy_path )
    end

    def undeploy_script(ctx)
      @deployed_scripts.reject! do |script|
        @host.delete script, ctx
      end
    end

    # returns true if the ini was changed.
    # this is so that server-based installs can get restarted.
    # the php_ini should be whatever is *on top* of this class' compiled ini.
    def apply_ini( php_ini=[] )
      new_ini = PhpIni.new(%Q{extension_dir="#{@deployed_php}/ext"})
      new_ini << base_ini
      new_ini << ( php_ini || [] )
      #filtered_ini = PhpIni.new new_ini.to_a.map{|e| f = @host.escape(e); puts "   #{f}"; f }
      if new_ini == @current_ini
        return false
      else
        @current_ini = new_ini
        true
      end
    end

    # the base_ini is the culumnation of all applied ini in middleware, host, php, and scenarios.
    def base_ini
      if @base_ini.nil?
        @base_ini = PhpIni.new()
        [@host,self,@php].flatten.each do |factor|
          next unless factor.respond_to? :ini
          @base_ini << factor.ini
        end
      end
      PhpIni.new @base_ini
    end
    
    def root r=nil
      if r
        return r
      else 
        return @host.systemdrive
      end
    end

    def unset_ini
      @current_ini = []
    end

    def current_ini
      @current_ini ||= []
    end

    # E_ALL | E_STRICT (from run-tests.php) == 32767
    # see: http://php.net/manual/en/errorfunc.constants.php
    ini <<-INI
      display_startup_errors=0
      output_handler=      
      safe_mode=0
      disable_functions=
      output_buffering=Off
      error_reporting=32767
      display_errors=1
      log_errors=0
      html_errors=0
      track_errors=1
      report_memleaks=1
      report_zend_debug=0      
      docref_ext=.html
      error_prepend_string=
      error_append_string=
      auto_prepend_file=
      auto_append_file=
      magic_quotes_runtime=0
      ignore_repeated_errors=0
      precision=14
      date.timezone='UTC'
      unicode.runtime_encoding=ISO-8859-1
      unicode.script_encoding=UTF-8
      unicode.output_encoding=UTF-8
      unicode.from_error_mode=U_INVALID_SUBSTITUTE
      phar.readonly=0
    INI

  end

  # TODO jruby All = (Class.new(TypedArray( Class )){include Test::FactorArray}).new #awkward, but it works.
end

# Load up all of our middleware classes right away instead of waiting for the autoloader
# this way they are actually available in Middleware::All
# although it technically does not matter the order in which they are loaded (as they will trigger
# autoload events on missing constants), reverse tends to get shallow before deep and should improve
# performance, if only marginally.
# TODO jruby Dir.glob(File.join( File.dirname(__FILE__), 'middleware/**/*.rb')).reverse_each &method(:require)
