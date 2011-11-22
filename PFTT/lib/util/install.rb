
# LATER config file
$share_name = "\\\\terastation\\share"
$folder = "\\PFTT\\deps"

require 'util/package.rb'

module Util
  module Install
    class Base
      #LATER abstract_class
      
      def self.add_to_path_temp(host, path_part)
        if host.windows?
          path = host.line!('echo %PATH%')
          
          new_path = '"' + path_part + '";' + path
          
          host.cmd("set PATH=#{new_path}")
          
          return new_path
        else
          path = host.line!('echo $PATH')
          
          new_path = '"' + path_part + '":' + path
          
          host.cmd("export PATH=#{new_path}")
          
          return new_path
        end
      end
      
      def self.add_to_path_permanently(host, path_part, ctx)
        new_path = add_to_path_temp(host, path_part)
        
        if host.windows?
          host.exec!("setx PATH #{new_path}", ctx)
          
        else
          # LATER update bash shell history, etc...
        end
      end
      
      def initialize(host)
        @host = host
      end
      
      def windows?
        true
      end
      
      def posix?
        true
      end
      
      def installed?
        ctx = Tracing::Context::Dependency::Check.new
        
        if @host.windows?
          return check_windows(ctx)
        else
          return check_posix(ctx)
        end
      end
        
      def ensure_installed
        unless windows?
          if @host.windows?
            return :wrong_platform
          end
        end
        unless posix?
          if @host.posix?
            return :wrong_platform
          end
        end
                            
        puts "PFTT:prereq: checking if #{@host.name} has #{label} installed..."
        if installed?
          puts "PFTT:prereq: #{@host.name} has #{label} installed."
          return :installed
        end
        puts "PFTT:prereq: installing #{label} on #{@host.name}..."
        return install()
      end
        
      def install
        ctx = Tracing::Context::Dependency::Install.new
        
        if @host.windows?
          install_windows(ctx)
        elsif has_cmd('emerge')
          # gentoo linux
          install_emerge(ctx)
        elsif has_cmd('yum')
          # redhat/fedora linux
          install_yum(ctx)
        elsif has_cmd('apt-get')
          # debian/ubuntu based
          install_apt_get(ctx)
        else
          return :not_installed
        end
        return :installed
      end
        
      protected
        
      def copy_files(src, dst, ctx)
        # copy files from src to dst instead of running a setup EXE or MSI file
        #
        # usage example:
        # copy_files('Ruby192', @host.systemdrive+'\\Ruby192')
        #
        @host.exec!("net use S: #{$share_name} /user:test /persistent:no test", ctx)
        # /I important! also /E
        @host.exec!("XCOPY /E /Q /I S:#{$folder}\\#{src} #{dst}", ctx)
        @host.exec!("net use S: /d", ctx)
      end
  
      def msi_install(msi, opts, ctx)
        @host.exec!("net use R: #{$share_name} /user:test /persistent:no test", ctx)
        if msi.ends_with?('.exe')
          @host.exec!("R:#{$folder}\\#{msi} #{opts}", ctx)
        else
          @host.exec!("msiexec /i R:#{$folder}\\#{msi} #{opts}", ctx)
        end
        @host.exec!("net use R: /d", ctx)
      end
  
      def emerge_install(name, ctx)
        @host.exec!("emerge #{name}", ctx)
      end
  
      def yum_install(name, ctx)
        @host.exec!("yum install #{name}", ctx)
      end
  
      def apt_get_install(name, ctx)
        # install using apt-get on Debian Linux or Mac OS X (with Fink installed)
        @host.exec!("apt-get install #{name}", ctx)
      end
  
      def has_cmd(cmd, ctx)
        if @host.windows?
          @host.line!("where #{cmd}", ctx).length > 0
        else
          @host.line!("which #{cmd}", ctx).length > 0
        end
      end
  
      def check_files(check, ctx)
        if check.is_a?(String)
          return @host.exist?(check, ctx)
        elsif check.is_a?(Hash)
          list = @host.glob(check[:base], check[:glob], ctx)
      
          count = check[:count] || 1
            
          return list.length >= count
        elsif check.is_a?(Array)
          check.each do |a|
            if check_files(a, ctx)
              return true
            end
          end
        end
        return false
      end
    end
  end
end
