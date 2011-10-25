
module Util
  
end

def generate_shell_script(cmd, ruby_script)
  if $is_windows
    if File.exists?("#{cmd}.cmd")
      return
    end
    File.open("#{cmd}.cmd", "wb") do |f|
        
      f.puts("@echo off")
  
      # %* in batch scripts refers to all arguments
      f.puts("bundle exec ruby #{ruby_script} %*")
      f.close()
    end
  elsif not File.exists?(cmd)
    File.open(cmd, "wb") do |f|
      f.puts("#!/bin/bash")
      # $* in bash scripts refers to all arguments
      f.puts("bundle exec ruby #{ruby_script} $*")
      f.close()
      
      # make script executable
      system("chmod +x pftt")
    end
  end # if
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
