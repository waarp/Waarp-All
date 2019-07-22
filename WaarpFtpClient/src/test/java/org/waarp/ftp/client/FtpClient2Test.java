package org.waarp.ftp.client;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.CopyStreamListener;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.ftp.FtpServer;
import org.waarp.ftp.client.transaction.Ftp4JClientTransactionTest;
import org.waarp.ftp.client.transaction.FtpApacheClientTransactionTest;
import org.waarp.ftp.client.transaction.FtpClientThread;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertFalse;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FtpClient2Test {
    public static AtomicLong numberOK = new AtomicLong(0);
    public static AtomicLong numberKO = new AtomicLong(0);
    /**
     * Internal Logger
     */
    protected static WaarpLogger logger =
            WaarpLoggerFactory.getLogger(FtpClient2Test.class);
    private static final int port = 2021;

    @BeforeClass
    public static void startServer() throws IOException {
        WaarpLoggerFactory
                .setDefaultFactory(new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
        DetectionUtils.setJunit(true);
        File file = new File("/tmp/GGFTP/fred/a");
        file.mkdirs();
        FtpServer.startFtpServer("config.xml");
        File localFilename = new File("/tmp/ftpfile.bin");
        FileWriter fileWriterBig = new FileWriter(localFilename);
        for (int i = 0; i < 100; i++) {
            fileWriterBig.write("0123456789");
        }
        fileWriterBig.flush();
        fileWriterBig.close();
        logger.warn("Will start server");
    }

    @AfterClass
    public static void stopServer() {
        logger.warn("Will shutdown from client");
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
        Ftp4JClientTransactionTest client =
                new Ftp4JClientTransactionTest("127.0.0.1", port, "fredo", "fred1", "a",
                                               0);
        if (!client.connect()) {
            logger.warn("Cant connect");
            FtpClientTest.numberKO.incrementAndGet();
            return;
        }
        try {
            String[] results = client.executeSiteCommand("internalshutdown abcdef");
            System.err.print("SHUTDOWN: ");
            for (String string : results) {
                System.err.println(string);
            }
        } finally {
            client.disconnect();
        }
        logger.warn("Will stop server");
        FtpServer.stopFtpServer();
        File file = new File("/tmp/GGFTP");
        FileUtils.forceDeleteRecursiveDir(file);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
    }

    @Before
    public void clean() {
        File file = new File("/tmp/GGFTP");
        FileUtils.forceDeleteRecursiveDir(file);
        file = new File("/tmp/GGFTP/fredo/a");
        file.mkdirs();
    }




    @Test
    public void test1_Ftp4JSimple() throws IOException {
        numberKO.set(0);
        numberOK.set(0);
        File localFilename = new File("/tmp/ftpfile.bin");
        testFtp4J("127.0.0.1", port, "fred", "fred2", "a", 0,
                  localFilename.getAbsolutePath(), 0, 50, true, 1, 1);
    }

    public void testFtp4J(String server, int port, String username, String passwd,
                          String account, int isSSL,
                          String localFilename, int type, int delay,
                          boolean shutdown, int numberThread,
                          int numberIteration) {
        // initiate Directories
        Ftp4JClientTransactionTest client =
                new Ftp4JClientTransactionTest(server, port, username, passwd, account,
                                               isSSL);

        logger.warn("First connexion");
        if (!client.connect()) {
            logger.error("Can't connect");
            FtpClientTest.numberKO.incrementAndGet();
            return;
        }
        try {
            logger.warn("Create Dirs");
            for (int i = 0; i < numberThread; i++) {
                client.makeDir("T" + i);
            }
            logger.warn("Feature commands");
            System.err.println("SITE: " + client.featureEnabled("SITE"));
            System.err.println("SITE CRC: " + client.featureEnabled("SITE XCRC"));
            System.err.println("CRC: " + client.featureEnabled("XCRC"));
            System.err.println("MD5: " + client.featureEnabled("XMD5"));
            System.err.println("SHA1: " + client.featureEnabled("XSHA1"));
        } finally {
            logger.warn("Logout");
            client.logout();
        }
        if (isSSL > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        ExecutorService executorService = Executors.newCachedThreadPool();
        logger.warn("Will start {} Threads", numberThread);
        long date1 = System.currentTimeMillis();
        for (int i = 0; i < numberThread; i++) {
            executorService.execute(
                    new FtpClientThread("T" + i, server, port, username, passwd, account,
                                        localFilename,
                                        numberIteration, type, delay, isSSL));
            if (delay > 0) {
                try {
                    long newdel = ((delay / 3) / 10) * 10;
                    if (newdel == 0) {
                        Thread.yield();
                    } else {
                        Thread.sleep(newdel);
                    }
                } catch (InterruptedException e) {
                }
            } else {
                Thread.yield();
            }
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
            executorService.shutdownNow();
            // Thread.currentThread().interrupt();
        }
        executorService.shutdown();
        long date2 = 0;
        try {
            if (!executorService.awaitTermination(12000, TimeUnit.SECONDS)) {
                date2 = System.currentTimeMillis() - 120000 * 60;
                executorService.shutdownNow();
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Really not shutdown normally");
                }
            } else {
                date2 = System.currentTimeMillis();
            }
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            executorService.shutdownNow();
            date2 = System.currentTimeMillis();
            // Thread.currentThread().interrupt();
        }

        logger.warn(
                localFilename + " " + numberThread + " " + numberIteration + " " +
                type + " Real: "
                + (date2 - date1) + " OK: " + numberOK.get() + " KO: " +
                numberKO.get() + " Trf/s: "
                + (numberOK.get() * 1000 / (date2 - date1)));
        assertTrue("No KO", numberKO.get() == 0);
    }

    @Test
    public void test2_FtpSimple() throws IOException {
        numberKO.set(0);
        numberOK.set(0);
        File localFilename = new File("/tmp/ftpfile.bin");

        int delay = 50;

        FtpApacheClientTransactionTest
                client =
                new FtpApacheClientTransactionTest("127.0.0.1", port, "fred", "fred2",
                                                   "a", 0);
        if (!client.connect()) {
            logger.error("Can't connect");
            FtpClientTest.numberKO.incrementAndGet();
            return;
        }
        try {
            logger.warn("Create Dirs");
            client.makeDir("T" + 0);
            logger.warn("Feature commands");
            System.err.println("SITE: " + client.featureEnabled("SITE"));
            System.err.println("SITE CRC: " + client.featureEnabled("SITE XCRC"));
            System.err.println("CRC: " + client.featureEnabled("XCRC"));
            System.err.println("MD5: " + client.featureEnabled("XMD5"));
            System.err.println("SHA1: " + client.featureEnabled("XSHA1"));
            client.changeDir("T0");
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
            }
            client.changeFileType(true);
            client.changeMode(true);
            internalApacheClient(client, localFilename, delay, true);
            client.changeMode(false);
            internalApacheClient(client, localFilename, delay, false);
        } finally {
            logger.warn("Logout");
            client.logout();
            client.disconnect();
        }
        assertTrue("No KO", numberKO.get() == 0);
    }


    private void internalApacheClient(FtpApacheClientTransactionTest client,
                                      File localFilename, int delay,
                                      boolean mode) {
        String smode = mode? "passive" : "active";
        logger.info(" transfer {} store ", smode);
        if (!client.transferFile(localFilename.getAbsolutePath(),
                                 localFilename.getName(), true)) {
            logger.warn("Cant store file {} mode ", smode);
            FtpClientTest.numberKO.incrementAndGet();
            return;
        } else {
            FtpClientTest.numberOK.incrementAndGet();
            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                }
            }
        }
        if (!client.deleteFile(localFilename.getName())) {
            logger.warn(" Cant delete file {} mode ", smode);
            FtpClientTest.numberKO.incrementAndGet();
            return;
        } else {
            FtpClientTest.numberOK.incrementAndGet();
            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                }
            }
        }
        if (!client.transferFile(localFilename.getAbsolutePath(),
                                 localFilename.getName(), true)) {
            logger.warn("Cant store file {} mode ", smode);
            FtpClientTest.numberKO.incrementAndGet();
            return;
        } else {
            FtpClientTest.numberOK.incrementAndGet();
            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                }
            }
        }
        Thread.yield();
        logger.info(" transfer {} retr ", smode);
        if (!client.transferFile(null,
                                 localFilename.getName(), false)) {
            logger.warn("Cant retrieve file {} mode ", smode);
            FtpClientTest.numberKO.incrementAndGet();
            return;
        } else {
            FtpClientTest.numberOK.incrementAndGet();
            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    @Test
    public void test0_FtpApacheClientActive() throws IOException {
        File localFilename = new File("/tmp/ftpfile.bin");
        FileWriter fileWriterBig = new FileWriter(localFilename);
        for (int i = 0; i < 100; i++) {
            fileWriterBig.write("0123456789");
        }
        fileWriterBig.flush();
        fileWriterBig.close();
        logger.warn("Active");
        launchFtpClient("127.0.0.1", port, "fredo", "fred1", "a", true,
                        localFilename.getAbsolutePath(), localFilename.getName());
        logger.warn("End Active");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    @Test
    public void test0_FtpApacheClientPassive() throws IOException {
        File localFilename = new File("/tmp/ftpfile.bin");
        FileWriter fileWriterBig = new FileWriter(localFilename);
        for (int i = 0; i < 100; i++) {
            fileWriterBig.write("0123456789");
        }
        fileWriterBig.flush();
        fileWriterBig.close();
        logger.warn("Passive");
        launchFtpClient("127.0.0.1", port, "fredo", "fred1", "a", false,
                        localFilename.getAbsolutePath(), localFilename.getName());
        logger.warn("End Passive");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    public static void launchFtpClient(String server, int port, String username,
                                       String password, String account,
                                       boolean localActive, String local,
                                       String remote) {
        boolean mustCallProtP = false;
        boolean binaryTransfer = true, error = false;
        boolean useEpsvWithIPv4 = false;
        boolean lenient = false;
        final FTPClient ftp;
        ftp = new FTPClient();

        ftp.setCopyStreamListener(createListener());
        ftp.setControlKeepAliveTimeout(30000);
        ftp.setControlKeepAliveReplyTimeout(30000);
        ftp.setListHiddenFiles(true);
        // suppress login details
        ftp.addProtocolCommandListener(
                new PrintCommandListener(new PrintWriter(System.out), true));

        try {
            int reply;
            if (port > 0) {
                ftp.connect(server, port);
            } else {
                ftp.connect(server);
            }
            System.out.println("Connected to " + server + " on " +
                               (port > 0? port : ftp.getDefaultPort()));

            // After connection attempt, you should check the reply code to verify
            // success.
            reply = ftp.getReplyCode();

            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                System.err.println("FTP server refused connection.");
                assertFalse("Can't connect", true);
                return;
            }
        } catch (IOException e) {
            if (ftp.getDataConnectionMode() ==
                FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE) {
                try {
                    ftp.disconnect();
                } catch (IOException f) {
                    // do nothing
                }
            }
            System.err.println("Could not connect to server.");
            e.printStackTrace();
            assertFalse("Can't connect", true);
            return;
        }

        __main:
        try {
            if (account == null) {
                if (!ftp.login(username, password)) {
                    ftp.logout();
                    error = true;
                    break __main;
                }
            } else {
                if (!ftp.login(username, password, account)) {
                    ftp.logout();
                    error = true;
                    break __main;
                }
            }
            System.out.println("Remote system is " + ftp.getSystemType());

            if (binaryTransfer) {
                ftp.setFileType(FTP.BINARY_FILE_TYPE);
            }

            // Use passive mode as default because most of us are
            // behind firewalls these days.
            if (localActive) {
                ftp.enterLocalActiveMode();
            } else {
                ftp.enterLocalPassiveMode();
            }

            ftp.setUseEPSVwithIPv4(useEpsvWithIPv4);

            if (mustCallProtP) {
                ((FTPSClient) ftp).execPBSZ(0);
                ((FTPSClient) ftp).execPROT("P");
            }

            InputStream input;

            input = new FileInputStream(local);

            if (!ftp.storeFile(remote, input)) {
                error = true;
                input.close();
                return;
            }
            input.close();

            OutputStream output;

            output = new FileOutputStream(local);

            if (!ftp.retrieveFile(remote, output)) {
                error = true;
                output.close();
                return;
            }
            output.close();
            if (lenient) {
                FTPClientConfig config = new FTPClientConfig();
                config.setLenientFutureDates(true);
                ftp.configure(config);
            }

            for (FTPFile f : ftp.listFiles(remote)) {
                System.out.println(f.getRawListing());
                System.out.println(f.toFormattedString());
            }
            for (FTPFile f : ftp.mlistDir(remote)) {
                System.out.println(f.getRawListing());
                System.out.println(f.toFormattedString());
            }
            FTPFile f = ftp.mlistFile(remote);
            if (f != null) {
                System.out.println(f.toFormattedString());
            }
            String[] results = ftp.listNames(remote);
            if (results != null) {
                for (String s : ftp.listNames(remote)) {
                    System.out.println(s);
                }
            }
            if (!ftp.deleteFile(remote)) {
                error = true;
                System.out.println("Failed delete");
            }
            // boolean feature check
            if (ftp.features()) {
                // Command listener has already printed the output
            } else {
                System.out.println("Failed: " + ftp.getReplyString());
                error = true;
            }

            ftp.noop(); // check that control connection is working OK
        } catch (FTPConnectionClosedException e) {
            error = true;
            System.err.println("Server closed connection.");
            e.printStackTrace();
        } catch (IOException e) {
            error = true;
            e.printStackTrace();
        } finally {
            if (ftp.getDataConnectionMode() ==
                FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE) {
                try {
                    ftp.disconnect();
                } catch (IOException f) {
                    // do nothing
                }
            }
            assertFalse("Error occurs", error);
        }
    }


    private static CopyStreamListener createListener() {
        return new CopyStreamListener() {
            private long megsTotal = 0;

            public void bytesTransferred(CopyStreamEvent event) {
                bytesTransferred(event.getTotalBytesTransferred(),
                                 event.getBytesTransferred(), event.getStreamSize());
            }

            public void bytesTransferred(long totalBytesTransferred,
                                         int bytesTransferred, long streamSize) {
                long megs = totalBytesTransferred / 1000000;
                for (long l = megsTotal; l < megs; l++) {
                    System.err.print("#");
                }
                megsTotal = megs;
            }
        };
    }
}
