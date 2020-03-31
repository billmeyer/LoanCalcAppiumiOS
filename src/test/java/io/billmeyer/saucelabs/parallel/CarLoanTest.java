package io.billmeyer.saucelabs.parallel;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.InvalidElementStateException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.net.MalformedURLException;

public class CarLoanTest extends TestBase
{
    /**
     * Runs a simple test verifying if the comment input is functional.
     *
     * @throws InvalidElementStateException
     */
    @Test(dataProvider = "hardCodedDevices")
    public void calculateCarLoan(String platformName, String deviceName, String platformVersion, Method method)
    throws MalformedURLException
    {
        IOSDriver driver = createDriver(platformName, platformVersion, deviceName, method.getName());

        long time1, time2, time3, time4;

        time1 = System.currentTimeMillis();

        WebElement tfLoanAmount = driver.findElementByAccessibilityId("tfLoanAmount");
        WebElement tfInterest = driver.findElementByAccessibilityId("tfInterest");
        WebElement tfSalesTax = driver.findElementByAccessibilityId("tfSalesTax");
        WebElement tfTerm = driver.findElementByAccessibilityId("tfTerm");
        WebElement tfDownPayment = driver.findElementByAccessibilityId("tfDownPayment");
        WebElement tfTradeIn = driver.findElementByAccessibilityId("tfTradeIn");
        WebElement tfFees = driver.findElementByAccessibilityId("tfFees");
        WebElement lblMonthlyPayment = driver.findElementByAccessibilityId("lblMonthlyPayment");
        WebElement lblTotalPayments = driver.findElementByAccessibilityId("lblTotalPayments");
        WebElement lblTotalInterest = driver.findElementByAccessibilityId("lblTotalInterest");
        WebElement lblTotalCost = driver.findElementByAccessibilityId("lblTotalCost");

        time2 = System.currentTimeMillis();

        // Set the input values for our loan calculation...
        tfLoanAmount.sendKeys("25000");
        tfInterest.sendKeys("3.42");
        tfSalesTax.sendKeys("8");
        tfTerm.sendKeys("60");
        tfDownPayment.sendKeys("500");
        tfTradeIn.sendKeys("7500");
        tfFees.sendKeys("300");

        if (unifiedPlatformTesting == false)
        {
            driver.getScreenshotAs(OutputType.FILE);
        }

        time3 = System.currentTimeMillis();

        // Check if within given time the correct result appears in the designated field.
        ExpectedCondition<Boolean> expected;
        expected = ExpectedConditions.textToBePresentInElement(lblMonthlyPayment, "339.52");
        expected = ExpectedConditions.textToBePresentInElement(lblTotalPayments, "20,370.97");
        expected = ExpectedConditions.textToBePresentInElement(lblTotalInterest, "1,670.97");
        expected = ExpectedConditions.textToBePresentInElement(lblTotalCost, "28,370.97");

        time4 = System.currentTimeMillis();

        System.out.printf("Locating elements took %.2f secs\n", (time2-time1)/1000f);
        System.out.printf("Populating elements took %.2f secs\n", (time3-time2)/1000f);
        System.out.printf("Asserting results took %.2f secs\n", (time4-time3)/1000f);
        System.out.printf("Total test execution took %.2f secs\n", (time4-time1)/1000f);

        WebDriverWait wait = new WebDriverWait(driver, 30);
        try
        {
            wait.until(expected);
        }
        catch (Throwable t)
        {
            System.err.println("Expected Condition Not Met: " + t.getMessage());
        }
    }
}