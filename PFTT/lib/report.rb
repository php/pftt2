
module Report
#class Telemetrys # 
#  def initialize
#    @telemetry_url = ''
#    @run_id = ''
#  end
#  def list(status)
#    # return contents of PASS.list, FAIL.list, SKIP.list file
#  end
#end
#
#class TelemetrySet # 
#  def initialize
#    @results_by_host = {}
#    @results_by_host['host_0'] = Telemetrys.new()
#    @title = 'PHP 5.3.8-nts-Win32-x86-vc9-r21111 (include time revision was downloaded)'
#    @test_time = '10:30 10/8/2011 GMT'
#    @ini = ''
#  end
#end

class Base
#  def initialize(db)
#  end
  
  def write_html
    return ''
  end
  
  def write_text
    return ''
  end
  
  def write_attachments
    return {}
  end
  
  def remove_html(html_string)
    html_string.gsub(/(<[^>]*>)|\n|\t/s) {" "}
  end
  
  def text_string
    write_text()
  end
  
  def text_print
    puts text_string
  end
  
  def text_file(filename)
    f = File.open(filename, 'wb')
    f.puts(write_text())
    f.close()
  end
  
  def html_file(filename)
    f = File.open(filename, 'wb')
    f.puts(write_text())
    f.close()
  end
  
  def email(from_email, to_emails, subject, mail_options, mail_via=:smtp)
#    $mail_via => :smtp # or :sendmail
#          $mail_via_options = {
#            :address              => 'smtp.gmail.com',
#              :port                 => '587',
#              :enable_starttls_auto => true,
#              :user_name            => 'user',
#              :password             => 'password',
#              :authentication       => :plain, # :plain, :login, :cram_md5, no auth by default
#              :domain               => "localhost.localdomain" # the HELO domain provided by the client to the server
#        
#          }  

    require 'pony.rb'
    
    Pony.mail(
        :from => from_email, 
        :to => to_emails, 
        :subject => subject, 
        :attachments => write_attachments(),
        :html_body => write_html(), 
        :body => write_text(),
        :via => mail_via, 
        :via_options => mail_options
      )
  end
end




end
