module TypedToken
  module StringToken
    class Base < String
      def == (o)
        return self.class == o.class
      end
    end
  end
end