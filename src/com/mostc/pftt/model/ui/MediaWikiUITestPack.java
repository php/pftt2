package com.mostc.pftt.model.ui;

import org.openqa.selenium.By;

// NOTE: some versions of opcache have broken mediawiki
// NOTE: test using opcache for object caching (mediawiki setting)
//            -CACHE=CACHE_ANY - will try using cache => for test should require that it be used
//       test turning it off too
public class MediaWikiUITestPack {

	public void run(UITestRunner test) {
		UIAccount admin_user = new UIAccount("WikiSysop", "password01!");
		
		test.test(new MainPageTest());
		testPrefAndSpecial(test.test(admin_user, new LoginTest(), new LogoutTest())
			.test(new AdminActiveUserListTest())
			.test(new AdminUserGroupRightsTest())
			.test(new AdminSystemMessagesTest())
			.test(new AdminBlockedUsersTest())
			.test(new AdminChangeEMailAddressTest())
			.test(new AdminResetPasswordTest())
			.test(new SpecUserContributionsTest())
			.test(new AdminUserListTest())
			// TODO .test(new CreateNewArticleTest())
			// TODO .test(new EditExistingArticleTest())
			);
		test.test(new LogoutTest());
		
		/* TODO 
		UIAccount created_user = new UIAccount("auser", "password01!");
		test.test(new UserCreateTest(created_user))
			.test(created_user, new LoginTest())
		// TODO testPrefAndSpecial(test);
			.test(new CreateNewArticleTest())
			.test(new EditExistingArticleTest())
			.test(new LogoutTest());
		// have user change password, logout, login again (testing password change)
		// and then try to login using the old password (XFAIL)
		test.test(created_user, new LoginTest())
			.test(new ChangePasswordTest())
			.test(new LogoutTest())
			.test(created_user_new_password, new LoginTest())
			.test(new LogoutTest())
			.testXFail(created_user, new LoginTest());
		test.test(admin_user, new LoginTest())
			.test(new AdminDeleteUserTest(created_user));
		test.test(new LogoutTest());
		*/
		// anonymous article
		test.test(new CreateNewArticleTest());
		test.test(new EditExistingArticleTest());
		
		test.test(new HelpTest());
		test.test(new PrivacyPolicyTest());
		test.test(new AboutMyWikiTest());
		test.test(new DisclaimersTest());
	}
	
	protected void testPrefAndSpecial(IUITestBranch test) {
		test.test(new PrefShowTest());
		test.test(new PrefChangeLanguageTest());
		test.test(new PrefChangeSkinTest());
		test.test(new PrefChangeDateFormatTest());
		test.test(new PrefChangeTimezoneTest());
		test.test(new MyWatchlistTest());
		test.test(new MyContributionsTest());
		test.test(new CommunityPortalTest());
		test.test(new CurrentEventsTest());
		test.test(new RecentChangesTest());
		test.test(new RandomPageTest());
		test.test(new SpecBrokenRedirectsTest());
		test.test(new SpecDeadEndPagesTest());
		test.test(new SpecDoubleRedirectsTest());
		test.test(new SpecLongPagesTest());
		test.test(new SpecOldestPagesTest());
		test.test(new SpecOrphanedPagesTest());
		test.test(new SpecPagesWithFewestRevisionsTest());
		test.test(new SpecPagesWithoutLanguageLinksTest());
		test.test(new SpecProtectedPagesTest());
		test.test(new SpecProtectedTitlesTest());
		test.test(new SpecShortPagesTest());
		test.test(new SpecUncategorizedCategoriesTest());
		test.test(new SpecUncategorizedFilesTest());
		test.test(new SpecUncategorizedPagesTest());
		test.test(new SpecUncategorizedTemplatesTest());
		test.test(new SpecUnusedCategoriesTest());
		test.test(new SpecUnusedFilesTest());
		test.test(new SpecUnusedTemplatesTest());
		test.test(new SpecWantedCategoriesTest());
		test.test(new SpecWantedFilesTest());
		test.test(new SpecWantedPagesTest());
		test.test(new SpecWantedTemplatesTest());
		test.test(new SpecAllPagesTest());
		test.test(new SpecAllPagesWithPrefixTest());
		test.test(new SpecCategoriesTest());
		test.test(new SpecListRedirectsTest());
		test.test(new SpecPagesLinkingToDisambiguationPagesTest());
		test.test(new SpecGalleryOfNewFilesTest());
		test.test(new SpecLogsTest());
		test.test(new SpecMyWatchlistTest());
		test.test(new SpecNewPagesTest());
		test.test(new SpecRelatedChangesTest());
		test.test(new SpecValidChangeTagsTest());
		test.test(new SpecFileListTest());
		test.test(new SpecFilePathTest());
		test.test(new SpecMIMESearchTest());
		test.test(new SpecSearchForDuplicateFilesTest());
		test.test(new SpecPopularPagesTest());
		test.test(new SpecStatisticsTest());
		test.test(new SpecVersionTest());
		test.test(new SpecExternalLinksSearchTest());
		test.test(new SpecRandomRedirectTest());
		test.test(new SpecSearchTest());
		test.test(new SpecMostLinkedToCategoriesTest());
		test.test(new SpecMostLinkedToFilesTest());
		test.test(new SpecMostLinkedToPagesTest());
		test.test(new SpecMostLinkedToTemplatesTest());
		test.test(new SpecPagesWithMostCategoriesTest());
		test.test(new SpecPagesWithMostRevisionsTest());
		test.test(new SpecComparePagesTest());
		test.test(new SpecExportPagesTest());
		test.test(new SpecWhatLinksHereTest());
		test.test(new SpecBookSourcesTest());
	} // end protected void testPrefAndSpecial

