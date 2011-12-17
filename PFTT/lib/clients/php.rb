
require 'clients.rb'

module Clients
  module PHP
      
# TODO function args
class Client < ClientBase
  include Clients::PHP::Building
  include Clients::PHP::Testing
  

  def host_config(host)
    # TODO host_config
  end # def host_config

  def cs
    # LATER scenario config
  end # def cs
    
  def inspect
    # inspects what the configuration and arguments will have pftt do for the core_full or core_part actions
    # (lists PHPTs that will be run, hosts, middlewares, php builds and contexts)
    
    report = Report::Inspect::Func.new(CONFIG.selected_tests().flatten())
    return report.text_print()
  end # def inspect
        
  def host_config
    # TODO
    
    iis = Util::Install::IIS.new($hosts.first)
    iis.ensure_installed
    
  end # def host_config
    
  def zip7(dir)
    # TODO install 7 zip using msi
    # TODO
    php = PhpBuild.new('C:\\php-sdk\\builds\\5_4\\'+$php_build_path)
    dir = 'C:/php-sdk/pftt-telemetry/'+php[:version]
    #
    # see http://docs.bugaco.com/7zip/MANUAL/switches/method.htm
    # see http://www.comprexx.com/UsrHelp/Advanced_Compression_Parameters_(7-Zip_Archive_Type).htm
    # see http://www.dotnetperls.com/7-zip-examples
    # (-mmt seems to override mt=)
    host = Host::Local.new
    zip_file = host.mktempfile('.7z', nil)
    
    cmd_string = "\""+host.systemdrive()+"\\Program Files\\7-Zip\\7z\" a -ms=on -mfb=20 -mx1 -m0=LZMA2:mt=32 -ssc #{zip_file} \"#{dir}\""
      puts cmd_string
    host.exec!(cmd_string, nil, {:null_output=>true})
    
    return zip_file
  end
  
  def recover_psc
    test_telem_dir = Host::Remote::PSC::HostRecoveryManager.new($hosts, $phps[0], $middlewares[0], $scenarios[0])
     
  #    $hosts.each do |host|
  #      i = Util::Install::IIS.new(host)
  #      i.ensure_installed
  #    end
      
#    zip_file = zip7(test_telem_dir)
 #   puts zip_file
#    upload(zip_file)
#        
#    base = lookup()
#    
#    store(test)
#    
#    report(base, test)
  end # def recover_psc
          
  def review
    d = Tracing::Dashboard::Display.new()
    s = Tracing::Dashboard::EnvVarSlash.new(Host::Local.new(), ARGV.join(' '), nil)#'C:\\php-sdk\\')
    d.add(s)
    s = Tracing::Dashboard::Diff.new(Host::Local.new())
    d.add(s)
    d.show # or d.show s.show ensures that d.show is called
        
    #    om = Diff::OverrideManager.new
    #    
    #    om.read
    #    
    #    exit
    #    diff = Diff::ZD::AllTestCases::SingleCombo::BaseRun.new(:added, Test::Telemetry::Folder::Phpt.new('C:\\php-sdk\\PFTT-Telemetry\\OI1-PHP-FUNC-15.2011-12-09_16-31-51_-0800'))
    #    #diff = Diff::ZD::AllTestCases::AllCombos::BaseMinusTest.new(:added, Test::Telemetry::Folder::Phpt.new('C:\\php-sdk\\PFTT-Telemetry\\Win2k8r2sp_539rc1_'), Test::Telemetry::Folder::Phpt.new('C:\\php-sdk\\PFTT-Telemetry\\Win2k8r2sp_539rc2_'))
    #    
    #    puts "All Extra Warnings(1): "
    #    puts diff.find('Warning').to_override
    #    puts
    #    puts "All Other Inserts/Deletes(2): "
    #    puts diff.find_not('Warning').to_s
        
    #    .to_chunk_replacements
    #    diff.save_chunk_replacements()
        
    #    exit
  end # def review
  

end # class Client  

end # module PHP  
end # module Clients
