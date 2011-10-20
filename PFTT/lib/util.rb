
module Util
  
end

def cd_php_sdk(host)
  if host.posix?
    unless host.exist?('$HOME/php-sdk')
      host.mkdir('$HOME/php-sdk')
    end
    host.cd('$HOME/php-sdk')
  else
    unless host.exist?('%SYSTEMDRIVE%/php-sdk')
      host.mkdir('%SYSTEMDRIVE%/php-sdk')
    end
    host.cd('%SYSTEMDRIVE%/php-sdk')
  end
end
