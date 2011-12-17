
# TODO package telemetry from a single OS/host (or subset of a few OS/hosts) in a single archive
def unpackage(remote_host, remote_dir, remote_zip)
  ctx = Tracing::Context::PhpBuild::Decompress.new
  
  remote_host.format_path!(remote_dir, ctx)
  remote_host.format_path!(remote_zip, ctx)
    
  remote_host.exec!(remote_host.systemdrive+"/php-sdk/bin/7za.exe x -o#{remote_dir}\\ #{remote_zip}", ctx, {:null_output=>true})
end

def upload_7zip(local_host, remote_host)
  # LATER linux support for 7zip
  remote_host.upload_if_not(local_host.systemdrive+"/php-sdk/0/PFTT2/PFTT/7za.exe", remote_host.systemdrive+'/php-sdk/bin/7za.exe', Tracing::Context::Dependency::Install.new)
end  

def package_php_build(local_host, build_path)
  ctx = Tracing::Context::PhpBuild::Compress.new
  
  cached_zip_name = 'c:/php-sdk/0/PFTT2/cache/PHP_5_4.7z'
  if local_host.exist?(cached_zip_name)
    puts "PFTT: compress: reusing #{cached_zip_name}"
    return cached_zip_name
  end
  
  zip_name = local_host.mktempdir(ctx) + '/' + File.basename(build_path)+'.7z'
  
  local_host.format_path!(zip_name, ctx)
  local_host.format_path!(build_path, ctx) 
  
  local_host.exec!(local_host.systemdrive+"/php-sdk/0/PFTT2/PFTT/7za a #{zip_name} #{build_path}", ctx)
  
  # cache archive for next time
  local_host.copy(zip_name, cached_zip_name, ctx)
  
  return zip_name
end

def package_telemetry(local_host, path)
  # TODO
  # pftt_php54_rXXXXX.pftt_telem.7z
  # php54_rxxxx/
  # php54_rxxxx/CLI/
  # php54_rxxxx/CLI/[hostname]
  #
  # store in /PFTT-Telemetry/PHP_5_4/[rXXXXX]/[pftt_php54_rXXXX].7z
  #
  # pftt_telem.7z indicates it was made by pftt, so pftt can search diff files directly from it, etc...
  
  #
  # see http://docs.bugaco.com/7zip/MANUAL/switches/method.htm
  # see http://www.comprexx.com/UsrHelp/Advanced_Compression_Parameters_(7-Zip_Archive_Type).htm
  # see http://www.dotnetperls.com/7-zip-examples
  # (-mmt seems to override mt=)
  # 7z a -ms=on -mfb=200 -mx1 -m0=LZMA2:mt=4 -ssc [archive] [folder]
  #
  # {:null_output=>true}
end

def package_svn(local_host, path, test_cases)
  ctx = Tracing::Context::PhpBuild::Compress.new
  
  tmp_dir = File.join(local_host.mktmpdir(ctx), File.basename(path))
    
  # put the PHPTs in a sub-folder of tmp_dir (tmp_dir will also store the archive)
  package_tmp_dir = tmp_dir+'/PHP_5_4' # TODO
  
  test_dirs = {}
  greatest_mtime = 0
  
  # copy all files, sub folders and files from any directory named 'test' (in ext, sapi, tests, or Zend, or others that get added)
  # (don't need the complete source code, just the PHPTs and files they might need)
  # (the fewer the files, the smaller the archive, the faster the test cycle => greater SDE/SDET productivity)
  # TODO local_host.glob(path, '**/*', ctx) do |file|
  
  # TODO should include the base path for each set of test cases
  #        here test_cases is just 1 array, but should be test_cases = [test_cases]
  
  test_cases.each do |test_case|
  #Dir.glob(path+'/**/*') do |file|
    # for each file
    s_file = test_case.relative_path
    
    file = Host.join(path, test_case.relative_path)
    
    #if s_file.include?('/tests/') or s_file.include?('/test/')
      
      mtime = local_host.mtime(file)
      if mtime > greatest_mtime
        greatest_mtime = mtime
      end
      
      # TODO cache test_files copy and only add to test_dirs if the cached file is older or missing
      
      # save the list of dirs... we could copy each test file, buts its fewer copy operations
      # to copy entire directories => so the copy process is faster
      test_dirs[File.dirname(file)] = File.dirname(Host.join(package_tmp_dir, s_file))
    #end
  end 
  #
  
  # TODO (also have separate cache folders for test files, builds, etc...)
  cached_zip_name = 'c:/php-sdk/0/PFTT2/cache/PHPT_5_4.7z'
  
  if local_host.exists?(cached_zip_name)
    # if none of the test files has been modified since creation of cached copy of archive, then
    # there is no need to copy and archive the test files a second time (just use the cached archive)
    if local_host.mtime(cached_zip_name) > greatest_mtime
      # don't need to copy+compress again
      puts "PFTT: reusing cached tests #{cached_zip_name}"
      return cached_zip_name
      # TODO check copy on remote_host
    end
    puts "PFTT: tests updated, re-creating #{cached_zip_name}"
  end
  # go ahead and create a new archive of test_files
  #
  
  # TODO if ony a few phpt files have changed, but host has the rest, just upload the changed ones (important to be fast)
  
  # copy test dirs
  test_dirs.each do |entry|
    local_host.copy(entry[0], entry[1], ctx)
  end
  
  # LATER check path for any PHPTs that didn't get included (PHPTs in wrong place)
  
  zip_name = tmp_dir+'.7z'
  
  # make it even smaller, compress it! (copying lots of little files takes a lot of overhead bandwidth)
  local_host.exec!(local_host.systemdrive+"/php-sdk/0/PFTT2/PFTT/7za a #{zip_name} #{package_tmp_dir}", ctx)
  
  # cleanup temp dir
  local_host.delete(package_tmp_dir, ctx)
  
  # cache archive for next time
  local_host.copy(zip_name, cached_zip_name, ctx)
  
  # return archive name (LATER delete tmp_dir and zip_name when no longer needed)
  return zip_name
end
