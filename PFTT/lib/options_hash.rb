# Lazily store things, build the hash as we go using
# multi-argument keys
class OptionsHash < Hash
  def []= *key,value
    key.map!{|i|i.to_s}
    if key.size > 1
      ky = key.shift
      self[ky]||= self.class.new
      self[ky][*key] = value
    else
      super *key,value
    end
  end
  def [] *key
    key.map!{|i|i.to_s}
    return nil unless has_key? key.first
    return (super(key.shift))[*key] if key.size > 1
    super *key
  end
  def replace(hsh)
    clear
    hsh.each_pair do |k,v|
      k = k.to_s
      if v.is_a? Hash
        self[k]= self.class.new
        self[k].replace(v)
      else
        self[k]=v
      end
    end
  end

  def merge(hsh)
    self.class.new.replace(self.merge hsh)
  end

  def merge!(hsh)
    replace merge
  end

  # ensure that hashes created with OptionsHash[] contain OH sub-hashes, not normal ones
  def self.[](*args)
    ret = super
    ret = ret.replace( ret.dup )
    ret
  end
end