
module Util
  class CombinationGenerator
  
      def initialize(n, r)
        @n = n
        @r = r
        @a = Array.new(r, 0)
        nFact = getFactorial(@n)
        rFact = getFactorial(@r)
        nminusrFact = getFactorial(@n - @r)
        @total = nFact / (rFact * nminusrFact )
      
        i = 0
        while (i < @a.length) do
          @a[i] = i
          i = i + 1
        end
        @numLeft = @total
        self
      end
  
      def hasMore
        @numLeft > 0
      end
  
      def getFactorial (n)
        fact = 1
        i = n
        while ( i > 1 )
          fact = fact * i
          i = i - 1
        end
        fact
      end
  
      def getNext
        if @numLeft == @total
          @numLeft = @numLeft - 1
          return @a
        end
  
        i = @r - 1;
        while (@a[i] == @n - @r + i) do
          i = i - 1;
        end
        @a[i] = @a[i] + 1;
        j = i + 1;
        while ( j < @r ) do
          @a[j] = @a[i] + j - i
          j = j + 1
        end
  
        @numLeft = @numLeft - 1
        return @a
      end
    
    end # end class CombinationGenerator
end # module Util
