module FilterByPropertyValue
  def filter( filters )
    r = self.class.new self.to_a
    filters.each_pair do |property,allowed_values|
      r.keep_if do |php_build|
        next true if allowed_values.nil? or allowed_values.empty?
        next true if allowed_values == php_build[property]
        next true if allowed_values.is_a? Array and allowed_values.include? php_build[property]
        false
      end
    end
    r
  end

  def filter!( filters )
    self.replace self.filter( filters )
  end
end