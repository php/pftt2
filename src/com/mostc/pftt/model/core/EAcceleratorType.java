package com.mostc.pftt.model.core;

import com.mostc.pftt.scenario.CodeCacheScenario;

public enum EAcceleratorType {
	APC {
		@Override
		public CodeCacheScenario getCodeCacheScenario() {
			return CodeCacheScenario.APC;
		}
	},
	WINCACHE {
		@Override
		public CodeCacheScenario getCodeCacheScenario() {
			return CodeCacheScenario.WINCACHE;
		}
	},
	NONE {
		@Override
		public CodeCacheScenario getCodeCacheScenario() {
			return CodeCacheScenario.NO;
		}
	},
	OPCACHE {
		@Override
		public CodeCacheScenario getCodeCacheScenario() {
			return CodeCacheScenario.OPCACHE;
		}
	};
	
	public abstract CodeCacheScenario getCodeCacheScenario();
}
