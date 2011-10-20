
# TODO delete
module LumberJack
  class Logger
    @@logs = []

    def self.writeline( line, level )
      puts "<#{line}> at <#{level}>"
      @@logs.each do |log|
        log.writeline( line, level )
      end
    end

    def self.close
      @@logs.each do |log|
        log.close if log.open?
      end
    end

    def self.logs
      @@logs
    end
  end

  #class TypeError < $.TypeError
  #end

  class Log
    def initialize
      LumberJack::Logger.logs << self
      @open = true
    end


    def open?
      @open
    end

    def close
      @open = false
    end
  end

  class LogStream < Log
    def initialize( level, stream, options={} )
      super()
      @level = level
      @stream = stream
      @options = options
      self
    end

    def writeline( line, level, options = {} )
      line = _format( line, level, options )
      _write line if write? level
    end

    def write?(level)
      return TokenStore[:log][@level].include? level
    end

    def close
      @stream.close unless @stream.fileno <=2 #don't close STD[IN/OUT/ERR]!
    rescue Errno::EBADF
      # puts "bad fd for #{@stream}"
    rescue IOError
      # puts "already closed? #{@stream}"
    ensure
      super()
    end

    protected 

    def _format( line, level, options={} )
      if @options.merge( options )[:print_level]
        line = "<#{level.to_s}> #{line}"
      end
      if @options.merge( options )[:print_timestamp]
        line = "#{Time.now}: #{line}"
      end
      line
    end

    def _write( line )
      @stream.write line
      @stream.flush
    rescue Errno::EBADF
    ensure
      self;
    end
  end

  class LogFile < LogStream
    def initialize( level, path, options={} )
      @path = path
      super level, nil, options
    end

    protected

    def _write( line )
      log = File.open @path, 'a'
      log.write "#{line}\n"
      log.close
    end
  end

  class LogTemplate < Log
    def initialize( path, template )
      super()
      @path = path
      @template = template
      @data = {}
    end

    def writeline( line, level )
      @data[level]||=[]
      @data[level] << line
    end

    def close
      puts "Template not implemented. Dumping instead.\n#{@data}"
      super()
    end
  end
end