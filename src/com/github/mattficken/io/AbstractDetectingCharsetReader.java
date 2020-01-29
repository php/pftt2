package com.github.mattficken.io;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.WeakHashMap;

import javax.annotation.concurrent.NotThreadSafe;

import com.ibm.icu.charset.CharsetDecoderICU;
import com.ibm.icu.text.CharsetRecognizer;

@NotThreadSafe
public abstract class AbstractDetectingCharsetReader extends AbstractReader {
	protected final CharsetDeciderDecoder cdd;
	
	public AbstractDetectingCharsetReader(InputStream in, CharsetDeciderDecoder cdd) {
		super(in);
		this.cdd = cdd;
	}
	
	public Charset cs; // TODO
	WeakHashMap<Charset,CharsetDecoderICU> cd_map = new WeakHashMap<Charset,CharsetDecoderICU>();
	WeakHashMap<Charset,CharsetEncoder> ce_map = new WeakHashMap<Charset,CharsetEncoder>();
	public CharsetEncoder ce;
	CharsetDecoderICU cd;
	public CharsetRecognizer[] recogs = CharsetDeciderDecoder.FIXED;//.ALL_RECOGNIZERS; // TODO
	CharsetRec hc_cm = null; // TODO usually start over for each #detectCharset call
	protected void detectCharset(byte[] bytes, int off, int len) {
		bytes = IOUtil.ensureLeftShifted(bytes, off, len);
		
		
				
		for (CharsetRecognizer r : recogs) {
			CharsetRec cm = new CharsetRec();
			
			r.match(bytes, len, cm);
			
			if (hc_cm==null||cm.confidence>hc_cm.confidence)
				hc_cm = cm;
		}
		
		CharsetRec cm = hc_cm;
		
		//
		cd = cd_map.get(cm.cs);
		if (cd==null) {
			cd = (CharsetDecoderICU) cm.cs.newDecoder();
			
			cd_map.put(cm.cs, cd);
		}
		ce = ce_map.get(cm.cs);
		if (ce==null) {
			ce = cm.cs.newEncoder();
			
			ce_map.put(cm.cs, ce);
		}
		//
		
		this.cs = cm.cs;
	}
	
	protected String convertLine(byte[] bytes, int off, int len, boolean end_of_input) {
		char[] chars = new char[len];
		convertChars(bytes, off, len+off, chars, 0, len, false, end_of_input); 
		return new String(chars, 0, cnv_clen);
	}
	
	int cnv_clen, cnv_blen=1;
	protected void convertChars(byte[] bytes, int bbuf_off, int bbuf_len, char[] chars, int coff, int clen, boolean end_of_input) {
		convertChars(bytes, bbuf_off, bbuf_len, chars, coff, clen, true, end_of_input);
	}
	private void convertChars(byte[] bytes, int bbuf_off, int bbuf_len, char[] chars, int coff, int clen, boolean check_has_more, boolean end_of_input) {
		//System.out.println("26 "+bytes.length+" "+bbuf_off+" "+bbuf_len+" "+chars.length+" "+coff+" "+clen);
		if (clen==0||bbuf_off>=bytes.length||bbuf_len-bbuf_off<0) {
			if (check_has_more)
				has_more = false;
			cnv_clen = cnv_blen = 0;
		} else if (bbuf_len==0) {
			cnv_clen = cnv_blen = 0;
		} else {
			
			ByteBuffer bbuf = ByteBuffer.wrap(bytes, bbuf_off, bbuf_len-bbuf_off);
			CharBuffer cbuf = CharBuffer.wrap(chars, coff, clen-coff);
			
			cnv_blen = bbuf.position();
			cnv_clen = cbuf.position();
			
			cd.decode(bbuf, cbuf, end_of_input);
						
			cnv_blen = bbuf.position()-cnv_blen;
			cnv_clen = cbuf.position()-cnv_clen;
		}
	}

}
