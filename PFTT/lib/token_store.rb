class TokenStore
  @@stores = {}
  @@reserved = [:all,:none]

  class << self
    def [](selector)
      if !@@stores.has_key? selector
        TokenStore.new selector
      end
      @@stores[selector]
    end
  end

  def initialize(name=nil)
    @@stores[name] = self unless name.nil?
    @data = {}
    @special = {
      :none=> TokenInfo::None.new(self),
      :all=> TokenInfo::All.new(self)
    }
  end

  def [](selector)
    return @special[selector] if @special.has_key? selector

    if !@data.has_key? selector
      @data[selector] = TokenInfo.new selector, self
    end
    @data[selector]
  end

  def []=(selector,params)
    check selector
    (self[selector]).set *params
  end

  def delete!(selector)
    @data.delete selector
    @data.values.each do |item|
      item.set :delete=>selector
    end
  end

  def all_a
    @data.values.map do |item|
      item.white | [item.to_sym]
    end.flatten.uniq
  end

  protected

  def check( selector )
    raise 'Reserved selector <#{selector}> cannot be edited!' if @@reserved.include? selector
  end
end







class TokenInfo
  def initialize( selector, store, *params )
    @symbol = selector
    @store = store
    clear!
    set *params unless params.nil? or params.size.zero?
  end

  def to_sym
    @symbol
  end

  def white
    @include.clone
  end

  def black
    @exclude.clone
  end

  def set( *params )
    params.each do |param|
      puts "Setting param on <#{@symbol}>: '#{param}'"
      if param.kind_of? Hash
        param.each_pair do |verb,data|
          data = [data] unless data.kind_of? Array
          data.map{ |datum| @store[datum] }
          case verb
          when :add,:and 
            @include |= data
          when :only
            @include.replace data
          when :except
            @exclude |= data
          when :delete
            @include -= data
            @exclude -= data
          end
          break
        end
      else
        set :add => param
      end
    end
    self
  end

  def delete!
    @store.delete! @symbol
    self
  end

  def clear!
    @include,@exclude = [],[]
    self
  end

  def to_a(chain=[])
    raise "Circular Reference for <:#{@symbol.to_sym}>!" if chain.include? @symbol
    
    white = @include.map do |tok|         # loop through includes, 
      #@store.expand tok, chain+[@symbol]  # extracting each
      @store[tok].to_a chain+[@symbol]
    end.flatten.uniq

    black = @exclude.map do |tok|         # then loop through excludes,
      #@store.expand tok, chain+[@symbol]  # extracting each
      @store[tok].to_a chain+[@symbol]
    end.flatten.uniq

    (white - black) | [@symbol]
  end

  def include?(selector)
    return true if @selector == @symbol
    self.to_a.include? selector
  end
end

class TokenInfo::Locked < TokenInfo
  def set( *params )
    raise "Locked selector <#{@symbol}> cannot be edited!"
  end
end

class TokenInfo::All < TokenInfo::Locked
  def initialize( store )
    super( :all, store )
  end

  def to_a( chain=[] )
    @store.all_a
  end
end

class TokenInfo::None < TokenInfo::Locked
  def initialize( store )
    super( :none, store )
  end

  def to_a( chain=[] )
    []
  end
end