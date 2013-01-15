package com.mostc.pftt.model.phpt;

import com.mostc.pftt.scenario.AbstractCodeCacheScenario;

public enum EAcceleratorType {
	APC {
		@Override
		public AbstractCodeCacheScenario getCodeCacheScenario() {
			return AbstractCodeCacheScenario.APC;
		}
	},
	WINCACHE {
		@Override
		public AbstractCodeCacheScenario getCodeCacheScenario() {
			return AbstractCodeCacheScenario.WINCACHE;
		}
	},
	NONE {
		@Override
		public AbstractCodeCacheScenario getCodeCacheScenario() {
			return AbstractCodeCacheScenario.NO;
		}
	};
	
	public abstract AbstractCodeCacheScenario getCodeCacheScenario();
}
