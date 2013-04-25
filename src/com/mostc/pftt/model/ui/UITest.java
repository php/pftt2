package com.mostc.pftt.model.ui;


import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;

import com.github.mattficken.Overridable;
import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.model.ui.UIAccount.IUserType;

/** 
 * 
 * @author Matt Ficken
 *
 */

public abstract class UITest {
	protected UIAccount user_account;
	
	public abstract EUITestStatus test(IUITestDriver driver) throws Exception;
	public abstract boolean start(IUITestDriver driver) throws Exception;
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof UITest) {
			return getClass().isAssignableFrom(o.getClass());
		} else {
			return false;
		}
	}
	
	@Overridable
	public String getComment() {
		return null;
	}
	
	public boolean isAnon() {
		return UIAccount.isAnon(user_account);
	}
	
	public static EUITestStatus testChild(UITest child, IUITestDriver driver) throws Exception {
		EUITestStatus status = child.test(driver);
		if (status==null)
			return EUITestStatus.NOT_IMPLEMENTED;
		else
			return status;
	}
	
	public static boolean testChildPass(UITest child, IUITestDriver driver) throws Exception {
		switch(testChild(child, driver)) {
		case PASS:
		case PASS_WITH_WARNING:
			return true;
		default:
			return false;
		}
	}
	
	public String createUniqueTestName(UIAccount account) {
		return ( account == null ? "Anon" : StringUtil.toTitle(account.username) ) + "-" +getName();
	}
	
	public static String createUniqueTestName(IUserType user_type, String name) {
		return user_type==null?name:user_type.getDefaultUserName()+"-"+name;
	}
	
	@Overridable
	public String getName() {
		String name = getClass().getSimpleName();
		if (name.endsWith("Test"))
			name = name.substring(0, name.length()-"Test".length());
		return StringUtil.join(StringUtil.splitOnUpperCase(name), "-");
	}
	
	/** scales screenshot image to a smaller size and optionally crops the image.
	 * 
	 * tests can override this to customize how screenshots are scaled/cropped.
	 * 
	 * we won't be interested in most of the screenshot image, so this saves storage space (result-pack size) and makes it more visible in reports (where the image would
	 * have to be scaled down anyway to fit on the page).
	 * 
	 * @param input_image
	 * @param last_element_location_on_page @Nullable - last element test case was working with (if available). can crop to/focus on this element
	 * @return
	 */
	@Overridable
	public Image scaleScreenshotImage(BufferedImage input_image, @Nullable Point last_element_location_on_page, Dimension screen_size) {
		int iw = input_image.getWidth();
		int ih = input_image.getHeight();
		
		if ( screen_size.height * 2 < ih ) {
			// if too tall:
			//  crop to screen size
			ih = screen_size.height * 2;
			input_image = input_image.getSubimage(
					0,
					0,
					iw,
					ih
				);
		} 
		if (last_element_location_on_page!=null) {
			int lx = last_element_location_on_page.x;
			int ly = last_element_location_on_page.y;
			
			if (iw-lx>0 && ih-ly>0) {
				// crop image to last element test worked with (FOCUS)
				input_image = input_image.getSubimage(
						lx,
						ly,
						iw - lx,
						ih - ly
					);
				iw = input_image.getWidth();
				ih = input_image.getHeight();
			}
		}
		
		// rescale maintaining aspect ratio
		final float aspect_ratio = ((float)iw)/((float)ih);
		int sw = Math.min(1280, iw);
		int sh = Math.min(1024, (int)( aspect_ratio * ((float)iw)));
		if ( sw * aspect_ratio > sh ) {
			sw = (int)( ((float)sh) * aspect_ratio );
			if (sw>iw) {
				sw = iw;
				sh = (int)(((float)sw)*aspect_ratio);
			}	
		} else if ( sh * aspect_ratio > sw ) {
			sh = (int)( ((float)sw) * aspect_ratio );
			if (sh>ih) {
				sh = ih;
				sw = (int)(((float)sh)*aspect_ratio);
			}
		}
		
		return input_image.getSubimage(0, 0, Math.min(sw, iw), Math.min(sh, ih));
	} // end public Image scaleScreenshotImage
	
	public byte[] getScaledScreenshotPNG(byte[] screenshot_png, Point last_element_location_on_page, Dimension screen_size) throws IOException {
		if (screenshot_png.length < 32 * 1024)
			return screenshot_png;
		
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(screenshot_png));
		
		Image scaled_img = scaleScreenshotImage(image, last_element_location_on_page, screen_size);
		
		ByteArrayOutputStream png_out = new ByteArrayOutputStream(screenshot_png.length);
		
		ImageIO.write(ensureRenderedImage(scaled_img), "PNG", png_out);
		
		return png_out.toByteArray();
	}
	
	protected static RenderedImage ensureRenderedImage(Image img) {
		if (img instanceof RenderedImage)
			return (RenderedImage) img;
		BufferedImage rimg = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_3BYTE_BGR);
		Graphics g = rimg.getGraphics();
		g.drawImage(img, 0, 0, null);
		g.dispose();
		return rimg;
	}
	
} // end public abstract class UITest
