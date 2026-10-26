package selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

public class PasswordTest extends BaseTest {

    // Live practice page hosted on GitHub Pages (works from any computer with internet)
    private static final String PAGE_URL = "https://ubedzz49.github.io/qa-interview-prep/demo-pages/demo-password.html";

    private static final String[] ALL_RULES =
            {"length", "upper", "lower", "number", "special", "nospace", "nouser"};

    // ---------- Locators: all in one place ----------
    private final By username = By.id("username");
    private final By password = By.id("password");
    private final By confirm = By.id("confirmPassword");
    private final By submitBtn = By.id("submitBtn");
    private final By toggleBtn = By.id("togglePwd");
    private final By strength = By.id("strength");
    private final By pwdError = By.id("pwdError");
    private final By confirmError = By.id("confirmError");
    private final By successMsg = By.id("successMsg");

    // ---------- Helper methods ----------
    private void open() {
        driver.get(PAGE_URL);
    }

    private void type(By locator, String text) {
        WebElement field = driver.findElement(locator);
        field.clear();
        if (!text.isEmpty()) {
            field.sendKeys(text);
        }
    }

    private void enter(String pwd, String confirmPwd, String user) {
        type(username, user);
        type(password, pwd);
        type(confirm, confirmPwd);
    }

    private void enter(String pwd) {
        enter(pwd, pwd, "");
    }

    private String ruleState(String rule) {
        return driver.findElement(By.id("rule-" + rule)).getDomAttribute("data-state");
    }

    private boolean buttonEnabled() {
        return driver.findElement(submitBtn).isEnabled();
    }

    // ---------- 1. Initial state ----------
    @Test
    public void tc01_initialState() {
        open();
        Assert.assertFalse(buttonEnabled(), "Button must be disabled at start");
        Assert.assertEquals(driver.findElement(strength).getText(), "Not entered");
        for (String rule : ALL_RULES) {
            Assert.assertEquals(ruleState(rule), "pending", "Rule " + rule);
        }
        Assert.assertEquals(driver.findElement(pwdError).getText(), "");
        Assert.assertFalse(driver.findElement(successMsg).isDisplayed());
    }

    // ---------- 2. Positive cases ----------
    @DataProvider(name = "positive")
    public Object[][] positive() {
        return new Object[][]{
                // id,     technique,                       password,           expected strength
                {"TC02", "Valid typical password",        "Abcd@1234",        "Medium"},
                {"TC03", "Boundary: 8 chars (min)",       "Abc@1234",         "Medium"},
                {"TC04", "Boundary: 16 chars (max)",      "Abcdefgh@1234567", "Strong"},
                {"TC05", "Strong password (12+ chars)",   "Str0ng@Passw0rd",  "Strong"},
        };
    }

    @Test(dataProvider = "positive")
    public void positiveCases(String id, String technique, String pwd, String expectedStrength) {
        open();
        enter(pwd);

        SoftAssert soft = new SoftAssert();                  // collect ALL failures, then report
        for (String rule : ALL_RULES) {
            soft.assertEquals(ruleState(rule), "pass", id + " rule " + rule);
        }
        soft.assertEquals(driver.findElement(strength).getText(), expectedStrength, id + " strength");
        soft.assertEquals(driver.findElement(pwdError).getText(), "", id + " error text");
        soft.assertTrue(buttonEnabled(), id + " button enabled");
        soft.assertAll();

        driver.findElement(submitBtn).click();
        Assert.assertEquals(driver.findElement(successMsg).getText(), "Password created successfully");
    }

    // ---------- 3. Negative cases: break exactly ONE rule at a time ----------
    @DataProvider(name = "negative")
    public Object[][] negative() {
        return new Object[][]{
                // id,     technique,                              password,             username, broken rule
                {"TC06", "Boundary: 7 chars (min - 1)",          "Abc@123",            "",     "length"},
                {"TC07", "Boundary: 17 chars (max + 1)",         "Abcdefgh@12345678",  "",     "length"},
                {"TC08", "Partition: no uppercase letter",       "abcd@1234",          "",     "upper"},
                {"TC09", "Partition: no lowercase letter",       "ABCD@1234",          "",     "lower"},
                {"TC10", "Partition: no number",                 "Abcd@efgh",          "",     "number"},
                {"TC11", "Partition: no special character",      "Abcd12345",          "",     "special"},
                {"TC12", "Error guessing: space inside",         "Abcd @1234",         "",     "nospace"},
                {"TC13", "Business rule: contains username",     "MyUBED@123",         "ubed", "nouser"},
        };
    }

