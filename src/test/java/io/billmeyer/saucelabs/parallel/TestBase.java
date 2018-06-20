package io.billmeyer.saucelabs.parallel;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.Connection;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Date;

/**
 * Simple TestNG test which demonstrates being instantiated via a DataProvider in order to supply multiple browser combinations.
 *
 * @author Bill Meyer
 */
public class TestBase
{
    protected static final boolean realDeviceTesting = true;

    protected static final String testobjectApiKey = System.getenv("TO_LOANCALC_APP");
    protected static final String userName = System.getenv("SAUCE_USERNAME");
    protected static final String accessKey = System.getenv("SAUCE_ACCESS_KEY");

    /**
     * ThreadLocal variable which contains the  {@link WebDriver} instance which is used to perform browser interactions with.
     */
    private ThreadLocal<AndroidDriver> androidDriverThreadLocal = new ThreadLocal<AndroidDriver>();

    /**
     * DataProvider that explicitly sets the browser combinations to be used.
     *
     * @param testMethod
     * @return Two dimensional array of objects with browser, version, and platform information
     */
    @DataProvider(name = "hardCodedDevices", parallel = true)
    public static Object[][] sauceDeviceDataProvider(Method testMethod)
    {
        /**
         * Create an array of test OS/Browser/Screen Resolution combinations we want to test on.
         * @see https://wiki.saucelabs.com/display/DOCS/Test+Configuration+Options#TestConfigurationOptions-SpecifyingtheScreenResolution
         */

        // @formatter:off
        if (realDeviceTesting == true)
        {
            return new Object[][]{
//                    new Object[]{"Android", "LG Nexus 5X", "8.1"},
                    new Object[]{"Android", "LG G6", "7"}
//                    new Object[]{"Android", "Samsung Galaxy S6", "6.0.1"}
//                    new Object[]{"Android", "Samsung Galaxy S9", "8.0"},
//                    new Object[]{"Android", "Samsung Note 5", "7.0"},
//                    new Object[]{"Android", "Google Pixel", "7"},
//                    new Object[]{"Android", "Google Pixel 2 XL", "8.1"}
            };
        }
        else
        {
            return new Object[][]{
//                    new Object[]{"Android", "Android GoogleAPI Emulator", "7.1"},
//                    new Object[]{"Android", "Android GoogleAPI Emulator", "7"},
//                    new Object[]{"Android", "Android GoogleAPI Emulator", "6"}
                    new Object[]{"Android", "Google Nexus 6P", "8.1"}
            };
        }
        // @formatter:on
    }

    protected void annotateJob(String text)
    {
        /**
         * Example of using the JavascriptExecutor to annotate the job execution as it runs
         *
         * @see https://wiki.saucelabs.com/display/DOCS/Annotating+Tests+with+Selenium%27s+JavaScript+Executor
         */

        androidDriverThreadLocal.get().executeScript("sauce:context=" + text);
    }

    /**
     * Constructs a new {@link RemoteWebDriver} instance which is configured to use the capabilities defined by the platformName,
     * platformVersion and deviceName parameters, and which is configured to run against ondemand.saucelabs.com, using
     * the userName and access key populated by the authentication instance.
     *
     * @param platformName    Represents the platformName to be used as part of the test run.
     * @param platformVersion Represents the platformVersion of the platformName to be used as part of the test run.
     * @param deviceName      Represents the operating system to be used as part of the test run.
     * @param methodName      Represents the name of the test case that will be used to identify the test on Sauce.
     * @return
     * @throws MalformedURLException if an error occurs parsing the url
     */
    protected AndroidDriver createDriver(String platformName, String platformVersion, String deviceName, String methodName)
    throws MalformedURLException
    {
        URL url = null;
        DesiredCapabilities caps = new DesiredCapabilities();

        // set desired capabilities to launch appropriate platformName on Sauce
        // For real device testing, connect to one URL using a certain set of credentials...
        if (realDeviceTesting == true)
        {
            url = new URL("http://us1.appium.testobject.com/wd/hub");
            caps.setCapability("testobject_api_key", testobjectApiKey);
        }
        // For emulator/simulator testing, connect to a different URL using a different certain set of credentials...
        else
        {
            url = new URL("http://localhost:4723/wd/hub");
//            url = new URL("https://" + userName + ":" + accessKey + "@ondemand.saucelabs.com:443/wd/hub");
            caps.setCapability("app", "https://raw.githubusercontent.com/billmeyer/LoanCalcAppiumTest/master/app-release.apk");
            caps.setCapability("automationName", "uiautomator2");
        }

        caps.setCapability("platformName", platformName);
        caps.setCapability("platformVersion", platformVersion);
        caps.setCapability("deviceName", deviceName);
        caps.setCapability("name", String.format("%s - %s %s [%s]", methodName, platformName, platformVersion, new Date()));
//        caps.setCapability("appiumVersion", "1.8.0");

        // Launch the remote platformName and set it as the current thread
        AndroidDriver driver = new AndroidDriver(url, caps);

        // turn on all (data and wi-fi)
//        driver.setConnection(Connection.ALL);

        // turn off all (data and wi-fi)
//        driver.setConnection(Connection.NONE);

        // turn on airplane
//        driver.setConnection(Connection.AIRPLANE);

        // turn on data
//        driver.setConnection(Connection.DATA);

        // turn on wi-fi
//        driver.setConnection(Connection.WIFI);

        androidDriverThreadLocal.set(driver);

        return androidDriverThreadLocal.get();
    }

    /**
     * Method that gets invoked after test.
     * Sets the job status (PASS or FAIL) and closes the browser.
     */
    @AfterMethod
    public void tearDown(ITestResult result)
    throws Exception
    {
        AndroidDriver driver = androidDriverThreadLocal.get();

        String sessionId = driver.getSessionId().toString();
        boolean success = result.isSuccess();

        if (realDeviceTesting == true)
        {
            reportTestResult(sessionId, success);
        }
        driver.quit();
    }

    /**
     * Uses the Appium V2 RESTful API to report test result status to the Sauce Labs dashboard.
     *
     * @param sessionId The session ID we want to set the status for
     * @param status    TRUE if the test was successful, FALSE otherwise
     * @see https://api.testobject.com/#!/Appium_Watcher_API/updateTest
     */
    public void reportTestResult(String sessionId, boolean status)
    {
        // The Appium REST Api expects JSON payloads...
        MediaType[] mediaType = new MediaType[]{MediaType.APPLICATION_JSON_TYPE};

        // Construct the new REST client...
        Client client = ClientBuilder.newClient();
        WebTarget resource = client.target("https://app.testobject.com/api/rest/v2/appium");

        // Construct the REST body payload...
        Entity entity = Entity.json(Collections.singletonMap("passed", status));

        // Build a PUT request to /v2/appium/session/{:sessionId}/test
        Invocation.Builder request = resource.path("session").path(sessionId).path("test").request(mediaType);

        // Execute the PUT request...
        request.put(entity);
    }
}
