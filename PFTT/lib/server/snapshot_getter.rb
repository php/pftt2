
module Server
  class SnapshotGetter
    def initialize
      @revision_index = "http://windows.php.net/downloads/snaps/php-5.4/" # MUST have trailing /
    end
    
    #
    # may raise exceptions
    def ensure_latest_snapshot
      urls = scrape_snapshot_urls()
      
      threads = []
      urls.each do |url|
        t = Thread.start do
          download_file_to_local(host, url)
        end
        threads.push(t)
      end
      
      # wait for them to all finish
      threads.each do |t|
        t.join
      end
      
      return ['local paths to decompressed latest snapshots'] # TODO
    end
    
    protected
    
    def scrape_snapshot_urls()
      rev_index_url = URI.parse(@revision_index)
    
      # URL goes to a page that shows a list of revisions ('Revision Index')
      # find the newest URL, then go to that page (the 'Snapshot Index')
      #  Snapshot index lists several files
      # find php-5.4-ts, nts, src, and provide those URLs
  #  
  #    http_response = Net::HTTP.get_response(rev_index_url)
  #      
  #    if http_response.code != '200'
  #      raise 'HttpErrorRevisionIndex'
  #    end
  #  
  #    doc = Nokogiri::HTML(http_response.body)
  #
  #    hrefs = doc.xpath("*//a/@href")
  #
  #    newest_snap_href = nil
  #    hrefs.each do |attribute|
  #      href = attribute.value
  #      # make sure its not some link to something else that got added to this page at some point
  #      if href.starts_with?(rev_index_url.path+'r')
  #        newest_snap_href = href
  #      end
  #    end
  #  
  #    unless newest_snap_href
  #      raise 'SnapshotLinkNotFoundError'
  #    end
  #    
      newest_snap_href = "downloads/snaps/php-5.4/r318303/" # TODO temp
      puts newest_snap_href.inspect
  
      http_response = Net::HTTP.get_response(URI.parse("http://"+rev_index_url.host+"/"+newest_snap_href))
  
      if http_response.code != '200'
        raise 'HttpErrorSnapshotIndex'
      end
    
      doc = Nokogiri::HTML(http_response.body)
  
      hrefs = doc.xpath("*//a")
  
      php_nts_url, php_ts_url, php_src_url = nil
  
      hrefs.each do |a_tag|
        href = a_tag.attributes['href'].value
        unless href.starts_with?('http://')
          href = "http://"+(rev_index_url.host+"/"+href).gsub('//', '/')
        end
    
        text = ((a_tag.children.empty?)?'':a_tag.children.first.to_s)
    
        if text.starts_with?('php-5.4-nts')
          php_nts_url = href
        elsif text.starts_with?('php-5.4-ts')
          php_ts_url = href
        elsif text.starts_with?('php-5.4-src')
          php_src_url = href
        end
    
      end
    
      unless php_nts_url and php_ts_url
        raise 'PHPBuildLinksNotFound'
      end
      puts [php_nts_url, php_ts_url, php_src_url].inspect
      return [php_nts_url, php_ts_url, php_src_url]
    end # def scrape_snapshot_urls
  
    def download_file_to_local(host, remote_url)
      local_filename = 'C:/PFTT-PHPS/'+File.basename(remote_url) # TODO add temp dir path
      local_dir = local_filename
      
      # local dir should be same as local filename of ZIP without the .zip
      if local_dir.ends_with?('.zip')
        local_dir = local_dir[0..local_dir.length-1-'.zip'.length]
      end
      
      unless File.exist?(local_dir)
        # not decompressed
        
        unless File.exist?(local_filename)
          # zip not downloaded
      
          remote_url = URI.parse(remote_url)
          
          puts remote_url.inspect
          
          localhost.exec!("C:/php-sdk/bin/wget -O #{local_filename} #{remote_url}") # TODO
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
        end
        
        # then decompress
        #
        if local_filename.include?('php-5.4-src') or local_filename.include?('php-5.5-src')
          # php src already comes compressed into a directory (NTS and TS don't)
          local_dir = "C:/PFTT-PHPS"
        end
        
        # decompress into a directory named with the filename
        localhost.mkdir(local_dir)
        
        # run unzip from that directory
        localhost.exec!("C:/php-sdk/bin/unzip #{local_filename}", {:chdir=>local_dir})
      end
    
      puts local_filename
    
      return local_filename
      
    end
  
  end # class SnapshotGetter
end
