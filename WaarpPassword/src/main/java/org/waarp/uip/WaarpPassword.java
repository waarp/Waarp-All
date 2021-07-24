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
package org.waarp.uip;

import org.waarp.common.crypto.Blowfish;
import org.waarp.common.crypto.Des;
import org.waarp.common.crypto.KeyObject;
import org.waarp.common.exception.CryptoException;
import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.utility.SystemPropertyUtil;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.common.utility.WaarpSystemUtil;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Console Command Line Main class to provide Password Management for GoldenGate
 * Products.
 */
public class WaarpPassword {
  static boolean desModel = true;
  static boolean clearPasswordView;
  static final String HELPOPTIONS = "Options available\r\n" +
                                    "* -ki file to specify the Key File by default\r\n" +
                                    "* -ko file to specify a new Key File to build and save\r\n\r\n" +
                                    "* -des to specify DES format (default)\r\n" +
                                    "* -blf to specify BlowFish format\r\n\r\n" +
                                    "* -pi file to specify a GGP File by default(password)\r\n" +
                                    "* -pwd to specify a clear ggp password as entry\r\n" +
                                    "* -cpwd to specify a crypted ggp password as entry\r\n" +
                                    "* -po file to specify a GGP File as output for the password\r\n" +
                                    "* -clear to specify uncrypted password shown as clear text";
  static final String GGPEXTENSION = "ggp";
  static String ki;
  static String ko;
  static String pi;
  static String po;
  static String pwd;
  static String cpwd;

  private File keyFile;
  private File passwordFile;
  private String clearPassword;
  private String cryptedPassword;

  private final KeyObject currentKey;

  /**
   * @param args
   *
   * @throws Exception
   */
  public static void main(final String[] args) throws Exception {
    if (!loadOptions(args)) {
      // Bad options
      WaarpSystemUtil.systemExit(2);
      return;
    }
    final WaarpPassword waarpPassword = new WaarpPassword();
    if (po == null && pi == null) {
      // stop
      SysErrLogger.FAKE_LOGGER.sysout("Key written");
      WaarpSystemUtil.systemExit(0);
      return;
    }
    if (waarpPassword.clearPassword == null ||
        waarpPassword.clearPassword.length() == 0) {
      SysErrLogger.FAKE_LOGGER.sysout("Password to crypt:");
      final String newp = waarpPassword.readString();
      if (newp == null || newp.length() == 0) {
        SysErrLogger.FAKE_LOGGER.syserr("No password as input");
        WaarpSystemUtil.systemExit(4);
        return;
      }
      waarpPassword.setClearPassword(newp);
      if (po != null) {
        waarpPassword.setPasswordFile(new File(po));
        waarpPassword.savePasswordFile();
      }
      if (clearPasswordView) {
        SysErrLogger.FAKE_LOGGER.sysout(
            "ClearPwd: " + waarpPassword.getClearPassword());
        SysErrLogger.FAKE_LOGGER.sysout(
            "CryptedPwd: " + waarpPassword.getCryptedPassword());
      }
    }
  }

  public static boolean loadOptions(final String[] args) {
    desModel = true;
    clearPasswordView = false;
    ki = null;
    ko = null;
    pi = null;
    po = null;
    pwd = null;
    cpwd = null;

    int i;
    if (args.length == 0) {
      SysErrLogger.FAKE_LOGGER.syserr(HELPOPTIONS);
      return false;
    }
    if (!SystemPropertyUtil.isFileEncodingCorrect()) {
      SysErrLogger.FAKE_LOGGER.syserr(
          "Issue while trying to set UTF-8 as default file encoding: use -Dfile.encoding=UTF-8 as java command argument\n" +
          "Currently file.encoding is: " +
          SystemPropertyUtil.get(SystemPropertyUtil.FILE_ENCODING));
      return false;
    }
    for (i = 0; i < args.length; i++) {
      if ("-ki".equalsIgnoreCase(args[i])) {
        i++;
        if (i < args.length) {
          ki = args[i];
        } else {
          SysErrLogger.FAKE_LOGGER.syserr("-ki needs a file as argument");
          return false;
        }
      } else if ("-ko".equalsIgnoreCase(args[i])) {
        i++;
        if (i < args.length) {
          ko = args[i];
        } else {
          SysErrLogger.FAKE_LOGGER.syserr("-ko needs a file as argument");
          return false;
        }
      } else if ("-pi".equalsIgnoreCase(args[i])) {
        i++;
        if (i < args.length) {
          pi = args[i];
        } else {
          SysErrLogger.FAKE_LOGGER.syserr("-pi needs a file as argument");
          return false;
        }
      } else if ("-po".equalsIgnoreCase(args[i])) {
        i++;
        if (i < args.length) {
          po = args[i];
        } else {
          SysErrLogger.FAKE_LOGGER.syserr("-po needs a file as argument");
          return false;
        }
      } else if ("-des".equalsIgnoreCase(args[i])) {
        desModel = true;
      } else if ("-blf".equalsIgnoreCase(args[i])) {
        desModel = false;
      } else if ("-pwd".equalsIgnoreCase(args[i])) {
        i++;
        if (i < args.length) {
          pwd = args[i];
        } else {
          SysErrLogger.FAKE_LOGGER.syserr("-pwd needs a password as argument");
          return false;
        }
      } else if ("-cpwd".equalsIgnoreCase(args[i])) {
        i++;
        if (i < args.length) {
          cpwd = args[i];
        } else {
          SysErrLogger.FAKE_LOGGER.syserr(
              "-cpwd needs a crypted password as argument");
          return false;
        }
      } else if ("-clear".equalsIgnoreCase(args[i])) {
        clearPasswordView = true;
      } else {
        SysErrLogger.FAKE_LOGGER.syserr("Unknown option: " + args[i]);
        return false;
      }
    }
    if (ki == null && ko == null) {
      SysErrLogger.FAKE_LOGGER.syserr(
          "You must specify one of ki or ko options");
      return false;
    }
    if (ki == null) {
      ki = ko;
    }
    if (ki == null && (po != null || pi != null)) {
      SysErrLogger.FAKE_LOGGER.syserr(
          "If pi or po options are set, ki or ko options must be set also!\n");
      return false;
    }
    if (pi == null && po == null && (pwd != null || cpwd != null)) {
      SysErrLogger.FAKE_LOGGER.syserr(
          "Cannot create a password if no password GGP file is specified with pi or po options");
      return false;
    }
    return true;
  }

