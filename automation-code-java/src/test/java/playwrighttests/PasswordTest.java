package playwrighttests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class PasswordTest extends BaseTest {

    // Live practice page hosted on GitHub Pages (works from any computer with internet)
    private static final String PAGE_URL = "https://ubedzz49.github.io/qa-interview-prep/demo-pages/demo-password.html";

    private static final String[] ALL_RULES =
            {"length", "upper", "lower", "number", "special", "nospace", "nouser"};

    // ---------- Helper methods ----------
    private Locator passwordBox() {
        return page.getByLabel("Password", new Page.GetByLabelOptions().setExact(true));
    }

    private void enter(String pwd, String confirm, String user) {
        page.getByLabel("Username").fill(user);
        passwordBox().fill(pwd);
        page.getByLabel("Confirm password").fill(confirm);
    }

    private void enter(String pwd) {
        enter(pwd, pwd, "");
    }

    private void expectRule(String rule, String state) {
        assertThat(page.locator("#rule-" + rule)).hasAttribute("data-state", state);
    }

    private void open() {
        page.navigate(PAGE_URL);
    }

    // ---------- 1. Initial state ----------
    @Test
    public void tc01_initialState() {
        open();
        assertThat(page.locator("#submitBtn")).isDisabled();
        assertThat(page.locator("#strength")).hasText("Not entered");
        for (String rule : ALL_RULES) {
            expectRule(rule, "pending");
        }
        assertThat(page.locator("#pwdError")).isEmpty();
        assertThat(page.locator("#successMsg")).isHidden();
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

        for (String rule : ALL_RULES) {
            expectRule(rule, "pass");
        }
        assertThat(page.locator("#strength")).hasText(expectedStrength);
        assertThat(page.locator("#pwdError")).isEmpty();
        assertThat(page.locator("#submitBtn")).isEnabled();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create password")).click();
        assertThat(page.locator("#successMsg")).hasText("Password created successfully");
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

        for (String rule : ALL_RULES) {
            expectRule(rule, rule.equals(broken) ? "fail" : "pass");
        }
        assertThat(page.locator("#strength")).hasText("Weak");
        assertThat(page.locator("#pwdError")).hasText("Password does not meet all the rules");
        assertThat(page.locator("#submitBtn")).isDisabled();
    }

    // ---------- 4. Error guessing: hostile and strange input ----------
    @Test
    public void tc14_sqlInjectionTextIsRejected() {
        open();
        enter("' OR '1'='1");
        assertThat(page.locator("#submitBtn")).isDisabled();
        assertThat(page.locator("#successMsg")).isHidden();
    }

    @Test
    public void tc15_scriptTextShowsNoPopup() {
        open();
        AtomicBoolean popupSeen = new AtomicBoolean(false);
        page.onDialog(dialog -> {            // runs only if a popup appears
            popupSeen.set(true);
            dialog.dismiss();
        });
        enter("<script>alert(1)</script>");
        assertThat(page.locator("#submitBtn")).isDisabled();
        Assert.assertFalse(popupSeen.get(), "An unexpected popup appeared");
    }

    @Test
    public void tc16_onlySpacesAreRejected() {
        open();
        enter("        ");
        expectRule("nospace", "fail");
        assertThat(page.locator("#submitBtn")).isDisabled();
    }

    // ---------- 5. Confirm password ----------
    @Test
    public void tc17_mismatchShowsError() {
        open();
        enter("Abcd@1234", "Abcd@12345", "");
        assertThat(page.locator("#confirmError")).hasText("Passwords do not match");
        assertThat(page.locator("#submitBtn")).isDisabled();
    }

    @Test
    public void tc18_caseDifferenceIsMismatch() {
        open();
        enter("Abcd@1234", "abcd@1234", "");
        assertThat(page.locator("#confirmError")).hasText("Passwords do not match");
    }

    @Test
    public void tc19_emptyConfirmKeepsButtonDisabled() {
        open();
        enter("Abcd@1234", "", "");
        assertThat(page.locator("#submitBtn")).isDisabled();
    }

    // ---------- 6. State transitions ----------
    @Test
    public void tc20_validInvalidValidChangesButton() {
        open();
        Locator button = page.locator("#submitBtn");
        enter("Abcd@1234");
        assertThat(button).isEnabled();

        passwordBox().fill("abcd@1234");                      // breaks the uppercase rule
        assertThat(button).isDisabled();

        passwordBox().fill("Abcd@1234");
        assertThat(button).isEnabled();
    }

    @Test
    public void tc21_changingUsernameCanBreakValidPassword() {
        open();
        enter("Abcd@1234");
        assertThat(page.locator("#submitBtn")).isEnabled();
        page.getByLabel("Username").fill("abcd");
        expectRule("nouser", "fail");
        assertThat(page.locator("#submitBtn")).isDisabled();
    }

    // ---------- 7. UI behaviour ----------
    @Test
    public void tc22_showHideTogglesFieldType() {
        open();
        Locator toggle = page.locator("#togglePwd");
        assertThat(passwordBox()).hasAttribute("type", "password");
        toggle.click();
        assertThat(passwordBox()).hasAttribute("type", "text");
        assertThat(toggle).hasText("Hide");
        toggle.click();
        assertThat(passwordBox()).hasAttribute("type", "password");
    }
}
