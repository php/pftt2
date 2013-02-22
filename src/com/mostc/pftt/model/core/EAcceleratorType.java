package com.mostc.pftt.model.core;

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
	},
	OPTIMIZER_PLUS {
		@Override
		public AbstractCodeCacheScenario getCodeCacheScenario() {
			return AbstractCodeCacheScenario.ZEND_OPTIMIZER_PLUS;
		}
	};
	
	public abstract AbstractCodeCacheScenario getCodeCacheScenario();
}
