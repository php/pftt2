
module Diff
  
  class File
    
    def self.guess_format(file)
    end
    
    def initialize(file=nil)
      @format = Format::Unified.new
    end
    
    def add_chunk(chunk)
      if chunk.file_name != @last_file_name
        @file.write_lines(@format.write_header())
      end
      
      @file.write(@format.lines(chunk))
    end
    
    def read_init()
      
    end
    
    def read_chunk()
      @format.read_chunk(@file)
    end
    
    class Format
      
      class Original
        #0a1,6
        #>
        #<
        #---        
      end # class Original
      
      class Edit
        # 24a
        # .
      end # class Edit
      
      class Context
        # *** [filename] ''timestamp''
        # --- [filename] ''timestamp''
        # ***
        # *** 1,3 ***
        # --- 1,9 ---
        # +
        # !
      end # class Context
      
      class Unified
        # --- [filename] ''timestamp''
        # +++ [filename] ''timestamp''
        # @@
        #+
        #-
        def initialize()#file_a=nil, file_b=nil, time_a=nil, time_b=nil)
          
        end
                
        def read_chunk(file)
          while !file.eof?
            line = file.next_line # TODO
            
            if line.starts_with?('+++') or line.starts_with?(' +++')
              line_type = :header_right
            elsif line.starts_with?('+')
              line_type = :added
            elsif line.starts_with?('---') or line.starts_with?(' ---')
              line_type = :header_left
            elsif line.starts_with?('-')
              line_type = :removed
            elsif line.starts_with?(' @') or line.starts_with?('@')
              line_type = :shifted
            else
              line_type = :unchanged
            end
            
            case line_type
            when :shifted
              parse_location(line)
            when :header_left
              parse_header_left(line)
            when :header_right
              parse_header_right(line)
            else
              # TODO increment location
            end
            
            @last_line_type = line_type
            
            if line_type != @last_line_type
              return Diff::Chunk.new(str, @file_name, line_number, 0)
            end
            
          end # while
        end # def read_chunk
        
        def parse_location(line)
          # TODO
          # @@ -a,b +c,d @@
          # a = removed chunk starting line number
          # b = number of lines of removed chunk
          # c = inserted chunk starting line number
          # d = number of lines of inserted chunk
        end
        
        def parse_header_left(line)
          # TODO
        end
        
        def parse_header_right(right)
          # TODO
        end
                
        def write_header()
          # TODO
          []
        end
        
        def line(chunk)
          location(chunk)
          if chunk.inserted?
            lines(chunk).map do |line|
              "+#{line}"
            end
          elsif chunk.removed?
            lines(chunk).map do |line|
              "-#{line}"
            end
          else
            lines(chunk)
          end
        end
        
        def location(chunk)
          # TODO
          chunk.line_number
        end
        
      end # class Unified
      
    end # class Format
    
  end # class File
  
  class ChunkReplacementsFile
    def initialize(file_name)
    end
    
    def read
    end
    
    def write
    end
  end # class ChunkReplacementsFile
  
  class Chunk
    attr_reader :str, :file_name, :type, :line_number, :column
    
    def initialize(str, file_name, line_number, column, type)
      case type
      when :insert
      when :delete
      when :equals
      else
        raise ArgumentError
      end
      @type = type
      @str = str
      @file_name = file_name
      @line_number = line_number
      @column = column
    end
    
    def lines
      @_lines ||= @str.split("\n")
    end
    
    def out_column
      lines().last.length
    end
    
    def line_count
      lines().length
    end
    
    def to_s
      "#{@file_name}:#{@type}:#{@line_number}:#{@column}:#{@str}"
    end
    
    def include?(up_needle, down_needle=nil)
      if @str.nil?
        return false
      elsif down_needle
        return ( @str.upcase.include?(up_needle) or @str.downcase.include?(down_needle) )
      else
        return ( @str.include?(up_needle) )
      end
    end
    
    def delete?
      type == :delete
    end
    
    def insert?
      type == :insert
    end
    
    def equals?
      type == :equals
    end
    
    def opposite
      if equals?
        self
      end
      
      Chunk.new(@str, @file_name, @line_number, @column, (@type==:delete)?:insert: :delete)      
    end
    
    
    class Array # TODO TUE < TypedArray(Diff::Chunk)
      
      def initialize
        super
        
        @files = {}
        @str = nil
      end
      
      def push(chunk)
        @files[chunk.file_name]||=[]
        @files[chunk.file_name].push(chunk)
        
        clear_string
        
        super(chunk)
      end
      
      def delete(chunk)
        clear_string
        super(chunk)
      end
      
      def to_s
        unless @str
          gen_string
        end
        return @str
      end
      
# TODO     def to_chunk_replacement(zd)
#        chunk_replacements = {}
#          
#        @files.map do |file_name, chunks|
#          chunks.each do |rep_chunk|
#            org_chunk = zd.expected(rep_chunk)
#            
#            chunk_replacements[org_chunk] = rep_chunk
#          end
#        end
#          
#        return ChunkReplacementFile.new(nil, chunk_replacements)
#      end
      
      def to_override
          str = ''
          @files.map do |file_name, chunks|
            file_name = file_name.gsub('C:\\php-sdk\\PFTT-Telemetry\\OI1-PHP-FUNC-15.2011-12-09_14-05-34_-0800/CLI/', '') # TODO TUE
            file_name = file_name.gsub('.diffx', '.phpt') # TODO TUE
            
            chunks.each do |chunk|
              if chunk.type == :insert
                str += "<insert test_case=\"#{file_name}\">#{chunk.str}</insert>"
              elsif chunk.type == :delete
                str += "<delete test_case=\"#{file_name}\">#{chunk.str}</delete>"
              end
            end
            str += "\n"
          end
          return str
        end
      
      protected
      
      def clear_string
        @str = nil
      end
      
      def gen_string
        str = ''
        # TODO generate a 'diff like'/pretty presentation for email and .diff and .diffx files
        @files.map do |file_name, chunks|
          file_name = file_name.gsub('C:\\php-sdk\\PFTT-Telemetry\\PH4.2011-12-06_17-41-15_-0800/CLI/', '') # TODO TUE
          file_name = file_name.gsub('.diffx', '.phpt') # TODO TUE
          
          str += "#{file_name}:\n"
          str += "inserted:\n"
          chunks.each do |chunk|
            if chunk.type == :insert
              line_count = chunk.str.split("\n").length
              str += "--- #{chunk.line_number},#{line_count} --- \n"#c#{chunk.column} ---"
              chunk.str.split("\n").each do |line|
              str += "+#{line}\n"
              end
            end
          end
          str += "\n"
          str += "removed:\n"
          chunks.each do |chunk|
            if chunk.type == :delete
              line_count = chunk.str.split("\n").length
              str += "--- #{chunk.line_number},#{line_count} --- \n"#c#{chunk.column} ---"
              chunk.str.split("\n").each do |line|
                str += "-#{line}\n"
              end
            end
          end
          str += "\n\n"
        end
        puts str
        @str = str
      end
      
    end # class Array
    
  end # class Chunk
  
end
