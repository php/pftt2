
def unpackage(remote_host, remote_dir, remote_zip)
  ctx = Tracing::Context::PhpBuild::Decompress.new
  
  remote_host.format_path!(remote_dir, ctx)
  remote_host.format_path!(remote_zip, ctx)
    
  # TODO null.txt
  remote_host.exec!(remote_host.systemdrive+"/php-sdk/bin/7za.exe x -o#{remote_dir}\\ #{remote_zip} > "+remote_host.systemdrive+"\\null.txt", ctx)
end

def upload_7zip(local_host, remote_host)
  # LATER linux support for 7zip
  remote_host.upload_if_not(local_host.systemdrive+"/php-sdk/0/PFTT2/PFTT/7za.exe", remote_host.systemdrive+'/php-sdk/bin/7za.exe', Tracing::Context::Dependency::Install.new)
end  

def package_php_build(local_host, build_path)
  ctx = Tracing::Context::PhpBuild::Compress.new
  
  zip_name = File.basename(build_path)+'.7z'
  
  local_host.format_path!(zip_name, ctx)
  local_host.format_path!(build_path, ctx) 
  
  local_host.exec!(host.systemdrive+"/php-sdk/0/PFTT2/PFTT/7za #{zip_name} #{build_path}", ctx)
  
  return zip_name
end

def package_telemetry(local_host, path)
  # TODO
  # pftt_php54_rXXXXX.7z
  # php54_rxxxx/
  # php54_rxxxx/CLI/
  # php54_rxxxx/CLI/[hostname]
  #
  # store in /PFTT-Results/PHP_5_4/[rXXXXX]/[pftt_php54_rXXXX].7z
end

def package_svn(local_host, path)
  ctx = Tracing::Context::PhpBuild::Compress.new
  
  tmp_dir = File.join(local_host.tmpdir(ctx), File.basename(path))
  
  local_host.format_path!(tmp_dir, ctx)
  
  # there's a bunch of files we should remove/not include in zip, copy folder to a temporary folder
  # and then remove the files from that and then compress it
  local_host.copy(path, tmp_dir, ctx)
  
  # remove any .svn directories
  if local_host.windows?(ctx)
    # FOR /F "tokens=*" %G IN ('DIR /B /AD /S *.svn*') DO RMDIR /S /Q "%G"
    local_host.cmd!("FOR /F \"tokens=*\" %%G IN ('DIR /B /AD /S *.svn*') DO RMDIR /S /Q \"%%G\"'", ctx)
  else
    local_host.exec!("rm -rf `find . -type d -name .svn`", ctx)
  end
  
  # remove Release and Release_TS (maybe this svn copy was compiled?)
  local_host.delete_if(File.join(tmp_dir, 'Release'), ctx)
  local_host.delete_if(File.join(tmp_dir, 'Release_TS'), ctx)
  # LATER local_host.delete_if(File.join(tmp_dir, 'php_test_results_*'))
  
  zip_name = tmp_dir+'.7z'
  
  # we've removed as much as we really can, compress it
  local_host.exec!(local_host.systemdrive+"/php-sdk/0/PFTT2/PFTT/7za a #{zip_name} #{tmp_dir}", ctx)
  
  local_host.delete(tmp_dir, ctx)
  
  return zip_name
end
