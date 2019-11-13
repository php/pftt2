package com.github.mattficken.io;

import com.ibm.icu.text.CharsetRecog_2022;
import com.ibm.icu.text.CharsetRecog_UTF8;
import com.ibm.icu.text.CharsetRecog_Unicode;
import com.ibm.icu.text.CharsetRecog_mbcs;
import com.ibm.icu.text.CharsetRecog_sbcs;
import com.ibm.icu.text.CharsetRecognizer;

public interface CharsetDeciderDecoder {
	void decideCharset(byte[] bytes, int off, int len);
	int decode(byte[] bytes, int boff, int blen, char[] chars, int coff, int clen);
	boolean decidedCharset();
	
	// NOTE: this recognizer will also check for WINDOWS_1252 (ISO-8859-* recognizers check for the windows charsets Windows_125*)
	public static final CharsetRecognizer ISO_8859_1 = new CharsetRecog_sbcs.CharsetRecog_8859_1_en();
	public static final CharsetRecognizer ISO_8859_2 = new CharsetRecog_sbcs.CharsetRecog_8859_2_cs();
	public static final CharsetRecognizer ISO_8859_5 = new CharsetRecog_sbcs.CharsetRecog_8859_5_ru();
	public static final CharsetRecognizer KOI8_R = new CharsetRecog_sbcs.CharsetRecog_KOI8_R();
	public static final CharsetRecognizer[] CHINESE_JAPANESE_KOREAN = new CharsetRecognizer[] {
		new CharsetRecog_mbcs.CharsetRecog_sjis(),
		new CharsetRecog_mbcs.CharsetRecog_euc.CharsetRecog_euc_jp(),
		new CharsetRecog_2022.CharsetRecog_2022JP(),
		new CharsetRecog_mbcs.CharsetRecog_gb_18030(),
		new CharsetRecog_mbcs.CharsetRecog_big5(),
		new CharsetRecog_mbcs.CharsetRecog_euc.CharsetRecog_euc_kr(),
		new CharsetRecog_2022.CharsetRecog_2022CN(),
		new CharsetRecog_2022.CharsetRecog_2022KR(),
	};
	public static final CharsetRecognizer[] UNICODE = new CharsetRecognizer[] {
		new CharsetRecog_Unicode.CharsetRecog_UTF_16_BE(),
		new CharsetRecog_Unicode.CharsetRecog_UTF_16_LE(),
		new CharsetRecog_Unicode.CharsetRecog_UTF_32_BE(),
		new CharsetRecog_Unicode.CharsetRecog_UTF_32_LE(),
		new CharsetRecog_UTF8(),
	};
	public static final CharsetRecognizer[] ARABIC = new CharsetRecognizer[] {
		new CharsetRecog_sbcs.CharsetRecog_IBM420_ar_ltr(),
		new CharsetRecog_sbcs.CharsetRecog_IBM420_ar_rtl()
	};
	public static final CharsetRecognizer[] HEBREW = new CharsetRecognizer[] {
		new CharsetRecog_sbcs.CharsetRecog_IBM424_he_ltr(),
		new CharsetRecog_sbcs.CharsetRecog_IBM424_he_rtl()
	};
	public static final CharsetRecognizer[] EASTERN_EUROPEAN = new CharsetRecognizer[] {
		ISO_8859_2,
		new CharsetRecog_sbcs.CharsetRecog_8859_2_hu(),
		new CharsetRecog_sbcs.CharsetRecog_8859_2_pl(),
		new CharsetRecog_sbcs.CharsetRecog_8859_2_ro(),
		new CharsetRecog_sbcs.CharsetRecog_8859_8_I_he(),
		ISO_8859_5,
		KOI8_R
	};
	public static final CharsetRecognizer[] WESTERN_EUROPEAN = new CharsetRecognizer[] {
		ISO_8859_1,
		new CharsetRecog_sbcs.CharsetRecog_8859_1_da(),
		new CharsetRecog_sbcs.CharsetRecog_8859_1_de(),
		new CharsetRecog_sbcs.CharsetRecog_8859_1_es(),
		new CharsetRecog_sbcs.CharsetRecog_8859_1_fr(),
		new CharsetRecog_sbcs.CharsetRecog_8859_1_it(),
		new CharsetRecog_sbcs.CharsetRecog_8859_1_nl(),
		new CharsetRecog_sbcs.CharsetRecog_8859_1_no(),
		new CharsetRecog_sbcs.CharsetRecog_8859_1_pt(),
		new CharsetRecog_sbcs.CharsetRecog_8859_1_sv(),
	};
	public static final CharsetRecognizer[] EUROPEAN = ArrayUtil.mergeNoDuplicates(CharsetRecognizer.class, WESTERN_EUROPEAN, EASTERN_EUROPEAN);
	public static final CharsetRecognizer[] EXPRESS_RECOGNIZERS = ArrayUtil.mergeNoDuplicates(CharsetRecognizer.class, CHINESE_JAPANESE_KOREAN, new CharsetRecognizer[] {
		ISO_8859_1,
		ISO_8859_2,
		ISO_8859_5,
		new CharsetRecog_sbcs.CharsetRecog_8859_6_ar(),
		new CharsetRecog_sbcs.CharsetRecog_8859_7_el(),
		new CharsetRecog_sbcs.CharsetRecog_8859_8_he(),
		new CharsetRecog_sbcs.CharsetRecog_8859_9_tr(),
		KOI8_R,
		new CharsetRecog_sbcs.CharsetRecog_windows_1251(),
		new CharsetRecog_sbcs.CharsetRecog_windows_1256()
	});
	public static final CharsetRecognizer[] ALL_RECOGNIZERS = ArrayUtil.mergeNoDuplicates(CharsetRecognizer.class, EXPRESS_RECOGNIZERS, EUROPEAN, ARABIC, HEBREW, UNICODE);

