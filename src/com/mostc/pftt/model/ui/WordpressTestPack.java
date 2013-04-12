package com.mostc.pftt.model.ui;

import java.util.Calendar;
import java.util.Locale;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.model.ui.UIAccount.IUserType;

/**
 * 
 * @author Matt Ficken
 *
 */

// TIP: when can't find text of something you just added, try finding the link (@see LinkCategoryAdd)
public class WordpressTestPack implements UITestPack {
	
	@Override
	public String getNameAndVersionInfo() {
		return "Wordpress-3.5.1-2013-04-11-16-07";
	}
	@Override
	public boolean isDevelopment() {
		return false;
	}
	@Override
	public String getBaseURL() {
		return "/wordpress/";
	}
	
	@Override
	public String getNotes() {
		return "Should implement Tools-Import and Media-Add to test file uploading - an operation that has an above average chance of failure (test on remote file systems)\n also should test setting WP_LANG to other languages (multi-byte?) and FORCE_SSL (both in wp-config.php)";
	}
	
	public static String toSlug(String text) {
		return text.replace(" ", "_");
	}
	
	public static enum EWordpressUserRole implements IUserType {
		ANONYMOUS {
				@Override
				public boolean isAnonymous() {
					return true;
				}
			},
		ADMINISTRATOR,
		EDITOR,
		AUTHOR,
		CONTRIBUTOR,
		SUBSCRIBER;

		@Override
		public String getType() {
			return StringUtil.toTitle(toString());
		}
		@Override
		public String getDefaultUserName() {
			return StringUtil.toTitle(toString());
		}
		@Override
		public boolean isAnonymous() {
			return false;
		}
		
	} // end public static enum EWordpressUserRole
	
