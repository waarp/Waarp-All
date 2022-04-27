/*
 * This file is part of Waarp Project (named also Waarp or GG).
 *
 *  Copyright (c) 2019, Waarp SAS, and individual contributors by the @author
 *  tags. See the COPYRIGHT.txt in the distribution for a full listing of
 * individual contributors.
 *
 *  All Waarp Project is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 * Waarp . If not, see <http://www.gnu.org/licenses/>.
 */

package org.waarp.common.utility;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxDriverLogLevel;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.waarp.common.file.FileUtils;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.logging.Level;

import static org.junit.Assert.*;

public abstract class TestWebAbstract {
  public static WebDriver driver = null;
  public static DriverType driverType = DriverType.FIREFOX;

  public static void initiateWebDriver(File file) {
    File libdir = file.getParentFile().getParentFile().getParentFile();
    // test-classes -> target -> WaarpR66 -> lib -> geckodriver (linux x64)
    File libPhantomJS = new File("/tmp/phantomjs-2.1.1");
    if (!libPhantomJS.canRead()) {
      File libPhantomJSZip = new File(libdir, "lib/phantomjs-2.1.1.bz2");
      if (libPhantomJSZip.canRead()) {
        FileUtils.uncompressedBz2File(libPhantomJSZip, libPhantomJS);
        libPhantomJS.setExecutable(true);
      }
    }
    assertTrue(libPhantomJS.exists());
    System.setProperty("phantomjs.binary.path", libPhantomJS.getAbsolutePath());

    File firefoxDriver = new File("/tmp/geckodriver");
    if (!firefoxDriver.canRead()) {
      File firefoxDriverZip = new File(libdir, "lib/geckodriver.bz2");
      if (firefoxDriverZip.canRead()) {
        FileUtils.uncompressedBz2File(firefoxDriverZip, firefoxDriver);
        firefoxDriver.setExecutable(true);
      } else {
        fail("Don't find: " + firefoxDriverZip.getAbsolutePath());
      }
    }
    assertTrue(firefoxDriver.exists());
    System.setProperty("webdriver.gecko.driver",
                       firefoxDriver.getAbsolutePath());
    try {
      driver = initializeDriver();
    } catch (InterruptedException e) {//NOSONAR
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  public static void reloadDriver() throws InterruptedException {
    if (driver != null) {
      finalizeDriver();
    }
    driver = initializeDriver();
  }

  public static WebDriver initializeDriver() throws InterruptedException {
    WebDriver driver;
    switch (driverType) {
      case PHANTOMJS:
        driver = createPhantomJSDriver();
        break;
      case FIREFOX:
      default:
        driver = createFirefoxDriver();
    }
    driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
    driver.manage().window().setSize(new Dimension(1920, 1080));
    driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(10));
    Thread.sleep(10);
    return driver;
  }

  private static WebDriver createPhantomJSDriver() {
    DesiredCapabilities desiredCapabilities =
        new DesiredCapabilities("phantomjs", "", Platform.ANY);
    desiredCapabilities.setCapability(
        PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,
        System.getProperty("phantomjs.binary.path"));
    desiredCapabilities.setCapability(CapabilityType.ELEMENT_SCROLL_BEHAVIOR,
                                      true);
    desiredCapabilities.setCapability(CapabilityType.TAKES_SCREENSHOT, false);
    desiredCapabilities.setCapability(
        CapabilityType.ENABLE_PROFILING_CAPABILITY, false);
    LoggingPreferences logPrefs = new LoggingPreferences();
    logPrefs.enable(LogType.BROWSER, Level.OFF);
    logPrefs.enable(LogType.CLIENT, Level.OFF);
    logPrefs.enable(LogType.DRIVER, Level.OFF);
    logPrefs.enable(LogType.PERFORMANCE, Level.OFF);
    logPrefs.enable(LogType.PROFILER, Level.OFF);
    logPrefs.enable(LogType.SERVER, Level.OFF);
    desiredCapabilities.setCapability(CapabilityType.LOGGING_PREFS, logPrefs);
    desiredCapabilities.setCapability(CapabilityType.HAS_NATIVE_EVENTS, true);
    desiredCapabilities.setCapability(
        PhantomJSDriverService.PHANTOMJS_GHOSTDRIVER_CLI_ARGS,
        "--webdriver-loglevel=NONE");
    desiredCapabilities.setJavascriptEnabled(true);

    ArrayList<String> cliArgs = new ArrayList<String>();
    cliArgs.add("--web-security=true");
    cliArgs.add("--ignore-ssl-errors=true");
    cliArgs.add("--webdriver-loglevel=NONE");
    desiredCapabilities.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS,
                                      cliArgs);

    PhantomJSDriver phantomJSDriver = new PhantomJSDriver(desiredCapabilities);
    phantomJSDriver.setLogLevel(Level.OFF);
    return phantomJSDriver;
  }

  private static WebDriver createFirefoxDriver() {
    FirefoxOptions options = new FirefoxOptions();
    options.setHeadless(true);
    options.setLogLevel(FirefoxDriverLogLevel.ERROR);
    options.setAcceptInsecureCerts(true);
    options.setCapability(CapabilityType.OVERLAPPING_CHECK_DISABLED, true);
    options.setCapability(CapabilityType.ELEMENT_SCROLL_BEHAVIOR, true);
    options.setCapability(CapabilityType.TAKES_SCREENSHOT, false);
    options.setCapability(CapabilityType.ENABLE_PROFILING_CAPABILITY, false);
    options.setCapability(CapabilityType.HAS_NATIVE_EVENTS, true);
    options.setCapability(CapabilityType.SUPPORTS_JAVASCRIPT, true);
    // options.setScriptTimeout(Duration.ofSeconds(10));
    new File("/tmp/chromedir").mkdirs();
    options.addArguments("--headless", "user-data-dir=/tmp/chromedir",
                         "disable-infobars", "--disable-extensions",
                         "--disable-dev-shm-usage", "--no-sandbox");
    FirefoxDriver firefoxDriver = new FirefoxDriver(options);
    // chromeDriver.manage().window().minimize();
    // Specifying pageLoadTimeout and Implicit wait
    firefoxDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(5));
    firefoxDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
    return firefoxDriver;
  }

  public static void finalizeDriver() throws InterruptedException {
    // 17 | close |  |  |
    // driver.close();
    if (driver != null) {
      try {
        driver.quit();
      } catch (Exception e) {
        // Ignore
      }
      driver = null;
    }
    Thread.sleep(10);
  }

  public enum DriverType {
    PHANTOMJS, FIREFOX
  }
}
