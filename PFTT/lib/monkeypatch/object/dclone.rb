#Monkeypatch - stolen verbatim from stdlib rexml/xpath_parser.rb
class Object
  def dclone
    clone
  end
end
class Symbol
  def dclone ; self ; end
end
class Fixnum
  def dclone ; self ; end
end
class Float
  def dclone ; self ; end
end
class Array
  def dclone
    klone = self.clone
    klone.clear
    self.each{|v| klone << v.dclone}
    klone
  end
end