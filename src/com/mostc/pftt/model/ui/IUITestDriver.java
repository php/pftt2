package com.mostc.pftt.model.ui;

import javax.annotation.Nonnull;

import org.openqa.selenium.By;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** 
 * 
 * @author Matt Ficken
 *
 */

public interface IUITestDriver {
	/** returns the name of the web browser being driven, the version and the OS platform string (Win7sp1x64, Gentoo-x86, etc...)
	 * 
	 * @return
	 */
	public String getWebBrowserNameAndVersion();
	/** gets the location on the page for the element last used by #getElement #click or other methods on this driver.
	 * 
	 * it may return null if none of those methods have been called or no element was found
	 * 
	 * @return
	 */
	public Point getLastElementLocationOnPage();
	public WebDriverWait driverWait();
	/** returns the underlying WebDriver that this wraps
	 * 
	 * @return
	 */
	public WebDriver driver();
	/** alias for #getElement
	 * 
	 * allows WebDriver code to run direct on this.
	 * 
	 * @param by
	 * @return
	 */
	public WebElement findElement(By by);
	/** gets element immediately, without waiting
	 * 
	 * @see #getElement - safer to use this, in case waiting for page to load
	 * @param by
	 * @return
	 */
	public WebElement getElementNow(By by);
	/** gets element, waiting up to a minute for it to become available.
	 * 
	 * using #getElement is usually a better method to use than #getElementNow because if page
	 * takes any longer to load than usual, #getElementNow might not find the element sometimes.
	 * 
	 * @see #getElementNow
	 * @param by
	 * @return
	 */
	public WebElement getElement(By by);
	public boolean inputTypeName(String name, String value);
	public boolean inputTypeId(String id, String value);
	/**
	 * sometimes WebDriver#sendKeys misses the first few characters of the string, this
	 * checks if the string was typed correctly and retypes it if it was not
	 * 
	 * @param by
	 * @param value
	 * @return
	 */
	public boolean inputType(By by, String value);
	public boolean selectByValueName(String name, String value);
	public boolean selectByValueId(String id, String value);
	public boolean selectByValue(By by, String value);
	public boolean selectByText(By by, String text);
	public boolean selectByTextName(String name, String text);
	public boolean selectByTextId(String id, String text);
	public boolean clickLinkText(String text);
	public boolean clickPartialLinkText(String partial_text);
	public boolean clickId(String id);
	public boolean clickName(String name);
	public boolean click(WebElement e);
	public boolean click(By by);
	/** gets the id= attribute set in the HTML that created this element.
	 * 
	 * Note: WebElement#getId does not return that. it returns the internal GUID that represents this element in the web browser's memory
	 * 
	 * @param we
	 * @return
	 */
	@Nonnull
	public String getId(WebElement we);
	@Nonnull
	public String getValue(WebElement we);
	@Nonnull
	public String getHref(WebElement we);
	public boolean mouseOverLinkText(String text);
	public boolean mouseOverPartialLinkText(String partial_text);
	public boolean mouseOverId(String id);
	public boolean mouseOver(By by);
	public boolean mouseOver(WebElement we);
	public boolean focusLinkText(String text);
	public boolean focusPartialLinkText(String partial_text);
	public boolean focusId(String id);
	public boolean focus(By by);
	public boolean focus(WebElement we);
	/** use this to build chains of actions with the mouse
	 * 
	 * @see Actions#perform  - to execute the actions once you've built them
	 * 
	 * @return
	 */
	public Actions createActions();
	public boolean hasElementId(String id);
	public boolean hasElement(By by);
	public boolean hasElementNowId(String id);
	public boolean hasElementNow(By by);
	public EUITestStatus hasElementIdPF(String id);
	public EUITestStatus hasElementPF(By by);
	public void get(String url);
	public WebElement getElementNow(WebElement parent, By by);
	public WebElement getElement(final WebElement parent, By by);
	public boolean hasElementNowId(WebElement we, String id);
	public boolean hasElementNow(WebElement we, By by);
	public boolean hasElement(WebElement we, By by);
	public boolean clickLinkText(WebElement parent, String text);
	public boolean clickPartialLinkText(WebElement parent, String partial_text);
	public boolean click(WebElement parent, By by);
	public boolean selectByText(WebElement parent, By by, String text);
	public boolean selectByTextId(WebElement parent, String id, String text);
	public boolean selectByValueName(WebElement parent, String name, String value);
	public boolean selectByValueId(WebElement parent, String id, String value);
	public boolean selectByValue(WebElement parent, By by, String value);
	public boolean inputTypeName(WebElement parent, String name, String value);
	public boolean inputTypeId(WebElement parent, String id, String value);
	public boolean inputType(WebElement parent, By by, String value);
	public boolean mouseOverLinkText(WebElement parent, String text);
	public boolean mouseOverPartialLinkText(WebElement parent, String partial_text);
	public boolean mouseOverId(WebElement parent, String id);
	public boolean mouseOver(WebElement parent, By by);
	public boolean focusLinkText(WebElement parent, String text);
	public boolean focusPartialLinkText(WebElement parent, String partial_text);
	public boolean focusId(WebElement parent, String id);
	public boolean focus(WebElement parent, By by);
	public boolean hasText(String text);
	public boolean hasTextAll(String ...text);
	public boolean hasNotTextAll(String ...text);
	public boolean hasTextAny1(String ...text);
	public boolean hasTextAny1(WebElement we, String ...text);
	public boolean hasNotTextAny1(String ...text);
	public boolean hasNotTextAny1(WebElement we, String ...text);
	public EUITestStatus hasTextAllPF(String ...text);
	public EUITestStatus hasNotTextAllPF(String ...text);
	public EUITestStatus hasTextAny1PF(String ...text);
	public EUITestStatus hasTextAny1PF(WebElement we, String ...text);
	public EUITestStatus hasNotTextAny1PF(String ...text);
	public EUITestStatus hasNotTextAny1PF(WebElement we, String ...text);
	public EUITestStatus pf(boolean true_if_pass);
	public boolean hasText(WebElement we, String text);
	public boolean hasText(WebElement we, String ...text);
	public boolean hasNotTextAll(WebElement we, String ...text);
	public EUITestStatus hasNotTextAllPF(WebElement we, String ...text);
	public EUITestStatus hasTextPF(WebElement we, String ...text);
	public boolean fileBrowse(By by, String file_ext, String content);
	public boolean fileBrowse(By by, String file_ext, byte[] content);
	public boolean isSelected(By by);
	public boolean isSelected(WebElement e);
	public boolean isSelectedName(String name);
	public boolean isSelectedId(String id);
	public boolean isChecked(By by);
	public boolean isChecked(WebElement e);
	public boolean isCheckedName(String name);
	public boolean isCheckedId(String id);
	public boolean hasValueName(String name, String value);
	public boolean hasValueId(String id, String value);
	public boolean hasValue(By id, String value);
	public EUITestStatus hasValueNamePF(String name, String value);
	public EUITestStatus hasValueIdPF(String name, String value);
	public EUITestStatus hasValuePF(By id, String value);
	public boolean hasValueName(WebElement we, String name, String value);
	public boolean hasValueId(WebElement we, String id, String value);
	public boolean hasValue(WebElement we, By id, String value);
	public EUITestStatus hasValueNamePF(WebElement we, String name, String value);
	public EUITestStatus hasValueIdPF(WebElement we, String name, String value);
	public EUITestStatus hasValuePF(WebElement we, By id, String value);
} // end public interface IUITestDriver
