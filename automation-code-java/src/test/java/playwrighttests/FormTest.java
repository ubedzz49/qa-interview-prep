package playwrighttests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.FilePayload;
import com.microsoft.playwright.options.SelectOption;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class FormTest extends BaseTest {

    // Live practice page hosted on GitHub Pages (works from any computer with internet)
    private static final String PAGE_URL = "https://ubedzz49.github.io/qa-interview-prep/demo-pages/demo-form.html";
    private static final int MB = 1024 * 1024;

    // ---------- Helper methods ----------
    private void open() {
        page.navigate(PAGE_URL);
    }

    private void clickSubmit() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit")).click();
    }

    private Locator error(String field) {
        return page.locator("#err-" + field);
    }

    // ---------- 1. Initial state ----------
    @Test
    public void tc01_initialState() {
        open();
        assertThat(page.locator("#state")).isDisabled();
        assertThat(page.locator("#expOut")).hasText("0");
        assertThat(page.locator("#addrCount")).hasText("0");
        assertThat(page.locator("input[name='gender']:checked")).hasCount(0);
        assertThat(page.locator("#terms")).not().isChecked();
        assertThat(page.locator("#result")).isHidden();
    }

    // ---------- 2. Empty form: every required field shows an error ----------
    @Test
    public void tc02_emptyFormShowsAllErrors() {
        open();
        clickSubmit();
        String[][] expected = {
                {"fullName", "Full name is required"},
                {"email", "Email is required"},
                {"phone", "Mobile number is required"},
                {"dob", "Date of birth is required"},
                {"gender", "Select a gender"},
                {"country", "Select a country"},
                {"skills", "Select at least one skill"},
                {"lang", "Select at least one language"},
                {"address", "Address is required"},
                {"resume", "Upload your resume"},
                {"terms", "You must accept the terms"},
        };
        for (String[] row : expected) {
            assertThat(error(row[0])).hasText(row[1]);
        }
    }

    // ---------- 3. Text inputs: data-driven validation ----------
    @DataProvider(name = "textCases")
    public Object[][] textCases() {
        String phoneMsg = "Enter a 10 digit number starting with 6, 7, 8 or 9";
        String nameLenMsg = "Name must be 2 to 40 characters";
        return new Object[][]{
                // field,      value,              expected message,  why
                {"fullName", "A",                 nameLenMsg, "Boundary: 1 char (min - 1)"},
                {"fullName", "Al",                "",         "Boundary: 2 chars (min)"},
                {"fullName", "A".repeat(40),      "",         "Boundary: 40 chars (max)"},
                {"fullName", "A".repeat(41),      nameLenMsg, "Boundary: 41 chars (max + 1)"},
                {"fullName", "John3",             "Name can have only letters, space, dot, hyphen and apostrophe", "Invalid: digit in name"},
                {"fullName", "Mary-Ann O'Neil",   "",         "Valid special characters"},
                {"email",    "abc",               "Enter a valid email address", "Invalid: no @"},
                {"email",    "abc@mail",          "Enter a valid email address", "Invalid: no domain extension"},
                {"email",    "a b@mail.com",      "Enter a valid email address", "Invalid: space"},
                {"email",    "user@mail.com",     "",         "Valid email"},
                {"phone",    "12345",             phoneMsg,   "Invalid: too short"},
                {"phone",    "5876543210",        phoneMsg,   "Invalid: starts with 5"},
                {"phone",    "98765432101",       phoneMsg,   "Boundary: 11 digits"},
                {"phone",    "abcdefghij",        phoneMsg,   "Invalid: letters"},
                {"phone",    "9876543210",        "",         "Valid mobile number"},
        };
    }

    @Test(dataProvider = "textCases")
    public void tc03_textFieldValidation(String field, String value, String message, String why) {
        open();
        page.locator("#" + field).fill(value);
        clickSubmit();
        assertThat(error(field)).hasText(message);
    }

    // ---------- 4. Date of birth: boundary around 18 years ----------
    @DataProvider(name = "dobCases")
    public Object[][] dobCases() {
        LocalDate today = LocalDate.now();
        return new Object[][]{
                {"exactly 18 years today (valid)",  today.minusYears(18).toString(),            ""},
                {"turns 18 tomorrow (invalid)",     today.minusYears(18).plusDays(1).toString(), "You must be at least 18 years old"},
                {"18 years and 1 day (valid)",      today.minusYears(18).minusDays(1).toString(), ""},
                {"tomorrow (future date)",          today.plusDays(1).toString(),               "Date of birth cannot be in the future"},
        };
    }

    @Test(dataProvider = "dobCases")
    public void tc04_dateOfBirth(String why, String date, String message) {
        open();
        page.locator("#dob").fill(date);                    // fill() works directly on date inputs
        clickSubmit();
        assertThat(error("dob")).hasText(message);
    }

    // ---------- 5. Radio buttons ----------
    @Test
    public void tc05_onlyOneRadioCanBeSelected() {
        open();
        page.getByLabel("Female").check();
        assertThat(page.getByLabel("Female")).isChecked();
        page.getByLabel("Other").check();
        assertThat(page.getByLabel("Other")).isChecked();
        assertThat(page.getByLabel("Female")).not().isChecked();
        assertThat(page.locator("input[name='gender']:checked")).hasCount(1);
    }

    // ---------- 6. Dropdowns: single and dependent ----------
    @Test
    public void tc06_countryChangesStateList() {
        open();
        Locator state = page.locator("#state");

        page.locator("#country").selectOption("India");
        assertThat(state).isEnabled();
        assertThat(state.locator("option")).hasText(
                new String[]{"-- Select state --", "Karnataka", "Maharashtra", "Delhi"});

        page.locator("#country").selectOption(new SelectOption().setLabel("USA"));    // by visible text
        assertThat(state.locator("option")).hasText(
                new String[]{"-- Select state --", "California", "Texas"});

        page.locator("#country").selectOption(new SelectOption().setIndex(0));        // back to first option
        assertThat(state).isDisabled();
    }

    @Test
    public void tc07_stateIsRequiredAfterChoosingCountry() {
        open();
        page.locator("#country").selectOption("UK");
        clickSubmit();
        assertThat(error("state")).hasText("Select a state");
    }

    // ---------- 7. Multi-select ----------
    @Test
    public void tc08_multiSelectAllowsOneToThreeSkills() {
        open();
        Locator skills = page.locator("#skills");

        skills.selectOption(new String[]{"Selenium", "Git"});
        clickSubmit();
        assertThat(error("skills")).hasText("");

        skills.selectOption(new String[]{"Selenium", "Playwright", "SQL", "Git"});   // 4 = max + 1
        clickSubmit();
        assertThat(error("skills")).hasText("Select at most 3 skills");

        skills.selectOption(new String[]{});                                         // clear all
        clickSubmit();
        assertThat(error("skills")).hasText("Select at least one skill");
    }

    // ---------- 8. Checkboxes ----------
    @Test
    public void tc09_languageCheckboxes() {
        open();
        page.getByLabel("English").check();
        page.getByLabel("Telugu").check();
        assertThat(page.locator("input[name='lang']:checked")).hasCount(2);
        page.getByLabel("English").uncheck();
        assertThat(page.locator("input[name='lang']:checked")).hasCount(1);
    }

    // ---------- 9. Slider ----------
    @Test
    public void tc10_sliderShowsItsValue() {
        open();
        page.locator("#experience").fill("12");
        assertThat(page.locator("#expOut")).hasText("12");
        page.locator("#experience").fill("20");               // max
        assertThat(page.locator("#expOut")).hasText("20");
    }

    // ---------- 10. Textarea ----------
    @Test
    public void tc11_textareaCounterAndLimit() {
        open();
        Locator box = page.locator("#address");
        box.pressSequentially("Hello");
        assertThat(page.locator("#addrCount")).hasText("5");

        box.fill("");
        box.pressSequentially("x".repeat(205));               // we type 205, the browser stops at 200
        assertThat(page.locator("#addrCount")).hasText("200");
    }

    @Test
    public void tc12_shortAddressIsRejected() {
        open();
        page.locator("#address").fill("Short");
        clickSubmit();
        assertThat(error("address")).hasText("Address must be at least 10 characters");
    }

    // ---------- 11. File upload ----------
    @DataProvider(name = "fileCases")
    public Object[][] fileCases() {
        return new Object[][]{
                // why,                      file name,      size,        expected message
                {"valid PDF",               "resume.pdf",  1000,        ""},
                {"wrong type (png)",        "photo.png",   1000,        "Only PDF, DOC or DOCX files are allowed"},
                {"exactly 2 MB (max)",      "big.pdf",     2 * MB,      ""},
                {"2 MB + 1 byte (max + 1)", "bigger.pdf",  2 * MB + 1,  "File must be 2 MB or smaller"},
                {"empty file",              "empty.pdf",   0,           "File is empty"},
                {"double extension",        "cv.pdf.exe",  1000,        "Only PDF, DOC or DOCX files are allowed"},
        };
    }

    @Test(dataProvider = "fileCases")
    public void tc13_resumeUpload(String why, String name, int size, String message) {
        open();
        // Playwright can upload a file made in memory. No real file is needed on disk.
        page.locator("#resume").setInputFiles(new FilePayload(name, "application/octet-stream", new byte[size]));
        clickSubmit();
        assertThat(error("resume")).hasText(message);
    }

    // ---------- 12. Terms checkbox ----------
    @Test
    public void tc14_termsMustBeAccepted() {
        open();
        clickSubmit();
        assertThat(error("terms")).hasText("You must accept the terms");
        page.locator("#terms").check();
        clickSubmit();
        assertThat(error("terms")).hasText("");
    }

    // ---------- 13. Happy path: whole form, plus the alert popup ----------
    @Test
    public void tc15_validFormSubmitsAndShowsResult() {
        open();
        page.getByLabel("Full name").fill("Ravi Kumar");
        page.getByLabel("Email").fill("ravi@example.com");
        page.getByLabel("Mobile number").fill("9876543210");
        page.getByLabel("Date of birth").fill("1998-05-20");
        page.getByLabel("Male", new Page.GetByLabelOptions().setExact(true)).check();
        page.getByLabel("Country").selectOption("India");
        page.getByLabel("State").selectOption("Karnataka");
        page.getByLabel(Pattern.compile("Skills")).selectOption(new String[]{"Selenium", "Playwright"});
        page.getByLabel("English").check();
        page.getByLabel("Hindi").check();
        page.getByLabel(Pattern.compile("Experience in years")).fill("2");
        page.getByLabel("Address").fill("12 MG Road, Bengaluru");
        page.getByLabel(Pattern.compile("Resume")).setInputFiles(
                new FilePayload("ravi.pdf", "application/pdf", "dummy pdf".getBytes()));
        page.getByLabel("I agree to the terms and conditions").check();

        // The page shows an alert popup after a good submit. Listen BEFORE clicking.
        AtomicReference<String> popupText = new AtomicReference<>("");
        page.onceDialog(dialog -> {
            popupText.set(dialog.message());
            dialog.accept();
        });
        clickSubmit();

        Assert.assertEquals(popupText.get(), "Application submitted successfully");
        Locator result = page.locator("#result");
        assertThat(result).containsText("\"fullName\": \"Ravi Kumar\"");
        assertThat(result).containsText("\"phone\": \"9876543210\"");
        assertThat(result).containsText("\"state\": \"Karnataka\"");
        assertThat(result).containsText("\"experience\": 2");
        assertThat(result).containsText("\"resume\": \"ravi.pdf\"");
    }

    // ---------- 14. Reset button and the confirm popup ----------
    @Test
    public void tc16_resetAsksForConfirmation() {
        open();
        page.getByLabel("Full name").fill("Ravi Kumar");
        page.locator("#country").selectOption("USA");

        page.onceDialog(dialog -> dialog.dismiss());                 // press Cancel
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Reset")).click();
        assertThat(page.getByLabel("Full name")).hasValue("Ravi Kumar");

        page.onceDialog(dialog -> dialog.accept());                  // press OK
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Reset")).click();
        assertThat(page.getByLabel("Full name")).hasValue("");
        assertThat(page.locator("#state")).isDisabled();
    }
}
