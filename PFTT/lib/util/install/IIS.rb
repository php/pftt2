
module Util
  module Install
    class IIS < Base
            
      def label
        'IIS'
      end
      
      def posix?
        false # windows only
      end
      
      protected
      
      def check_windows(ctx)
        if @host.longhorn?(ctx)
          return @host.exist?(@host.systemroot+'/System32/inetsrv/appcmd.exe', ctx)
        else
          return @host.exist?(@host.systemroot+'/System32/inetsrv/metabase.xml', ctx)
        end
      end
      
      def install_windows(ctx)
        # there is a different installation procedure for Longhorn (Vista/2008+) and pre-Longhorn Windows (2003r2-)
        if @host.longhorn?(ctx)
          install_longhorn(ctx)
        else
          install_prelh(ctx)
        end
      end
      
      def install_longhorn(ctx)
        # longhorn+
        # see: http://learn.iis.net/page.aspx/334/install-and-configure-iis-on-server-core/
        # note: start /w is critical: without it pkgmgr will return immediately (even though its not done)
        @host.exec!("START /w PKGMGR.EXE /norestart /l:log.etw /iu:IIS-WebServerRole;IIS-WebServer;IIS-CommonHttpFeatures;IIS-StaticContent;IIS-DefaultDocument;IIS-DirectoryBrowsing;IIS-HttpErrors;IIS-HttpRedirect;IIS-CGI;IIS-ISAPIExtensions;IIS-ISAPIFilter;IIS-HealthAndDiagnostics;IIS-HttpLogging;IIS-LoggingLibraries;IIS-RequestMonitor;IIS-HttpTracing;IIS-CustomLogging;IIS-Performance;IIS-HttpCompressionStatic;IIS-HttpCompressionDynamic;IIS-WebServerManagementTools;IIS-ManagementScriptingTools;IIS-IIS6ManagementCompatibility;IIS-Metabase;IIS-WMICompatibility;IIS-LegacyScripts;WAS-WindowsActivationService;WAS-ProcessModel;IIS-FTPServer;IIS-FTPSvc;IIS-FTPExtensibility;IIS-WebDAV;IIS-ManagementService", ctx)
      end
      
      def install_prelh
        # pre-longhorn windows (xp, 2003, 2003r2)
        # see: http://www.microsoft.com/technet/prodtechnol/WindowsServer2003/Library/IIS/efefcb53-b86e-4cac-9b4b-fcf5f1145aa9.mspx
        # see: http://support.microsoft.com/kb/222444
        answer_str = <<-ANSWER
[Components]
iis_www = on
iis_common = on
iis_ftp = on
aspnet= on
iis_asp = on
ANSWER

        # ensure tmp dir exists
        @host.mkdir(@host.systemdrive(ctx)+'\\temp', ctx)
        
        answer_file = @host.systemdrive(ctx)+'\\temp\\iis_answer.txt' # LATER standardize c:\temp access
        @host.write(answer_str, answer_file, ctx)
        
        # need the installation media for this OS (exact version and service pack), to install IIS from
        install_path = '\\terastation\share\Windows\ASI\Win2k3SP2'
              
        
        
        # have to set this in registry entries
        @host.exec!('REG ADD HKLM\\Software\\Microsoft\\Windows\\CurrentVersion\\Setup /v \"Installation Sources\" /t REG_MULTI_SZ /d \"'+install_path+'\" /f', ctx)
        @host.exec!('REG ADD HKLM\\Software\\Microsoft\\Windows\\CurrentVersion\\Setup /v ServicePackSourcePath /t REG_SZ /d \"'+install_path+'\" /f', ctx)
        @host.exec!('REG ADD HKLM\\Software\\Microsoft\\Windows\\CurrentVersion\\Setup /v SourcePath /t REG_SZ /d \"'+install_path+'\" /f', ctx)
        
        # /r is important, without it, windows will reboot
        @host.exec!("start /w Sysocmgr.exe /r /i:sysoc.inf /u:#{answer_file}", ctx)
      
        #
        # IIS6 is installed
        # now, install FastCGI extension and configure IIS6 to use FastCGI
        #
        
        # can't edit metabase.xml while IIS is running
        @host.exec!('net stop w3svc', ctx)  
      
        # TODO add fcgiext.dll
        
        #
        # edit metabase.xml to use the FastCGI extension
        #
        metabase_xml_raw = @host.read_file(@host.systemroot+'/System32/inetsrv/metabase.xml', ctx)
        
        doc = Nokogiri::XML(metabase_xml_raw)
       
        # add to (don't replace existing attribute values, rather, add to them (if they don't already contain these values)) 
        add_xml_attr_list(doc, '//IIsWebService[@ScriptMaps]', '.php,'+@host.systemroot+"\\system32\\inetsrv\\fcgiext.dll,5,GET,HEAD,POST\n")
        add_xml_attr_list(doc, '//IIsWebService[@WebSvcExtRestrictionList]', '1,'+@host.systemroot+"\\system32\\inetsrv\\fcgiext.dll,1,FASTCGI,FastCGI Handler\n")
        
        metabase_xml_raw = doc.to_s
        
        # replace metabase.xml
        @host.write_file(metabase_xml_raw, @host.systemroot+'/System32/inetsrv/metabase.xml', ctx)
      
        # put IIS back into a running state
        # (DOES THIS MAKE SENSE? iis will be stopped by IIS middleware to edit fcgiext.ini)
        @host.exec!('net start w3svc', ctx)
        
        #
        # For IIS6, IIS middleware will edit fcgiext.ini to set which php build to use
      end
      
      protected
      
      def add_xml_attr_list(doc, xpath, value)
        doc.xpath(xpath) do |elem|
          unless elem.value.include?(value)
            elem.value = value + elem.value
          end
        end
      end
      
    end
  end
end