    @Test(dataProvider = "negative")
    public void negativeCases(String id, String technique, String pwd, String user, String broken) {
        open();
        enter(pwd, pwd, user);

        SoftAssert soft = new SoftAssert();
        for (String rule : ALL_RULES) {
            String expected = rule.equals(broken) ? "fail" : "pass";
            soft.assertEquals(ruleState(rule), expected, id + " rule " + rule);
        }
        soft.assertEquals(driver.findElement(strength).getText(), "Weak", id + " strength");
        soft.assertEquals(driver.findElement(pwdError).getText(), "Password does not meet all the rules");
        soft.assertFalse(buttonEnabled(), id + " button must be disabled");
        soft.assertAll();
    }

    // ---------- 4. Error guessing: hostile and strange input ----------
    @Test
    public void tc14_sqlInjectionTextIsRejected() {
        open();
        enter("' OR '1'='1");
        Assert.assertFalse(buttonEnabled());
        Assert.assertFalse(driver.findElement(successMsg).isDisplayed());
    }

    @Test
    public void tc15_scriptTextShowsNoPopup() {
        open();
        enter("<script>alert(1)</script>");
        Assert.assertFalse(buttonEnabled());
        // if the text had run as a script, an alert would be open now
        try {
            driver.switchTo().alert();
            Assert.fail("An unexpected popup appeared");
        } catch (org.openqa.selenium.NoAlertPresentException expected) {
            // good: no popup
        }
    }

    @Test
    public void tc16_onlySpacesAreRejected() {
        open();
        enter("        ");
        Assert.assertEquals(ruleState("nospace"), "fail");
        Assert.assertFalse(buttonEnabled());
    }

    // ---------- 5. Confirm password ----------
    @Test
    public void tc17_mismatchShowsError() {
        open();
        enter("Abcd@1234", "Abcd@12345", "");
        Assert.assertEquals(driver.findElement(confirmError).getText(), "Passwords do not match");
        Assert.assertFalse(buttonEnabled());
    }

    @Test
    public void tc18_caseDifferenceIsMismatch() {
        open();
        enter("Abcd@1234", "abcd@1234", "");
        Assert.assertEquals(driver.findElement(confirmError).getText(), "Passwords do not match");
    }

    @Test
    public void tc19_emptyConfirmKeepsButtonDisabled() {
        open();
        enter("Abcd@1234", "", "");
        Assert.assertFalse(buttonEnabled());
    }

    // ---------- 6. State transitions ----------
    @Test
    public void tc20_validInvalidValidChangesButton() {
        open();
        enter("Abcd@1234");
        Assert.assertTrue(buttonEnabled());

        type(password, "abcd@1234");                         // breaks the uppercase rule
        Assert.assertFalse(buttonEnabled());

        type(password, "Abcd@1234");
        Assert.assertTrue(buttonEnabled());
    }

    @Test
    public void tc21_changingUsernameCanBreakValidPassword() {
        open();
        enter("Abcd@1234");
        Assert.assertTrue(buttonEnabled());
        type(username, "abcd");
        Assert.assertEquals(ruleState("nouser"), "fail");
        Assert.assertFalse(buttonEnabled());
    }

    // ---------- 7. UI behaviour ----------
    @Test
    public void tc22_showHideTogglesFieldType() {
        open();
        WebElement field = driver.findElement(password);
        Assert.assertEquals(field.getDomAttribute("type"), "password");
        driver.findElement(toggleBtn).click();
        Assert.assertEquals(field.getDomAttribute("type"), "text");
        Assert.assertEquals(driver.findElement(toggleBtn).getText(), "Hide");
        driver.findElement(toggleBtn).click();
        Assert.assertEquals(field.getDomAttribute("type"), "password");
    }
}
