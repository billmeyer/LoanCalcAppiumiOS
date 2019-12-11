package io.billmeyer.saucelabs.parallel;


import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.openqa.selenium.JavascriptExecutor;
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
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;

/**
 * Simple TestNG test which demonstrates being instantiated via a DataProvider in order to supply multiple browser combinations.
 *
 * @author Bill Meyer
 */
public class TestBase
{
    protected static final boolean realDeviceTesting = true;
    protected static final boolean unifiedPlatformTesting = true;

    protected static final String testobjectApiKey = System.getenv("LOANCALC_IOS_APIKEY");
    protected static final String userName = System.getenv("SAUCE_USERNAME");
    protected static final String accessKey = System.getenv("SAUCE_ACCESS_KEY");

    /**
     * ThreadLocal variable which contains the  {@link WebDriver} instance which is used to perform browser interactions with.
     */
    private ThreadLocal<IOSDriver> driverThreadLocal = new ThreadLocal<>();

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
            if (unifiedPlatformTesting == true)
                return new Object[][] {
                        new Object[]{"iOS", "iPhone_XS_POC166", "12.1.4"}
                };
            else
                return new Object[][]{
                    new Object[]{"iOS", "iPhone.*", "13.1.2"}
                };
        }
        else
        {
            return new Object[][]{
                    new Object[]{"iOS", "iPhone XS Simulator", "13.0"},
                    new Object[]{"iOS", "iPad Pro (12.9 inch) Simulator", "13.0"}
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

        driverThreadLocal.get().executeScript("sauce:context=" + text);
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
    protected IOSDriver createDriver(String platformName, String platformVersion, String deviceName, String methodName)
    throws MalformedURLException
    {
        URL url = null;
        DesiredCapabilities caps = new DesiredCapabilities();

        // set desired capabilities to launch appropriate platformName on Sauce
        // For real device testing, connect to one URL using a certain set of credentials...
        if (realDeviceTesting == true && unifiedPlatformTesting == false)
        {
            url = new URL("http://us1.appium.testobject.com/wd/hub");
            caps.setCapability("testobject_api_key", testobjectApiKey);
            caps.setCapability("recordDeviceVitals", true);
        }
        // For emulator/simulator testing, connect to a different URL using a different certain set of credentials...
        else
        {
            url = new URL("https://" + userName + ":" + accessKey + "@ondemand.us-west-1.saucelabs.com/wd/hub");
            if (realDeviceTesting == true)
            {
                caps.setCapability("app", "sauce-storage:LoanCalc.ipa");
            }
            else
            {
                caps.setCapability("app", "sauce-storage:LoanCalc.app.zip");
            }

            caps.setCapability("automationName", "XCUITest");
        }

//        caps.setCapability("appiumVersion", "1.14.0");
        caps.setCapability("platformName", platformName);
        caps.setCapability("platformVersion", platformVersion);
        caps.setCapability("deviceName", deviceName);
//        caps.setCapability("name", String.format("%s - %s %s [%s]", methodName, platformName, platformVersion, new Date()));
        caps.setCapability("name", methodName);

        // Launch the remote platformName and set it as the current thread

        long start = System.currentTimeMillis();
        IOSDriver driver = new IOSDriver(url, caps);
        long stop = System.currentTimeMillis();
        info(driver, "Device allocation took %.2f secs\n", (stop - start) / 1000f);

        driverThreadLocal.set(driver);

        return driverThreadLocal.get();
    }

    /**
     * Method that gets invoked after test.
     * Sets the job status (PASS or FAIL) and closes the browser.
     */
    @AfterMethod
    public void tearDown(ITestResult result)
    throws Exception
    {
        AppiumDriver driver = driverThreadLocal.get();

        if (driver != null)
        {
            boolean success = result.isSuccess();

            if (realDeviceTesting == true)
            {
                if (unifiedPlatformTesting == true)
                {
                    reportSauceLabsMobileResult(driver, success);
                }
                else
                {
                    reportTestObjectResult(driver, success);
                }
            }
            driver.quit();
        }
    }

    public static void pushToSauceStorage(String localFileName, String remoteFileName)
    {
        File file = new File("/Users/bmeyer/github/billmeyer/LoanCalcAppiumTest/app-release.apk");

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(userName, accessKey);
        credsProvider.setCredentials(AuthScope.ANY, credentials);

        HttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(credsProvider).build();

        HttpPost postRequest = new HttpPost("https://api.us-west-1.saucelabs.com/v1/rdc/apps/upload");
        String fileName = file.getName();
        FileBody fileBody = new FileBody(file);

        HttpEntity httpEntity = MultipartEntityBuilder.create().addPart("fileName", new StringBody(fileName, ContentType.APPLICATION_OCTET_STREAM)).addPart("attachment", fileBody).build();

        postRequest.setEntity(httpEntity);
        try
        {
            HttpResponse response = client.execute(postRequest);
            StatusLine sl = response.getStatusLine();
            System.out.printf("HTTP Response: %d, %s\n", sl.getStatusCode(), sl.getReasonPhrase());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Logs the given line in the job’s commands list. No spaces can be between sauce: and context.
     */
    public static void info(RemoteWebDriver driver, String format, Object... args)
    {
        System.out.printf(format, args);
        return; // Not currently implemented
//        String msg = String.format(format, args);
//        ((JavascriptExecutor) driver).executeScript("sauce:context=" + msg);
    }

    public static void reportSauceLabsMobileResult(RemoteWebDriver driver, boolean status)
    {
        ((JavascriptExecutor) driver).executeScript("sauce:job-result=" + (status ? "passed" : "false"));
    }

    /**
     * Uses the Appium V2 RESTful API to report test result status to the Sauce Labs dashboard.
     *
     * @param driver The WebDriver instance we'll use to get the session ID we want to set the status for
     * @param status TRUE if the test was successful, FALSE otherwise
     * @see https://api.testobject.com/#!/Appium_Watcher_API/updateTest
     */
    public void reportTestObjectResult(RemoteWebDriver driver, boolean status)
    {
        String sessionId = driver.getSessionId().toString();

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
