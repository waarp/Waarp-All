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

package org.waarp.openr66.it;

import org.apache.tools.ant.Project;
import org.junit.After;
import org.junit.Before;
import org.openqa.selenium.WebDriver;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.openr66.configuration.FileBasedConfiguration;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.dao.TransferDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.ChannelUtils;

import java.io.File;
import java.io.FileFilter;

/**
 *
 */
public abstract class S3TestAbstract extends S3TestAbstractMinimal {

  protected static final String TMP_R_66_CONF_CONFIG_XML =
      "/tmp/R66/conf/config.xml";

  protected static NetworkTransaction networkTransaction;
  protected static Project project;
  protected static File homeDir;
  public static WebDriver driver = null;

  public enum DriverType {
    PHANTOMJS
  }

  public static void tearDownAfterClassServer() throws Exception {
    Configuration.configuration.setTimeoutCon(100);
    ChannelUtils.exit();
    deleteBase();
  }

  public static void deleteBase() {
    final File tmp = new File("/tmp");
    final File[] files = tmp.listFiles(new FileFilter() {
      @Override
      public boolean accept(File file) {
        return file.getName().startsWith("openr66");
      }
    });
    for (final File file : files) {
      file.delete();
    }
  }

  public static void setUpBeforeClassClient(String clientConfig)
      throws Exception {
    final File clientConfigFile;
    if (clientConfig.startsWith("/")) {
      clientConfigFile = new File(clientConfig);
    } else {
      clientConfigFile = new File(dir, clientConfig);
    }
    logger.warn("Will try to load {}", clientConfigFile);
    if (clientConfigFile.isFile()) {
      System.err.println(
          "Find serverInit file: " + clientConfigFile.getAbsolutePath());
      if (!FileBasedConfiguration
          .setClientConfigurationFromXml(Configuration.configuration,
                                         clientConfigFile.getAbsolutePath())) {
        logger.error("Needs a correct configuration file as first argument");
        return;
      }
    } else {
      logger.error("Needs a correct configuration file as first argument: {}",
                   clientConfigFile.getAbsolutePath());
      return;
    }
    Configuration.configuration.setTimeoutCon(100);
    Configuration.configuration.pipelineInit();
    networkTransaction = new NetworkTransaction();
    DbTaskRunner.clearCache();
    TransferDAO transferAccess = null;
    try {
      transferAccess = DAOFactory.getInstance().getTransferDAO();
      transferAccess.deleteAll();
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } finally {
      if (transferAccess != null) {
        transferAccess.close();
      }
    }
  }

  public static void setUpBeforeClassClient() throws Exception {
    Configuration.configuration.setTimeoutCon(100);
    networkTransaction =
        Configuration.configuration.getInternalRunner().getNetworkTransaction();
    DbTaskRunner.clearCache();
    TransferDAO transferAccess = null;
    try {
      transferAccess = DAOFactory.getInstance().getTransferDAO();
      transferAccess.deleteAll();
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } finally {
      if (transferAccess != null) {
        transferAccess.close();
      }
    }
  }

  public static void tearDownAfterClassClient() throws Exception {
    Configuration.configuration.setTimeoutCon(100);
    if (networkTransaction != null) {
      networkTransaction.closeAll();
    }
  }

  @Before
  public void setUp() throws Exception {
    Configuration.configuration.setTimeoutCon(10000);
    Configuration.configuration
        .changeNetworkLimit(1000000000, 1000000000, 1000000000, 1000000000,
                            1000);
  }

  @After
  public void tearDown() throws Exception {
    Configuration.configuration.setTimeoutCon(100);
    Thread.sleep(100);
  }
}
