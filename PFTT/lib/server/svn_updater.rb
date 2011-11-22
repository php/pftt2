
module Server
  class SVNUpdater
    def initialize(localhost)
      @localhost = localhost
    end
    def schedule(scheduler)
      # every 20 minutes, update an SVN checkout of PHP_5_4 and PHP_5_3 branches
      scheduler.every '20m' do |job|
        execute
      end
    end
    def execute
      ctx = Tracing::Context::Dependency::Install.new
      
      if @localhost.windows?
        svn_dir = @localhost.systemdrive+"\\php-sdk\\svn\\"
     
        unless @localhost.exist?(svn_dir+'\\branches')
          @localhost.mkdir(svn_dir)
          @localhost.exec!("svn checkout https://svn.php.net/repository/php/php-src", ctx, {:chdir=>svn_dir})
          @localhost.exec!("move php-src\branches .", ctx, {:chdir=>svn_dir})
          @localhost.exec!("move php-src\trunk .", ctx, {:chdir=>svn_dir})
          @localhost.exec!("move php-src\tags .", ctx, {:chdir=>svn_dir})
        end
        
        @localhost.exec!("svn update trunk branches\\PHP_5_4 branches\\PHP_5_3", ctx, {:chdir=>svn_dir})
      else
        svn_dir = "~/php-sdk/svn/"
              
        unless @localhost.exist?(svn_dir+'/branches')
          @localhost.mkdir(svn_dir)
          @localhost.exec!("svn checkout https://svn.php.net/repository/php/php-src", ctx, {:chdir=>svn_dir})
          @localhost.exec!("mv php-src/branches .", ctx, {:chdir=>svn_dir})
          @localhost.exec!("mv php-src/trunk .", ctx, {:chdir=>svn_dir})
          @localhost.exec!("mv php-src/tags .", ctx, {:chdir=>svn_dir})
        end
        
        @localhost.exec!("svn update branches/PHP_5_4 branches/PHP_5_3", ctx, {:chdir=>svn_dir})
      end
    end
  end
end

if __FILE__ == $0
  # bundle exec ruby lib\server\svn_updater.rb
  require Dir.pwd+'/bootstrap.rb'
  
  su = Server::SVNUpdater.new(Host::Local.new())
  su.execute
end
