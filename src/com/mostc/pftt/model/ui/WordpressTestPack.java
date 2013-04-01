package com.mostc.pftt.model.ui;

import java.util.Calendar;
import java.util.Locale;

import org.openqa.selenium.By;

import com.github.mattficken.io.StringUtil;

// PFTT-Wordpress-2013-03-19-15-14
public class WordpressTestPack implements UITestPack {
	
	@Override
	public String getNameAndVersionInfo() {
		return "PFTT-Wordpress";
	}
	@Override
	public boolean isDevelopment() {
		return true;
	}
	@Override
	public String getBaseURL() {
		return "/wordpress/";
	}
	
	public void run(IUITestBranch anon_branch) {
		// TODO make sure it tests moderator approving the comment
		UIAccount admin_user = new UIAccount("admin", "password01!");
		//
		UIAccount admin_user_wrong_passwd = new UIAccount("admin", "wrong_password");
		//
		UIAccount subscriber_user = new UIAccount("user", "password01!");
		// new password for `subscriber_user` 
		UIAccount subscriber_user_new_passwd = new UIAccount("user", "wrong_password");
		
		/////////////
		
		/*anon_branch.test(
				// test widgets that are there by default (before adding/removing widgets)
				new WidgetRecentComments(),
				new WidgetRecentPosts(),
				new WidgetArchives(),
				new WidgetCategories()
			);
		anon_branch.test("Comment without being Logged In", new CommentAdd());*/
		anon_branch.test(admin_user, new LoginTest(), new LogoutTest())
			.test(
				/*new AppearanceThemesActivate(),
				new AppearanceWidgetAddArchives(),
				new AppearanceWidgetAddCalendar(),
				new AppearanceWidgetAddCategories(),
				new AppearanceWidgetAddCustomMenu(),
				new AppearanceWidgetAddLinks(),
				new AppearanceWidgetAddMeta(),
				new AppearanceWidgetAddPages(),
				new AppearanceWidgetAddRecentComments(),
				new AppearanceWidgetAddRecentPosts(),
				new AppearanceWidgetAddRSS(),
				new AppearanceWidgetAddSearch(),
				new AppearanceWidgetAddTagCloud(),
				new AppearanceWidgetAddText(),
				new AppearanceWidgetAddTwentyElevenEphemera(),
				new AppearanceMenusActivate(),
				new AppearanceHeaderSet(),
				new AppearanceBackgroundSet(),
				new LinkAddNew(),
				new LinkAll(),
				new LinkDelete(),
				new LinkCategories(),
				new LinkCategoryAdd(),
				new LinkCategoryDelete(),
				new MediaAddNew(),
				new MediaLibrary(),
				new MediaDelete(),*/
				new PagesAddNew(),
				new PagesAllPages(),
				new PagesTrash(),
				new PagesEdit(),
				new PagesQuickEdit(),
				new PostsAll(),
				new PostQuickEdit(),
				new PostEdit(),
				new PostTrash(),
				new PostCategoriesAll(),
				new PostCategoriesAdd(),
				new PostCategoriesDelete(),
				new PostTagsAll(),
				new PostTagsAdd(),
				new PostTagsDelete(),
				new PluginsInstalledPlugins(),
				// test activating and deactivating plugins
				new PluginsInstalledActivate(),
				// akismet is an important plugin (any real wordpress instance needs it activated)
				new PluginsAkismetConfiguration(), // depends on PluginsInstalledActivate
				new PluginsInstalledDeactivate(),
				/*new PostsAddNew(),
				new CommentsAllComments(),
				new CommentsTrash(),
				new CommentsApprove(),
				new CommentsUnapprove()*/
				// change settings, especially timezone and date/time format
				/*new SettingsGeneralChangeTimezone(),
				new SettingsChangeDateTimeFormat(),
				new SettingsWriting(),
				new SettingsReading(),
				new SettingsDiscussion(),
				new SettingsMedia(),
				new SettingsPrivacy(),
				new SettingsPermalinks(),*/
				new ToolsExport(),
				new ToolsImport(),
				// TODO new EditProfile(),
				new UsersAddNew(),
				new UsersAllUsers(),
				new UsersDeleteNew(),
				new UsersChangeRole(),
				new UsersUpdateUser()
			);
		// TODO test all user roles - for some roles, retest posting and approving comments
		testUserRole(anon_branch, subscriber_user, subscriber_user_new_passwd);
		// test admin login with wrong password
		if (!admin_user.username.equalsIgnoreCase(admin_user_wrong_passwd.username)||admin_user.password.equals(admin_user_wrong_passwd.password)) {
			anon_branch.testException("Admin-Login-2", "Don't have the wrong password to try to login as admin");
		} else {
			anon_branch.testXFail(admin_user_wrong_passwd, "Must not be able to login as admin with wrong password", new LoginTest(), new LogoutTest());
		}
		anon_branch.test(
				new WidgetArchives(),
				new WidgetCalendar(),
				new WidgetCategories(),
				new WidgetMeta(),
				new WidgetPages(),
				new WidgetRecentComments(),
				new WidgetRecentPosts(),
				new WidgetCommentsRSS(),
				new WidgetEntriesRSS(),
				new WidgetSearch(),
				new WidgetTagCloud(),
				new WidgetText()
			);
		anon_branch.test(admin_user, "Delete extra widgets (cleanup)", new LoginTest(), new LogoutTest())
			.test(
				new AppearanceWidgetDeleteCalendar(),
				new AppearanceWidgetDeleteCustomMenu(),
				new AppearanceWidgetDeleteLinks(),
				new AppearanceWidgetDeleteMeta(),
				new AppearanceWidgetDeletePages(),
				new AppearanceWidgetDeleteSearch(),
				new AppearanceWidgetDeleteTagCloud(),
				new AppearanceWidgetDeleteText()
					);
		anon_branch.test("Ensure still visible after extra widgets deleted",
				new CommentAdd(),
				new PostView(),
				new WidgetRecentComments(),
				new WidgetRecentPosts(),
				new WidgetArchives(),
				new WidgetCategories()
			);
	} // end void 
	
