require 'monkeypatch/string/unquote.rb'

# PhpIni class for handling ini directives from multiple sources
class PhpIni
  def initialize(ini=nil)
    @tokens=[]
    @raw=[]
    configure ini unless ini.nil?
  end

  def configure( ini )
    changes = 0
    changes += case
    when ini.nil? || !ini then 0
    when ini.kind_of?(self.class) then _configure ini.tokens
    when ini.kind_of?(Array)      then _configure _tokenize ini
    when ini.kind_of?(String)
      #ini = File.open( ini[1 .. ini.length], 'rb').read if ini.start_with? '@'
      _configure _tokenize ini.split(/\r?\n/)
    else raise Exception, %Q{ini:#{ini.inspect}}  
    end
    !changes.zero?
  end
  # while this isn't strictly kosher, it *does* append a directive like its Array counterpart
  # and is a lot easier to type.
  alias :<< :configure
  alias :insert :configure

  def + other_ini
    (new_ini = self.clone).configure other_ini
    new_ini
  end
  
  def clone
    PhpIni.new( self )
  end

  def ==( other_PhpIni )
    other_PhpIni = PhpIni.new( other_PhpIni ) unless other_PhpIni.is_a? PhpIni
    a,b=[tokens,other_PhpIni.tokens].map do |token_ary| # strip out all directives that aren't commands
      kv={}
      token_ary.each do |token|
        next if token[:token] != :command
        if token[:key] == 'extension'
          kv[:extensions] ||= []
          kv[:extensions] << token[:value]
        else kv[token[:key]]=token[:value]
        end
      end
      kv
    end
    a==b #and compare
  end
  
  def to_s(raw=false)
    to_a(raw).join("\n")
  end

  def to_a(raw=false)
    ( raw ? @raw : @tokens ).map do |token|
      #puts token.inspect
      ret = token[:key]
      ret += '='
      if /[ =><&]/ =~ token[:value]
        # if it contains any funky values
        # quote it. We'll deal with shell escaping at the host/middleware levels/ 
        ret += %Q{"#{token[:value]}"} 
      else
        ret += token[:value]
      end
      ret
    end
  end

  protected

  def tokens
    @tokens.collect {|token| token.clone}
  end

  private

  def _tokenize( ini_array )
    ini_array.map do |line|
      token = { :raw=>line }
      command = line.strip
      if command.length.zero? or command.start_with? ';'
        token[:token]=:comment
      else
        if command.start_with? '-'
          token[:token]=:remove
          command.slice! 0 #strip the minus sign
        else token[:token]=:command
        end
        if command.include? '='
          token[:key],token[:value] = command.split('=', 2).map{|str|str.strip}
          token[:value].unquote!
        else token[:key]=command
        end
      end
      token
    end
  end

  def _configure( ini_tokens )
    changes = 0
    ini_tokens.each do |token|
      changes += _set token
    end
    changes
  end

  def _set( directive )
    @raw << directive
    return 0 if @tokens.include? directive #token already exists. no need for action.

    case directive[:token]
    when :remove
      to_delete = @tokens.select do |token|
        next false unless ( directive[:key] == token[:key] )  # keep items that do not match keys
        next true unless directive.has_key?(:value)           # remove items with blanket removal
        next true if directive[:value]==token[:value]         # remove items that explicitly match
        next false                                            # keep the rest
      end
      @tokens-=to_delete
      return to_delete.length
    when :command
      if directive[:key]=='extension'
        @tokens << directive
        return 1
      else # 
        changes = 0
        @tokens.map do |token|
          token.replace directive if ( directive[:key]==token[:key] and changes+=1 )
        end
        if changes.zero?
          @tokens << directive
          changes += 1
        end
        return changes
      end
    when :comment
      return 0;
    end
  end
end
