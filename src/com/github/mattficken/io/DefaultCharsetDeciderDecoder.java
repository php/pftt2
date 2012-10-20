package com.github.mattficken.io;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import com.ibm.icu.charset.CharsetDecoderICU;
import com.ibm.icu.charset.CharsetICU;
import com.ibm.icu.text.CharsetRecognizer;

public class DefaultCharsetDeciderDecoder implements CharsetDeciderDecoder {
	protected final CharsetRecognizer[] recogs;
	protected CharsetICU cs;
	protected CharsetDecoderICU cd;
	
	public DefaultCharsetDeciderDecoder(CharsetRecognizer[] recogs) {
		this.recogs = recogs;
	}
	
	public CharsetICU getCommonCharset() {
		// TODO pick the one with the most confidence
		return this.cs;
	}

	// XXX off > 0
	@Override
	public void decideCharset(byte[] bytes, int off, int len) {
		CharsetICU cs = null;
		int max_conf = -1;
		for ( CharsetRecognizer recog : recogs ) {
			CharsetRec cr = new CharsetRec();
			recog.match(bytes, len, cr);
			//if ( cr.confidence > max_conf )
				cs = (CharsetICU) cr.cs; // XXX
				System.out.println(cs+" "+recog);
		}		
		this.cs = cs;
		if (this.cs==null) {
			cd = null;
		} else {
			cd = (CharsetDecoderICU) cs.newDecoder();
		}
	}

	@Override
	public int decode(byte[] bytes, int boff, int blen, char[] chars, int coff, int clen) {
		//if (cd==null)
			//cd = (CharsetDecoderICU) CharsetRec.UTF8.newDecoder();// TODO return CharacterReader.END_OF_STREAM;
	
		CharBuffer cbuf = CharBuffer.wrap(chars, coff, clen-coff);
		
		//System.out.println("46 "+bytes.length+" "+boff+" "+blen+" "+clen);
		
		cd.decode(ByteBuffer.wrap(bytes, boff, blen-boff), cbuf, false); // XXX false
		
		return clen;
	}

	@Override
	public boolean decidedCharset() {
		return cd != null;
	}

}
