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
package org.waarp.gateway.ftp.exec;

import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.future.WaarpFuture;
import org.waarp.common.guid.GUID;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.gateway.kernel.session.CommandExecutorInterface;
import org.waarp.gateway.kernel.session.HttpAuthInterface;

import java.util.regex.Pattern;

/**
 * Abstract Executor class. If the command starts with "REFUSED", the command
 * will be refused for execution.
 * If "REFUSED" is set, the command "RETR" or "STOR" like operations will be
 * stopped at starting of
 * command.<br>
 * If the command starts with "EXECUTE", the following will be a command to be
 * executed.<br>
 * If the command starts with "JAVAEXECUTE", the following will be a command
 * through Java class to be
 * executed.<br>
 * If the command starts with "R66PREPARETRANSFER", the following will be a r66
 * prepare transfer execution
 * (asynchronous operation only).<br>
 * <p>
 * <p>
 * The following replacement are done dynamically before the command is
 * executed:<br>
 * - #BASEPATH# is replaced by the full path for the root of FTP Directory<br>
 * - #FILE# is replaced by the current file path relative to FTP Directory (so
 * #BASEPATH##FILE# is the full
 * path of the file)<br>
 * - #USER# is replaced by the username<br>
 * - #ACCOUNT# is replaced by the account<br>
 * - #COMMAND# is replaced by the command issued for the file<br>
 * - #SPECIALID# is replaced by the FTP id of the transfer (whatever in or
 * out)<br>
 * - #UUID# is replaced by a special UUID globally unique for the transfer, in
 * general to be placed in -info
 * part (for instance ##UUID## giving #uuid#)<br>
 */
public abstract class AbstractExecutor {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(AbstractExecutor.class);
  protected static final Pattern BLANK = WaarpStringUtils.BLANK;

  protected static final String USER = "#USER#";
  protected static final String ACCOUNT = "#ACCOUNT#";
  protected static final String BASEPATH = "#BASEPATH#";
  protected static final String FILE = "#FILE#";
  protected static final String COMMAND = "#COMMAND#";
  protected static final String SPECIALID = "#SPECIALID#";
  protected static final String S_UUID = "#UUID#";

  protected static final String REFUSED = "REFUSED";
  protected static final String NONE = "NONE";
  protected static final String EXECUTE = "EXECUTE";
  protected static final String JAVAEXECUTE = "JAVAEXECUTE";
  protected static final String R66PREPARETRANSFER = "R66PREPARETRANSFER";

  protected static final int T_REFUSED = -1;
  protected static final int T_NONE = 0;
  protected static final int T_EXECUTE = 1;
  protected static final int T_R_66_PREPARETRANSFER = 2;
  protected static final int T_JAVAEXECUTE = 3;

  protected static CommandExecutor commandExecutor;

  /**
   * For OpenR66 access
   */
  public static boolean useDatabase;

  /**
   * Local Exec Daemon is used or not for execution of external commands
   */
  public static boolean useLocalExec;

  public static class CommandExecutor implements CommandExecutorInterface {
    /**
     * Retrieve External Command
     */
    public final String pretrCMD;
    public final int pretrType;
    private boolean pretrRefused;
    /**
     * Retrieve Delay (0 = unlimited)
     */
    private long pretrDelay;
    /**
     * Store External Command
     */
    public final String pstorCMD;
    public final int pstorType;
    private boolean pstorRefused;
    /**
     * Store Delay (0 = unlimited)
     */
    private long pstorDelay;

    /**
     * @param retrieve
     * @param retrDelay
     * @param store
     * @param storDelay
     */
    public CommandExecutor(final String retrieve, final long retrDelay,
                           final String store, final long storDelay) {
      if (retrieve == null || retrieve.trim().length() == 0) {
        pretrCMD = commandExecutor.pretrCMD;
        pretrType = commandExecutor.pretrType;
        setPretrRefused(commandExecutor.isPretrRefused());
      } else if (isRefused(retrieve)) {
        pretrCMD = REFUSED;
        pretrType = T_REFUSED;
        setPretrRefused(true);
      } else {
        if (isExecute(retrieve)) {
          pretrCMD = getExecuteCmd(retrieve);
          pretrType = T_EXECUTE;
        } else if (isR66PrepareTransfer(retrieve)) {
          pretrCMD = getR66PrepareTransferCmd(retrieve);
          pretrType = T_R_66_PREPARETRANSFER;
          useDatabase = true;
        } else if (isJavaExecute(retrieve)) {
          pretrCMD = getJavaExecuteCmd(retrieve);
          pretrType = T_JAVAEXECUTE;
        } else {
          // Default NONE
          pretrCMD = getNone(retrieve);
          pretrType = T_NONE;
        }
      }
      setPretrDelay(retrDelay);
      if (store == null || store.trim().length() == 0) {
        pstorCMD = commandExecutor.pstorCMD;
        setPstorRefused(commandExecutor.isPstorRefused());
        pstorType = commandExecutor.pstorType;
      } else if (isRefused(store)) {
        pstorCMD = REFUSED;
        setPstorRefused(true);
        pstorType = T_REFUSED;
      } else {
        if (isExecute(store)) {
          pstorCMD = getExecuteCmd(store);
          pstorType = T_EXECUTE;
        } else if (isR66PrepareTransfer(store)) {
          pstorCMD = getR66PrepareTransferCmd(store);
          pstorType = T_R_66_PREPARETRANSFER;
          useDatabase = true;
        } else if (isJavaExecute(store)) {
          pstorCMD = getJavaExecuteCmd(store);
          pstorType = T_JAVAEXECUTE;
        } else {
          // Default NONE
          pstorCMD = getNone(store);
          pstorType = T_NONE;
        }
      }
      setPstorDelay(storDelay);
    }

