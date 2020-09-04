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

package org.waarp.gateway.ftp.file;

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.gateway.ftp.exec.AbstractExecutor.CommandExecutor;

/**
 * Simple Authentication based on a previously load XML file.
 */
public class SimpleAuth {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(SimpleAuth.class);

  /**
   * User name
   */
  private String user;

  /**
   * Password
   */
  private String password;

  /**
   * Multiple accounts
   */
  private String[] accounts;

  /**
   * Is the current user an administrator (which can shutdown or change
   * bandwidth limitation)
   */
  private boolean isAdmin;
  /**
   * Specific Store command for this user
   */
  private String storCmd;
  /**
   * Specific Store command delay for this user
   */
  private long storDelay;
  /**
   * Specific Retrieve command for this user
   */
  private String retrCmd;
  /**
   * Specific Retrieve command delay for this user
   */
  private long retrDelay;

  private CommandExecutor commandExecutor;

  /**
   * @param user
   * @param password
   * @param accounts
   * @param storCmd
   * @param storDelay
   * @param retrCmd
   * @param retrDelay
   */
  public SimpleAuth(final String user, final String password,
                    final String[] accounts, final String storCmd,
                    final long storDelay, final String retrCmd,
                    final long retrDelay) {
    setUser(user);
    setPassword(password);
    setAccounts(accounts);
    setStorCmd(storCmd);
    setStorDelay(storDelay);
    setRetrCmd(retrCmd);
    setRetrDelay(retrDelay);
    setCommandExecutor(
        new CommandExecutor(retrCmd, retrDelay, storCmd, storDelay));
    logger.info("Executor for " + user + " configured as [RETR: " +
                getCommandExecutor().getRetrType() + ':' +
                getCommandExecutor().pretrCMD + ':' +
                getCommandExecutor().getPretrDelay() + ':' +
                getCommandExecutor().isPretrRefused() + "] [STOR: " +
                getCommandExecutor().getStorType() + ':' +
                getCommandExecutor().pstorCMD + ':' +
                getCommandExecutor().getPstorDelay() + ':' +
                getCommandExecutor().isPstorRefused() + ']');
  }

  /**
   * Is the given password a valid one
   *
   * @param newpassword
   *
   * @return True if the password is valid (or any password is valid)
   */
  public boolean isPasswordValid(final String newpassword) {
    if (getPassword() == null) {
      return true;
    }
    if (newpassword == null) {
      return false;
    }
    return getPassword().equals(newpassword);
  }

  /**
   * Is the given account a valid one
   *
   * @param account
   *
   * @return True if the account is valid (or any account is valid)
   */
  public boolean isAccountValid(final String account) {
    if (getAccounts() == null) {
      logger.debug("No account needed");
      return true;
    }
    if (account == null) {
      logger.debug("No account given");
      return false;
    }
    for (final String acct : getAccounts()) {
      if (acct.equals(account)) {
        logger.debug("Account found");
        return true;
      }
    }
    logger.debug("No account found");
    return false;
  }

  /**
   * @param isAdmin True if the user should be an administrator
   */
  public void setAdmin(final boolean isAdmin) {
    this.isAdmin = isAdmin;
  }

  /**
   * @return the user
   */
  public String getUser() {
    return user;
  }

  /**
   * @param user the user to set
   */
  private void setUser(final String user) {
    this.user = user;
  }

  /**
   * @return the password
   */
  public String getPassword() {
    return password;
  }

  /**
   * @param password the password to set
   */
  private void setPassword(final String password) {
    this.password = password;
  }

  /**
   * @return the accounts
   */
  public String[] getAccounts() {
    return accounts;
  }

  /**
   * @param accounts the accounts to set
   */
  private void setAccounts(final String[] accounts) {
    this.accounts = accounts;
  }

  /**
   * @return the isAdmin
   */
  public boolean isAdmin() {
    return isAdmin;
  }

  /**
   * @return the storCmd
   */
  public String getStorCmd() {
    return storCmd;
  }

  /**
   * @param storCmd the storCmd to set
   */
  private void setStorCmd(final String storCmd) {
    this.storCmd = storCmd;
  }

  /**
   * @return the storDelay
   */
  public long getStorDelay() {
    return storDelay;
  }

  /**
   * @param storDelay the storDelay to set
   */
  private void setStorDelay(final long storDelay) {
    this.storDelay = storDelay;
  }

  /**
   * @return the retrCmd
   */
  public String getRetrCmd() {
    return retrCmd;
  }

  /**
   * @param retrCmd the retrCmd to set
   */
  private void setRetrCmd(final String retrCmd) {
    this.retrCmd = retrCmd;
  }

  /**
   * @return the retrDelay
   */
  public long getRetrDelay() {
    return retrDelay;
  }

  /**
   * @param retrDelay the retrDelay to set
   */
  private void setRetrDelay(final long retrDelay) {
    this.retrDelay = retrDelay;
  }

  /**
   * @return the commandExecutor
   */
  public CommandExecutor getCommandExecutor() {
    return commandExecutor;
  }

  /**
   * @param commandExecutor the commandExecutor to set
   */
  private void setCommandExecutor(final CommandExecutor commandExecutor) {
    this.commandExecutor = commandExecutor;
  }
}
