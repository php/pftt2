require File.join( File.dirname(__FILE__),'..','..','..','lib','monkeypatch','string','unquote.rb')


describe 'String#unquote' do

  def unquote_changes?(str)
      return !(str == str.unquote)
  end
  
  context 'when quotes are matched' do
    
    context 'and anchored at the ends' do
      it "should strip single quotes" do
        quo = "'foo'"
        unquo = quo.unquote
        unquo.should == "foo"
      end
      
      it "should strip double quotes" do
        quo = "'foo'"
        unquo = quo.unquote
        unquo.should == "foo"
      end
    end

    context 'and not anchored at the ends' do

      it 'should not strip quotes' do
        unquote_changes?('a"foo"').should be_false
        unquote_changes?('a\'foo\'').should be_false
      end
    end
  end

  context 'when quotes are mismatched' do

    it "should ignore single-double" do
      str = '\'foo"'
      str.clone.unquote.should == str
    end

    it "should ignore double-single" do
      str = '"foo\''
      str.clone.unquote.should == str
    end

    it "should ignore double-none" do
      str = '"foo'
      str.clone.unquote.should == str
    end

    it "should ignore none-double" do
      str = 'foo"'
      str.clone.unquote.should == str
    end

    it "should ignore single-none" do
      str = 'foo\''
      str.clone.unquote.should == str
    end

    it "should ignore none-single" do
      str = 'foo\''
      str.clone.unquote.should == str
    end

  end

end