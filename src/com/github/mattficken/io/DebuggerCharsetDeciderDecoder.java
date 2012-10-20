package com.github.mattficken.io;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.annotation.concurrent.NotThreadSafe;

import com.ibm.icu.charset.CharsetDecoderICU;
import com.ibm.icu.charset.CharsetDecoderICU.CharsetDecoderListener;
import com.ibm.icu.text.CharsetRecognizer;

@NotThreadSafe
public class DebuggerCharsetDeciderDecoder extends DefaultCharsetDeciderDecoder implements CharsetDeciderDecoder, CharsetDecoderListener {
	protected final ArrayList<AllInfo> selected;
	protected final ArrayList<HashMap<Charset, AllInfo>> all_info; // all_info.size() == selected.size()
	protected int pos;
	protected AllInfo last_info;
		
	public void clear() {
		pos = 0;
		selected.clear();
		all_info.clear();
	}
	public AllInfo getSelectedInfoForChar(int char_num) {
		return selected.get(char_num);
	}
	public int getCharCount() {
		return selected.size();
	}
	public Collection<AllInfo> getAllInfoForChar(int char_num) {
		return all_info.get(char_num).values();
	}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(selected.size());
		for (AllInfo i:selected)
			sb.append(i.c);
		return sb.toString();
	}
	public void reset() {
		pos = 0;
	}
	public static final class AllInfo {
		public final char c;
		public final int confidence;
		public final Charset cs;
		public final ERecognizedLanguage lang;
		public final byte[] in_bytes, out_bytes;
		public byte[] detection_window_bytes;
		
		public AllInfo(char c, int confidence, Charset cs, ERecognizedLanguage lang, byte[] in_bytes, byte[] out_bytes) {
			this.c = c;
			this.confidence = confidence;
			this.cs = cs;
			this.lang = lang;
			this.in_bytes = in_bytes;
			this.out_bytes = out_bytes;
		}
	}
	public void pushDetectionWindow(byte[] bytes) {
		last_info.detection_window_bytes = bytes;
	}
	protected void set(char c, int confidence, Charset cs, ERecognizedLanguage lang, byte[] in_bytes, byte[] out_bytes) {
		out_bytes = new byte[]{}; // TODO
		if (lang==null)
			lang = ERecognizedLanguage.ANY;
		
		AllInfo info = new AllInfo(c, confidence, cs, lang, in_bytes, out_bytes);
		this.last_info = info;
		HashMap<Charset,AllInfo> map;
		if (pos<selected.size()) {
			map = all_info.get(pos);
							
			// select info if it has the highest confidence
			if ( selected.get(pos).confidence < confidence )
				selected.set(pos, info);				
		} else {
			map = new HashMap<Charset,AllInfo>();
			all_info.add(map);
			
			selected.add(info);
		}
		map.put(cs, info);
		pos++;
	}
	public CharsetDecoderListener newCharsetDecoderListener(Charset cs, ERecognizedLanguage lang, int confidence) { // XXX get rid of this
		this.cdl_cs = cs;
		this.cdl_conf = confidence;
		this.cdl_lang = lang;
		return this;
	}
	protected Charset cdl_cs;
	protected ERecognizedLanguage cdl_lang;
	protected int cdl_conf;
	protected byte[] dbuf;
	@Override
	public void decoded(byte[] in, char c) {
		// TODO encode c into bytes too
		this.set(c, cdl_conf, cdl_cs, cdl_lang, in, null);
		dbuf = null;
	}
	@Override
	public void decoded(byte in, char c) {
		decoded(new byte[]{in}, c);
	}
	@Override
	public void decoded(char c) {
		if (dbuf==null)
			dbuf = new byte[]{}; // XXX shouldn't happen
		decoded(dbuf, c);
	}
	@Override
	public void push(byte b) {
		if (dbuf==null) {
			dbuf = new byte[]{b};
		} else {
			byte[] new_dbuf = new byte[dbuf.length+1];
			System.arraycopy(dbuf, 0, new_dbuf, 0, dbuf.length);
			new_dbuf[new_dbuf.length-1] = b;
			dbuf = new_dbuf;
		}
	}
	public void push(byte[] b) {
		for ( int i=0 ; i < b.length ; i++ )
			push(b[i]);
	}
		
	public DebuggerCharsetDeciderDecoder(CharsetRecognizer[] recogs) {
		super(recogs);
		selected = new ArrayList<AllInfo>();
		all_info = new ArrayList<HashMap<Charset,AllInfo>>();
	}
	
	HashMap<Charset,CharsetDecoderICU> cd_map = new HashMap<Charset,CharsetDecoderICU>();
	HashMap<Charset,CharsetEncoder> ce_map = new HashMap<Charset,CharsetEncoder>();
	
	// XXX if off > 0
	@Override
	public void decideCharset(byte[] bytes, int off, int len) {
		// TODO off > 0 or len!=bytes.length
		
		// TODO this.pushDetectionWindow(bytes);
		
		CharsetRecognizer[] recogs = CharsetDeciderDecoder.ALL_RECOGNIZERS;

		CharsetEncoder ce;
		for (CharsetRecognizer r : recogs) {
			CharsetRec cm = new CharsetRec();
			
			r.match(bytes, len, cm);
			cm.lang = r.getLang(); // TODO move into CharsetRecognizer#match
		
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
			
			CharBuffer cbuf = CharBuffer.allocate(len);
			cd.decode(ByteBuffer.wrap(bytes), cbuf, true, newCharsetDecoderListener(cs, cm.lang, cm.confidence));
		} // end for
	}
	
}
