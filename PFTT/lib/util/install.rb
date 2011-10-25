
def add_to_path_temp(host, path_part)
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

def add_to_path_permanently(host, path_part)
  new_path = add_to_path_temp(host, path_part)
  
  if host.windows?
    host.exec!("setx PATH #{new_path}")
    
  else
    # LATER update bash shell history, etc...
  end
end

# deps/client deps/server deps/host
def install(host, cmd, linux_name, win_installer_server_path, win_installer_options)#, type=:client)
  if host.windows?
    win_install(host, cmd, win_installer_server_path, win_installer_options)
  else
    linux_install(host, cmd, linux_name)
  end
end

def linux_install(host, cmd, linux_name)
  out_err, status = host.exec!("WHICH #{cmd}")
    
  if status == 0
    # already installed
    return
  end
    
  # determine which linux distro is on host by searching for emerge, yum and apt-get
  #
  if host.line!("which emerge").length > 0
    # Gentoo based, try installing
    exec("emerge #{linux_name}")
    
  elsif host.line!("which yum").length > 0
    # Redhat/Fedora based, try installing
    exec("yum install #{linux_name}")
    
  elsif host.line!("which apt-get").length > 0
    # Debian based, try installing
    exec("apt-get install #{linux_name}")
    
  end
end

def win_install_installed?(cmd)
  out_err, status = host.exec!("WHERE #{cmd}")
      
  return status == 0
end
        
def win_install_copy(host, win_installer_server_path)
  # download installer into  __DIR__/deps folder
  host.exec!("NET USE R: \\#{$dep_server_share}\ /user:#{$dep_server_user} /persistent:no #{$dep_server_password}")
    
  local_win_install_path = "#{__DIR__}/deps/#{win_installer_server_path}"
    
  host.exec!("COPY R:/PFTT/DEPS/#{win_installer_server_path} #{local_win_install_path}")
    
  host.exec!("NET USE R: /D")
end
   
def win_install(host, cmd, win_installer_server_path, win_installer_options)
  if win_install_installed?(cmd)
    return
  end  
  
  win_install_copy(win_installer_server_path)
  
  # run installer
  host.exec!("#{local_win_install_path} #{win_installer_options}")
end
