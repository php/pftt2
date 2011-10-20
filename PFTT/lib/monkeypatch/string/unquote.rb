# strip quotes surrounding a string
class String
  def unquote
    self.sub(/\A(['"])(.*)\1\z/, '\2')
  end

  def unquote!
    self.replace self.unquote
  end
end