
module Diff
  class OverrideManager
    
  def initialize
    @replace = {}
    @ignore = {
          :insert=>{ # :insert :delete :any
            :all=> { # filename
              :windows => { # platform_type
                :any => { # platform_arch
                  :sdk => { # platform_sdk
                    :any => { # os_version
                      :any => [ # sp
                        [] # each insert chunk is an [] of strings here
                      ]
                    }
                  }
                }
              }
            }
          },
          :delete=>[]
        }
  end # def initialize
  
  def read
    xml = XmlSimple.xml_in(override_xml, 'AttrPrefix' => true, 'ContentKey'=>'text')
        
    xml['platform'].each do |platform_xml|
      platform_type = :any # TODO platform_xml['@type']
      platform_sdk = :any # TODO platform_xml['@sdk']
      platform_arch = :any # TODO platform_xml['@arch']
        
      platform_xml['os'].each do |os_xml|
        os_version = :any # TODO os_xml['@version']
          
        os_xml['sp'].each do |sp_xml|
          sp = :any # TODO sp_xml['@sp']
            
          sp_xml['ignore'].each do |ignore_xml|
            
            type = :insert # :insert :delete :any
            @ignore[type]||={}
            ignore_xml['insert'].each do |insert_xml|
              filename = insert_xml['@testcase']
              text = insert_xml['text']
                
              # %s can be used to match any string between other parts of chunk
              # to do that, split here on %s so ignore? then gets for each part in order
              proc_text = text.split("%s")
              
              # store
              @ignore[type][filename]||={}
              @ignore[type][filename][platform_type]||={}
              @ignore[type][filename][platform_type][platform_arch]||={}
              @ignore[type][filename][platform_type][platform_arch][platform_sdk]||={}
              @ignore[type][filename][platform_type][platform_arch][platform_sdk][os_version]||={}
              @ignore[type][filename][platform_type][platform_arch][platform_sdk][os_version][sp]||=[]
              @ignore[type][filename][platform_type][platform_arch][platform_sdk][os_version][sp].push(proc_text)
                
            end
            
          end # ignore
          
        end # sp
      end # os
    end # platform
    
    #ignore_text.split('%s')
  end # def read
  
  def will_replace?(chunk)
    # TODO
  end
  
  def replace(chunk)
    # TODO
  end
    
  def ignore?(chunk)
    return false # TODO TUE
    if chunk.type == :equals
      return false
    end
    
    return ( check_ignore(chunk, @ignore[chunk.type]) or check_ignore(chunk, @ignore[:all]) )
  end

  def override_xml
    # TODO file
    return <<-OVERRIDE_XML
<?xml version="1.0" ?>    
<override>
<platform type="windows" sdk="any" arch="any"><!-- any windows linux freeBSD -->
<os version="any"><!-- any or (distro version num) distro="name" and MinVersion(int) and MaxVersion(int) or (ship names) XP 2003 2003r2 Vista 2008 7 2008r2 8 -->
<sp sp="any"><!-- any or 0+ -->

<ignore><!-- ignore different chunk of output when pass/fail evaluated: any insert delete -->


