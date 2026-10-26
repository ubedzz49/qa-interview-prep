package playwrighttests;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

public class BaseTest {

    protected Playwright playwright;      // the main object: it starts everything
    protected Browser browser;            // one browser (Chrome), shared by all tests in a class
    protected BrowserContext context;     // a fresh "profile" for each test (like an incognito window)
    protected Page page;                  // one tab inside the context

    @BeforeClass
    public void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setChannel("chrome")                              // use the Chrome installed on the machine
                .setHeadless(Boolean.getBoolean("headless")));     // false = you can watch the browser
    }

    @BeforeMethod
    public void createPage() {
        context = browser.newContext();       // new context = clean cookies, storage, no leftovers
        page = context.newPage();
    }

    @AfterMethod(alwaysRun = true)
    public void closeContext() {
        context.close();
    }

    @AfterClass(alwaysRun = true)
    public void closeBrowser() {
        browser.close();
        playwright.close();
    }
}
