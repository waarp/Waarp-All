package org.waarp.gateway.ftp.client;

import java.io.File;

import org.waarp.common.file.filesystembased.FilesystemBasedFileParameterImpl;
import org.waarp.gateway.ftp.ExecGatewayFtpServer;
import org.waarp.gateway.ftp.client.transaction.Ftp4JClientTransactionTest;
import org.waarp.gateway.ftp.config.FileBasedConfiguration;
import org.waarp.gateway.ftp.control.ExecBusinessHandler;
import org.waarp.gateway.ftp.data.FileSystemBasedDataBusinessHandler;
import org.waarp.gateway.ftp.database.DbConstantFtp;

public class ShutdownTestClient {

  public ShutdownTestClient() {
    // TODO Auto-generated constructor stub
  }

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.err.println("No argument: configuration file missing");
      System.exit(3);
    }
    File file = new File(args[0]);
    final FileBasedConfiguration configuration =
      new FileBasedConfiguration(ExecGatewayFtpServer.class,
        ExecBusinessHandler.class, FileSystemBasedDataBusinessHandler.class,
        new FilesystemBasedFileParameterImpl());
    try {
      if (!configuration.setConfigurationServerFromXml(file.getAbsolutePath())) {
        System.err.println("Bad main configuration");
        System.exit(1);
      }
	} finally {
      if (DbConstantFtp.gatewayAdmin != null) {
        DbConstantFtp.gatewayAdmin.close();
      }
    }
    System.err.println("Will start server");
    String key = configuration.getCryptoKey().decryptHexInString("c5f4876737cf351a");
    final Ftp4JClientTransactionTest client =
            new Ftp4JClientTransactionTest("127.0.0.1", 2021, "fredo", key, "a", 0);
    if (!client.connect()) {
      System.err.println("Cant connect");
      System.exit(2);
      return;
    }
    try {
      final String[] results =
          client.executeSiteCommand("internalshutdown pwdhttp");
      System.err.print("SHUTDOWN: ");
      for (final String string : results) {
        System.err.println(string);
      }
    } finally {
      client.disconnect();
    }
  }
}
