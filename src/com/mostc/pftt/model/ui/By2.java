package com.mostc.pftt.model.ui;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

public class By2 {
	
	public static By partialClassName(String ...class_name_fragments) {
		return new ByPartialClassName(class_name_fragments);
	}
	
	public static By partialId(String ...id_fragments) {
		return new ByPartialId(id_fragments);
	}
	
	public static class ByPartialId extends By {
		protected final String[] id_fragments;

		public ByPartialId(String[] id_fragments) {
			this.id_fragments = id_fragments;
		}

		@Override
		public List<WebElement> findElements(SearchContext context) {
			// XXX make this work for other SearchContext impls (will only for Firefox now)
			FirefoxDriver ff = (FirefoxDriver) context;
			
			String xpath = "//*[contains(@id, '"+id_fragments[0];
			if (id_fragments.length > 1) {
				for ( int i=1 ; i < id_fragments.length ; i++ ) {
					xpath += "') and contains(@id, '"+id_fragments[i];
				}
			}
			xpath += "')]";
			
			return ff.findElementsByXPath(xpath);
		}
		
	}

	public static class ByPartialClassName extends By {
		protected final String[] class_name_fragments;

		public ByPartialClassName(String[] class_name_fragments) {
			this.class_name_fragments = class_name_fragments;
		}

		@Override
		public List<WebElement> findElements(SearchContext context) {
			// XXX make this work for other SearchContext impls (will only for Firefox now)
			FirefoxDriver ff = (FirefoxDriver) context;
			
			String xpath = "//*[contains(@class, '"+class_name_fragments[0];
			if (class_name_fragments.length > 1) {
				for ( int i=1 ; i < class_name_fragments.length ; i++ ) {
					xpath += ") and contains(@class, '"+class_name_fragments[i];
				}
			}
			xpath += "')]";
			
			return ff.findElementsByXPath(xpath);
		}
		
	}
}