    private static String getNone(final String cmd) {
      return cmd.substring(NONE.length()).trim();
    }

    private static String getExecuteCmd(final String cmd) {
      return cmd.substring(EXECUTE.length()).trim();
    }

    private static String getJavaExecuteCmd(final String cmd) {
      return cmd.substring(JAVAEXECUTE.length()).trim();
    }

    private static String getR66PrepareTransferCmd(final String cmd) {
      return cmd.substring(R66PREPARETRANSFER.length()).trim();
    }

    private static boolean isRefused(final String cmd) {
      return cmd.startsWith(REFUSED);
    }

    private static boolean isExecute(final String cmd) {
      return cmd.startsWith(EXECUTE);
    }

    private static boolean isJavaExecute(final String cmd) {
      return cmd.startsWith(JAVAEXECUTE);
    }

    private static boolean isR66PrepareTransfer(final String cmd) {
      return cmd.startsWith(R66PREPARETRANSFER);
    }

    @Override
    public boolean isValidOperation(final boolean isStore) {
      if (isStore && isPstorRefused()) {
        logger.info("STORe like operations REFUSED");
        return false;
      } else if (!isStore && isPretrRefused()) {
        logger.info("RETRieve operations REFUSED");
        return false;
      }
      return true;
    }

    @Override
    public String getRetrType() {
      switch (pretrType) {
        case T_REFUSED:
          return REFUSED;
        case T_EXECUTE:
          return EXECUTE;
        case T_R_66_PREPARETRANSFER:
          return R66PREPARETRANSFER;
        case T_JAVAEXECUTE:
          return JAVAEXECUTE;
        default:
          return NONE;
      }
    }

    @Override
    public String getStorType() {
      switch (pstorType) {
        case T_REFUSED:
          return REFUSED;
        case T_EXECUTE:
          return EXECUTE;
        case T_R_66_PREPARETRANSFER:
          return R66PREPARETRANSFER;
        case T_JAVAEXECUTE:
          return JAVAEXECUTE;
        default:
          return NONE;
      }
    }

    /**
     * @return the pretrRefused
     */
    public boolean isPretrRefused() {
      return pretrRefused;
    }

    /**
     * @param pretrRefused the pretrRefused to set
     */
    public void setPretrRefused(final boolean pretrRefused) {
      this.pretrRefused = pretrRefused;
    }

    /**
     * @return the pretrDelay
     */
    public long getPretrDelay() {
      return pretrDelay;
    }

    /**
     * @param pretrDelay the pretrDelay to set
     */
    public void setPretrDelay(final long pretrDelay) {
      this.pretrDelay = pretrDelay;
    }

    /**
     * @return the pstorRefused
     */
    public boolean isPstorRefused() {
      return pstorRefused;
    }

    /**
     * @param pstorRefused the pstorRefused to set
     */
    public void setPstorRefused(final boolean pstorRefused) {
      this.pstorRefused = pstorRefused;
    }

    /**
     * @return the pstorDelay
     */
    public long getPstorDelay() {
      return pstorDelay;
    }

    /**
     * @param pstorDelay the pstorDelay to set
     */
    public void setPstorDelay(final long pstorDelay) {
      this.pstorDelay = pstorDelay;
    }
  }

  /**
   * Initialize the Executor with the correct command and delay
   *
   * @param retrieve
   * @param retrDelay
   * @param store
   * @param storDelay
   */
  public static void initializeExecutor(final String retrieve,
                                        final long retrDelay,
                                        final String store,
                                        final long storDelay) {
    commandExecutor =
        new CommandExecutor(retrieve, retrDelay, store, storDelay);
    logger.info(
        "Executor configured as [RETR: " + commandExecutor.getRetrType() + ':' +
        commandExecutor.pretrCMD + ':' + commandExecutor.getPretrDelay() + ':' +
        commandExecutor.isPretrRefused() + "] [STOR: " +
        commandExecutor.getStorType() + ':' + commandExecutor.pstorCMD + ':' +
        commandExecutor.getPstorDelay() + ':' +
        commandExecutor.isPstorRefused() + ']');
  }

  /**
   * Check if the given operation is allowed Globally
   *
   * @param isStore
   *
   * @return True if allowed, else False
   */
  public static boolean isValidOperation(final boolean isStore) {
    return commandExecutor.isValidOperation(isStore);
  }

