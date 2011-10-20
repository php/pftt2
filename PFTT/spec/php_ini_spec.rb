require File.join( File.dirname(__FILE__),'..','bootstrap.rb')

describe PhpIni do

  before :each do
    @php_ini = PhpIni.new
  end

  context "#configure" do

    context 'when handling append directives' do

      context "and the directive already exists" do
        it "should not change" do
          str = 'foo=bar'
          @php_ini.configure( str )
          @php_ini.configure( str )
          @php_ini.to_s.should == str
        end

        it "should return false" do
          @php_ini.configure('foo=bar')
          @php_ini.configure('foo=bar').should be_false
        end
      end

      context "and the directive's key is extension" do

        it "should append the directive" do
          ary = ['extension=foo','extension=bar']
          @php_ini.configure(ary[0])
          @php_ini.configure(ary[1])
          (@php_ini.to_s == ary.join("\n")).should be_true
        end

        it "should return true" do
          ary = ['extension=foo','extension=bar']
          @php_ini.configure(ary[0])
          @php_ini.configure(ary[1]).should be_true
        end

      end
      
      context "and the key is not extension" do

        context "and the directive's key already exists" do

          it "should overwrite the old directive" do
            ary = ['foo=foo','foo=bar']
            @php_ini.configure(ary[0])
            @php_ini.configure(ary[1])
            (@php_ini.to_s == ary[1]).should be_true
          end

          it "should return true" do
            ary = ['foo=foo','foo=bar']
            @php_ini.configure(ary[0])
            @php_ini.configure(ary[1]).should be_true
          end

        end
        
        context "and the directive's key does not exist" do
          
          it "should append the directive" do
            ary = ['foo=foo','bar=bar']
            @php_ini.configure(ary[0])
            @php_ini.configure(ary[1])
            #(@php_ini.to_s == ary.join("\n")).should be_true
            @php_ini.to_s.split("\n").should == ary
          end

          it "should return true" do
            ary = ['foo=foo','bar=bar']
            @php_ini.configure(ary[0])
            @php_ini.configure(ary[1]).should be_true
          end

        end
                
      end

    end

    context 'when handling delete directives' do

      context "and value is given (explicit)" do

        context "and a key match is present" do

          context "and key/value match is present" do
            it "should delete the match" do
              strplus = 'foo=foo'
              strminus = '-foo=foo'
              @php_ini.configure [strplus,strminus]
              @php_ini.should == PhpIni.new
            end

            it "should leave non-matches alone" do
              @php_ini.configure ['foo=foo','bar=bar']
              @php_ini.configure ['-foo=foo']
              @php_ini.should == PhpIni.new('bar=bar')
            end

            it "should return true" do
              @php_ini.configure ['foo=foo','bar=foo']
              (@php_ini.configure '-foo=foo').should be_true
            end
          end

          context "and a key/value match is not present" do
            it "should not change" do
              ary = ['foo=bar','bar=foo']
              @php_ini.configure ary
              @php_ini.configure '-foo=baz'
              @php_ini.should == PhpIni.new(ary)
            end

            it "should return false" do
              @php_ini.configure ['foo=bar','bar=foo']
              (@php_ini.configure '-foo=baz').should be_false
            end

          end

        end
        
        context "and a key match is not present" do
          it "should not change" do
            ary = ['foo=bar','bar=foo']
            @php_ini.configure ary
            @php_ini.configure '-baz=foo'
            @php_ini.should == PhpIni.new(ary)
          end

          it "should return false" do
            @php_ini.configure ['foo=bar','bar=foo']
            (@php_ini.configure '-baz=foo').should be_false
          end

        end

      end

      context "and no value is present (implicit)" do
        
        context "and match is present" do

          it "should delete all matches" do
            @php_ini.configure ['foo=foo','foo=bar']
            @php_ini.configure '-foo'
            (@php_ini.to_s == '').should be_true
          end

          it "should leave directives with mismatching keys alone" do
            @php_ini.configure ['foo=foo','bar=bar']
            @php_ini.configure '-foo'
            (@php_ini.to_s == 'bar=bar').should be_true
          end

          it "should return true" do
            @php_ini.configure ['foo=foo','foo=bar']
            (@php_ini.configure '-foo').should be_true 
          end

        end

        context "and match is not present" do
          it "should not change" do
            ary = ['foo=bar','bar=foo']
            @php_ini.configure ary
            @php_ini.configure '-baz'
            @php_ini.should == PhpIni.new(ary)
          end

          it "should return false" do
            @php_ini.configure ['foo=bar','bar=foo']
            (@php_ini.configure '-baz').should be_false
          end
        
        end

      end

    end

  end

  context "#==" do

    it "should return false when no overlap" do
      PhpIni.new('foo=bar').should_not == PhpIni.new('bar=foo')
    end

    it "should return false when keys match but values do not" do
      PhpIni.new('foo=bar').should_not == PhpIni.new('foo=foo')
    end

    it "should return true on perfect match" do
      ary = ['foo=bar','bar=foo','baz=bat']
      PhpIni.new( ary.clone ).should == PhpIni.new( ary.clone )
    end

    it "should return true if *all* extension directives match" do
      ary = ['extension=foo','extension=bar','extension=baz']
      PhpIni.new( ary.clone ).should == PhpIni.new( ary.clone )
    end

    it "should return true if directives match but are misordered" do
      ary = ['foo=bar','bar=foo','baz=bat']
      PhpIni.new( ary.clone ).should == PhpIni.new( ary.rotate )
    end

    it "should return false unless *all* extension directives match" do
      ary = ['extension=foo','extension=bar','extension=baz']
      PhpIni.new( ary.clone ).should_not == PhpIni.new( ary.clone + ['extension=asdf'] )
    end

    it "should return true if one directive is quoted and the other is not" do
      left = 'foo=foo'; right = 'foo="foo"'
      PhpIni.new( left ).should == PhpIni.new( right )
    end

  end

end