	@Override
	public void start(IUITestBranch anon_branch) { 
		UIAccount admin_user = new UIAccount("admin", "password01!");
		//
		UIAccount subscriber_user = new UIAccount(EWordpressUserRole.SUBSCRIBER, "password01!");
		UIAccount contributor_user = new UIAccount(EWordpressUserRole.CONTRIBUTOR, "password01!");
		UIAccount author_user = new UIAccount(EWordpressUserRole.AUTHOR, "password01!");
		UIAccount editor_user = new UIAccount(EWordpressUserRole.EDITOR, "password01!");
		// new password for `subscriber_user` 
		UIAccount subscriber_user_new_passwd = new UIAccount(subscriber_user, "wrong_password");
		UIAccount contributor_user_new_passwd = new UIAccount(subscriber_user, "wrong_password");
		UIAccount author_user_new_passwd = new UIAccount(author_user, "wrong_password");
		UIAccount editor_user_new_passwd = new UIAccount(editor_user, "wrong_password");
		UIAccount admin_user_wrong_passwd = new UIAccount(admin_user, "wrong_password");
		/////////////
		
		anon_branch.test(
		 		new MainPage(),
				// test widgets that are there by default (before adding/removing widgets)
				new WidgetRecentComments(),
				new WidgetRecentPosts(),
				new WidgetArchives(),
				new WidgetCategories()
			);
		anon_branch.test("Comment without being Logged In", new CommentAdd()); 
		anon_branch.test(admin_user, new LoginTest(), new LogoutTest())
			.test(
				new WelcomeLinksTest(),
				new CountWaitingCommentsGt1(), // XXX depends on CommentAdd test above
				new RecentCommentsDashboardWidget(),
				new RearrangeDashboardWidgets(),
				new AppearanceThemesActivate(),
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
				// change settings, especially timezone and date/time format
				new SettingsGeneralChangeTimezone(),
				new SettingsChangeDateTimeFormat(),
				new SettingsWriting(),
				new SettingsReading(),
				new SettingsDiscussion(),
				new SettingsMedia(),
				new SettingsPrivacy(),
				new SettingsPermalinks(),
				new ToolsExport(),
				new ToolsImport(),
				new PluginsInstalledPlugins(),
				// test activating and deactivating plugins
				// XXX deactivate plugin on cleanup
				new PluginsInstalledActivate(),
				// akismet is an important plugin (any real wordpress instance needs it activated)
				new PluginsAkismetConfiguration(), // XXX express dependency on PluginsInstalledActivate
				new PluginsInstalledDeactivate(),
				new UsersAllUsers(),
				new UsersAllAddNew(),
				new UsersAllFilter(),
				new UsersAllSearch(),
				// XXX if exiting, or skipping or stopping execution, should delete these users to cleanup for next time
				new UsersAddNew(author_user, EWordpressUserRole.AUTHOR),
				new UsersAddNew(editor_user, EWordpressUserRole.EDITOR),
				new UsersAddNew(contributor_user, EWordpressUserRole.CONTRIBUTOR),
				new UsersAddNew(subscriber_user, EWordpressUserRole.SUBSCRIBER),
				new UsersEdit(author_user),
				new UsersEdit(editor_user),
				new UsersEdit(contributor_user),
				new UsersEdit(subscriber_user)
			);
		// test admin login with wrong password
		if (!admin_user.username.equalsIgnoreCase(admin_user_wrong_passwd.username)||admin_user.password.equals(admin_user_wrong_passwd.password)) {
			anon_branch.testException("Admin-Login-2", "Don't have the wrong password to try to login as admin");
		} else {
			anon_branch.testXFail(admin_user_wrong_passwd, "Must not be able to login as admin with wrong password", new LoginTest(), new LogoutTest());
		}
		
		// these 2 operations, especially without being logged in, are the most critical functions of wordpress
		anon_branch.test(new CommentAdd(), new PostView());
		
		anon_branch.test("Make sure added widgets are visible",
				new MainPage(),
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
				new WidgetTagCloud()
			);
		
		// test all user roles
		testUserRole(EWordpressUserRole.ADMINISTRATOR, anon_branch, admin_user, null);
		boolean e = testUserRole(EWordpressUserRole.EDITOR, anon_branch, editor_user, editor_user_new_passwd);
		testUserRole(EWordpressUserRole.AUTHOR, anon_branch, author_user, author_user_new_passwd);
		testUserRole(EWordpressUserRole.CONTRIBUTOR, anon_branch, contributor_user, contributor_user_new_passwd);
		boolean s = testUserRole(EWordpressUserRole.SUBSCRIBER, anon_branch, subscriber_user, subscriber_user_new_passwd);
				
		anon_branch.test(admin_user, new LoginTest(), new LogoutTest())
			.test(
				new UsersChangeRole(editor_user, EWordpressUserRole.SUBSCRIBER),
				new UsersChangeRole(author_user, EWordpressUserRole.SUBSCRIBER),
				new UsersChangeRole(contributor_user, EWordpressUserRole.SUBSCRIBER),
				new UsersChangeRole(subscriber_user, EWordpressUserRole.EDITOR)
				);
		if (e&&s) {
			anon_branch.test(subscriber_user_new_passwd, "Approve comments after role changed from subscriber to editor", new LoginTest(), new LogoutTest())
				.test(
					new CommentsApprove()
					);
			anon_branch.testXFail(editor_user_new_passwd, "Can't approve comments after role changed from editor to subscriber", new LoginTest(), new LogoutTest())
				.test(
					new CommentsApprove()
					);
		}
		
		
		anon_branch.test(admin_user, "Admin Cleanup", new LoginTest(), new LogoutTest())
			.test(
						new UsersDelete(author_user),
						new UsersDelete(editor_user),
						new UsersDelete(contributor_user),
						new UsersDelete(subscriber_user),
						new UsersDeleteIncludingAttributions(),
						new UsersDeleteIncludingAttributions(),
						new AppearanceWidgetDeleteCalendar(),
						new AppearanceWidgetDeleteCustomMenu(),
						new AppearanceWidgetDeleteLinks(),
						new AppearanceWidgetDeleteMeta(),
						new AppearanceWidgetDeletePages(),
						new AppearanceWidgetDeleteSearch(),
						new AppearanceWidgetDeleteTagCloud(),
						new AppearanceWidgetDeleteText()
					);
		// XXX skip these if delete user failed
		anon_branch.testXFail(author_user, "can't login as deleted user", new LoginTest(), new LogoutTest());
		anon_branch.testXFail(editor_user, "can't login as deleted user", new LoginTest(), new LogoutTest());
		anon_branch.testXFail(contributor_user, "can't login as deleted user", new LoginTest(), new LogoutTest());
		anon_branch.testXFail(subscriber_user, "can't login as deleted user", new LoginTest(), new LogoutTest());
		anon_branch.test("Ensure still visible after extra widgets deleted",
				new MainPage(),
				new WidgetRecentComments(),
				new WidgetRecentPosts(),
				new WidgetArchives(),
				new WidgetCategories()
			);
	} // end public void run 
	