	public static abstract class MWTest extends UITest {
		String baseUrl = "http://192.168.1.73/";
		
		public boolean start(IUITestDriver driver) throws Exception {
			driver.get(baseUrl + "/mediawiki/index.php?title=Main_Page");
			return true;
		}
	}
	
	public static abstract class SpecMWTest extends MWTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			if (!super.start(driver))
				return false;
			driver.findElement(By.linkText("Special pages")).click();
			return true;
		}
	} // end public static abstract class SpecMWTest
	
	public static abstract class PrefMWTest extends MWTest {
		@Override
		public boolean start(IUITestDriver driver) throws Exception {
			if (!super.start(driver))
				return false;
			driver.findElement(By.linkText("My preferences")).click();
			return true;
		}
	} // end public static abstract class PrefMWTest

	public static class LoginTest extends MWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Log in / create account")).click();
			driver.findElement(By.id("wpName1")).clear();
			driver.findElement(By.id("wpName1")).sendKeys(user_account.username);
			driver.findElement(By.id("wpPassword1")).clear();
			driver.findElement(By.id("wpPassword1")).sendKeys(user_account.password);
			driver.findElement(By.id("wpLoginAttempt")).click();

			return driver.findElement(By.linkText(user_account.username))==null ? EUITestStatus.FAIL : EUITestStatus.PASS;
		}
	}
	public static class LogoutTest extends MWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Log out")).click();
			return null; // TODO
		}
	}
	public static class CreateNewArticleTest extends MWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.id("searchGoButton")).click();
			driver.findElement(By.linkText("PFTT")).click(); // TODO unique title
			driver.findElement(By.id("wpTextbox1")).clear();
			driver.findElement(By.id("wpTextbox1")).sendKeys("Homer Simpson's internet startup.\n\nHad one customer:\n'Comic Book Guy' - who wanted a T1 line - homer didn't know what that was and just asked him for money");
			driver.findElement(By.id("wpSave")).click();
			return null;
		}
	}
	public static class EditExistingArticleTest extends MWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			// TODO Auto-generated method stub
			return null;
		}
	}
	public static class UserCreateTest extends MWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Log in / create account")).click();
			driver.findElement(By.linkText("Create an account")).click();
			driver.findElement(By.id("wpName2")).clear();
			driver.findElement(By.id("wpName2")).sendKeys("HomerSimpson");
			driver.findElement(By.id("wpPassword2")).clear();
			driver.findElement(By.id("wpPassword2")).sendKeys("password01!");
			driver.findElement(By.id("wpRetype")).clear();
			driver.findElement(By.id("wpRetype")).sendKeys("password01!");
			driver.findElement(By.id("wpEmail")).clear();
			driver.findElement(By.id("wpEmail")).sendKeys("homer@compuglobalhypermega.net");
			driver.findElement(By.id("wpRealName")).clear();
			driver.findElement(By.id("wpRealName")).sendKeys("Homer Simpson");
			driver.findElement(By.id("wpCreateaccount")).click();
			return null;
		}
	}
	public static class AdminSystemMessagesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("System messages")).click();
			driver.findElement(By.cssSelector("input[type=\"submit\"]")).click();
			return null;
		}
	}
	public static class AdminUserGroupRightsTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("User group rights")).click();
			driver.findElement(By.cssSelector("#sysop > td > a[title=\"Special:ListUsers\"]")).click();
			driver.findElement(By.linkText("WikiSysop")).click();
			return null;
		}
	}
	public static class AdminActiveUserListTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Active users list")).click();
			driver.findElement(By.linkText("WikiSysop")).click();
			return null;
		}
	}
	public static class PrefShowTest extends PrefMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return null;
		}
	}
	public static class PrefChangeLanguageTest extends PrefMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			// TODO Auto-generated method stub
			return null;
		}
	}
	public static class PrefChangeSkinTest extends PrefMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			// TODO Auto-generated method stub
			return null;
		}
	}
	public static class PrefChangeDateFormatTest extends PrefMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			// TODO Auto-generated method stub
			return null;
		}
	}
	public static class PrefChangeTimezoneTest extends PrefMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			// TODO Auto-generated method stub
			return null;
		}
	}
	public static class MyWatchlistTest extends MWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("My watchlist")).click();
			return null;
		}
	}
	public static class MyContributionsTest extends MWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("My contributions")).click();
			return null;
		}
	}
	public static class MainPageTest extends MWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			return null;
		}
	}
	public static class CommunityPortalTest extends MWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Community portal")).click();
			return null;
		}
	}
	public static class CurrentEventsTest extends MWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Current events")).click();
			return null;
		}
	}
	public static class RecentChangesTest extends MWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Recent changes")).click();
			return null;
		}
	}
	public static class RandomPageTest extends MWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Random page")).click();
			return null;
		}

	}
	public static class HelpTest extends MWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Help")).click();
			return null;
		}
	}
	public static class SpecBrokenRedirectsTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Broken redirects")).click();
			return null;
		}
	}
	public static class SpecDeadEndPagesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Dead-end pages")).click();
			return null;
		}
	}
	public static class SpecDoubleRedirectsTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Double redirects")).click();
			return null;
		}
	}
	public static class SpecLongPagesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Long pages")).click();
			return null;
		}
	}
	public static class SpecOldestPagesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Oldest pages")).click();
			return null;
		}
	}
	public static class SpecOrphanedPagesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Orphaned pages")).click();
			return null;
		}
	}
	public static class SpecPagesWithFewestRevisionsTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Pages with the fewest revisions")).click();
			return null;
		}
	}
	public static class SpecPagesWithoutLanguageLinksTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Pages without language links")).click();
			return null;
		}
	}
	public static class SpecProtectedPagesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Protected pages")).click();
			return null;
		}
	}
	public static class SpecProtectedTitlesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Protected titles")).click();
			return null;
		}
	}
	public static class SpecShortPagesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Short pages")).click();
			return null;
		}
	}
	public static class SpecUncategorizedCategoriesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Uncategorized categories")).click();
			return null;
		}
	}
	public static class SpecUncategorizedFilesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Uncategorized files")).click();
			return null;
		}
	}
	public static class SpecUncategorizedPagesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Uncategorized pages")).click();
			return null;
		}
	}
	public static class SpecUncategorizedTemplatesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Uncategorized templates")).click();
			return null;
		}
	}
	public static class SpecUnusedCategoriesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Unused categories")).click();
			return null;
		}
	}
	public static class SpecUnusedFilesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Unused files")).click();
			return null;
		}
	}
	public static class SpecUnusedTemplatesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Unused templates")).click();
			return null;
		}
	}
	public static class SpecWantedCategoriesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Wanted categories")).click();
			return null;
		}
	}
	public static class SpecWantedFilesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Wanted files")).click();
			return null;
		}
	}
	public static class SpecWantedPagesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Wanted pages")).click();
			return null;
		}
	}
	public static class SpecWantedTemplatesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Wanted templates")).click();
			return null;
		}
	}
	public static class SpecAllPagesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("All pages")).click();
			return null;
		}
	}
	public static class SpecAllPagesWithPrefixTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("All pages with prefix")).click();
			return null;
		}
	}
	public static class SpecCategoriesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Categories")).click();
			return null;
		}
	}
	public static class SpecListRedirectsTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("List of redirects")).click();
			return null;
		}
	}
	public static class SpecPagesLinkingToDisambiguationPagesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Pages linking to disambiguation pages")).click();
			return null;
		}
	}
	public static class AdminBlockedUsersTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Blocked users")).click();
			return null;
		}
	}
	public static class AdminChangeEMailAddressTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Change e-mail address")).click();
			return null;
		}
	}
	public static class AdminResetPasswordTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Reset password")).click();
			return null;
		}
	}
	public static class SpecUserContributionsTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("User contributions")).click();
			driver.findElement(By.cssSelector("input.mw-submit")).click();
			return null;
		}
	}
	public static class AdminUserListTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("User list")).click();
			return null;
		}
	}
	public static class SpecGalleryOfNewFilesTest extends MWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Gallery of new files")).click();
			return null;
		}
	}
	public static class SpecLogsTest extends MWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Logs")).click();
			return null;
		}
	}
	public static class SpecMyWatchlistTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("My Watchlist")).click();
			return null;
		}
	}
	public static class SpecNewPagesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("New pages")).click();
			return null;
		}
	}
	public static class SpecRelatedChangesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Related changes")).click();
			return null;
		}
	}
	public static class SpecValidChangeTagsTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Valid change tags")).click();
			return null;
		}
	}
	public static class SpecFileListTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("File list")).click();
			return null;
		}
	}
	public static class SpecFilePathTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("File path")).click();
			return null;
		}
	}
	public static class SpecMIMESearchTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("MIME search")).click();
			return null;
		}
	}
	public static class SpecSearchForDuplicateFilesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Search for duplicate files")).click();
			return null;
		}
	}
	public static class SpecPopularPagesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Popular pages")).click();
			return null;
		}
	}
	public static class SpecStatisticsTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Statistics")).click();
			return driver.findElement(By.linkText("(list of members)")) == null ? EUITestStatus.FAIL : EUITestStatus.PASS;
		}
	}
	public static class SpecVersionTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Version")).click();
			return null;
		}
	}
	public static class SpecExternalLinksSearchTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("External links search")).click();
			return null;
		}
	}
	public static class SpecRandomRedirectTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Random redirect")).click();
			return null;
		}
	}
	public static class SpecSearchTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Search")).click();
			return null;
		}
	}
	public static class SpecMostLinkedToCategoriesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Most linked-to categories")).click();
			return null;
		}
	}
	public static class SpecMostLinkedToFilesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Most linked-to files")).click();
			return null;
		}
	}
	public static class SpecMostLinkedToPagesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Most linked-to pages")).click();
			return null;
		}
	}
	public static class SpecMostLinkedToTemplatesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Most linked-to templates")).click();
			return null;
		}
	}
	public static class SpecPagesWithMostCategoriesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Pages with the most categories")).click();
			return null;
		}
	}
	public static class SpecPagesWithMostRevisionsTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Pages with the most revisions")).click();
			return null;
		}
	}
	public static class SpecComparePagesTest extends MWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Compare pages")).click();
			return null;
		}
	}
	public static class SpecExportPagesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Export pages")).click();
			return null;
		}
	}
	public static class SpecWhatLinksHereTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("What links here")).click();
			return null;
		}
	}
	public static class SpecBookSourcesTest extends SpecMWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Book sources")).click();
			return null;
		}
	}
	public static class PrivacyPolicyTest extends MWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Privacy policy")).click();
			return null;
		}
	}
	public static class AboutMyWikiTest extends MWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("About MyWiki")).click(); // TODO wiki name
			return null;
		}
	}
	public static class DisclaimersTest extends MWTest {
		@Override
		public EUITestStatus test(IUITestDriver driver) throws Exception {
			driver.findElement(By.linkText("Disclaimers")).click();
			return null;
		}
	}
	
} // end public class MediaWikiUITestPack
