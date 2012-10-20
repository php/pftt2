package com.github.mattficken.io;

import java.nio.charset.Charset;

import com.ibm.icu.charset.CharsetICU;

public class CharsetRec {
	//
	public static final Charset SHIFT_JIS = CharsetICU.forNameICU("Shift_JIS");
	public static final Charset BIG5 = CharsetICU.forNameICU("Big5");
	public static final Charset EUC_JP = CharsetICU.forNameICU("EUC-JP");
	public static final Charset EUC_KR = CharsetICU.forNameICU("EUC-KR");
	public static final Charset GB18030 = CharsetICU.forNameICU("GB18030");
	public static final Charset ISO_2022_CN = CharsetICU.forNameICU("ISO-2022-CN");
	public static final Charset ISO_2022_JP = CharsetICU.forNameICU("ISO-2022-JP");
	public static final Charset ISO_2022_KR = CharsetICU.forNameICU("ISO-2022-KR");
	public static final Charset UTF16_BE = CharsetICU.forNameICU("UTF-16BE");
	public static final Charset UTF16_LE = CharsetICU.forNameICU("UTF-16LE");
	public static final Charset UTF32_LE = CharsetICU.forNameICU("UTF-32LE");
	public static final Charset UTF32_BE = CharsetICU.forNameICU("UTF-32BE");
	public static final Charset UTF8 = CharsetICU.forNameICU("UTF-8");
	public static final Charset WINDOWS_1252 = CharsetICU.forNameICU("windows-1252");
	public static final Charset ISO_8859_1 = CharsetICU.forNameICU("ISO-8859-1");
	public static final Charset WINDOWS_1250 = CharsetICU.forNameICU("windows-1250");
	public static final Charset ISO_8859_2 = CharsetICU.forNameICU("ISO-8859-2");
	public static final Charset ISO_8859_5 = CharsetICU.forNameICU("ISO-8859-5");
	public static final Charset ISO_8859_6 = CharsetICU.forNameICU("ISO-8859-6");
	public static final Charset WINDOWS_1253 = CharsetICU.forNameICU("windows-1253");
	public static final Charset ISO_8859_7 = CharsetICU.forNameICU("ISO-8859-7");
	public static final Charset WINDOWS_1255 = CharsetICU.forNameICU("windows-1255");
	public static final Charset ISO_8859_8 = CharsetICU.forNameICU("ISO-8859-8");
	public static final Charset WINDOWS_1254 = CharsetICU.forNameICU("windows-1254");
	public static final Charset ISO_8859_9 = CharsetICU.forNameICU("ISO-8859-9");
	public static final Charset WINDOWS_1251 = CharsetICU.forNameICU("windows-1251");
	public static final Charset WINDOWS_1256 = CharsetICU.forNameICU("windows-1256");
	public static final Charset KOI8R = CharsetICU.forNameICU("KOI8R");
	public static final Charset IBM424 = CharsetICU.forNameICU("IBM424");
	public static final Charset IBM420 = CharsetICU.forNameICU("IBM420");
	//
	public static final Charset[] RECOGNIZABLE_CHARSETS = new Charset[]{
		SHIFT_JIS, BIG5, EUC_JP, EUC_KR, GB18030, ISO_2022_CN, ISO_2022_JP, ISO_2022_KR,
		UTF16_BE, UTF16_LE, UTF32_LE, UTF32_BE, UTF8,
		WINDOWS_1252, ISO_8859_1, WINDOWS_1250, ISO_8859_2, ISO_8859_5, ISO_8859_6, WINDOWS_1253,ISO_8859_7,
		WINDOWS_1255, ISO_8859_8, WINDOWS_1254, ISO_8859_9, WINDOWS_1251, WINDOWS_1256,
		KOI8R,
		IBM424, IBM420 
	};
	// TODO support for these other charsets
	public static final Charset WINDOWS_1257 = Charset.forName("Windows-1257");
	public static final Charset WINDOWS_1258 = Charset.forName("Windows-1258");
	public static final Charset GB2312 = Charset.forName("GB2312");
	public static final Charset EUC_TW = Charset.forName("EUC-TW");
	public static final Charset UTF_8 = Charset.forName("UTF-8");
	public static final Charset UTF_16 = Charset.forName("UTF-16");
	public static final Charset UTF_16BE = Charset.forName("UTF-16BE");
	public static final Charset UTF_16LE = Charset.forName("UTF-16LE");
	public static final Charset UTF_32 = Charset.forName("UTF-32");
	public static final Charset ISO_8859_3 = Charset.forName("ISO-8859-3");
	public static final Charset ISO_8859_4 = Charset.forName("ISO-8859-4");
	public static final Charset ISO_8859_11 = Charset.forName("ISO-8859-11");
	public static final Charset ISO_8859_13 = Charset.forName("ISO-8859-13");
	public static final Charset ISO_8859_15 = Charset.forName("ISO-8859-15");
	public static final Charset US_ASCII = Charset.forName("US-ASCII");
	public static final Charset KOI8_R = Charset.forName("KOI8-R");
	public static final Charset HZ = CharsetICU.forNameICU("HZ"); // aka? ISO 639-1 (bantu)
	public static final Charset ISCII = CharsetICU.forNameICU("ISCII"); // Indian Script Code for Information Interchange
	public static final Charset LMBCS = CharsetICU.forNameICU("LMBCS");
	public static final Charset BOCU1 = CharsetICU.forNameICU("BOCU1");
	//
	public static final int MAX_CONFIDENCE = 100;
	public static final int MIN_CONFIDENCE = 0;
	public static final int UNKNOWN_CONFIDENCE = MIN_CONFIDENCE;
	//
	public Charset cs;
	public int confidence;
	public ERecognizedLanguage lang;
	
	public CharsetRec(Charset cs, int confidence, ERecognizedLanguage lang) {
		this();
		this.cs = cs;
		this.confidence = confidence;
		this.lang = lang;
	}
	public CharsetRec() {
		
	}
	public static void clear(CharsetRec cr) {
		cr.cs = null;
		cr.lang = null;
		cr.confidence = UNKNOWN_CONFIDENCE;
	}
}
