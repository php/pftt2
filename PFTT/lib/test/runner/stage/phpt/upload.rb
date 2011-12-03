
module Test
  module Runner
    module Stage
      module PHPT
      
class Upload < Tracing::Stage::ByHostMiddleware
  
  def run(psc, host, local_phpt_zip)
    notify_start
    psc.deploy
  
    local_host = Host::Local.new()
            
    upload_7zip(local_host, host)
    remote_phpt_zip = host.systemdrive+'/PHP_5_4.7z'
    host.upload_force(local_phpt_zip, remote_phpt_zip, Tracing::Context::Phpt::Upload.new)
    host.delete_if(host.systemdrive+'/abc', Tracing::Context::Phpt::Decompress.new)
    host.delete_if(host.systemdrive+'/PHP_5_4', Tracing::Context::Phpt::Decompress.new)
    # TODO unpackage(host, host.systemdrive, remote_phpt_zip)
    sd = host.systemdrive
    #sleep(20)
    # critical: must chdir to output directory or directory where 7zip file is stored!!!
    host.exec!("#{sd}\\php-sdk\\bin\\7za.exe x -o#{sd}\\ #{sd}\\PHP_5_4.7z ", Tracing::Context::Phpt::Decompress.new, {:chdir=>"#{sd}\\", :null_output=>true})
    # TODO host.move(host.systemdrive+'/PHP_5_4', host.systemdrive+'/abc')
    #sleep(20)
    # TODO use test_ctx.new
    host.cmd!("move #{sd}\\php_5_4 #{sd}\\abc", Tracing::Context::PhpBuild::Compress.new)
            
    notify_end(true)
  end # def run
  
end # class Upload

      end # module PHPT
    end # module Stage
  end
end
