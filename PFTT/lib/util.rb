
module Util
  
end

class String
  def convert_path
    self.gsub('\\','/')
  end
  def convert_path!
    self.gsub!('\\','/')
  end
  def self.not_nil(a)
    if a==nil
      ''
    else
      a
    end
  end
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
  os.gsub!('Windowsr', 'Win') # (r)
  os.gsub!('Microsoftr', '') # (r)
  os.gsub!('Serverr', '') # (r)
  os.gsub!('Developer Preview', 'Win 8')
  os.gsub!('Win 8 Win 8', 'Win 8')
  os.gsub!('Full', '')
  os.gsub!('Installation', '')
  os.gsub!('(', '')
  os.gsub!(')', '')
  os.gsub!('tm', '')
  os.gsub!('VistaT', 'Vista')
  
  os.gsub!('Windows', 'Win')
    
  # remove common words
  os.gsub!('Professional', '')
  os.gsub!('Standard', '')
  os.gsub!('Enterprise', '')
  os.gsub!('Basic', '')
  os.gsub!('Premium', '')
  os.gsub!('Ultimate', '')
  unless os.include?('XP')
    # XP Home != XP Pro
    os.gsub!('Home', '')
  end
  os.gsub!('Win Win', 'Win')
  os.gsub!('(R)', '')
  os.gsub!(',', '')
  os.gsub!('Edition', '')
  os.gsub!('2008 R2', '2008r2')
  os.gsub!('2003 R2', '2003r2')
  os.gsub!('RTM', '')
  os.gsub!('Service Pack 1', '')
  os.gsub!('Service Pack 2', '')
  os.gsub!('Service Pack 3', '')
  os.gsub!('Service Pack 4', '')
  os.gsub!('Service Pack 5', '')
  os.gsub!('Service Pack 6', '')
  os.gsub!('Microsoft', '')
  os.gsub!('N/A', '')
  os.gsub!('PC', '')
  os.gsub!('Server', '')
  os.gsub!('-based', '')
  os.gsub!('Build', '')
  # 
  os.gsub!('6.1.7600', '')
  os.gsub!('6.1.7601', '')
  os.gsub!('6.0.6000', '')
  os.gsub!('6.0.6001', '')
  os.gsub!('6.0.6002', '')
  os.gsub!('5.1.3786', '')
  os.gsub!('5.1.3787', '')
  os.gsub!('5.1.3788', '')
  os.gsub!('5.1.3789', '')
  os.gsub!('5.1.3790', '')
  os.gsub!('5.0.2600', '')
  os.gsub!('5.0.2599', '')
  os.gsub!('5.2.SP2', '')
  os.gsub!('5.2', '')
  os.gsub!('7600', 'SP0') # win7/win2k8r2 sp0
  os.gsub!('7601', 'SP1') # win7/win2k8r2 sp1
  os.gsub!('6000', 'SP0') # winvista/win2k8 sp0
  os.gsub!('6001', 'SP1') # winvista/win2k8 sp1
  os.gsub!('6002', 'SP2') # winvista/win2k8 sp2
  os.gsub!('3786', 'SP0') # win2k3 sp0?
  os.gsub!('3787', 'SP1') # win2k3 sp1?
  os.gsub!('3788', 'SP0') # win2k3r2 sp0?
  os.gsub!('3789', 'SP1') # win2k3r2 sp1?
  os.gsub!('3790', 'SP2') # win2k3r2 sp2
  os.gsub!('2600', 'SP3') # windows xp sp3
  os.gsub!('2599', 'SP2') # windows xp sp2?
  #
  os.gsub!('  ', ' ')
  os.gsub!('  ', ' ')
  os.gsub!('  ', ' ')
  os.gsub!('  ', ' ')
  os.gsub!('  ', ' ')
  os.gsub!('  ', ' ')
  os.gsub!('  ', ' ')
  os.gsub!('  ', ' ')
  os.gsub!('  ', ' ')
  os.gsub!('  ', ' ')
  os.gsub!('  ', ' ')
  os.gsub!('  ', ' ')
  os.gsub!('  ', ' ')
  os.gsub!('  ', ' ')
  os.gsub!('  ', ' ')
  os.gsub!('  ', ' ')
  os.gsub!('  ', ' ')
  os.gsub!('  ', ' ')
  os.gsub!('  ', ' ')
  os.gsub!('  ', ' ')
  os.gsub!('  ', ' ')
  os.gsub!('  ', ' ')
  os.gsub!('  ', ' ')
  os.gsub!('  ', ' ')
  
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