	public static final CharsetRecognizer[] FIXED = new CharsetRecognizer[] {new CharsetRecog_sbcs.CharsetRecog_windows_1251()};
	
	public static enum ERecognizerGroup {
		EXPRESS {
			@Override
			public CharsetRecognizer[] getRecognizers() {
				return CharsetDeciderDecoder.EXPRESS_RECOGNIZERS;
			}
		},
		ALL {
			@Override
			public CharsetRecognizer[] getRecognizers() {
				return CharsetDeciderDecoder.ALL_RECOGNIZERS;
			}
		},
		CHINESE_JAPANESE_KOREAN {
			@Override
			public CharsetRecognizer[] getRecognizers() {
				return CharsetDeciderDecoder.CHINESE_JAPANESE_KOREAN;
			}
		},
		UNICODE {
			@Override
			public CharsetRecognizer[] getRecognizers() {
				return CharsetDeciderDecoder.UNICODE;
			}
		},
		ARABIC {
			@Override
			public CharsetRecognizer[] getRecognizers() {
				return CharsetDeciderDecoder.ARABIC;
			}
		},
		EUROPEAN {
			@Override
			public CharsetRecognizer[] getRecognizers() {
				return CharsetDeciderDecoder.EUROPEAN;
			}
		},
		EASTERN_EUROPEAN {
			@Override
			public CharsetRecognizer[] getRecognizers() {
				return CharsetDeciderDecoder.EASTERN_EUROPEAN;
			}
		},
		WESTERN_EUROPEAN {
			@Override
			public CharsetRecognizer[] getRecognizers() {
				return CharsetDeciderDecoder.WESTERN_EUROPEAN;
			}
		};
		
		public abstract CharsetRecognizer[] getRecognizers();
		
		public static CharsetRecognizer[] getRecgonizers(ERecognizerGroup g) {
			return g == null ? ALL.getRecognizers() : g.getRecognizers();
		}
	}
}
