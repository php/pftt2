
module Util
  module Install
    class Ruby < Base
   
      def label
        'Ruby'
      end
      
      protected
      
      def check_windows(ctx)
        @host.exist?(@host.systemdrive+'/jruby-1.6.5', ctx)#@host.exist?(@host.systemdrive+'/Ruby192')
      end
      
      def check_posix(ctx)
        has_cmd('ruby', ctx)
      end
      
      def install_windows(ctx)
        #copy_files('Ruby192', @host.systemdrive+'\\Ruby192')
        
        local_host = Host::Local.new()
        upload_7zip(local_host, @host)
        
        sd = @host.systemdrive
        unless @host.exist?("#{sd}\\jruby-1.6.5", ctx)# TODO ruby and jruby support Ruby192")
          # TODO Util::Install::Ruby should do MRI and JRuby (both implementations of ruby)
          
          # TODO TUE
          @host.upload_force(local_host.systemdrive+"/php-sdk/Ruby192.7z", @host.systemdrive+'/Ruby192.7z', ctx, false) # critical: false
                  
          @host.exec!("#{sd}\\php-sdk\\bin\\7za.exe x -o#{sd}\\ #{sd}\\Ruby192.7z ", ctx, {:chdir=>"#{sd}\\", :null_output=>true})
        end
        
        # TODO @host.exec!(@host.systemdrive+'\\Ruby192\\bin\\bundle install')
      end
      
      def install_emerge(ctx)
        emerge_install('ruby', ctx)
        
        @host.exec!('bundle install', ctx)
      end
      
      def install_yum(ctx)
        yum_install('ruby', ctx)
        
        @host.exec!('bundle install', ctx)
      end
      
      def install_apt_get(ctx)
        apt_get_install('ruby', ctx)
        
        @host.exec!('bundle install', ctx)
      end
      
    end
  end
end
