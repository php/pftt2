package com.github.mattficken.io;

public class Trie {
	private final Trie[]  myLinks;
    private boolean myIsWord;
    private final char start;
    
    public Trie(char start) {
    	this.start = start;
    	myLinks = new Trie[255 - start];
    	myIsWord = false;
    }
    
    private int index(char c) {
    	return c;//( c - start ) + 2;
    }
    
    public void addString(String s) {
    	Trie t = this;
    	final int limit = s.length();
    	int index;
    	for(int k=0; k < limit; k++) {
    		index = index(s.charAt(k));
    		if (t.myLinks[index] == null)
    			t.myLinks[index] = t = new Trie(start);
    		else
    			t = t.myLinks[index];
    	}
    	t.myIsWord = true;
    }

    public boolean equals(String s) {
    	Trie t = this;
    	final int limit = s.length();
    	int index, k;
    	for(k=0; k < limit; k++) {
    		index = index(s.charAt(k));
    		if (t.myLinks[index] == null)
    			return false;
    		t = t.myLinks[index];
    	}
    	return t.myIsWord && k + 1 >= limit;
    }
    
    public boolean startsWith(String s) {
    	Trie t = this;
    	final int limit = s.length();
    	int index;
    	for(int k=0; k < limit; k++) {
    		index = index(s.charAt(k));
    		if (t.myLinks[index] == null)
    			return false;
    		t = t.myLinks[index];
    	}
    	return t != this;
    }
    
} // end public class Trie