	protected boolean testUserRole(EWordpressUserRole role, IUITestBranch anon_branch, UIAccount user, UIAccount user_new_passwd) {
		boolean ok;
		if (role==EWordpressUserRole.SUBSCRIBER||role==EWordpressUserRole.AUTHOR) {
			ok = anon_branch.test(user, new LoginTest(), new LogoutTest())
				.test(
					new PostsAddNew(),
					new PostView(),
					new CommentAdd(),
					new EditProfile()
				).getStatus().isPass();
		} else {
			ok = anon_branch.test(user, new LoginTest(), new LogoutTest())
				.test(
					new CollapseDashboardMenu(),
					new QuickPress(),
					new RightNow(),
					new Help(),
					new RecentDraftsDashboardWidget(),
					new ScreenOptions(),
					new MySiteMenu(),
					new WordpressMenu(),
					new PlusNewPost(),
					new PlusNewMedia(),
					new PlusNewLink(),
					new PlusNewPage(),
					new PlusNewUser(),
					new EditProfile(),
					new LinkAddNew(),
					new LinkAll(),
					new LinkAllAddNew(),
					new LinkFilter(),
					new LinkSearch(),
					new LinkDelete(),
					new LinkCategoriesAll(),
					new LinkCategoriesAllAddNew(),
					new LinkCategoriesFilter(),
					new LinkCategoriesSearch(),
					new LinkCategoryAdd(),
					new LinkCategoryDelete(),
					new MediaAddNew(),
					new MediaLibrary(),
					new MediaDelete(),
					new PagesAddNew(),
					new PagesAllPages(),
					new PagesAllAddNew(),
					new PagesFilter(),
					new PagesSearch(),
					new PagesTrash(),
					new PagesTrashUndo(),
					new PagesEdit(),
					new PagesQuickEdit(),
					new PostsAll(),
					new PostsAllAddNew(),
					new PostsFilter(),
					new PostsSearch(),
					new PostQuickEdit(),
					new PostEdit(),
					new PostTrash(),
					new PostTrashUndo(),
					new PostCategoriesAll(),
					new PostCategoriesAllAddNew(),
					new PostCategoriesFilter(),
					new PostCategoriesSearch(),
					new PostCategoriesAdd(),
					new PostCategoriesDelete(),
					new PostTagsAll(),
					new PostTagsAllAddNew(),
					new PostTagsFilter(),
					new PostTagsSearch(),
					new PostTagsAdd(),
					new PostTagsDelete(),
					new PostsAddNew(),
					new CommentsAllComments(),
					new CommentsAllFilter(),
					new CommentsAllSearch(),
					new CommentsTrash(),
					new CommentsTrashUndo(),
					new CommentsApprove(),
					new CommentsUnapprove()
				).getStatus().isPass();
		} // end if
		//
		
		
		// try to change password 
		if (role!=EWordpressUserRole.ADMINISTRATOR) {
			if (!user.username.equalsIgnoreCase(user_new_passwd.username)) {
				// XXX get actual test name - clean that up
				anon_branch.testException(WPTest.createUniqueTestName(role, "Login-2"), "Try to change password with wrong username");
			} else if (user.password.equals(user_new_passwd.password)) {
				anon_branch.testException(WPTest.createUniqueTestName(role, "Login-2"), "Try to change password to current password");
			} else {
				ok = anon_branch.test(user, new LoginTest(), new LogoutTest())
					.test(new ChangePassword(user_new_passwd))
						.test(new LogoutTest()) // logout first, to test logging back in (which is the test that should fail)
						.testXFail(user, "Must not be able to login with old password after password changed", new LoginTest(), new LogoutTest())
					.getStatus().isPass();
			}
			ok = anon_branch.test(new LostPassword())
				.getStatus().isPass();
		}
		return ok;
	} // end protected boolean testUserRole
	
