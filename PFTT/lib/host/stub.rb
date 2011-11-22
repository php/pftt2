module Host
  class Stub < HostBase
    def exec
      Thread.start do
        ['content','',-255]
      end
    end

    def exist? path
      !_get_item(path).nil?
    end

    def directory? path
      return false unless exist? path
      _get_item(path).is_a? Hash
    end

    def list path
      return false unless directory? path
      _get_item(path).keys
    end

    def glob spec, &block
      spec = spec.chop if path.end_with? '/'
      results = [] unless block_given?

      _tree.each do |path|
        
      end

      return results unless block_given?
    end

    def glob(path, pattern, flags=0)
      flags |= ::File::FNM_PATHNAME
      path = path.chop if path[-1,1] == "/"

      results = [] unless block_given?
      queue = list(path).reject { |e| e.name == "." || e.name == ".." }
      while queue.any?
        entry = queue.shift

        if entry.directory? && !%w(. ..).include?(::File.basename(entry.name))
          queue += entries("#{path}/#{entry.name}").map do |e|
            e.name.replace("#{entry.name}/#{e.name}")
            e
          end
        end

        if ::File.fnmatch(pattern, entry.name, flags)
          if block_given?
            yield entry
          else
            results << entry
          end
        end
      end

      return results unless block_given?
    end

    protected

    def _fs
      @fs ||= Hash.new
    end

    def _get_item path
      cd = _fs
      path.split('/').each do |item|
        return nil unless cd.is_a? Hash and cd.has_key? item
        cd = cd[item]
      end
      return cd
    end

  end
end
