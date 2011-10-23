
module Server
  class SVNUpdater
    def execute
      localhost.exec!("svn checkout https://svn.php.net/repository/php/php-src", {:chdir=>"c:/php-sdk/svn/"})
      localhost.exec!("move php-src\branches .", {:chdir=>"c:/php-sdk/svn/"})
      localhost.exec!("move php-src\trunk .", {:chdir=>"c:/php-sdk/svn/"})
      localhost.exec!("move php-src\tags .", {:chdir=>"c:/php-sdk/svn/"})
      localhost.exec!("svn update branches\PHP_5_4 branches\PHP_5_3", {:chdir=>"c:/php-sdk/svn/"})
    end
  end
end