/**
 * This file is part of Waarp Project.
 * 
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual contributors.
 * 
 * All Waarp Project is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Waarp . If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.waarp.ftp.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.util.TrustManagerUtils;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

/**
 * FTP Client using Apache Commons net FTP client (not working using FTPS or FTPSE)
 * 
 * @author "Frederic Bregier"
 * 
 */
public class WaarpFtpClient {
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(WaarpFtpClient.class);

    String server = null;
    int port = 21;
    String user = null;
    String pwd = null;
    String acct = null;
    int timeout;
    boolean isPassive = false;
    int ssl = 0; // -1 native, 1 auth
    protected FTPClient ftpClient = null;
    protected String result = null;
    private boolean binaryTransfer = true;

    /**
     * WARNING: SSL mode (FTPS and FTPSE) are not working due to a bug in Apache Commons-Net
     * 
     * @param server
     * @param port
     * @param user
     * @param pwd
     * @param acct
     * @param isPassive
     * @param ssl
     * @param timeout
     */
    public WaarpFtpClient(String server, int port,
            String user, String pwd, String acct, boolean isPassive, int ssl, int controlTimeout,
            int timeout) {
        this.server = server;
        this.port = port;
        this.user = user;
        this.pwd = pwd;
        this.acct = acct;
        this.isPassive = isPassive;
        this.ssl = ssl;
        if (this.ssl != 0) {
            // implicit or explicit
            this.ftpClient = new FTPSClient(this.ssl == -1);
            ((FTPSClient) this.ftpClient).setTrustManager(TrustManagerUtils
                    .getAcceptAllTrustManager());
        } else {
            this.ftpClient = new FTPClient();
        }
        if (controlTimeout > 0) {
            this.ftpClient.setControlKeepAliveTimeout(controlTimeout / 1000);
            this.ftpClient.setControlKeepAliveReplyTimeout(controlTimeout);
        }
        if (timeout > 0) {
            this.ftpClient.setDataTimeout(timeout);
        }
        this.ftpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(
                System.out), true));
    }

    /**
     * Try to connect to the server and goes with the authentication
     * 
     * @return True if connected and authenticated, else False
     */
    public boolean connect() {
        result = null;
        boolean isActive = false;
        try {
            try {
                this.ftpClient.connect(this.server, this.port);
            } catch (SocketException e) {
                result = "Connection in error";
                logger.error(result, e);
                return false;
            } catch (IOException e) {
                result = "Connection in error";
                logger.error(result, e);
                return false;
            }
            int reply = this.ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                this.disconnect();
                result = "Connection in error: " + reply;
                logger.error(result);
                return false;
            }
            try {
                if (this.acct == null) {
                    // no account
                    if (!this.ftpClient.login(this.user, this.pwd)) {
                        this.logout();
                        result = "Login in error";
                        logger.error(result);
                        return false;
                    }
                } else if (!this.ftpClient.login(this.user, this.pwd,
                        this.acct)) {
                    this.logout();
                    result = "Login in error";
                    logger.error(result);
                    return false;
                }
            } catch (IOException e) {
                result = "Login in error";
                logger.error(result, e);
                return false;
            }
            try {
                this.ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            } catch (IOException e1) {
                result = "Set BINARY in error";
                logger.error(result, e1);
                return false;
            }
            changeMode(isPassive);
            this.ftpClient.setUseEPSVwithIPv4(false);
            if (ssl == 1) {
                // now send request for PROT (AUTH already sent)
                try {
                    ((FTPSClient) this.ftpClient).execPBSZ(0);
                    logger.debug("PBSZ 0");
                    ((FTPSClient) this.ftpClient).execPROT("P");
                    logger.debug("Info: " +
                            ((FTPSClient) this.ftpClient).getEnableSessionCreation());
                } catch (IOException e) {
                    this.logout();
                    result = "Explicit SSL in error";
                    logger.error(result, e);
                    return false;
                }
            }
            isActive = true;
            return true;
        } finally {
            if ((!isActive) && this.ftpClient.getDataConnectionMode() == FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE) {
                this.disconnect();
            }
        }
    }

    /**
     * Logout from the Control connection
     */
    public void logout() {
        try {
            this.ftpClient.logout();
        } catch (IOException e) {
            if (this.ftpClient.getDataConnectionMode() == FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE) {
                try {
                    this.ftpClient.disconnect();
                } catch (IOException f) {
                    // do nothing
                }
            }
        }
    }

    /**
     * Disconnect the Ftp Client
     */
    public void disconnect() {
        try {
            this.ftpClient.disconnect();
        } catch (IOException e) {
            logger.debug("Disconnection in error", e);
        }
    }

    /**
     * Create a new directory
     * 
     * @param newDir
     * @return True if created
     */
    public boolean makeDir(String newDir) {
        result = null;
        try {
            return this.ftpClient.makeDirectory(newDir);
        } catch (IOException e) {
            result = "MKDIR in error";
            logger.info(result, e);
            return false;
        }
    }

    /**
     * Change remote directory
     * 
     * @param newDir
     * @return True if the change is OK
     */
    public boolean changeDir(String newDir) {
        result = null;
        try {
            return this.ftpClient.changeWorkingDirectory(newDir);
        } catch (IOException e) {
            result = "CHDIR in error";
            logger.info(result, e);
            return false;
        }
    }

    /**
     * Change the FileType of Transfer (Binary true, ASCII false)
     * 
     * @param binaryTransfer1
     * @return True if the change is OK
     */
    public boolean changeFileType(boolean binaryTransfer1) {
        result = null;
        this.binaryTransfer = binaryTransfer1;
        try {
            if (this.binaryTransfer) {
                return this.ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            } else {
                return this.ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
            }
        } catch (IOException e) {
            result = "FileType in error";
            logger.warn(result, e);
            return false;
        }
    }

    /**
     * Change to passive (true) or active (false) mode
     * 
     * @param passive
     */
    public void changeMode(boolean passive) {
        this.isPassive = passive;
        if (this.isPassive) {
            this.ftpClient.enterLocalPassiveMode();
        } else {
            this.ftpClient.enterLocalActiveMode();
        }
    }

    /**
     * Ask to transfer a file
     * 
     * @param local
     *            local filepath (limited to path if get, else full path)
     * @param remote
     *            filename
     * @param getStoreOrAppend
     *            -1 = get, 1 = store, 2 = append
     * @return True if the file is correctly transfered
     */
    public boolean transferFile(String local, String remote, int getStoreOrAppend) {
        result = null;
        boolean status = false;
        FileOutputStream output = null;
        FileInputStream fileInputStream = null;
        try {
            if (getStoreOrAppend > 0) {
                fileInputStream = new FileInputStream(local);
                if (getStoreOrAppend == 1) {
                    status = this.ftpClient.storeFile(remote, fileInputStream);
                } else {
                    // append
                    status = this.ftpClient.appendFile(remote, fileInputStream);
                }
                fileInputStream.close();
                fileInputStream = null;
                if (!status) {
                    result = "Cannot finalize store like operation";
                    logger.error(result);
                    return false;
                }
                return true;
            } else {
                output = new FileOutputStream(new File(local, remote));
                status = this.ftpClient.retrieveFile(remote, output);
                output.flush();
                output.close();
                output = null;
                if (!status) {
                    result = "Cannot finalize retrieve like operation";
                    logger.error(result);
                    return false;
                }
                return true;
            }
        } catch (IOException e) {
            if (output != null)
                try {
                    output.close();
                } catch (IOException e1) {
                }
            if (fileInputStream != null)
                try {
                    fileInputStream.close();
                } catch (IOException e1) {
                }
            result = "Cannot finalize operation";
            logger.error(result, e);
            return false;
        }
    }

    /**
     * 
     * @return the list of files as returned by the FTP command
     */
    public String[] listFiles() {
        try {
            FTPFile[] list = this.ftpClient.listFiles();
            String[] results = new String[list.length];
            int i = 0;
            for (FTPFile file : list) {
                results[i] = file.toFormattedString();
                i++;
            }
            return results;
        } catch (IOException e) {
            result = "Cannot finalize transfer operation";
            logger.error(result, e);
            return null;
        }
    }

    /**
     * 
     * @param feature
     * @return True if the feature is listed
     */
    public boolean featureEnabled(String feature) {
        try {
            System.err.println(this.ftpClient.features());
            if (this.ftpClient.featureValue(feature) == null) {
                String result = this.ftpClient.getReplyString();
                return (result.contains(feature.toUpperCase()));
            }
            return true;
        } catch (IOException e) {
            result = "Cannot execute operation Feature";
            logger.error(result, e);
            return false;
        }
    }

    /**
     * 
     * @param params
     * @return the string lines returned by the command params
     */
    public String[] executeCommand(String params) {
        result = null;
        try {
            System.err.println(params);
            int pos = params.indexOf(' ');
            String command = params;
            String args = null;
            if (pos > 0) {
                command = params.substring(0, pos);
                args = params.substring(pos + 1);
            }
            String[] results = this.ftpClient.doCommandAsStrings(command, args);
            if (results == null) {
                results = new String[1];
                results[0] = this.ftpClient.getReplyString();
            }
            return results;
        } catch (IOException e) {
            result = "Cannot execute operation Site";
            logger.error(result, e);
            return null;
        }
    }

    /**
     * 
     * @param params
     * @return the string lines returned by the SITE command params
     */
    public String[] executeSiteCommand(String params) {
        result = null;
        try {
            System.err.println("SITE " + params);
            String[] results = this.ftpClient.doCommandAsStrings("SITE", params);
            if (results == null) {
                results = new String[1];
                results[0] = this.ftpClient.getReplyString();
            }
            return results;
        } catch (IOException e) {
            result = "Cannot execute operation Site";
            logger.error(result, e);
            return null;
        }
    }

}
