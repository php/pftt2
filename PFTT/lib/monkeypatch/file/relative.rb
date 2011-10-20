class File
  class << self
    def relative( path, base=nil )
      base = Dir.getwd if base.nil?
      path = (File.absolute_path path).split(/[\\\/]/)
      base = (File.absolute_path base).split(/[\\\/]/)
      #build an array [[p0,b0],[p1,b1],[p2,nil]]
      p,b = Array.new([path.size,base.size].max).zip(path,base).map!{|i|i.drop(1)}.
        # get rid of the entries that match
        drop_while{|i| i[0]==i[1]}.
        # regroup by path
        transpose.map!{|i|i.compact}
      
      ( '../' * b.size ) + File.join(*p)
    end
  end
end