<!-- can't write to /tmp/ on Windows ('extra warning')-->
<insert test_case="ext/session/tests/session_set_save_handler_class_002.phpt">
Warning: file_put_contents(/tmp/u_sess_PHPSESSID07b613ebe9c270b421696daf8fd94a88): failed to open stream: No such file or directory in %s\ext\session\tests\session_set_save_handler_class_002.php on line 33
array(0) {

Warning: file_put_contents(/tmp/u_sess_PHPSESSID07b613ebe9c270b421696daf8fd94a88): failed to open stream: No such file or directory in %s\ext\session\tests\session_set_save_handler_class_002.php on line 33

Warning: file_put_contents(/tmp/u_sess_PHPSESSID07b613ebe9c270b421696daf8fd94a88): failed to open stream: No such file or directory in %s\ext\session\tests\session_set_save_handler_class_002.php on line 33
array(0) {
</insert>
<!-- extra warning -->
<insert test_case="ext/standard/tests/dir/opendir_variation6-win32.phpt">Warning: opendir(%s\ext\standard\tests\dir/opendir_var*,%s\ext\standard\tests\dir/opendir_var*): No error in %s\ext\standard\tests\dir\opendir_variation6-win32.php on line 23
Warning: opendir(%s\ext\standard\tests\dir/*,%s\ext\standard\tests\dir/*): No error in %s\ext\standard\tests\dir\opendir_variation6-win32.php on line 24
Warning: opendir(%s\ext\standard\tests\dir/opendir_variation6/sub_dir?,%s\ext\standard\tests\dir/opendir_variation6/sub_dir?): No error in %s\ext\standard\tests\dir\opendir_variation6-win32.php on line 27
Warning: opendir(%s\ext\standard\tests\dir/opendir_variation6/sub?dir1,%s\ext\standard\tests\dir/opendir_variation6/sub?dir1): No error in %s\ext\standard\tests\dir\opendir_variation6-win32.php on line 28
</insert>
<!-- extra warning -->
<insert test_case="ext/standard/tests/dir/scandir_variation6-win32.phpt">Warning: scandir(%s\ext\standard\tests\dir/scandir_var*,%s\ext\standard\tests\dir/scandir_var*): No error in %s\ext\standard\tests\dir\scandir_variation6-win32.php on line 24
Warning: scandir(%s\ext\standard\tests\dir/scandir_var*): failed to open dir: No error in %s\ext\standard\tests\dir\scandir_variation6-win32.php on line 24
Warning: scandir(%s\ext\standard\tests\dir/*,%s\ext\standard\tests\dir/*): No error in %s\ext\standard\tests\dir\scandir_variation6-win32.php on line 25

Warning: scandir(%s\ext\standard\tests\dir/*): failed to open dir: No error in %s\ext\standard\tests\dir\scandir_variation6-win32.php on line 25
Warning: scandir(%s\ext\standard\tests\dir/scandir_variation6/sub_dir?,%s\ext\standard\tests\dir/scandir_variation6/sub_dir?): No error in %s\ext\standard\tests\dir\scandir_variation6-win32.php on line 28

Warning: scandir(%s\ext\standard\tests\dir/scandir_variation6/sub_dir?): failed to open dir: No error in %s\ext\standard\tests\dir\scandir_variation6-win32.php on line 28
Warning: scandir(%s\ext\standard\tests\dir/scandir_variation6/sub?dir1,%s\ext\standard\tests\dir/scandir_variation6/sub?dir1): No error in %s\ext\standard\tests\dir\scandir_variation6-win32.php on line 29

Warning: scandir(%s\ext\standard\tests\dir/scandir_variation6/sub?dir1): failed to open dir: No error in %s\ext\standard\tests\dir\scandir_variation6-win32.php on line 29
</insert>
<!-- test environment/setup problem -->
<insert test_case="ext/standard/tests/streams/stream_get_meta_data_socket_variation1.phpt">Warning: stream_socket_accept(): accept failed: A connection attempt failed because the connected party did not properly respond after a period of time, or established connection failed because connected host has failed to respond.
 in %s\ext\standard\tests\streams\stream_get_meta_data_socket_variation1.php on line 13

Warning: fwrite() expects parameter 1 to be resource, boolean given in %s\ext\standard\tests\streams\stream_get_meta_data_socket_variation1.php on line 16
  int(0)
  bool(true)
  int(0)
  bool(true)

Warning: fclose() expects parameter 1 to be resource, boolean given in %s\ext\standard\tests\streams\stream_get_meta_data_socket_variation1.php on line 28
</insert>
<!-- test environment/setup problem -->
<insert test_case="ext/standard/tests/streams/stream_get_meta_data_socket_variation2.phpt">Warning: stream_socket_accept(): accept failed: A connection attempt failed because the connected party did not properly respond after a period of time, or established connection failed because connected host has failed to respond.
 in %s\ext\standard\tests\streams\stream_get_meta_data_socket_variation2.php on line 13

Warning: fwrite() expects parameter 1 to be resource, boolean given in %s\ext\standard\tests\streams\stream_get_meta_data_socket_variation2.php on line 23
  bool(true)
</insert>
<!-- test environment/setup problem -->
<insert test_case="ext/standard/tests/streams/stream_get_meta_data_socket_variation4.phpt">Warning: stream_socket_accept(): accept failed: A connection attempt failed because the connected party did not properly respond after a period of time, or established connection failed because connected host has failed to respond.
 in %s\ext\standard\tests\streams\stream_get_meta_data_socket_variation4.php on line 13

Warning: fwrite() expects parameter 1 to be resource, boolean given in %s\ext\standard\tests\streams\stream_get_meta_data_socket_variation4.php on line 16
  bool(true)

Warning: fclose() expects parameter 1 to be resource, boolean given in %s\ext\standard\tests\streams\stream_get_meta_data_socket_variation4.php on line 24
</insert>


</ignore>

<replace>
<!-- replace chunks of output before pass/fail evaluated: Any Insert Delete -->
</replace>

</sp>
</os>
</platform>
</override>

OVERRIDE_XML
  end

  protected
    
  def check_ignore(chunk, ignore_list)
    if ignore_list.nil?
      return false
    end
    
    return ( check_ignore_file(chunk, ignore_list[chunk.file_name]) or check_ignore_file(chunk, ignore_list[:all]) )
  end    
  
  def check_ignore_file(chunk, platform_types)
    if platform_types.nil?
      return false
    end
    
    platform_types.each do |platform_archs|
      platform_archs.each do |platform_sdks|
        platform_sdks.each do |os_versions|
          os_versions.each do |sp|
            sp.each do |ignore|
              #
              #
              # check each text chunk to ignore
              ignore_list.each do |ignore|
                i = 0
                match = true
                
                ignore.each do |ignore_part|
                  i = chunk.str.index(ignore_part, i)
                  if i.nil?
                    match = false
                    break
                  end
                  
                  # search for next ignore_part
                  i = i + ignore_part.length
                end
                
                #
                if match
                  # found ignore match, can stop looking (short-circuit)
                  return true
                end
                #
              end    
              #
              #
            end
          end
        end
      end
    end
    
    return false
  end # def check_ignore_file
  
    
  end # class OverrideManager
end
