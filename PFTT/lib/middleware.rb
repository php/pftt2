
require 'monkeypatch/string/unindent.rb'

module Middleware
  class Base
    
    include TestBenchFactor
    include PhpIni::Inheritable

    def self.instantiable
      All << self
    end

    attr_accessor :docroot, :host
    attr_accessor :deployed_php
    
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

    def _deploy_php_bin
      
      # ask scenarios for the folder to deploy PHP to
      deploy_to = nil
      # TODO @scenarios.map{|scn_type, scn| deploy_to||= scn.deployed_php(self) }
      
      unless deploy_to
        # fallback on storing php in a sub-folder in %SYSTEMDRIVE%/php-sdk/PFTT-PHPs or ~/php-sdk/PFTT-PHPs
        if @host.windows?
          deploy_to = @host.systemdrive+"/php-sdk/PFTT-PHPs"
        else
          deploy_to = '~/php-sdk/PFTT-PHPs'
        end
      end
      
      # ensure folder exists
      @host.mkdir(deploy_to)
      
      # if $force_deploy, make a new directory! otherwise, reuse existing directory (for quick manual testing can't take the time
      #          to copy everything again)
      @deployed_php ||= @host.join(deploy_to, ( @php_build[:version] + ((@php_build[:threadsafe])?'-TS':'-NTS') + ( $force_deploy ? '_'+String.random(4) : '' ) ) )
      
      #
      if $force_deploy or not File.exists?(php_binary()) or File.mtime(@php_build.path) >= File.mtime(php_binary())
        puts "uploading... "+@deployed_php
        @host.upload(@php_build.path,@deployed_php) unless @host.exist? @deployed_php
        puts "uploaded!"
      else
        puts "PFTT: reusing deployed php: #{@deployed_php}"
      end
      
      @deployed_php 
    end

    def _undeploy_php_bin
      if $force_deploy
       @host.delete( @deployed_php )
      end
    end

    def install r=nil
      _deploy_php_bin
      apply_ini(@current_ini)
    end
    
    def create_ini(scn_set, platform)
      apply_ini(@current_ini)
      
      # ask scenarios to add anything they need to this INI
      # TODO scn_set.ini(platform, @current_ini)
                  
      @current_ini
    end
 
    def uninstall r=nil
      _undeploy_php_bin
      unset_ini
    end

    def deploy_script( local_file )
      @deployed_scripts||=[]
      @deployed_scripts << @host.deploy( local_file, deploy_path )
    end

    def undeploy_script()
      @deployed_scripts.reject! do |script|
        @host.delete script
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
      date.timezone = 'UTC'
      unicode.runtime_encoding=ISO-8859-1
      unicode.script_encoding=UTF-8
      unicode.output_encoding=UTF-8
      unicode.from_error_mode=U_INVALID_SUBSTITUTE
    INI

  end

  All = (Class.new(TypedArray( Class )){include TestBenchFactorArray}).new #awkward, but it works.
end

# Load up all of our middleware classes right away instead of waiting for the autoloader
# this way they are actually available in Middleware::All
# although it technically does not matter the order in which they are loaded (as they will trigger
# autoload events on missing constants), reverse tends to get shallow before deep and should improve
# performance, if only marginally.
Dir.glob(File.join( File.dirname(__FILE__), 'middleware/**/*.rb')).reverse_each &method(:require)
