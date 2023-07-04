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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxDriverLogLevel;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.waarp.common.file.FileUtils;

import java.io.File;
import java.time.Duration;

import static org.junit.Assert.*;

public abstract class TestWebAbstract {
  static final Logger logger = LoggerFactory.getLogger(TestWebAbstract.class);

  public static WebDriver driver = null;

  public static void initiateWebDriver(File file) {

    File libdir = file.getParentFile().getParentFile().getParentFile();
    // test-classes -> target -> WaarpR66 -> lib -> geckodriver (linux x64)

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
    System.setProperty("webdriver.gecko.driver", firefoxDriver.getAbsolutePath());
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

  private static WebDriver initializeDriver() throws InterruptedException {
    logger.debug("initializeDriver()");
    WebDriver driver = createFirefoxDriver();
    driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
    driver.manage().window().setSize(new Dimension(1920, 1080));
    driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(10));
    Thread.sleep(10);
    return driver;
  }

  private static int createDriver = 0;

  private static WebDriver createFirefoxDriver() {
    FirefoxOptions options = new FirefoxOptions();
    options.setLogLevel(FirefoxDriverLogLevel.DEBUG);
    options.setAcceptInsecureCerts(true);
    options.setCapability(CapabilityType.OVERLAPPING_CHECK_DISABLED, true);
    options.setCapability(CapabilityType.ELEMENT_SCROLL_BEHAVIOR, true);
    options.setCapability(CapabilityType.TAKES_SCREENSHOT, false);
    options.setCapability(CapabilityType.ENABLE_PROFILING_CAPABILITY, false);
    options.setCapability(CapabilityType.HAS_NATIVE_EVENTS, true);
    options.setCapability(CapabilityType.SUPPORTS_JAVASCRIPT, true);
    // options.setScriptTimeout(Duration.ofSeconds(10));
    options.addArguments("--headless", "--log debug");
    FirefoxDriver firefoxDriver = null;
    int wait = 10;
    createDriver++;
    for (int i = 0; i < 10; i++) {
      try {
        logger.debug("Trying to instantiate FirefoxDriver, attempt #{} using driver located in: {}", i, System.getProperty("webdriver.gecko.driver"));
        firefoxDriver = new FirefoxDriver(options);
        logger.debug("FirefoxDriver successfully created: {}", firefoxDriver);
        break;
      } catch (Exception e) {
        logger.error("Error while creating Selenium Firefox driver", e);
        try {
          Thread.sleep(wait);
          wait += 10;
        } catch (InterruptedException ex) {
          // Ignore
        }
      }
    }
    if (firefoxDriver == null) {
      throw new RuntimeException("Cannot start Driver");
    }
    createDriver = 0;
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
}