  public WaarpPassword() throws Exception {
    if (desModel) {
      currentKey = new Des();
    } else {
      currentKey = new Blowfish();
    }
    if (ko != null) {
      createNewKey();
      saveKey(new File(ko));
    }
    if (ki != null) {
      loadKey(new File(ki));
    }
    if (pi != null) {
      setPasswordFile(new File(pi));
      loadPasswordFile();
    }
    if (pwd != null) {
      setClearPassword(pwd);
    }
    if (cpwd != null) {
      setCryptedPassword(cpwd);
    }
    if (po != null) {
      setPasswordFile(new File(po));
      savePasswordFile();
    }
    if (clearPassword != null) {
      if (clearPasswordView) {
        SysErrLogger.FAKE_LOGGER.sysout("ClearPwd: " + getClearPassword());
      }
      SysErrLogger.FAKE_LOGGER.sysout("CryptedPwd: " + getCryptedPassword());
    }
  }

  private String readString() {
    String read = "";
    final InputStreamReader input =
        new InputStreamReader(System.in, WaarpStringUtils.UTF8);
    final BufferedReader reader = new BufferedReader(input);
    try {
      read = reader.readLine();
    } catch (final IOException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
    }
    return read;
  }

  /**
   * Create a new Key but do not save it on file
   *
   * @throws Exception
   */
  public final void createNewKey() throws Exception {
    try {
      currentKey.generateKey();
    } catch (final Exception e) {
      throw new CryptoException("Create New Key in error", e);
    }
    if (clearPassword != null) {
      setClearPassword(clearPassword);
    }
  }

  /**
   * @param file source file
   *
   * @throws CryptoException
   */
  public final void loadKey(final File file) throws CryptoException {
    keyFile = file;
    try {
      currentKey.setSecretKey(file);
    } catch (final IOException e) {
      throw new CryptoException("Load Key in error", e);
    }
  }

  /**
   * @param file destination file, if null previously set file is used
   *
   * @throws CryptoException
   */
  public final void saveKey(final File file) throws CryptoException {
    if (file != null) {
      keyFile = file;
    }
    try {
      currentKey.saveSecretKey(keyFile);
    } catch (final IOException e) {
      throw new CryptoException("Save Key in error", e);
    }
  }

  /**
   * @return True if the associated key is ready
   */
  public final boolean keyReady() {
    return currentKey.keyReady();
  }

  /**
   * @return The File associated with the current Key
   */
  public final File getKeyFile() {
    return keyFile;
  }

  /**
   * Set the new password and its crypted value
   *
   * @param passwd
   *
   * @throws Exception
   */
  public final void setClearPassword(final String passwd) throws Exception {
    clearPassword = passwd;
    cryptedPassword = currentKey.cryptToHex(clearPassword);
  }

  /**
   * @return the passwordFile
   */
  public final File getPasswordFile() {
    return passwordFile;
  }

  /**
   * @param passwordFile the passwordFile to set
   *
   * @throws IOException
   */
  public final void setPasswordFile(final File passwordFile) {
    this.passwordFile = passwordFile;
  }

  /**
   * Save the Crypted Paswword to the File
   *
   * @throws IOException
   */
  public final void savePasswordFile() throws IOException {
    final FileOutputStream outputStream = new FileOutputStream(passwordFile);
    try {
      outputStream.write(cryptedPassword.getBytes(WaarpStringUtils.UTF8));
    } finally {
      FileUtils.close(outputStream);
    }
  }

  /**
   * Load the crypted password from the file
   *
   * @throws Exception
   */
  public final void loadPasswordFile() throws Exception {
    if (passwordFile.canRead()) {
      final int len = (int) passwordFile.length();
      final byte[] key = new byte[len];
      final FileInputStream inputStream;
      inputStream = new FileInputStream(passwordFile);
      DataInputStream dis = null;
      try {
        dis = new DataInputStream(inputStream);
        dis.readFully(key);
      } finally {
        if (dis != null) {
          FileUtils.close(dis);
        } else {
          FileUtils.close(inputStream);
        }
      }
      setCryptedPassword(new String(key, WaarpStringUtils.UTF8));
    } else {
      throw new CryptoException("Cannot read crypto file");
    }
  }

  /**
   * @return the cryptedPassword
   */
  public final String getCryptedPassword() {
    return cryptedPassword;
  }

  /**
   * @param cryptedPassword the cryptedPassword to set
   *
   * @throws Exception
   */
  public final void setCryptedPassword(final String cryptedPassword)
      throws Exception {
    this.cryptedPassword = cryptedPassword;
    clearPassword = currentKey.decryptHexInString(cryptedPassword);
  }

  /**
   * @return the clearPassword
   */
  public final String getClearPassword() {
    return clearPassword;
  }

}
