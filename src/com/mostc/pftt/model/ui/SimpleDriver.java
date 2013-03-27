package com.mostc.pftt.model.ui;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.github.mattficken.io.StringUtil;
import com.google.common.base.Predicate;
import com.mostc.pftt.util.StringUtil2;

public class SimpleDriver {
	protected final WebDriver driver;
	protected final WebDriverWait wait;
	protected final String base_url;
	
	public SimpleDriver(String base_url, WebDriver driver) {
		this.base_url = base_url;
		this.driver = driver;
		
		// 60 second timeout, sleep 30 seconds
		wait = new WebDriverWait(driver, 60, 30000);
	}
	public WebDriverWait driverWait() {
		return wait;
	}
	public WebDriver driver() {
		return driver;
	}
	public WebElement findElement(By by) {
		return getElement(by);
	}
	/** gets element immediately, without waiting
	 * 
	 * @see #getElement - safer to use this, in case waiting for page to load
	 * @param by
	 * @return
	 */
	public WebElement getElementNow(By by) {
		try {
			return driver.findElement(by);
		} catch ( NoSuchElementException ex ) {
			return null;
		}
	}
	/** gets element, waiting up to a minute for it to become available.
	 * 
	 * @see #getElementNow
	 * @param by
	 * @return
	 */
	public WebElement getElement(final By by) {
		WebElement we = getElementNow(by);
		if (we!=null)
			return we;
		final AtomicReference<WebElement> shared = new AtomicReference<WebElement>();
		wait.until(new Predicate<WebDriver>() {
				@Override
				public boolean apply(@Nullable WebDriver d) {
					WebElement we = getElementNow(by);
					if (we==null)
						return false; // continue
					shared.set(we);
					return true;// break
				}
			});
		return getElementNow(by);// TODO shared.get();
	}
	public boolean inputType(String id, String value) {
		return inputType(By.id(id), value);
	}
	/**
	 * sometimes WebDriver#sendKeys misses the first few characters of the string, this
	 * checks if the string was typed correctly and retypes it if it was not
	 * 
	 * @param by
	 * @param value
	 * @return
	 */
	public boolean inputType(By by, String value) {
		WebElement we = getElement(by);
		return we == null ? false : inputType(we, value);
	}
	protected boolean inputType(WebElement we, String value) {
		for ( int i=0 ; i < 10 ; i++ ) {
			we.clear();
			we.sendKeys(value);
			if (StringUtil.equalsCS(value, we.getAttribute("value")))
				return true;
			try {
				Thread.sleep(500);
			} catch ( InterruptedException ex ) {}
		}
		return true;
	}
	public boolean selectByValue(By by, String value) {
		WebElement we = getElement(by);
		return we == null ? false : selectByValue(we, value);
	}
	protected boolean selectByValue(WebElement we, String value) {
		new Select(we).selectByValue(value);
		return true;
	}
	public boolean selectByText(By by, String text) {
		WebElement we = getElement(by);
		return we == null ? false : selectByText(we, text);
	}
	protected boolean selectByText(WebElement we, String text) {
		new Select(we).selectByVisibleText(text);
		return true;
	}
	public boolean clickLinkText(String text) {
		return click(By.linkText(text));
	}
	public boolean clickId(String id) {
		return click(By.id(id));
	}
	public boolean click(By by) {
		WebElement we = getElement(by);
		return we == null ? false : click(we);
	}
	protected boolean click(WebElement we) {
		we.click();
		return true; // TODO exception?
	}
	public boolean hasElement(By by) {
		return getElement(by) != null;
	}
	public boolean hasElementNow(By by) {
		return getElementNow(by) != null;
	}
	public EUITestStatus hasElementPF(By by) {
		return hasElement(by) ? EUITestStatus.PASS : EUITestStatus.FAIL;
	}
	public void get(String url) {
		if (!StringUtil2.hasHttp(url)) {
			try {
				url = new URL(base_url+"/"+url).toString();
			} catch (MalformedURLException e) {
			}
		}
		
		driver.get(url);
	}
	public WebElement getElementNow(WebElement parent, By by) {
		try {
			return parent.findElement(by);
		} catch ( NoSuchElementException ex ) {
			return null;
		}
	}
	public WebElement getElement(final WebElement parent, final By by) {
		WebElement we = getElementNow(parent, by);
		if (we!=null)
			return we;
		final AtomicReference<WebElement> shared = new AtomicReference<WebElement>();
		wait.until(new Predicate<WebDriver>() {
				@Override
				public boolean apply(@Nullable WebDriver d) {
					WebElement we = getElementNow(parent, by);
					if (we==null)
						return false; // continue
					shared.set(we);
					return true;// break
				}
			});
		return getElementNow(parent, by);// TODO shared.get();
	}
	public boolean hasElementNow(WebElement we, By by) {
		return getElementNow(we, by) != null;
	}
	public boolean hasElement(WebElement we, By by) {
		return getElement(we, by) != null;
	}
	public boolean click(WebElement parent, By by) {
		WebElement we = getElement(parent, by);
		return we == null ? false : click(we);
	}
	public boolean selectByText(WebElement parent, By by, String text) {
		WebElement we = getElement(parent, by);
		return we == null ? false : selectByText(we, text);
	}
	public boolean selectByValue(WebElement parent, By by, String value) {
		WebElement we = getElement(parent, by);
		return we == null ? false : selectByValue(we, value);
	}
	public boolean inputType(WebElement parent, By by, String value) {
		WebElement we = getElement(parent, by);
		return we == null ? false : inputType(we, value);
	}
	public boolean hasText(String text) {
		try {
			return driver.findElement(By.xpath("//*[contains(., '"+text+"')]")) != null;
		} catch ( NoSuchElementException ex ) {
			return false;
		}
	}
	public boolean hasText(String ...text) {
		for ( String t : text ) {
			if (!hasText(t))
				return false;
		}
		return true;
	}
	public EUITestStatus hasTextPF(String ...text) {
		return hasText(text) ? EUITestStatus.PASS : EUITestStatus.FAIL;
	}
	public boolean hasText(WebElement we, String text) {
		try {
			return we.findElement(By.xpath("//*[contains(., '"+text+"')]")) != null;
		} catch ( NoSuchElementException ex ) {
			return false;
		}
	}
	public boolean hasText(WebElement we, String ...text) {
		for ( String t : text ) {
			if (!hasText(we, t))
				return false;
		}
		return true;
	}
	public EUITestStatus hasTextPF(WebElement we, String ...text) {
		return hasText(we, text) ? EUITestStatus.PASS : EUITestStatus.FAIL;	
	}
} // end public static class SimpleDriver