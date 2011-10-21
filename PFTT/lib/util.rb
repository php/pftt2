
module Util
  
end

def os_short_name(os)
  os.sub!('Windows', 'Win')
  os.sub!('RTM', 'SP0')
  
  # remove common words
  os.sub!('Professional', '')
  os.sub!('Standard', '')
  os.sub!('Enterprise', '')
  os.sub!('Microsoft', '')
  os.sub!('PC', '')
  os.sub!('-based', '')
  os.sub!('  ', ' ')
  os.sub!('  ', ' ')
  
  return os.lstrip.rstrip
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