	void testUserRole(IUITestBranch anon_branch, UIAccount user, UIAccount user_new_passwd) {
		anon_branch.test(user, new LoginTest(), new LogoutTest())
			.test(
				new PostsAddNew(),
				new PostView(),
				new CommentAdd()
			);
		// try to change password 
		if (!user.username.equalsIgnoreCase(user_new_passwd.username)) {
			anon_branch.testException("Subscriber-Login-2", "Try to change password with wrong username");
		} else if (user.password.equals(user_new_passwd.password)) {
			anon_branch.testException("Subscriber-Login-2", "Try to change password to current password");
		} else {
			anon_branch.test(user, new LoginTest(), new LogoutTest())
				.test(new ChangePassword(user_new_passwd))
					.test(new LogoutTest()) // logout first, to test logging back in (which is the test that should fail)
					.testXFail(user, "Must not be able to login with old password after password changed", new LoginTest(), new LogoutTest());
		}
		anon_branch.test(new LostPassword());
	} // end void
	
	abstract class WPTest extends UITest {
		@Override
		public boolean start(SimpleDriver driver) throws Exception {
			driver.get("/");
			return true;
		}
	}
	abstract class LoggedInTest extends WPTest {
		@Override
		public boolean start(SimpleDriver driver) throws Exception {
			if (true) {// TODO driver.hasElementNow(By.cssSelector("a[title=\"Password Lost and Found\"]"))) {
				// NOT LOGGED IN ANY MORE, need to login again
				// return testChildPass(new LoginTest(), driver);
				driver.inputType("user_login", user_account.username);
				driver.inputType("user_pass", user_account.password);
				driver.clickId("wp-submit");
				
			}// else {
				return true;
			//}
		}
	}
	abstract class AdminTest extends LoggedInTest {
		@Override
		public boolean start(SimpleDriver driver) throws Exception {
			driver.get("/wp-admin/index.php");
			return super.start(driver);
		}
	}
	
