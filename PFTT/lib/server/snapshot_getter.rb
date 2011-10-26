
require 'nokogiri'
require 'net/http'

module Server
  class SnapshotGetter
    def initialize(localhost)
      @localhost = localhost
      
      @revision_index = "http://windows.php.net/downloads/snaps/php-5.4/" # MUST have trailing /
    end
    
    #
    # returns
    # [local_filename] => :new or :no_update
    #
    # may raise exceptions
    def ensure_latest_snapshot
      urls = scrape_snapshot_urls()
      
      files_and_status = {}
      
      threads = []
      urls.each do |url|
        t = Thread.start do
          r = download_file_to_local(url)
          files_and_status[r[0]] = r[1]
        end
        threads.push(t)
      end
      
      # wait for them to all finish
      threads.each do |t|
        t.join
      end
      
      return files_and_status
    end
    
    protected
    
    def wget(local_filename, remote_url)
      if @localhost.windows?
        @localhost.exec!(@localhost.systemdrive+"/php-sdk/bin/wget -O #{local_filename} #{remote_url}")
      else
        @localhost.exec!("wget -O #{local_filename} #{remote_url}")
      end
    end
    
    def scrape_snapshot_urls()
      # URL goes to a page that shows a list of revisions ('Revision Index')
      # find the newest URL, then go to that page (the 'Snapshot Index')
      #  Snapshot index lists several files
      # find php-5.4-ts, nts, src, and provide those URLs
    
      http_response = wget('-', @revision_index)
      
      unless http_response[1] != 0
        raise 'HttpErrorRevisionIndex'
      end
      
      doc = Nokogiri::HTML(http_response[0])
  
      hrefs = doc.xpath("*//a/@href")
      
      rev_index_url = URI.parse(@revision_index)  
  
      newest_snap_href = nil
      hrefs.each do |attribute|
        href = attribute.value
        # make sure its not some link to something else that got added to this page at some point
        if href.starts_with?(rev_index_url.path+'r')
          newest_snap_href = href
        end
      end
    
      unless newest_snap_href
        raise 'SnapshotLinkNotFoundError'
      end
      
      puts newest_snap_href.inspect
  
      http_response = wget('-', "http://"+rev_index_url.host+"/"+newest_snap_href)
  
      unless http_response[1] != 0
        raise 'HttpErrorSnapshotIndex'
      end
    
      doc = Nokogiri::HTML(http_response[0])
  
      hrefs = doc.xpath("*//a")
  
      php_nts_url, php_ts_url, php_src_url = nil
  
      hrefs.each do |a_tag|
        href = a_tag.attributes['href'].value
        unless href.starts_with?('http://')
          href = "http://"+(rev_index_url.host+"/"+href).gsub('//', '/')
        end
    
        text = ((a_tag.children.empty?)?'':a_tag.children.first.to_s)
    
        if text.starts_with?('php-5.5-nts')
          php_nts_url = href
        elsif text.starts_with?('php-5.5-ts')
          php_ts_url = href
        elsif text.starts_with?('php-5.5-src')
          php_src_url = href
        elsif text.starts_with?('php-5.4-nts')
          php_nts_url = href
        elsif text.starts_with?('php-5.4-ts')
          php_ts_url = href
        elsif text.starts_with?('php-5.4-src')
          php_src_url = href
        elsif text.starts_with?('php-5.3-nts')
          php_nts_url = href
        elsif text.starts_with?('php-5.3-ts')
          php_ts_url = href
        elsif text.starts_with?('php-5.3-src')
          php_src_url = href
        end
    
      end
    
      unless php_nts_url and php_ts_url
        raise 'PHPBuildLinksNotFound'
      end
      puts [php_nts_url, php_ts_url, php_src_url].inspect
      return [php_nts_url, php_ts_url, php_src_url]
    end # def scrape_snapshot_urls
  
    def download_file_to_local(remote_url)
      local_filename = ((@localhost.windows?)?@localhost.systemdrive+'/php-sdk/builds/':'~/php-sdk/builds/')+File.basename(remote_url)
      local_dir = local_filename
      
      # local dir should be same as local filename of ZIP without the .zip
      if local_dir.ends_with?('.zip')
        local_dir = local_dir[0..local_dir.length-1-'.zip'.length]
      end
      
      status = :no_update
      unless File.exist?(local_dir)
        # not decompressed
        
        unless File.exist?(local_filename)
          # zip not downloaded
      
          remote_url = URI.parse(remote_url)
          
          puts remote_url.inspect
          
          wget(local_filename, remote_url)
          # this code doesn't work (very slowly downloads files then hangs. sometimes hangs on small html files too!)
          # just use 'wget' to do it since it actually works
  #        Net::HTTP.start(remote_url.host, remote_url.port) do |http|
  #          begin
  #            file = open(local_filename, 'wb')
  #            http.request_get(remote_url.path) do |resp|
  #              resp.read_body do |segment|
  #                file.write(segment)
  #                #sleep 10 # hack to wait for buffer to refill
  #              end
  #            end
  #          ensure
  #            file.close
  #          end
  #        end
          
          status = :new
        end
        
        # then decompress
        #
        if local_filename.include?('php-5.4-src') or local_filename.include?('php-5.5-src')
          # php src already comes compressed into a directory (NTS and TS don't)
          local_dir = (@localhost.windows?)? @localhost.systemdrive+"/PFTT-PHPS" : "~/PFTT-PHPs/"
        end
        
        # decompress into a directory named with the filename
        @localhost.mkdir(local_dir)
        
        # run unzip from that directory
        if @localhost.windows?
          @localhost.exec!(@localhost.systemdrive+"/php-sdk/bin/unzip #{local_filename}", {:chdir=>local_dir})
        else
          @localhost.exec!("unzip #{local_filename}", {:chdir=>local_dir})
        end
      end
    
      puts local_filename
    
      return [local_filename, status]
      
    end
  
  end # class SnapshotGetter
end

if __FILE__ == $0
  # bundle exec ruby lib\server\snapshot_getter.rb
  require Dir.pwd+'/bootstrap.rb'

  sg = Server::SnapshotGetter.new(Host::Local.new())
  files = sg.ensure_latest_snapshot
  puts files.inspect
  
end