  /**
   * @param auth the current Authentication
   * @param args containing in that order "User Account BaseDir
   *     FilePath(relative to BaseDir)
   *     Command"
   * @param isStore True for a STORE like operation, else False
   * @param futureCompletion
   */
  public static AbstractExecutor createAbstractExecutor(
      final HttpAuthInterface auth, final String[] args, final boolean isStore,
      final WaarpFuture futureCompletion) {
    if (isStore) {
      CommandExecutor executor = (CommandExecutor) auth.getCommandExecutor();
      if (executor == null) {
        executor = commandExecutor;
      } else if (executor.pstorType == T_NONE) {
        final String replaced = getPreparedCommand(executor.pstorCMD, args);
        return new NoTaskExecutor(replaced, executor.getPstorDelay(),
                                  futureCompletion);
      }
      if (executor.isPstorRefused()) {
        logger.error("STORe like operation REFUSED");
        futureCompletion.cancel();
        return null;
      }
      final String replaced = getPreparedCommand(executor.pstorCMD, args);
      switch (executor.pstorType) {
        case T_REFUSED:
          logger.error("STORe like operation REFUSED");
          futureCompletion.cancel();
          return null;
        case T_EXECUTE:
          return new ExecuteExecutor(replaced, executor.getPstorDelay(),
                                     futureCompletion);
        case T_JAVAEXECUTE:
          return new JavaExecutor(replaced, executor.getPstorDelay(),
                                  futureCompletion);
        case T_R_66_PREPARETRANSFER:
          return new R66PreparedTransferExecutor(replaced,
                                                 executor.getPstorDelay(),
                                                 futureCompletion);
        default:
          return new NoTaskExecutor(replaced, executor.getPstorDelay(),
                                    futureCompletion);
      }
    } else {
      CommandExecutor executor = (CommandExecutor) auth.getCommandExecutor();
      if (executor == null) {
        executor = commandExecutor;
      } else if (executor.pretrType == T_NONE) {
        final String replaced = getPreparedCommand(executor.pretrCMD, args);
        return new NoTaskExecutor(replaced, executor.getPretrDelay(),
                                  futureCompletion);
      }
      if (executor.isPretrRefused()) {
        logger.error("RETRieve operation REFUSED");
        futureCompletion.cancel();
        return null;
      }
      final String replaced = getPreparedCommand(executor.pretrCMD, args);
      switch (executor.pretrType) {
        case T_REFUSED:
          logger.error("RETRieve operation REFUSED");
          futureCompletion.cancel();
          return null;
        case T_EXECUTE:
          return new ExecuteExecutor(replaced, executor.getPretrDelay(),
                                     futureCompletion);
        case T_JAVAEXECUTE:
          return new JavaExecutor(replaced, executor.getPretrDelay(),
                                  futureCompletion);
        case T_R_66_PREPARETRANSFER:
          return new R66PreparedTransferExecutor(replaced,
                                                 executor.getPretrDelay(),
                                                 futureCompletion);
        default:
          return new NoTaskExecutor(replaced, executor.getPretrDelay(),
                                    futureCompletion);
      }
    }
  }

  /**
   * @param command
   * @param args as {User, Account, BaseDir, FilePath(relative to
   *     BaseDir),
   *     Command}
   *
   * @return the prepared command
   */
  public static String getPreparedCommand(final String command,
                                          final String[] args) {
    final StringBuilder builder = new StringBuilder(command);
    logger.debug("Will replace value in " + command + " with User=" + args[0] +
                 ":Acct=" + args[1] + ":Base=" + args[2] + ":File=" + args[3] +
                 ":Cmd=" + args[4]);
    replaceAll(builder, USER, args[0]);
    replaceAll(builder, ACCOUNT, args[1]);
    replaceAll(builder, BASEPATH, args[2]);
    replaceAll(builder, FILE, args[3]);
    replaceAll(builder, COMMAND, args[4]);
    replaceAll(builder, SPECIALID, args[5]);
    if (builder.indexOf(S_UUID) > 0) {
      replaceAll(builder, S_UUID, new GUID().toString());
    }
    logger.debug("Result: {}", builder);
    return builder.toString();
  }

  /**
   * Make a replacement of first "find" string by "replace" string into the
   * StringBuilder
   *
   * @param builder
   * @param find
   * @param replace
   */
  public static boolean replace(final StringBuilder builder, final String find,
                                final String replace) {
    final int start = builder.indexOf(find);
    if (start == -1) {
      return false;
    }
    final int end = start + find.length();
    builder.replace(start, end, replace);
    return true;
  }

  /**
   * Make replacement of all "find" string by "replace" string into the
   * StringBuilder
   *
   * @param builder
   * @param find
   * @param replace
   */
  public static void replaceAll(final StringBuilder builder, final String find,
                                final String replace) {
    while (replace(builder, find, replace)) {
      // nothing
    }
  }

  public static CommandExecutor getCommandExecutor() {
    return commandExecutor;
  }

  public abstract void run() throws CommandAbstractException;
}
