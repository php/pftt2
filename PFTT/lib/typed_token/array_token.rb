module Token
  module ArrayToken
    class Base < Array
      def == (o)
        return self.class == o.class
      end
    end
  end
end