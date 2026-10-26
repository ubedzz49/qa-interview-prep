package selenium;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FormTest extends BaseTest {

    // Live practice page hosted on GitHub Pages (works from any computer with internet)
    private static final String PAGE_URL = "https://ubedzz49.github.io/qa-interview-prep/demo-pages/demo-form.html";
    private static final int MB = 1024 * 1024;

    // ---------- Helper methods ----------
    private void open() {
        driver.get(PAGE_URL);
    }

    private void type(String id, String text) {
        WebElement field = driver.findElement(By.id(id));
        field.clear();
        field.sendKeys(text);
    }

    private void clickSubmit() {
        driver.findElement(By.id("submitBtn")).click();
    }

    private String error(String field) {
        return driver.findElement(By.id("err-" + field)).getText();
    }

    // Date and range inputs are awkward to type into, so we set them with JavaScript
    private void setValueWithJs(String id, String value) {
        WebElement el = driver.findElement(By.id(id));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1];" +
                "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));", el, value);
    }

    // Create a fake file of a given name and size, return its full path
    private String makeFile(String name, int sizeInBytes) throws IOException {
        Path folder = Files.createTempDirectory("upload");
        Path file = folder.resolve(name);
        Files.write(file, new byte[sizeInBytes]);
        return file.toString();
    }

    // ---------- 1. Initial state ----------
    @Test
    public void tc01_initialState() {
        open();
        Assert.assertFalse(driver.findElement(By.id("state")).isEnabled());
        Assert.assertEquals(driver.findElement(By.id("expOut")).getText(), "0");
        Assert.assertEquals(driver.findElement(By.id("addrCount")).getText(), "0");
        Assert.assertEquals(driver.findElements(By.cssSelector("input[name='gender']:checked")).size(), 0);
        Assert.assertFalse(driver.findElement(By.id("terms")).isSelected());
        Assert.assertFalse(driver.findElement(By.id("result")).isDisplayed());
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
            Assert.assertEquals(error(row[0]), row[1], "Field " + row[0]);
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
        type(field, value);
        clickSubmit();
        Assert.assertEquals(error(field), message, why);
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
        setValueWithJs("dob", date);
        clickSubmit();
        Assert.assertEquals(error("dob"), message, why);
    }

    // ---------- 5. Radio buttons ----------
    @Test
    public void tc05_onlyOneRadioCanBeSelected() {
        open();
        driver.findElement(By.id("genderFemale")).click();
        Assert.assertTrue(driver.findElement(By.id("genderFemale")).isSelected());
        driver.findElement(By.id("genderOther")).click();
        Assert.assertTrue(driver.findElement(By.id("genderOther")).isSelected());
        Assert.assertFalse(driver.findElement(By.id("genderFemale")).isSelected());
        Assert.assertEquals(driver.findElements(By.cssSelector("input[name='gender']:checked")).size(), 1);
    }

    // ---------- 6. Dropdowns: single and dependent ----------
    private List<String> optionTexts(Select select) {
        return select.getOptions().stream().map(WebElement::getText).collect(Collectors.toList());
    }

    @Test
    public void tc06_countryChangesStateList() {
        open();
        Select country = new Select(driver.findElement(By.id("country")));
        WebElement stateElement = driver.findElement(By.id("state"));

        country.selectByVisibleText("India");
        Assert.assertTrue(stateElement.isEnabled());
        Assert.assertEquals(optionTexts(new Select(stateElement)),
                Arrays.asList("-- Select state --", "Karnataka", "Maharashtra", "Delhi"));

        country.selectByValue("USA");
        Assert.assertEquals(optionTexts(new Select(stateElement)),
                Arrays.asList("-- Select state --", "California", "Texas"));

        country.selectByIndex(0);                            // back to "-- Select country --"
        Assert.assertFalse(stateElement.isEnabled());
    }

    @Test
    public void tc07_stateIsRequiredAfterChoosingCountry() {
        open();
        new Select(driver.findElement(By.id("country"))).selectByValue("UK");
        clickSubmit();
        Assert.assertEquals(error("state"), "Select a state");
    }

    // ---------- 7. Multi-select ----------
    @Test
    public void tc08_multiSelectAllowsOneToThreeSkills() {
        open();
        Select skills = new Select(driver.findElement(By.id("skills")));
        Assert.assertTrue(skills.isMultiple());

        skills.selectByValue("Selenium");
        skills.selectByValue("Git");
        clickSubmit();
        Assert.assertEquals(error("skills"), "");

        skills.selectByValue("Playwright");
        skills.selectByValue("SQL");                         // now 4 selected = max + 1
        clickSubmit();
        Assert.assertEquals(error("skills"), "Select at most 3 skills");

        skills.deselectAll();
        clickSubmit();
        Assert.assertEquals(error("skills"), "Select at least one skill");
    }

    // ---------- 8. Checkboxes ----------
    @Test
    public void tc09_languageCheckboxes() {
        open();
        driver.findElement(By.id("langEnglish")).click();
        driver.findElement(By.id("langTelugu")).click();
        Assert.assertEquals(driver.findElements(By.cssSelector("input[name='lang']:checked")).size(), 2);
        driver.findElement(By.id("langEnglish")).click();      // click again = untick
        Assert.assertEquals(driver.findElements(By.cssSelector("input[name='lang']:checked")).size(), 1);
    }

    // ---------- 9. Slider ----------
    @Test
    public void tc10_sliderShowsItsValue() {
        open();
        setValueWithJs("experience", "12");
        Assert.assertEquals(driver.findElement(By.id("expOut")).getText(), "12");
        setValueWithJs("experience", "20");                    // max
        Assert.assertEquals(driver.findElement(By.id("expOut")).getText(), "20");
    }

    // ---------- 10. Textarea ----------
    @Test
    public void tc11_textareaCounterAndLimit() {
        open();
        WebElement box = driver.findElement(By.id("address"));
        box.sendKeys("Hello");
        Assert.assertEquals(driver.findElement(By.id("addrCount")).getText(), "5");

        box.clear();
        box.sendKeys("x".repeat(205));                         // we type 205, the browser stops at 200
        Assert.assertEquals(box.getAttribute("value").length(), 200);
        Assert.assertEquals(driver.findElement(By.id("addrCount")).getText(), "200");
    }

    @Test
    public void tc12_shortAddressIsRejected() {
        open();
        type("address", "Short");
        clickSubmit();
        Assert.assertEquals(error("address"), "Address must be at least 10 characters");
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
    public void tc13_resumeUpload(String why, String name, int size, String message) throws IOException {
        open();
        // For a file input we do NOT click. We send the file path as text.
        driver.findElement(By.id("resume")).sendKeys(makeFile(name, size));
        clickSubmit();
        Assert.assertEquals(error("resume"), message, why);
    }

    // ---------- 12. Terms checkbox ----------
    @Test
    public void tc14_termsMustBeAccepted() {
        open();
        clickSubmit();
        Assert.assertEquals(error("terms"), "You must accept the terms");
        driver.findElement(By.id("terms")).click();
        clickSubmit();
        Assert.assertEquals(error("terms"), "");
    }

    // ---------- 13. Happy path: whole form, plus the alert popup ----------
    @Test
    public void tc15_validFormSubmitsAndShowsResult() throws IOException {
        open();
        type("fullName", "Ravi Kumar");
        type("email", "ravi@example.com");
        type("phone", "9876543210");
        setValueWithJs("dob", "1998-05-20");
        driver.findElement(By.id("genderMale")).click();

        new Select(driver.findElement(By.id("country"))).selectByVisibleText("India");
        new Select(driver.findElement(By.id("state"))).selectByVisibleText("Karnataka");

        Select skills = new Select(driver.findElement(By.id("skills")));
        skills.selectByValue("Selenium");
        skills.selectByValue("Playwright");

        driver.findElement(By.id("langEnglish")).click();
        driver.findElement(By.id("langHindi")).click();
        setValueWithJs("experience", "2");
        type("address", "12 MG Road, Bengaluru");
        driver.findElement(By.id("resume")).sendKeys(makeFile("ravi.pdf", 500));
        driver.findElement(By.id("terms")).click();

        clickSubmit();

        // The page shows an alert popup. We must handle it before doing anything else.
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        Assert.assertEquals(alert.getText(), "Application submitted successfully");
        alert.accept();

        String json = driver.findElement(By.id("result")).getText();
        Assert.assertTrue(json.contains("\"fullName\": \"Ravi Kumar\""));
        Assert.assertTrue(json.contains("\"phone\": \"9876543210\""));
        Assert.assertTrue(json.contains("\"state\": \"Karnataka\""));
        Assert.assertTrue(json.contains("\"experience\": 2"));
        Assert.assertTrue(json.contains("\"resume\": \"ravi.pdf\""));
    }

    // ---------- 14. Reset button and the confirm popup ----------
    @Test
    public void tc16_resetAsksForConfirmation() {
        open();
        type("fullName", "Ravi Kumar");
        new Select(driver.findElement(By.id("country"))).selectByValue("USA");

        driver.findElement(By.id("resetBtn")).click();
        Alert confirm = wait.until(ExpectedConditions.alertIsPresent());
        Assert.assertEquals(confirm.getText(), "Clear the whole form?");
        confirm.dismiss();                                     // press Cancel
        Assert.assertEquals(driver.findElement(By.id("fullName")).getAttribute("value"), "Ravi Kumar");

        driver.findElement(By.id("resetBtn")).click();
        wait.until(ExpectedConditions.alertIsPresent()).accept();   // press OK
        Assert.assertEquals(driver.findElement(By.id("fullName")).getAttribute("value"), "");
        Assert.assertFalse(driver.findElement(By.id("state")).isEnabled());
    }
}
