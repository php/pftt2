package com.github.mattficken.io;

public enum ERecognizedLanguage {
	ANY,
	CS,
	DA,
	DE,
	EL,
	EN,
	ES,
	FR,
	HU,
	IT,
	JP,
	KR,
	NL,
	NO,
	PL,
	PT,
	RO,
	RU,
	SV,
	TR,
	ZH,
	AR_LEFT_TO_RIGHT,
	AR_RIGHT_TO_LEFT {
		@Override
		public ETextDirection getTextDirection() {
			return ETextDirection.RIGHT_TO_LEFT;
		}
	},
	HE_LEFT_TO_RIGHT,
	HE_RIGHT_TO_LEFT {
		@Override
		public ETextDirection getTextDirection() {
			return ETextDirection.RIGHT_TO_LEFT;
		}
	};
	
	public ETextDirection getTextDirection() {
		// default
		return ETextDirection.LEFT_TO_RIGHT;
	}
	
	public static enum ETextDirection {
		LEFT_TO_RIGHT,
		RIGHT_TO_LEFT
	}
}