	abstract class WPTest extends EasyUITest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			get("/");
			return true;
		}
	}
	abstract class LoggedInTest extends WPTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			if (hasText("Log in")||hasElementNow(By.cssSelector("a[title=\"Password Lost and Found\"]"))) {
				// NOT LOGGED IN ANY MORE, need to login again
				// return testChildPass(new LoginTest(), driver);
				inputTypeId("user_login", user_account.username);
				inputTypeId("user_pass", user_account.password);
				clickId("wp-submit");
				return hasText("Log Out");
			} else {
				return true;
			}
		}
	}
	abstract class DashboardTest extends LoggedInTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			//dashboardStart();
			return super.start(driver);
		}
		protected void dashboardStart() {
			get("/wp-admin/");
		}
	}
	
	class LoginTest extends WPTest {
		@Override
		public EUITestStatus test(final IUITestDriver driver) throws Exception {
			// go to url, `log in` hyperlink might not always be there (depends on widgets and themes)
			get("/wp-login.php");
			inputTypeId("user_login", user_account.username);
			inputTypeId("user_pass", user_account.password);
			clickId("wp-submit");
			return !hasText("incorrect") && hasElement(By.cssSelector("#wp-admin-bar-logout > a.ab-item")) ? EUITestStatus.PASS : EUITestStatus.FAIL;
		}
	} // end class LoginTest
	class LogoutTest extends WPTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			if (hasElement(By.partialLinkText("Log in"))) 
				return EUITestStatus.PASS;
			click(By.cssSelector("#wp-admin-bar-logout > a.ab-item"));
			clickPartialLinkText("Back");
			return hasElementPF(By.partialLinkText("Log in"));
		}
	}
	class WelcomeLinksTest extends DashboardTest {
		@Override
		public String getComment() {
			return "Links from Welcome message on Dashboard(if this fails some other tests should fail too)";
		}
		static final String WELCOME_MESSAGE = "Welcome";
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			if (!hasText(WELCOME_MESSAGE))
				return EUITestStatus.SKIP; // message already dismissed - can't test it
					
			// try all these links from the message - each will be checked for error/warning messages
			//       -these pages will be tested specifically and in detail in other tests (don't need to do it here)
			for ( String link_text : new String[]{
			            "Choose your privacy setting",
			            "Select your tagline and time zone",
			            "Turn comments on or off",
			            "Fill in your profile",
			            "Create an About Me page",
			            "Write your first post",
			            "Set a background color",
			            "Select a new header image",
			            "Add some widgets"
					} ) {
				clickLinkText(link_text);
				// return to dashboard for next link
				dashboardStart();
			}
			// test dimissing the message
			clickLinkText("Dismiss this message");
			// make sure the message is gone
			return hasNotTextAllPF(WELCOME_MESSAGE);
		}
	}
	class CollapseDashboardMenu extends DashboardTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class RecentCommentsDashboardWidget extends DashboardTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class QuickPress extends DashboardTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class RightNow extends DashboardTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class Help extends DashboardTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class RecentDraftsDashboardWidget extends DashboardTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class ScreenOptions extends DashboardTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class MySiteMenu extends DashboardTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class RearrangeDashboardWidgets extends DashboardTest {
		// using Drang-And-Drop to move around widgets on the dashboard main page
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class WordpressMenu extends DashboardTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class PlusNewPost extends DashboardTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class PlusNewLink extends DashboardTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class PlusNewPage extends DashboardTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class PlusNewMedia extends DashboardTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class PlusNewUser extends DashboardTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class CountWaitingCommentsGt1 extends DashboardTest {
		@Override
		public String getComment() {
			return "Check for a waiting comment listed on the top of the dashboard";
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			// should be at least 1
			// XXX express order here - CommentsAdd test must be run before this (otherwise this might be 0)
			return hasElementPF(By.xpath("//span[@id='ab-awaiting-mod' and text() > 0 ]"));
		}
	}
	class AppearanceThemesActivate extends AppearanceTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			clickLinkText("Themes");
			clickLinkText("Activate");
			return hasTextAllPF("New theme activated");
		}
	}
	abstract class AppearanceTest extends DashboardTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			return super.start(driver) && clickLinkText("Appearance");
		}
		protected boolean verifyChangesSaved() {
			return hasText("Changes saved");
		}
		protected EUITestStatus verifyChangesSavedPF() {
			return pf(verifyChangesSaved());
		}		
		protected boolean verifyUpdated() {
			return hasText("updated");
		}
		protected EUITestStatus verifyUpdatedPF() {
			return pf(verifyUpdated());
		}
	}
	abstract class AppearanceWidget extends AppearanceTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			return super.start(driver) && clickLinkText("Widgets");
		}
	}
	class AppearanceWidgetDeleteCalendar extends AppearanceWidget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			click(getElement(By2.partialId("widget", "calendar")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.edit"));
			clickId("removewidget");
			return verifyChangesSavedPF();
		}
	}
	class AppearanceWidgetDeleteCustomMenu extends AppearanceWidget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			click(getElement(By2.partialId("widget", "nav_menu")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.edit"));
			clickId("removewidget");
			return verifyChangesSavedPF();
		}
	}
	class AppearanceWidgetDeleteLinks extends AppearanceWidget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			click(getElement(By2.partialId("widget", "links")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.edit"));
			clickId("removewidget");
			return verifyChangesSavedPF();
		}
	}
	class AppearanceWidgetDeleteMeta extends AppearanceWidget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			click(getElement(By2.partialId("widget", "meta")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.edit"));
			clickId("removewidget");
			return verifyChangesSavedPF();
		}
	}
	class AppearanceWidgetDeletePages extends AppearanceWidget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			click(getElement(By2.partialId("widget", "pages")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.edit"));
			clickId("removewidget");
			return verifyChangesSavedPF();
		}
	}
	class AppearanceWidgetDeleteSearch extends AppearanceWidget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			click(getElement(By2.partialId("widget", "search")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.edit"));
			clickId("removewidget");
			return verifyChangesSavedPF();
		}
	}
	class AppearanceWidgetDeleteTagCloud extends AppearanceWidget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			click(getElement(By2.partialId("widget", "tag_cloud")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.edit"));
			clickId("removewidget");
			return verifyChangesSavedPF();
		}
	}
	class AppearanceWidgetDeleteText extends AppearanceWidget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			click(getElement(By2.partialId("widget", "text")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.edit"));
			clickId("removewidget");
			return verifyChangesSavedPF();
		}
	}
	class AppearanceWidgetAddArchives extends AppearanceWidget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			// each widget is given an auto-generated id that depends on its order (same css class) ... can only select by id
			// ex: #widget-3_archives-__i__
			click(getElement(By2.partialId("widget", "archives")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.add"));
			clickId("savewidget");
			return verifyChangesSavedPF();
		}
	}
	class AppearanceWidgetAddCalendar extends AppearanceWidget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			click(getElement(By2.partialId("widget", "calendar")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.add"));
			clickId("savewidget");
			return verifyChangesSavedPF();
		}
	}
	class AppearanceWidgetAddCategories extends AppearanceWidget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			click(getElement(By2.partialId("widget", "categories")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.add"));
			clickId("savewidget");
			return verifyChangesSavedPF();
		}
	}
	class AppearanceWidgetAddCustomMenu extends AppearanceWidget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			click(getElement(By2.partialId("widget", "menu")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.add"));
			clickId("savewidget");
			return verifyChangesSavedPF();
		}
	}
	class AppearanceWidgetAddLinks extends AppearanceWidget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			click(getElement(By2.partialId("widget", "links")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.add"));
			clickId("savewidget");
			return verifyChangesSavedPF();
		}
	}
	class AppearanceWidgetAddMeta extends AppearanceWidget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			click(getElement(By2.partialId("widget", "meta")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.add"));
			clickId("savewidget");
			return verifyChangesSavedPF();
		}
	}
	class AppearanceWidgetAddPages extends AppearanceWidget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			click(getElement(By2.partialId("widget", "pages")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.add"));
			clickId("savewidget");
			return verifyChangesSavedPF();
		}
	}
	class AppearanceWidgetAddRecentComments extends AppearanceWidget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			click(getElement(By2.partialId("widget", "recent-comments")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.add"));
			clickId("savewidget");
			return verifyChangesSavedPF();
		}
	}
	class AppearanceWidgetAddRecentPosts extends AppearanceWidget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			click(getElement(By2.partialId("widget", "recent-posts")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.add"));
			clickId("savewidget");
			return verifyChangesSavedPF();
		}
	}
	class AppearanceWidgetAddRSS extends AppearanceWidget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			click(getElement(By2.partialId("widget", "rss")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.add"));
			clickId("savewidget");
			return verifyChangesSavedPF();
		}
	}
	class AppearanceWidgetAddSearch extends AppearanceWidget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			click(getElement(By2.partialId("widget", "search")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.add"));
			clickId("savewidget");
			return verifyChangesSavedPF();
		}
	}
	class AppearanceWidgetAddTagCloud extends AppearanceWidget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			click(getElement(By2.partialId("widget", "tag_cloud")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.add"));
			clickId("savewidget");
			return verifyChangesSavedPF();
		}
	}
	class AppearanceWidgetAddText extends AppearanceWidget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			click(getElement(By2.partialId("widget", "text")), By.cssSelector("div.widget-top > div.widget-title-action > a.widget-control-edit.hide-if-js > span.add"));
			clickId("savewidget");
			return verifyChangesSavedPF();
		}
	}
	class AppearanceWidgetAddTwentyElevenEphemera extends WPTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class AppearanceMenusActivate extends AppearanceTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			clickPartialLinkText("Menus");
			click(By.cssSelector("abbr[title=\"Add menu\"]"));
			inputTypeId("menu-name", StringUtil.randomLettersStr(5, 10));
			clickId("save_menu_header");
			clickId("save_menu_header");
			return verifyUpdatedPF();
		}
	}
	class AppearanceHeaderSet extends AppearanceTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			clickPartialLinkText("Header");
			click(By.cssSelector("div.default-header > label > input[name=\"default-header\"]"));
			clickId("save-header-options");
			return verifyUpdatedPF();
		}
	}
	class AppearanceBackgroundSet extends AppearanceTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			clickPartialLinkText("Background");
			clickId("pickcolor");
			clickId("save-background-options");
			return verifyUpdatedPF();
		}
	}
	abstract class Link extends DashboardTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			return super.start(driver) && clickLinkText("Links");
		}
	}
	class LinkAll extends Link {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return hasTextAllPF("Links");
		}
	}
	class LinkAllAddNew extends Link {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class LinkFilter extends Link {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class LinkSearch extends Link {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class LinkAddNew extends Link {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			clickLinkText("Add New");
			inputTypeId("link_name", StringUtil.randomLettersStr(10, 20));
			inputTypeId("link_url", "http://windows.php.net/");
			inputTypeId("link_description", "PHP on Windows");
			clickId("in-link-category-2");
			clickId("link_target_top");
			clickId("co-worker");
			clickId("co-resident");
			clickId("child");
			clickId("friend");
			clickId("publish");
			return hasTextAllPF("added");
		}
	}
	class LinkDelete extends Link {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			click(getElement(By2.partialClassName("link")), By.cssSelector("th.check-column > input[name=\"linkcheck[]\"]"));
			selectByTextName("action", "Delete");
			clickId("doaction");
			return hasTextAllPF("deleted");
		}
	}
	abstract class LinkCategory extends Link {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			return super.start(driver) && clickLinkText("Link Categories");
		}
	}
	class LinkCategoriesAll extends LinkCategory {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return hasTextAllPF("Link Categories");
		}
	}
	class LinkCategoriesAllAddNew extends DashboardTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class LinkCategoriesFilter extends DashboardTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class LinkCategoriesSearch extends LinkCategory {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class LinkCategoryAdd extends LinkCategory {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			String cat_name = StringUtil.randomLettersStr(10, 15);
			String cat_slug = toSlug(cat_name);
			String description = StringUtil.randomLettersStr(20, 30);
			
			inputTypeId("tag-name", cat_name);
			inputTypeId("tag-slug", cat_slug);
			inputTypeId("tag-description", description);
			clickId("submit");
			// get list of categories back (not status message) ... check category list
			return pf(clickLinkText(cat_name));
		}
	}
	class LinkCategoryDelete extends LinkCategory {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			click(By.cssSelector("input[name=\"delete_tags[]\"]"));
			selectByTextName("action", "Delete");
			clickId("doaction");
			return hasTextAllPF("deleted");
		}
	}
	class MainPage extends WPTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.PASS;
		}
	}
	class MediaAddNew extends DashboardTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return null;
		}
	}
	class MediaLibrary extends DashboardTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class MediaDelete extends DashboardTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	abstract class Pages extends DashboardTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			return super.start(driver) && clickLinkText("Pages");
		}
	}
	class PagesAddNew extends Pages {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			clickPartialLinkText("Add New");
			inputTypeId("title", "title");
			clickId("publish");
			return EUITestStatus.PASS;
		}
	}
	class PagesAllPages extends Pages {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return hasTextAllPF("Pages");
		}
	}
	class PagesAllAddNew extends DashboardTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class PagesFilter extends DashboardTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class PagesSearch extends DashboardTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class PagesTrash extends Pages {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			click(By.cssSelector("input[name=\"post[]\"]"));
			selectByText(By.cssSelector("select[name=\"action\"]"), "Move to Trash");
			clickId("doaction");
			return hasTextAllPF("moved to the Trash");
		}
	}
	class PagesTrashUndo extends DashboardTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class PagesEdit extends Pages {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			WebElement a = getElement(By.xpath("//a[contains(@href, '/wp-admin/post.php')]"));
			click(a);
			clickId("publish");
			return hasTextAllPF("updated");
		}
	}
	class PagesQuickEdit extends Pages {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			WebElement a = getElement(By.xpath("//a[contains(@href, '/wp-admin/edit.php')]"));
			click(a);
			clickId("publish");
			return hasTextAllPF("updated");
		}
	}
	abstract class Posts extends DashboardTest { 
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			return super.start(driver) && clickLinkText("Posts");
		}
	}
	class PostsAll extends Posts {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return hasTextAllPF("Posts", "All");
		}
	}
	class PostsAllAddNew extends Posts {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class PostsFilter extends Posts {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class PostsSearch extends Posts {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class PostTrash extends Posts {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			click(By2.partialId("cb-select"));
			selectByTextName("action", "Move to Trash");
			clickId("doaction");
			return hasTextAllPF("moved to the Trash");
		}
	}
	class PostTrashUndo extends Posts {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class PostQuickEdit extends Posts {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			WebElement a = getElement(By.xpath("//a[contains(@href, '/wp-admin/edit.php')]"));
			click(a);
			clickId("publish");
			return hasTextAllPF("Post updated");
		}
	}
	class PostEdit extends Posts {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			WebElement a = getElement(By.xpath("//a[contains(@href, '/wp-admin/post.php')]"));
			click(a);
			clickId("publish");
			return hasTextAllPF("Post updated");
		}
	}
	abstract class PostCategories extends Posts {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			return super.start(driver) && clickPartialLinkText("Categories");
		}
	}
	abstract class PostTags extends Posts {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			return super.start(driver) && clickPartialLinkText("Tags");
		}
	}
	class PostCategoriesAll extends PostCategories {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return hasTextAllPF("Categories", "All");
		}
	}
	class PostCategoriesAllAddNew extends PostCategories {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class PostCategoriesFilter extends PostCategories {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class PostCategoriesSearch extends PostCategories {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class PostCategoriesAdd extends PostCategories {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			String cat_name = StringUtil.randomLettersStr(5, 7)+" "+StringUtil.randomLettersStr(5, 7);
			String cat_slug = toSlug(cat_name);
			
			inputTypeId("tag-name", cat_name);
			inputTypeId("tag-slug", cat_slug);
			inputTypeId("tag-description", "some themes may show this");
			clickId("submit");
			return pf(clickLinkText(cat_name));
		}
	}
	class PostCategoriesDelete extends PostCategories {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			click(By.cssSelector("input[name=\"delete_tags[]\"]"));
			selectByText(By.cssSelector("select[name=\"action\"]"), "Delete");
			clickId("doaction");
			return hasTextAllPF("Items deleted");
		}
	}
	class PostTagsAll extends PostTags {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return hasTextAllPF("Posts", "All");
		}
	}
	class PostTagsAllAddNew extends PostTags {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class PostTagsFilter extends PostTags {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class PostTagsSearch extends PostTags {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class PostTagsAdd extends PostTags {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			String tag_name = StringUtil.randomLettersStr(5, 7);
			String tag_slug = tag_name;
			
			inputTypeId("tag-name", tag_name);
			inputTypeId("tag-slug", tag_slug);
			inputTypeId("tag-description", "some themes may show it");
			clickId("submit");
			return pf(clickLinkText(tag_name));
		}
	}
	class PostTagsDelete extends PostTags {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			click(By.cssSelector("input[name=\"delete_tags[]\"]"));
			selectByTextName("action", "Delete");
			clickId("doaction");
			return hasTextAllPF("deleted");
		}
	}
	abstract class Plugins extends DashboardTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			return super.start(driver) && clickPartialLinkText("Plugins");
		}
	}
	class PluginsInstalledPlugins extends Plugins {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return hasTextAllPF("Installed Plugins");
		}
	}
	class PluginsInstalledActivate extends Plugins {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			clickLinkText("Activate"); // XXX assumes Akismet is first
			selectByText(By.name("action"), "Activate");
			return hasTextAllPF("activated");
		}
	}
	class PluginsAkismetConfiguration extends Plugins {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			clickLinkText("Akismet Configuration");
			clickName("submit");
			return hasTextAllPF("Options saved");
		}
	}
	class PluginsInstalledDeactivate extends Plugins {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			clickLinkText("Deactivate");
			return hasTextAllPF("deactivated");
		}
	}
	class PostView extends WPTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED; // TODO
		}
	}
	class PostsAddNew extends Posts {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			clickPartialLinkText("Add New");
			inputTypeId("title", "enter title here");
			clickId("save-post");
			clickId("publish");
			return hasText("updated") || hasText("published") ? EUITestStatus.PASS : EUITestStatus.FAIL;
		}
	}
	class CommentAdd extends WPTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			final String text = StringUtil.randomLettersStr(20, 40);
			
			// open comment page:
			// find link titled: `Comment on <post title>`
			driver().findElement(By.cssSelector("a[title=\"Comment on enter title here\"]")).click();

			if (isAnon()) {
				// fill in name and email fields
				inputTypeId("author", "anonymous author");
				inputTypeId("email", "a@a.com");
			}
			
			// enter comment text
			inputTypeId("comment", text);
			
			// submit comment form
			clickId("submit");
			
			// verify
			return hasTextAllPF(text);
		}
	}
	abstract class Comments extends DashboardTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			return super.start(driver) && clickPartialLinkText("Comments");
		}
	}
	class CommentsAllComments extends Comments {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return hasTextAllPF("Comments", "All");
		}
	}
	class CommentsAllFilter extends Comments {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class CommentsAllSearch extends Comments {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class CommentsTrash extends Comments {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			click(getElement(By2.partialClassName("comment")), By.cssSelector("th.check-column > input[name=\"delete_comments[]\"]"));
			selectByText(By.cssSelector("select[name=\"action\"]"), "Move to Trash");
			clickId("doaction");
			return hasTextAllPF("comment moved to the Trash");
		}
	}
	class CommentsTrashUndo extends Comments {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class CommentsApprove extends Comments {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			//click(By.xpath("//li[@id='menu-comments']/a"));
			//click(By.cssSelector("input[name=\"delete_comments[]\"]"));
			selectByText(By.cssSelector("select[name=\"action\"]"), "Approve");
			clickId("doaction");
			return hasTextAllPF("Approved");
		}
	}
	class CommentsUnapprove extends Comments {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			//click(By.xpath("//li[@id='menu-comments']/a"));
			//click(By.cssSelector("input[name=\"delete_comments[]\"]"));
			selectByText(By.cssSelector("select[name=\"action\"]"), "Unapprove");
			clickId("doaction");
			return hasTextAllPF("Approve");
		}
	}
	abstract class Settings extends DashboardTest {
		public boolean start(IUITestDriver driver) throws Exception {
			return super.start(driver) && clickLinkText("Settings");
		}
		protected EUITestStatus verifyChangedPF() {
			return pf(verifyChanged());
		}
		protected boolean verifyChanged() {
			return hasText("changed");
		}
		protected EUITestStatus verifySavedPF() {
			return pf(verifySaved());
		}
		protected boolean verifySaved() {
			return hasText("saved");
		}
		protected boolean verifyUpdated() {
			return hasText("updated");
		}
		protected EUITestStatus verifyUpdatedPF() {
			return pf(verifyUpdated());
		}
	}
	class SettingsGeneralChangeTimezone extends Settings {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			clickLinkText("General");
			selectByTextId("timezone_string", "UTC-8");
			clickId("submit");
			return verifySavedPF();
		}
	}
	class SettingsChangeDateTimeFormat extends Settings {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			clickLinkText("General");
			click(By.cssSelector("label[title=\"Y/m/d\"] > input[name=\"date_format\"]"));
			click(By.cssSelector("label[title=\"g:i A\"] > input[name=\"time_format\"]"));
			clickId("submit");
			return pf(
					isChecked(By.cssSelector("label[title=\"Y/m/d\"] > input[name=\"date_format\"]")) &&
					isChecked(By.cssSelector("label[title=\"g:i A\"] > input[name=\"time_format\"]")) &&
					verifySaved()
				);
		}
	}
	class SettingsWriting extends Settings {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			clickPartialLinkText("Writing");
			inputTypeId("default_post_edit_rows", "30");
			clickId("submit");
			return pf(
					hasValue(By.id("default_post_edit_rows"), "30") &&
					verifySaved()
				);
		}
	}
	class SettingsReading extends Settings {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			clickPartialLinkText("Reading");
			inputTypeId("posts_per_page", "20");
			clickId("submit");
			return pf(
					hasValue(By.id("posts_per_page"), "20") &&
					verifySaved()
				);
		}
	}
	class SettingsDiscussion extends Settings {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			clickPartialLinkText("Discussion");
			clickId("avatar_gravatar_default");
			clickId("submit");
			return verifySavedPF();
		}
	}
	class SettingsMedia extends Settings {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			// find Media in Settings, not top-level Media (media library) link
			click(By.xpath("//a[contains(text(),'Media')]"));
			clickId("submit");
			return verifySavedPF();
		}
	}
	class SettingsPrivacy extends Settings {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			clickPartialLinkText("Privacy");
			clickId("blog-public");
			clickId("submit");
			return verifySavedPF();
		}
	}
	class SettingsPermalinks extends Settings {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			/* clickPartialLinkText("Permalinks"));
			click(By.xpath("(//input[@name='selection'])[2]"));
			clickId("submit"));
			return verifyUpdated(driver);*/
			
			// TODO this breaks wordpress - test variations of this
			return null;
		}
	}
	abstract class Tools extends DashboardTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			return super.start(driver) && clickLinkText("Tools");
		}
	}
	class ToolsExport extends Tools {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return null;
		}
	}
	class ToolsImport extends Tools {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			/*upload(By.id("file"), ".csv", "");
			clickId("submit");
			return hasTextPF("Imported");*/
			return null;
		}
	}
	class LostPassword extends WPTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	abstract class EditProfilePassword extends LoggedInTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			return super.start(driver) && clickLinkText("Howdy, "+user_account.username);
		}
	}
	class ChangePassword extends EditProfilePassword {
		final UIAccount new_account;
		public ChangePassword(UIAccount new_account) {
			this.new_account = new_account;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			inputTypeId("pass1", new_account.password);
			inputTypeId("pass2", new_account.password);
			clickId("submit");
			return hasTextAllPF("Profile updated");
		}
	}
	class EditProfile extends EditProfilePassword {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			inputTypeId("description", StringUtil.randomLettersStr(5, 10));
			clickId("submit");
			return hasTextAllPF("Profile updated");
		}
	}
	abstract class Users extends DashboardTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			return super.start(driver) && clickLinkText("Users");
		}
	}
	class UsersAddNew extends Users {
		final UIAccount user_account;
		final EWordpressUserRole role;
		
		public UsersAddNew(UIAccount user_account, EWordpressUserRole role) {
			this.user_account = user_account;
			this.role = role;
		}

		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			click(By.xpath("(//a[contains(text(),'Add New')])[6]"));
			inputTypeId("user_login", user_account.username);
			inputTypeId("email", user_account.username+"@compuglobalhypermega.net");
			inputTypeId("first_name", user_account.username);
			inputTypeId("last_name", "Simpson");
			inputTypeId("url", "http://compuglobalhypermega.net");
			inputTypeId("pass1", user_account.password);
			inputTypeId("pass2", user_account.password);
			selectByTextId("role", role.getType());
			clickId("createusersub");
			return hasTextAllPF("New user created");
		}
	}
	class UsersAllUsers extends Users {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return hasTextAllPF("Users");
		}
	}
	class UsersAllAddNew extends Comments {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class UsersAllFilter extends Comments {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class UsersAllSearch extends Comments {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class UsersDelete extends Users {
		final UIAccount user_account;
		
		public UsersDelete(UIAccount user_account) {
			this.user_account = user_account;
		}

		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			click(getElement(By.xpath("//a[text()='"+user_account.username+"']/../../..")), By.name("users[]"));
			selectByTextName("action", "Delete");
			clickId("doaction"); // go to confirmation page
			clickId("submit"); // confirm deletion
			return hasTextAllPF("User deleted");
		}
	}
	class UsersDeleteIncludingAttributions extends Comments {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			// overriding super#start just for performance
			// remove this method and use super#start if you actually implement this test
			return true;
		}
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return EUITestStatus.NOT_IMPLEMENTED;
		}
	}
	class UsersChangeRole extends Users {
		final UIAccount user_account;
		final EWordpressUserRole new_role;
		
		public UsersChangeRole(UIAccount user_account, EWordpressUserRole new_role) {
			this.user_account = user_account;
			this.new_role = new_role;
		}

		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			click(getElement(By.xpath("//a[text()='"+user_account.username+"']/../../..")), By.name("users[]"));
			selectByTextId("new_role", new_role.getType());
			clickId("changeit");
			return hasTextAllPF("Changed");
		}
	}
	class UsersEdit extends Users {
		final UIAccount user_account;
		
		UsersEdit(UIAccount user_account) {
			this.user_account = user_account;
		}
		
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			WebElement row = getElement(By.xpath("//a[text()='"+user_account.username+"']/../../.."));
			focus(row);
			click(row, By.linkText("Edit"));
			inputTypeId("description", "Founder of CompuGlobalHyperMegaNet");
			clickId("submit");
			return hasTextAllPF("Updated");
		}
	}
	abstract class Widget extends WPTest {
		
	}
	class WidgetArchives extends Widget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return hasElementPF(By.xpath("//h3[contains(., 'Archives')]"));
		}
	}
	class WidgetCalendar extends Widget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			Calendar cal = Calendar.getInstance();
			String current_month_name = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US);
			return hasElementPF(By.xpath("//a[contains(., '"+current_month_name+"')]"));
		}
	}
	class WidgetCategories extends Widget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return hasElementPF(By.xpath("//h3[contains(., 'Categories')]"));
		}
	}
	class WidgetMeta extends Widget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return hasElementPF(By.xpath("//h3[contains(., 'Meta')]"));
		}
	}
	class WidgetPages extends Widget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return hasElementPF(By.xpath("//h3[contains(., 'Pages')]"));
		}
	}
	class WidgetRecentComments extends Widget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return hasElementPF(By.xpath("//h3[contains(., 'Comments')]"));
		}
	}
	class WidgetRecentPosts extends Widget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return hasElementPF(By.xpath("//h3[contains(., 'Recent Posts')]"));
		}
	}
	class WidgetEntriesRSS extends Widget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			clickLinkText("Entries RSS");
			return EUITestStatus.PASS;
		}
	}
	class WidgetCommentsRSS extends Widget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			clickLinkText("Comments RSS");
			return EUITestStatus.PASS;
		}
	}
	class WidgetSearch extends Widget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return hasElementPF(By.xpath("//input[@value='Search']"));
		}
	}
	class WidgetTagCloud extends Widget {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return hasElementPF(By.xpath("//h3[contains(., 'Tag')]"));
		}
	}
} // end public class WordpressTestPack
