class String
  def unindent
    gsub /^#{self[/\A\s*/]}/, ''
  end
  def unindent!
    replace unindent
  end
end
