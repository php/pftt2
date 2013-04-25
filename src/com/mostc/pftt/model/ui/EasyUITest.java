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

public abstract class EasyUITest extends UITest implements IUITestDriver {
	protected IUITestDriver driver;
	
	public abstract EUITestStatus test() throws Exception;
	public abstract boolean start() throws Exception;

	public final boolean start(IUITestDriver driver) throws Exception {
		this.driver = driver;
		return start();
	}
	
	public final EUITestStatus test(IUITestDriver driver) throws Exception {
		this.driver = driver;
		return test();
	}
	
	@Override
	public String getWebBrowserNameAndVersion() {
		return driver.getWebBrowserNameAndVersion();
	}
	@Override
	public Point getLastElementLocationOnPage() {
		return driver.getLastElementLocationOnPage();
	}
	@Override
	public WebDriverWait driverWait() {
		return driver.driverWait();
	}
	@Override
	public WebDriver driver() {
		return driver.driver();
	}
	@Override
	public WebElement findElement(By by) {
		return driver.findElement(by);
	}
	@Override
	public WebElement getElementNow(By by) {
		return driver.getElementNow(by);
	}
	@Override
	public WebElement getElement(By by) {
		return driver.getElement(by);
	}
	@Override
	public boolean inputTypeName(String name, String value) {
		return driver.inputTypeName(name, value);
	}
	@Override
	public boolean inputTypeId(String id, String value) {
		return driver.inputTypeId(id, value);
	}
	@Override
	public boolean inputType(By by, String value) {
		return driver.inputType(by, value);
	}
	@Override
	public boolean selectByValueName(String name, String value) {
		return driver.selectByValueName(name, value);
	}
	@Override
	public boolean selectByValueId(String id, String value) {
		return driver.selectByValueId(id, value);
	}
	@Override
	public boolean selectByValue(By by, String value) {
		return driver.selectByValue(by, value);
	}
	@Override
	public boolean selectByText(By by, String text) {
		return driver.selectByText(by, text);
	}
	@Override
	public boolean selectByTextName(String name, String text) {
		return driver.selectByTextName(name, text);
	}
	@Override
	public boolean selectByTextId(String id, String text) {
		return driver.selectByTextId(id, text);
	}
	@Override
	public boolean clickLinkText(String text) {
		return driver.clickLinkText(text);
	}
	@Override
	public boolean clickPartialLinkText(String partial_text) {
		return driver.clickPartialLinkText(partial_text);
	}
	@Override
	public boolean clickId(String id) {
		return driver.clickId(id);
	}
	@Override
	public boolean click(By by) {
		return driver.click(by);
	}
	@Override
	@Nonnull
	public String getId(WebElement we) {
		return driver.getId(we);
	}
	@Override
	@Nonnull
	public String getValue(WebElement we) {
		return driver.getValue(we);
	}
	@Override
	@Nonnull
	public String getHref(WebElement we) {
		return driver.getHref(we);
	}
	@Override
	public boolean mouseOverLinkText(String text) {
		return driver.mouseOverLinkText(text);
	}
	@Override
	public boolean mouseOverPartialLinkText(String partial_text) {
		return driver.mouseOverPartialLinkText(partial_text);
	}
	@Override
	public boolean mouseOverId(String id) {
		return driver.mouseOverId(id);
	}
	@Override
	public boolean mouseOver(WebElement we) {
		return driver.mouseOver(we);
	}
	@Override
	public boolean focus(WebElement we) {
		return driver.focus(we);
	}
	@Override
	public Actions createActions() {
		return driver.createActions();
	}
	@Override
	public boolean hasElementId(String id) {
		return driver.hasElementId(id);
	}
	@Override
	public boolean hasElement(By by) {
		return driver.hasElement(by);
	}
	@Override
	public boolean hasElementNowId(String id) {
		return driver.hasElementNowId(id);
	}
	@Override
	public boolean hasElementNow(By by) {
		return driver.hasElementNow(by);
	}
	@Override
	public EUITestStatus hasElementIdPF(String id) {
		return driver.hasElementIdPF(id);
	}
	@Override
	public EUITestStatus hasElementPF(By by) {
		return driver.hasElementPF(by);
	}
	@Override
	public void get(String url) {
		driver.get(url);
	}
	@Override
	public WebElement getElementNow(WebElement parent, By by) {
		return driver.getElementNow(parent, by);
	}
	@Override
	public WebElement getElement(WebElement parent, By by) {
		return driver.getElement(parent, by);
	}
	@Override
	public boolean hasElementNowId(WebElement we, String id) {
		return driver.hasElementNowId(we, id);
	}
	@Override
	public boolean hasElementNow(WebElement we, By by) {
		return driver.hasElementNow(we, by);
	}
	@Override
	public boolean hasElement(WebElement we, By by) {
		return driver.hasElement(we, by);
	}
	@Override
	public boolean clickLinkText(WebElement parent, String text) {
		return driver.clickLinkText(parent, text);
	}
	@Override
	public boolean clickPartialLinkText(WebElement parent, String partial_text) {
		return driver.clickPartialLinkText(parent, partial_text);
	}
	@Override
	public boolean click(WebElement parent, By by) {
		return driver.click(parent, by);
	}
	@Override
	public boolean selectByText(WebElement parent, By by, String text) {
		return driver.selectByText(parent, by, text);
	}
	@Override
	public boolean selectByTextId(WebElement parent, String id, String text) {
		return driver.selectByTextId(parent, id, text);
	}
	@Override
	public boolean selectByValueName(WebElement parent, String name, String value) {
		return driver.selectByValueName(parent, name, value);
	}
	@Override
	public boolean selectByValueId(WebElement parent, String id, String value) {
		return driver.selectByValueId(parent, id, value);
	}
	@Override
	public boolean selectByValue(WebElement parent, By by, String value) {
		return driver.selectByValue(parent, by, value);
	}
	@Override
	public boolean inputTypeName(WebElement parent, String name, String value) {
		return driver.inputTypeName(parent, name, value);
	}
	@Override
	public boolean inputTypeId(WebElement parent, String id, String value) {
		return driver.inputTypeId(parent, id, value);
	}
	@Override
	public boolean inputType(WebElement parent, By by, String value) {
		return driver.inputType(parent, by, value);
	}
	@Override
	public boolean mouseOverLinkText(WebElement parent, String text) {
		return driver.mouseOverLinkText(parent, text);
	}
	@Override
	public boolean mouseOverPartialLinkText(WebElement parent, String partial_text) {
		return driver.mouseOverPartialLinkText(parent, partial_text);
	}
	@Override
	public boolean mouseOverId(WebElement parent, String id) {
		return driver.mouseOverId(parent, id);
	}
	@Override
	public boolean mouseOver(WebElement parent, By by) {
		return driver.mouseOver(parent, by);
	}
	@Override
	public boolean hasText(String text) {
		return driver.hasText(text);
	}
	@Override
	public boolean hasTextAll(String... text) {
		return driver.hasTextAll(text);
	}
	@Override
	public EUITestStatus hasTextAllPF(String... text) {
		return driver.hasTextAllPF(text);
	}
	@Override
	public EUITestStatus hasNotTextAllPF(String... text) {
		return driver.hasNotTextAllPF(text);
	}
	@Override
	public EUITestStatus pf(boolean true_if_pass) {
		return driver.pf(true_if_pass);
	}
	@Override
	public boolean hasText(WebElement we, String text) {
		return driver.hasText(we, text);
	}
	@Override
	public boolean hasText(WebElement we, String... text) {
		return driver.hasText(we, text);
	}
	@Override
	public EUITestStatus hasTextPF(WebElement we, String... text) {
		return driver.hasTextPF(we, text);
	}
	@Override
	public boolean fileBrowse(By by, String file_ext, String content) {
		return driver.fileBrowse(by, file_ext, content);
	}
	@Override
	public boolean fileBrowse(By by, String file_ext, byte[] content) {
		return driver.fileBrowse(by, file_ext, content);
	}
	@Override
	public boolean hasValue(By id, String value) {
		return driver.hasValue(id, value);
	}
	@Override
	public EUITestStatus hasValuePF(By id, String value) {
		return driver.hasValuePF(id, value);
	}
	@Override
	public boolean hasValue(WebElement we, By id, String value) {
		return driver.hasValue(we, id, value);
	}
	@Override
	public EUITestStatus hasValuePF(WebElement we, By id, String value) {
		return driver.hasValuePF(we, id, value);
	}
	@Override
	public boolean hasTextAny1(String... text) {
		return driver.hasTextAny1(text);
	}
	@Override
	public EUITestStatus hasTextAny1PF(String... text) {
		return driver.hasTextAny1PF(text);
	}
	@Override
	public EUITestStatus hasNotTextAny1PF(String... text) {
		return driver.hasNotTextAny1PF(text);
	}
	@Override
	public boolean hasValueName(String name, String value) {
		return driver.hasValueName(name, value);
	}
	@Override
	public boolean hasValueId(String id, String value) {
		return driver.hasValueId(id, value);
	}
	@Override
	public EUITestStatus hasValueNamePF(String name, String value) {
		return driver.hasValueNamePF(name, value);
	}
	@Override
	public EUITestStatus hasValueIdPF(String name, String value) {
		return driver.hasValueIdPF(name, value);
	}
	@Override
	public boolean hasValueName(WebElement we, String name, String value) {
		return driver.hasValueName(we, name, value);
	}
	@Override
	public boolean hasValueId(WebElement we, String id, String value) {
		return driver.hasValueId(we, id, value);
	}
	@Override
	public EUITestStatus hasValueNamePF(WebElement we, String name, String value) {
		return driver.hasValueNamePF(we, name, value);
	}
	@Override
	public EUITestStatus hasValueIdPF(WebElement we, String name, String value) {
		return driver.hasValueIdPF(we, name, value);
	}
	@Override
	public boolean mouseOver(By by) {
		return driver.mouseOver(by);
	}
	@Override
	public boolean focusLinkText(String text) {
		return driver.focusLinkText(text);
	}
	@Override
	public boolean focusPartialLinkText(String partial_text) {
		return driver.focusPartialLinkText(partial_text);
	}
	@Override
	public boolean focusId(String id) {
		return driver.focusId(id);
	}
	@Override
	public boolean focus(By by) {
		return driver.focus(by);
	}
	@Override
	public boolean focusLinkText(WebElement parent, String text) {
		return driver.focusLinkText(parent, text);
	}
	@Override
	public boolean focusPartialLinkText(WebElement parent, String partial_text) {
		return driver.focusPartialLinkText(parent, partial_text);
	}
	@Override
	public boolean focusId(WebElement parent, String id) {
		return driver.focusId(parent, id);
	}
	@Override
	public boolean focus(WebElement parent, By by) {
		return driver.focus(parent, by);
	}
	@Override
	public boolean hasNotTextAll(String... text) {
		return driver.hasNotTextAll(text);
	}
	@Override
	public boolean hasNotTextAny1(String... text) {
		return driver.hasNotTextAny1(text);
	}
	@Override
	public boolean hasNotTextAll(WebElement we, String... text) {
		return driver.hasNotTextAll(we, text);
	}
	@Override
	public EUITestStatus hasNotTextAllPF(WebElement we, String... text) {
		return driver.hasNotTextAllPF(we, text);
	}
	@Override
	public boolean hasTextAny1(WebElement we, String... text) {
		return driver.hasTextAny1(we, text);
	}
	@Override
	public boolean hasNotTextAny1(WebElement we, String... text) {
		return driver.hasNotTextAny1(we, text);
	}
	@Override
	public EUITestStatus hasTextAny1PF(WebElement we, String... text) {
		return driver.hasTextAny1PF(we, text);
	}
	@Override
	public EUITestStatus hasNotTextAny1PF(WebElement we, String... text) {
		return driver.hasNotTextAny1PF(we, text);
	}
	@Override
	public boolean clickName(String name) {
		return driver.clickName(name);
	}
	@Override
	public boolean click(WebElement e) {
		return driver.click(e);
	}
	@Override
	public boolean isSelected(By by) {
		return driver.isSelected(by);
	}
	@Override
	public boolean isSelected(WebElement e) {
		return driver.isSelected(e);
	}
	@Override
	public boolean isSelectedName(String name) {
		return driver.isSelectedName(name);
	}
	@Override
	public boolean isSelectedId(String id) {
		return driver.isSelectedId(id);
	}
	@Override
	public boolean isChecked(By by) {
		return driver.isChecked(by);
	}
	@Override
	public boolean isChecked(WebElement e) {
		return driver.isChecked(e);
	}
	@Override
	public boolean isCheckedName(String name) {
		return driver.isCheckedName(name);
	}
	@Override
	public boolean isCheckedId(String id) {
		return driver.isCheckedId(id);
	}
	@Override
	public String randomSentence(int word_count) {
		return driver.randomSentence(word_count);
	}
	@Override
	public String randomSentence(int min_word_count, int max_word_count) {
		return driver.randomSentence(min_word_count, max_word_count);
	}
	@Override
	public String randomSentence(int min_word_count, int max_word_count, int max_char_len) {
		return driver.randomSentence(min_word_count, max_word_count, max_char_len);
	}
	
} // end public abstract class EasyUITest
