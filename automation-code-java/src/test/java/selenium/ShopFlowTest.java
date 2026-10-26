package selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ShopFlowTest extends BaseTest {

    // Live practice page hosted on GitHub Pages (works from any computer with internet)
    private static final String PAGE_URL = "https://ubedzz49.github.io/qa-interview-prep/demo-pages/demo-shop.html";
    private static final String EMAIL = "qa@test.com";
    private static final String PASSWORD = "Test@1234";

    // ---------- Helper methods ----------
    private void type(String id, String text) {
        WebElement field = driver.findElement(By.id(id));
        field.clear();
        field.sendKeys(text);
    }

    private void click(String id) {
        driver.findElement(By.id(id)).click();
    }

    private String text(String id) {
        return driver.findElement(By.id(id)).getText();
    }

    private void login(String email, String password) {
        type("email", email);
        type("pass", password);
        click("loginBtn");
    }

    // "₹1530" -> 1530
    private int money(String id) {
        return Integer.parseInt(text(id).replace("₹", ""));
    }

    private int productCount() {
        return driver.findElements(By.cssSelector(".product")).size();
    }

    // Empty a text box by pressing Backspace (this fires the key events the page listens to)
    private void emptyBox(String id) {
        WebElement box = driver.findElement(By.id(id));
        int length = box.getAttribute("value").length();
        for (int i = 0; i < length; i++) {
            box.sendKeys(Keys.BACK_SPACE);
        }
    }

    // ---------- Login: negative cases ----------
    @DataProvider(name = "badLogins")
    public Object[][] badLogins() {
        return new Object[][]{
                {"both empty",             "",                 "",         "Email and password are required"},
                {"wrong password",         EMAIL,              "wrong",    "Invalid email or password"},
                {"unknown email",          "nobody@test.com",  PASSWORD,   "Invalid email or password"},
                {"password in wrong case", EMAIL,              "test@1234", "Invalid email or password"},
        };
    }

    @Test(dataProvider = "badLogins")
    public void loginFails(String why, String email, String password, String message) {
        driver.get(PAGE_URL);
        login(email, password);
        Assert.assertEquals(text("loginError"), message, why);
        Assert.assertFalse(driver.findElement(By.id("shopView")).isDisplayed());
    }

    // ---------- Complete end-to-end flow ----------
    @Test
    public void endToEndPurchase() {
        driver.get(PAGE_URL);

        // 1. Login with valid details
        login(EMAIL, PASSWORD);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("shopView")));
        Assert.assertEquals(text("welcome"), "Welcome, qa@test.com");
        Assert.assertEquals(productCount(), 3);

        // 2. Search and filter products
        type("search", "pen");
        Assert.assertEquals(productCount(), 1);
        Assert.assertEquals(driver.findElement(By.cssSelector("#product-2 .name")).getText(), "Pen Pack");

        emptyBox("search");
        type("search", "zzz");
        Assert.assertTrue(driver.findElement(By.id("noResults")).isDisplayed());

        emptyBox("search");
        Select category = new Select(driver.findElement(By.id("category")));
        category.selectByVisibleText("Bags");
        Assert.assertEquals(productCount(), 1);
        category.selectByValue("All");
        Assert.assertEquals(productCount(), 3);

        // 3. Add products to the cart
        click("add-1");                                  // Notebook
        click("add-1");                                  // Notebook again, so quantity = 2
        driver.findElement(By.xpath("//div[@id='product-3']//button")).click();   // Backpack, found with XPath
        Assert.assertEquals(text("cartCount"), "3");

        // 4. Check the cart and the maths
        click("cartLink");
        Assert.assertEquals(text("qty-1"), "2");
        Assert.assertEquals(driver.findElement(By.cssSelector("#cart-row-1 .line-total")).getText(), "₹500");
        Assert.assertEquals(money("subtotal"), 500 + 1200);
        Assert.assertEquals(money("discount"), 0);
        Assert.assertEquals(money("total"), 1700);

        // 5. Change quantity
        click("inc-1");
        Assert.assertEquals(text("qty-1"), "3");
        click("dec-1");
        Assert.assertEquals(text("qty-1"), "2");

        // 6. Coupon: invalid first, then valid
        type("couponInput", "ABC");
        click("applyCoupon");
        Assert.assertEquals(text("couponMsg"), "Invalid coupon code");
        Assert.assertEquals(money("discount"), 0);

        type("couponInput", "SAVE10");
        click("applyCoupon");
        Assert.assertEquals(text("couponMsg"), "Coupon SAVE10 applied: 10% off");
        Assert.assertEquals(money("discount"), 170);
        Assert.assertEquals(money("total"), 1530);

        // 7. Remove an item and the total updates
        click("rm-3");                                   // remove Backpack
        Assert.assertEquals(driver.findElements(By.id("cart-row-3")).size(), 0);
        Assert.assertEquals(money("subtotal"), 500);
        Assert.assertEquals(money("discount"), 50);
        Assert.assertEquals(money("total"), 450);

        // 8. Checkout: validation first
        click("checkoutBtn");
        click("placeOrder");
        Assert.assertEquals(text("checkoutError"), "Name and address are required");

        type("shipName", "Ravi Kumar");
        type("shipAddress", "12 MG Road, Bengaluru");
        type("pin", "123");
        click("placeOrder");
        Assert.assertEquals(text("checkoutError"), "Enter a valid 6 digit PIN code");

        type("pin", "560001");
        click("placeOrder");
        Assert.assertEquals(text("checkoutError"), "Select a payment method");

        // 9. Pay and confirm the order
        click("payUpi");
        click("placeOrder");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("doneView")));
        Assert.assertEquals(text("orderStatus"), "Order placed");
        Assert.assertTrue(text("orderId").matches("ORD-\\d{5}"), "Order id format");
        Assert.assertEquals(text("orderTotal"), "₹450");
        Assert.assertEquals(text("orderPay"), "UPI");
        Assert.assertEquals(text("cartCount"), "0");     // cart is emptied

        // 10. Logout returns to the login page
        click("logoutBtn");
        Assert.assertTrue(driver.findElement(By.id("loginView")).isDisplayed());
        Assert.assertFalse(driver.findElement(By.id("topNav")).isDisplayed());
    }

    // ---------- Empty cart rule ----------
    @Test
    public void checkoutIsDisabledForEmptyCart() {
        driver.get(PAGE_URL);
        login(EMAIL, PASSWORD);
        click("cartLink");
        Assert.assertTrue(driver.findElement(By.id("emptyCart")).isDisplayed());
        Assert.assertFalse(driver.findElement(By.id("checkoutBtn")).isEnabled());
    }
}
