class String
  class << self
    def random *args
      if args.last.is_a? Hash
        length = args.last.delete(:length)
        chars = args.last.delete(:characters)
      else
        length = args.shift
        chars = args.shift
      end

      length = 1 if length.nil?
      chars = [('0'..'9'),('A'..'Z')].map{|rng| rng.to_a}.flatten if chars.nil?
      chars = chars.split('') if chars.is_a? String
      chars = chars.to_a if chars.respond_to? :to_a

      (0...length).map{ chars[Kernel.rand(chars.size)]}.join
    end
  end
end