	class LoginTest extends WPTest {
		@Override
		public EUITestStatus test(final SimpleDriver driver) throws Exception {
			// go to url, `log in` hyperlink might not always be there (depends on widgets and themes)
			driver.get("/wp-login.php");
			driver.inputType("user_login", user_account.username);
			driver.inputType("user_pass", user_account.password);
			driver.clickId("wp-submit");
			return !driver.hasText("incorrect") && driver.hasElement(By.cssSelector("#wp-admin-bar-logout > a.ab-item")) ? EUITestStatus.PASS : EUITestStatus.FAIL;
		}
	} // end class LoginTest
	class LogoutTest extends WPTest {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			if (driver.hasElement(By.partialLinkText("Log in"))) 
				return EUITestStatus.PASS;
			driver.click(By.cssSelector("#wp-admin-bar-logout > a.ab-item"));
			driver.click(By.partialLinkText("Back"));
			return driver.hasElementPF(By.partialLinkText("Log in"));
		}
	}
	class AppearanceThemesActivate extends AppearanceTest {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.clickLinkText("Themes");
			driver.clickLinkText("Activate");
			return EUITestStatus.PASS;
		}
	}
	abstract class AppearanceTest extends AdminTest {
		@Override
		public boolean start(SimpleDriver driver) throws Exception {
			return super.start(driver) && driver.clickLinkText("Appearance");
		}
		protected boolean verifyChangesSaved(SimpleDriver driver) {
			return driver.hasText("Changes saved");
		}
		protected EUITestStatus verifyChangesSavedPF(SimpleDriver driver) {
			return verifyChangesSaved(driver) ? EUITestStatus.PASS : EUITestStatus.FAIL;
		}
	}
	abstract class AppearanceWidget extends AppearanceTest {
		@Override
		public boolean start(SimpleDriver driver) throws Exception {
			return super.start(driver) && driver.clickLinkText("Widgets");
		}
	}
	class AppearanceWidgetDeleteCalendar extends AppearanceWidget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			// TODO don't be dependent on the order of the widgets on the dashboard page
			driver.click(By.cssSelector("#widget-33_calendar-2 > div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.edit"));
			driver.click(By.id("removewidget"));
			return verifyChangesSavedPF(driver);
		}
	}
	class AppearanceWidgetDeleteCustomMenu extends AppearanceWidget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(By.cssSelector("#widget-23_nav_menu-2 > div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.edit"));
			driver.click(By.id("removewidget"));
			return verifyChangesSavedPF(driver);
		}
	}
	class AppearanceWidgetDeleteLinks extends AppearanceWidget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(By.cssSelector("#widget-23_links-2 > div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.edit"));
			driver.click(By.id("removewidget"));
			return verifyChangesSavedPF(driver);
		}
	}
	class AppearanceWidgetDeleteMeta extends AppearanceWidget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(By.cssSelector("#widget-20_meta-2 > div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.edit"));
			driver.click(By.id("removewidget"));
			return verifyChangesSavedPF(driver);
		}
	}
	class AppearanceWidgetDeletePages extends AppearanceWidget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(By.cssSelector("#widget-23_pages-2 > div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.edit"));
			driver.click(By.id("removewidget"));
			return verifyChangesSavedPF(driver);
		}
	}
	class AppearanceWidgetDeleteSearch extends AppearanceWidget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(By.cssSelector("#widget-26_search-3 > div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.edit"));
			driver.click(By.id("removewidget"));
			return verifyChangesSavedPF(driver);
		}
	}
	class AppearanceWidgetDeleteTagCloud extends AppearanceWidget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(By.cssSelector("#widget-26_tag_cloud-3 > div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.edit"));
			driver.click(By.id("removewidget"));
			return verifyChangesSavedPF(driver);
		}
	}
	class AppearanceWidgetDeleteText extends AppearanceWidget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(By.cssSelector("#widget-26_text-2 > div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.edit"));
			driver.click(By.id("removewidget"));
			return verifyChangesSavedPF(driver);
		}
	}
	class AppearanceWidgetAddArchives extends AppearanceWidget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			// each widget is given an auto-generated id that depends on its order (same css class) ... can only select by id
			// ex: #widget-3_archives-__i__
			driver.click(driver.getElement(By2.partialId("widget", "archives")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.add"));
			driver.click(By.id("savewidget"));
			return verifyChangesSavedPF(driver);
		}
	}
	class AppearanceWidgetAddCalendar extends AppearanceWidget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(driver.getElement(By2.partialId("widget", "calendar")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.add"));
			driver.click(By.id("savewidget"));
			return verifyChangesSavedPF(driver);
		}
	}
	class AppearanceWidgetAddCategories extends AppearanceWidget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(driver.getElement(By2.partialId("widget", "categories")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.add"));
			driver.click(By.id("savewidget"));
			return verifyChangesSavedPF(driver);
		}
	}
	class AppearanceWidgetAddCustomMenu extends AppearanceWidget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(driver.getElement(By2.partialId("widget", "menu")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.add"));
			driver.click(By.id("savewidget"));
			return verifyChangesSavedPF(driver);
		}
	}
	class AppearanceWidgetAddLinks extends AppearanceWidget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(driver.getElement(By2.partialId("widget", "links")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.add"));
			driver.click(By.id("savewidget"));
			return verifyChangesSavedPF(driver);
		}
	}
	class AppearanceWidgetAddMeta extends AppearanceWidget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(driver.getElement(By2.partialId("widget", "meta")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.add"));
			driver.click(By.id("savewidget"));
			return verifyChangesSavedPF(driver);
		}
	}
	class AppearanceWidgetAddPages extends AppearanceWidget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(driver.getElement(By2.partialId("widget", "pages")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.add"));
			driver.click(By.id("savewidget"));
			return verifyChangesSavedPF(driver);
		}
	}
	class AppearanceWidgetAddRecentComments extends AppearanceWidget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(driver.getElement(By2.partialId("widget", "recent-comments")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.add"));
			driver.click(By.id("savewidget"));
			return verifyChangesSavedPF(driver);
		}
	}
	class AppearanceWidgetAddRecentPosts extends AppearanceWidget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(driver.getElement(By2.partialId("widget", "recent-posts")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.add"));
			driver.click(By.id("savewidget"));
			return verifyChangesSavedPF(driver);
		}
	}
	class AppearanceWidgetAddRSS extends AppearanceWidget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(driver.getElement(By2.partialId("widget", "rss")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.add"));
			driver.click(By.id("savewidget"));
			return verifyChangesSavedPF(driver);
		}
	}
	class AppearanceWidgetAddSearch extends AppearanceWidget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(driver.getElement(By2.partialId("widget", "search")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.add"));
			driver.click(By.id("savewidget"));
			return verifyChangesSavedPF(driver);
		}
	}
	class AppearanceWidgetAddTagCloud extends AppearanceWidget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			/*driver.click(driver.getElement(By2.partialId("widget", "tag_cloud")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.add"));
			driver.click(By.id("savewidget"));
			return verifyChangesSavedPF(driver);*/
			return null; // TODO
		}
	}
	class AppearanceWidgetAddText extends AppearanceWidget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			/*driver.click(driver.getElement(By2.partialId("widget", "text")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.add"));
			driver.click(By.id("savewidget"));
			return verifyChangesSavedPF(driver);*/
			return null; // TODO
		}
	}
	class AppearanceWidgetAddTwentyElevenEphemera extends WPTest {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class AppearanceMenusActivate extends AppearanceTest {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(By.partialLinkText("Menus"));
			driver.click(By.cssSelector("abbr[title=\"Add menu\"]"));
			driver.inputType(By.id("menu-name"), "Custom Menu");
			driver.click(By.id("save_menu_header"));
			driver.click(By.id("save_menu_header"));
			return verifyChangesSavedPF(driver);
		}
	}
	class AppearanceHeaderSet extends AppearanceTest {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(By.partialLinkText("Header"));
			driver.click(By.cssSelector("div.default-header > label > input[name=\"default-header\"]"));
			driver.click(By.id("save-header-options"));
			return verifyChangesSavedPF(driver);
		}
	}
	class AppearanceBackgroundSet extends AppearanceTest {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(By.partialLinkText("Background"));
			driver.click(By.id("pickcolor"));
			driver.click(By.id("save-background-options"));
			return verifyChangesSavedPF(driver);
		}
	}
	abstract class Link extends AdminTest {
		@Override
		public boolean start(SimpleDriver driver) throws Exception {
			return super.start(driver) && driver.clickLinkText("Links");
		}
	}
	class LinkAll extends Link {

		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	class LinkAddNew extends Link {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.clickLinkText("Add New");
			driver.inputType(By.id("link_name"), "Nifty blogging software");
			driver.inputType(By.id("link_url"), "http://windows.php.net/");
			driver.inputType(By.id("link_description"), "show when someone hovers over link");
			driver.click(By.id("in-link-category-2"));
			driver.click(By.id("link_target_top"));
			driver.click(By.id("co-worker"));
			driver.click(By.id("co-resident"));
			driver.click(By.id("child"));
			driver.click(By.id("friend"));
			driver.click(By.id("publish"));
			return EUITestStatus.PASS;
		}
	}
	class LinkDelete extends Link {

		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	class LinkCategories extends Link {

		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	class LinkCategoryAdd extends Link {

		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	class LinkCategoryDelete extends Link {

		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	class MainPage extends WPTest {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			return EUITestStatus.PASS;
		}
	}
	class MediaAddNew extends AdminTest {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class MediaLibrary extends AdminTest {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class MediaDelete extends AdminTest {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	abstract class Pages extends AdminTest {
		@Override
		public boolean start(SimpleDriver driver) throws Exception {
			return super.start(driver) && driver.clickLinkText("Pages");
		}
	}
	class PagesAddNew extends Pages {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(By.partialLinkText("Add New"));
			driver.inputType(By.id("title"), "title");
			driver.click(By.id("publish"));
			return EUITestStatus.PASS;
		}
	}
	class PagesAllPages extends Pages {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			return EUITestStatus.PASS;
		}
	}
	class PagesTrash extends Pages {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(By.cssSelector("input[name=\"post[]\"]"));
			driver.selectByText(By.cssSelector("select[name=\"action\"]"), "Move to Trash");
			driver.click(By.id("doaction"));
			return EUITestStatus.PASS;
		}
	}
	class PagesEdit extends Pages {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.clickLinkText("Edit");
			driver.clickId("publish");
			return driver.hasTextPF("updated");
		}
	}
	class PagesQuickEdit extends Pages {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.clickLinkText("Quick Edit");
			driver.clickId("publish");
			return driver.hasTextPF("updated");
		}
	}
	abstract class Posts extends AdminTest { 
		@Override
		public boolean start(SimpleDriver driver) throws Exception {
			return super.start(driver) && driver.clickLinkText("Posts");
		}
	}
	class PostsAll extends Posts {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			return driver.hasTextPF("Posts", "All");
		}
	}
	class PostTrash extends Posts {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(By.partialLinkText("Trash"));
			return EUITestStatus.PASS;
		}
	}
	class PostQuickEdit extends Posts {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.clickLinkText("Quick Edit");
			driver.clickId("publish");
			return driver.hasTextPF("Post updated");
		}
	}
	class PostEdit extends Posts {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.clickLinkText("Edit");
			driver.clickId("publish");
			return driver.hasTextPF("Post updated");
		}
	}
	abstract class PostCategories extends Posts {
		@Override
		public boolean start(SimpleDriver driver) throws Exception {
			return super.start(driver) && driver.click(By.partialLinkText("Categories"));
		}
	}
	abstract class PostTags extends Posts {
		@Override
		public boolean start(SimpleDriver driver) throws Exception {
			return super.start(driver) && driver.click(By.partialLinkText("Tags"));
		}
	}
	class PostCategoriesAll extends PostCategories {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			return driver.hasTextPF("Categories", "All");
		}
	}
	class PostCategoriesAdd extends PostCategories {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.inputType(By.id("tag-name"), "New Category2"); // TODO
			driver.inputType(By.id("tag-slug"), "new_category2"); // TODO
			driver.inputType(By.id("tag-description"), "some themes may show this");
			driver.click(By.id("submit"));
			return driver.hasTextPF("Added");
		}
	}
	class PostCategoriesDelete extends PostCategories {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(By.cssSelector("input[name=\"delete_tags[]\"]"));
			driver.selectByText(By.cssSelector("select[name=\"action\"]"), "Delete");
			driver.click(By.id("doaction"));
			return driver.hasTextPF("Deleted");
		}
	}
	class PostTagsAll extends PostTags {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			return driver.hasTextPF("Posts", "All");
		}
	}
	class PostTagsAdd extends PostTags {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.inputType(By.id("tag-name"), "NewTag");
			driver.inputType(By.id("tag-slug"), "NewTag");
			driver.inputType(By.id("tag-description"), "some themes may show it");
			driver.click(By.id("submit"));
			return driver.hasTextPF("Added");
		}
	}
	class PostTagsDelete extends PostTags {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(By.cssSelector("input[name=\"delete_tags[]\"]"));
			driver.selectByValue(By.cssSelector("select[name=\"action\"]"), "Delete");
			driver.click(By.id("doaction"));
			return driver.hasTextPF("Deleted");
		}
	}
	abstract class Plugins extends AdminTest {
		@Override
		public boolean start(SimpleDriver driver) throws Exception {
			return super.start(driver) && driver.click(By.partialLinkText("Plugins"));
		}
	}
	class PluginsInstalledPlugins extends Plugins {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			return EUITestStatus.PASS;
		}
	}
	class PluginsInstalledActivate extends AdminTest {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.clickLinkText("Installed Plugins");
			driver.clickId("checkbox_daab5d2d514cf7d293376be3ded708f0");
			driver.selectByText(By.name("action"), "Activate");
			driver.clickId("doaction");
			return driver.hasTextPF("Activated");
		}
	}
	class PluginsAkismetConfiguration extends Plugins {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(By.cssSelector("span.0 > a"));
			return EUITestStatus.PASS;
		}
	}
	class PluginsInstalledDeactivate extends AdminTest {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(By.cssSelector("a[title=\"Deactivate this plugin\"]"));
			return driver.hasTextPF("Deactivated");
		}
	}
	class PostView extends WPTest {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED; // TODO
		}
	}
	class PostsAddNew extends Posts {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(By.partialLinkText("Add New"));
			driver.inputType(By.id("title"), "enter title here");
			driver.click(By.id("save-post"));
			driver.click(By.id("publish"));
			return driver.hasText("updated") || driver.hasText("published") ? EUITestStatus.PASS : EUITestStatus.FAIL;
		}
	}
	class CommentAdd extends WPTest {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			final String text = StringUtil.randomLettersStr(20, 40);
			
			// open comment page:
			// find link titled: `Comment on <post title>`
			driver.driver().findElement(By.cssSelector("a[title=\"Comment on enter title here\"]")).click();

			if (isAnon()) {
				// fill in name and email fields
				driver.inputType(By.id("author"), "anonymous author");
				driver.inputType(By.id("email"), "a@a.com");
			}
			
			// enter comment text
			driver.inputType("comment", text);
			
			// submit comment form
			driver.click(By.id("submit"));
			
			// verify
			return driver.hasTextPF(text);
		}
	}
	abstract class Comments extends AdminTest {
		@Override
		public boolean start(SimpleDriver driver) throws Exception {
			return super.start(driver) && driver.click(By.partialLinkText("Comments"));
		}
	}
	class CommentsAllComments extends Comments {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			return driver.hasTextPF("Comments", "All");
		}
	}
	class CommentsTrash extends Comments {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			//driver.click(By.cssSelector("input[name=\"delete_comments[]\"]"));
			driver.selectByText(By.cssSelector("select[name=\"action\"]"), "Move to Trash");
			driver.click(By.id("doaction"));
			return driver.hasTextPF("comment moved to the Trash");
		}
	}
	class CommentsApprove extends Comments {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			//driver.click(By.xpath("//li[@id='menu-comments']/a"));
			//driver.click(By.cssSelector("input[name=\"delete_comments[]\"]"));
			driver.selectByText(By.cssSelector("select[name=\"action\"]"), "Approve");
			driver.click(By.id("doaction"));
			return driver.hasTextPF("Approved");
		}
	}
	class CommentsUnapprove extends Comments {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			//driver.click(By.xpath("//li[@id='menu-comments']/a"));
			//driver.click(By.cssSelector("input[name=\"delete_comments[]\"]"));
			driver.selectByText(By.cssSelector("select[name=\"action\"]"), "Unapprove");
			driver.click(By.id("doaction"));
			return driver.hasTextPF("Approve");
		}
	}
	abstract class Settings extends AdminTest {
		public boolean start(SimpleDriver driver) throws Exception {
			return super.start(driver) && driver.clickLinkText("Settings");
		}
	}
	class SettingsGeneralChangeTimezone extends Settings {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.clickLinkText("General");
			driver.selectByText(By.id("timezone_string"), "UTC-8");
			driver.clickId("submit");
			return EUITestStatus.PASS;
		}
	}
	class SettingsChangeDateTimeFormat extends Settings {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.clickLinkText("General");
			driver.click(By.cssSelector("label[title=\"Y/m/d\"] > input[name=\"date_format\"]"));
			driver.click(By.cssSelector("label[title=\"g:i A\"] > input[name=\"time_format\"]"));
			driver.click(By.id("submit"));
			return EUITestStatus.PASS;
		}
	}
	class SettingsWriting extends Settings {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(By.partialLinkText("Writing"));
			driver.inputType(By.id("default_post_edit_rows"), "30");
			driver.click(By.id("submit"));
			return EUITestStatus.PASS;
		}
	}
	class SettingsReading extends Settings {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(By.partialLinkText("Reading"));
			driver.inputType(By.id("posts_per_page"), "20");
			driver.click(By.id("submit"));
			return EUITestStatus.PASS;
		}
	}
	class SettingsDiscussion extends Settings {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(By.partialLinkText("Discussion"));
			driver.click(By.id("avatar_gravatar_default"));
			driver.click(By.id("submit"));
			return EUITestStatus.PASS;
		}
	}
	class SettingsMedia extends Settings {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(By.partialLinkText("Media"));
			driver.click(By.id("submit"));
			return EUITestStatus.PASS;
		}
	}
	class SettingsPrivacy extends Settings {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(By.partialLinkText("Privacy"));
			driver.click(By.id("blog-public"));
			driver.click(By.id("submit"));
			return EUITestStatus.PASS;
		}
	}
	class SettingsPermalinks extends Settings {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(By.partialLinkText("Permalinks"));
			driver.click(By.xpath("(//input[@name='selection'])[2]"));
			driver.click(By.id("submit"));
			return EUITestStatus.PASS;
		}
	}
	class ToolsExport extends AdminTest {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.clickLinkText("Tools");
			driver.click(By.partialLinkText("Export"));
			driver.clickId("submit");
			return EUITestStatus.PASS;
		}
	}
	class ToolsImport extends AdminTest {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class LostPassword extends WPTest {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class ChangePassword extends WPTest {
		final UIAccount new_account;
		public ChangePassword(UIAccount new_account) {
			this.new_account = new_account;
		}
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(By.cssSelector("#wp-admin-bar-edit-profile > a.ab-item"));
			driver.inputType(By.id("pass1"), new_account.password);
			driver.inputType(By.id("pass2"), new_account.password);
			driver.click(By.id("submit"));
			return EUITestStatus.PASS;
		}
	}
	class EditProfile extends WPTest {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(By.cssSelector("#wp-admin-bar-edit-profile > a.ab-item"));
			return EUITestStatus.PASS;
		}
	}
	abstract class Users extends AdminTest {
		@Override
		public boolean start(SimpleDriver driver) throws Exception {
			return super.start(driver) && driver.clickLinkText("Users");
		}
	}
	class UsersAddNew extends Users {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(By.xpath("(//a[contains(text(),'Add New')])[6]"));
			driver.inputType(By.id("user_login"), "Homer");
			driver.inputType(By.id("email"), "homer@compuglobalhypermega.net");
			driver.inputType(By.id("first_name"), "homer");
			driver.inputType(By.id("last_name"), "Simpson");
			driver.inputType(By.id("url"), "http://compuglobalhypermega.net");
			driver.inputType(By.id("pass1"), "password01!");
			driver.inputType(By.id("pass2"), "password01!"); // TODO
			driver.click(By.id("createusersub"));
			return EUITestStatus.PASS;
		}
	}
	class UsersAllUsers extends Users {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			return EUITestStatus.PASS;
		}
	}
	class UsersDeleteNew extends Users {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			return EUITestStatus.PASS;
		}
	}
	class UsersChangeRole extends Users {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.click(By.id("user_2"));
			driver.selectByText(By.id("new_role"), "Editor");
			driver.click(By.id("changeit"));
			return EUITestStatus.PASS;
		}
	}
	class UsersUpdateUser extends Users {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.clickLinkText("Users");
			driver.clickLinkText("Edit");
			driver.inputType(By.id("description"), "Founder of CompuGlobalHyperMegaNet");
			driver.click(By.id("submit"));
			return EUITestStatus.PASS;
		}
	}
	abstract class Widget extends WPTest {
		
	}
	class WidgetArchives extends Widget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			return driver.hasElementPF(By.xpath("//h3[contains(., 'Archives')]"));
		}
	}
	class WidgetCalendar extends Widget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			Calendar cal = Calendar.getInstance();
			String current_month_name = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US);
			return driver.hasElementPF(By.xpath("//a[contains(., '"+current_month_name+"')]"));
		}
	}
	class WidgetCategories extends Widget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			return driver.hasElementPF(By.xpath("//h3[contains(., 'Categories')]"));
		}
	}
	class WidgetMeta extends Widget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			return driver.hasElementPF(By.xpath("//h3[contains(., 'Meta')]"));
		}
	}
	class WidgetPages extends Widget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			return driver.hasElementPF(By.xpath("//h3[contains(., 'Pages')]"));
		}
	}
	class WidgetRecentComments extends Widget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			return driver.hasElementPF(By.xpath("//h3[contains(., 'Comments')]"));
		}
	}
	class WidgetRecentPosts extends Widget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			return driver.hasElementPF(By.xpath("//h3[contains(., 'Recent Posts')]"));
		}
	}
	class WidgetEntriesRSS extends Widget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.clickLinkText("Entries RSS");
			return EUITestStatus.PASS;
		}
	}
	class WidgetCommentsRSS extends Widget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			driver.clickLinkText("Comments RSS");
			return EUITestStatus.PASS;
		}
	}
	class WidgetSearch extends Widget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			return driver.hasElementPF(By.xpath("//input[@value='Search']"));
		}
	}
	class WidgetTagCloud extends Widget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			return driver.hasElementPF(By.xpath("//h3[contains(., 'Tag')]"));
		}
	}
	class WidgetText extends Widget {
		@Override
		public EUITestStatus test(SimpleDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
}
