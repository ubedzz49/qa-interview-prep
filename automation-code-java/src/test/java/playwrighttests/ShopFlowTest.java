package playwrighttests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.SelectOption;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class ShopFlowTest extends BaseTest {

    // Live practice page hosted on GitHub Pages (works from any computer with internet)
    private static final String PAGE_URL = "https://ubedzz49.github.io/qa-interview-prep/demo-pages/demo-shop.html";
    private static final String EMAIL = "qa@test.com";
    private static final String PASSWORD = "Test@1234";

    // ---------- Helper methods ----------
    private void login(String email, String password) {
        page.getByLabel("Email").fill(email);
        page.getByLabel("Password").fill(password);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();
    }

    // "₹1530" -> 1530
    private int money(String css) {
        return Integer.parseInt(page.locator(css).innerText().replace("₹", ""));
    }

    // ---------- Login: negative cases ----------
    @DataProvider(name = "badLogins")
    public Object[][] badLogins() {
        return new Object[][]{
                {"both empty",             "",                 "",          "Email and password are required"},
                {"wrong password",         EMAIL,              "wrong",     "Invalid email or password"},
                {"unknown email",          "nobody@test.com",  PASSWORD,    "Invalid email or password"},
                {"password in wrong case", EMAIL,              "test@1234", "Invalid email or password"},
        };
    }

    @Test(dataProvider = "badLogins")
    public void loginFails(String why, String email, String password, String message) {
        page.navigate(PAGE_URL);
        login(email, password);
        assertThat(page.locator("#loginError")).hasText(message);
        assertThat(page.locator("#shopView")).isHidden();
    }

    // ---------- Complete end-to-end flow ----------
    @Test
    public void endToEndPurchase() {
        page.navigate(PAGE_URL);

        // 1. Login with valid details
        login(EMAIL, PASSWORD);
        assertThat(page.locator("#welcome")).hasText("Welcome, qa@test.com");
        assertThat(page.locator(".product")).hasCount(3);

        // 2. Search and filter products
        Locator search = page.getByPlaceholder("Search products");
        search.fill("pen");
        assertThat(page.locator(".product")).hasCount(1);
        assertThat(page.locator("#product-2 .name")).hasText("Pen Pack");

        search.fill("zzz");
        assertThat(page.locator("#noResults")).isVisible();

        search.fill("");
        page.getByLabel("Category").selectOption("Bags");
        assertThat(page.locator(".product")).hasCount(1);
        page.getByLabel("Category").selectOption("All");
        assertThat(page.locator(".product")).hasCount(3);

        // 3. Add products to the cart
        page.locator("#add-1").click();                  // Notebook
        page.locator("#add-1").click();                  // Notebook again, so quantity = 2
        page.locator("#product-3").getByRole(AriaRole.BUTTON).click();    // Backpack: button inside its card
        assertThat(page.locator("#cartCount")).hasText("3");

        // 4. Check the cart and the maths
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(Pattern.compile("Cart"))).click();
        assertThat(page.locator("#qty-1")).hasText("2");
        assertThat(page.locator("#cart-row-1 .line-total")).hasText("₹500");
        Assert.assertEquals(money("#subtotal"), 500 + 1200);
        Assert.assertEquals(money("#discount"), 0);
        Assert.assertEquals(money("#total"), 1700);

        // 5. Change quantity
        page.locator("#inc-1").click();
        assertThat(page.locator("#qty-1")).hasText("3");
        page.locator("#dec-1").click();
        assertThat(page.locator("#qty-1")).hasText("2");

        // 6. Coupon: invalid first, then valid
        page.getByLabel("Coupon code").fill("ABC");
        page.locator("#applyCoupon").click();
        assertThat(page.locator("#couponMsg")).hasText("Invalid coupon code");
        Assert.assertEquals(money("#discount"), 0);

        page.getByLabel("Coupon code").fill("SAVE10");
        page.locator("#applyCoupon").click();
        assertThat(page.locator("#couponMsg")).hasText("Coupon SAVE10 applied: 10% off");
        Assert.assertEquals(money("#discount"), 170);
        Assert.assertEquals(money("#total"), 1530);

        // 7. Remove an item and the total updates
        page.locator("#rm-3").click();                   // remove Backpack
        assertThat(page.locator("#cart-row-3")).hasCount(0);
        Assert.assertEquals(money("#subtotal"), 500);
        Assert.assertEquals(money("#discount"), 50);
        Assert.assertEquals(money("#total"), 450);

        // 8. Checkout: validation first
        page.locator("#checkoutBtn").click();
        page.locator("#placeOrder").click();
        assertThat(page.locator("#checkoutError")).hasText("Name and address are required");

        page.getByLabel("Full name").fill("Ravi Kumar");
        page.getByLabel("Address").fill("12 MG Road, Bengaluru");
        page.getByLabel("PIN code").fill("123");
        page.locator("#placeOrder").click();
        assertThat(page.locator("#checkoutError")).hasText("Enter a valid 6 digit PIN code");

        page.getByLabel("PIN code").fill("560001");
        page.locator("#placeOrder").click();
        assertThat(page.locator("#checkoutError")).hasText("Select a payment method");

        // 9. Pay and confirm the order
        page.getByLabel("UPI").check();
        page.locator("#placeOrder").click();
        assertThat(page.locator("#orderStatus")).hasText("Order placed");
        assertThat(page.locator("#orderId")).hasText(Pattern.compile("^ORD-\\d{5}$"));
        assertThat(page.locator("#orderTotal")).hasText("₹450");
        assertThat(page.locator("#orderPay")).hasText("UPI");
        assertThat(page.locator("#cartCount")).hasText("0");          // cart is emptied

        // 10. Logout returns to the login page
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Logout")).click();
        assertThat(page.locator("#loginView")).isVisible();
        assertThat(page.locator("#topNav")).isHidden();
    }

    // ---------- Empty cart rule ----------
    @Test
    public void checkoutIsDisabledForEmptyCart() {
        page.navigate(PAGE_URL);
        login(EMAIL, PASSWORD);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(Pattern.compile("Cart"))).click();
        assertThat(page.locator("#emptyCart")).isVisible();
        assertThat(page.locator("#checkoutBtn")).isDisabled();
    }
}
