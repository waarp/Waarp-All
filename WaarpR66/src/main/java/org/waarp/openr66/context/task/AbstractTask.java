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
package org.waarp.openr66.context.task;

import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.filesystem.R66Dir;
import org.waarp.openr66.context.filesystem.R66File;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoSslException;
import org.waarp.openr66.protocol.utils.R66Future;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract implementation of task
 */
public abstract class AbstractTask implements Runnable {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(AbstractTask.class);
  protected static final Pattern BLANK = WaarpStringUtils.BLANK;
  private static final Pattern COMPILE_HASH =
      Pattern.compile("#", Pattern.LITERAL);
  /**
   * Current full path of current FILENAME
   */
  public static final String TRUEFULLPATH = "#TRUEFULLPATH#";

  /**
   * Current FILENAME (basename) (change in retrieval part)
   */
  public static final String TRUEFILENAME = "#TRUEFILENAME#";
  /**
   * Current full path of Original FILENAME (as transmitted) (before changing
   * in
   * retrieval part)
   */
  public static final String ORIGINALFULLPATH = "#ORIGINALFULLPATH#";

  /**
   * Original FILENAME (basename) (before changing in retrieval part)
   */
  public static final String ORIGINALFILENAME = "#ORIGINALFILENAME#";

  /**
   * Size of the current FILE
   */
  public static final String FILESIZE = "#FILESIZE#";

  /**
   * Current full path of current RULE
   */
  public static final String RULE = "#RULE#";

  /**
   * Date in yyyyMMdd format
   */
  public static final String DATE = "#DATE#";

  /**
   * Hour in HHmmss format
   */
  public static final String HOUR = "#HOUR#";

  /**
   * Remote host id (if not the initiator of the call)
   */
  public static final String REMOTEHOST = "#REMOTEHOST#";

  /**
   * Remote host address
   */
  public static final String REMOTEHOSTADDR = "#REMOTEHOSTADDR#";

  /**
   * Local host id
   */
  public static final String LOCALHOST = "#LOCALHOST#";

  /**
   * Local host address
   */
  public static final String LOCALHOSTADDR = "#LOCALHOSTADDR#";

  /**
   * Transfer id
   */
  public static final String TRANSFERID = "#TRANSFERID#";

  /**
   * Requester Host
   */
  public static final String REQUESTERHOST = "#REQUESTERHOST#";

  /**
   * Requested Host
   */
  public static final String REQUESTEDHOST = "#REQUESTEDHOST#";

  /**
   * Full Transfer id (TRANSFERID_REQUESTERHOST_REQUESTEDHOST)
   */
  public static final String FULLTRANSFERID = "#FULLTRANSFERID#";

  /**
   * Current or final RANK of block
   */
  public static final String RANKTRANSFER = "#RANKTRANSFER#";

  /**
   * Block size used
   */
  public static final String BLOCKSIZE = "#BLOCKSIZE#";

  /**
   * IN Path used
   */
  public static final String INPATH = "#INPATH#";

  /**
   * OUT Path used
   */
  public static final String OUTPATH = "#OUTPATH#";

  /**
   * WORK Path used
   */
  public static final String WORKPATH = "#WORKPATH#";

  /**
   * ARCH Path used
   */
  public static final String ARCHPATH = "#ARCHPATH#";

  /**
   * HOME Path used
   */
  public static final String HOMEPATH = "#HOMEPATH#";
  /**
   * Last Current Error Message
   */
  public static final String ERRORMSG = "#ERRORMSG#";
  /**
   * Last Current Error Code
   */
  public static final String ERRORCODE = "#ERRORCODE#";
  /**
   * Last Current Error Code in Full String
   */
  public static final String ERRORSTRCODE = "#ERRORSTRCODE#";
  /**
   * If specified, no Wait for Task Validation (default is wait)
   */
  public static final String NOWAIT = "#NOWAIT#";
  /**
   * If specified, use the LocalExec Daemon specified in the global
   * configuration (default no usage of
   * LocalExec)
   */
  public static final String LOCALEXEC = "#LOCALEXEC#";
  /**
   * If specified, and if the partner allows Compression (from 3.6.0 and
   * if compression is enabled), this will allow one transfer through
   * TransferInformation to use compression by block.
   */
  public static final String COMPRESS = "#COMPRESS#";

  /**
   * Type of operation
   */
  final TaskType type;

  /**
   * Argument from Rule
   */
  final String argRule;

  /**
   * Delay from Rule (if applicable)
   */
  final int delay;

  /**
   * Argument from Transfer
   */
  final String argTransfer;

  /**
   * Current session
   */
  final R66Session session;

  /**
   * R66Future of completion
   */
  final R66Future futureCompletion;
  /**
   * Do we wait for a validation of the task ? Default = True
   */
  boolean waitForValidation = true;
  /**
   * Do we need to use LocalExec for an Exec Task ? Default = False
   */
  boolean useLocalExec;

  /**
   * Constructor
   *
   * @param type
   * @param delay
   * @param argRule
   * @param argTransfer
   * @param session
   */
  AbstractTask(final TaskType type, final int delay, final String argRule,
               final String argTransfer, final R66Session session) {
    this.type = type;
    this.delay = delay;
    if (argRule != null) {
      this.argRule = argRule.replaceAll(WaarpStringUtils.BLANK_REGEX, " ");
    } else {
      this.argRule = null;
    }
    if (argTransfer != null) {
      this.argTransfer =
          argTransfer.replaceAll(WaarpStringUtils.BLANK_REGEX, " ");
    } else {
      this.argTransfer = null;
    }
    this.session = session;
    futureCompletion = new R66Future(true);
  }

  /**
   * @return the TaskType of this AbstractTask
   */
  public TaskType getType() {
    return type;
  }

  /**
   * @return True if the operation is in success status
   */
  public boolean isSuccess() {
    futureCompletion.awaitOrInterruptible();
    return futureCompletion.isSuccess();
  }

  /**
   * @return the R66Future of completion
   */
  public R66Future getFutureCompletion() {
    return futureCompletion;
  }

  /**
   * @param arg
   * @param session
   *
   * @return True if the argument contains #COMPRESS# and if the current host
   *     allow compression
   */
  public static boolean isCompressionRequested(final String arg,
                                               final R66Session session) {
    logger.debug("isCompEnabled {} {}",
                 Configuration.configuration.isCompressionAvailable(),
                 arg.contains(COMPRESS));
    if (session == null) {
      return Configuration.configuration.isCompressionAvailable() &&
             arg.contains(COMPRESS);
    } else {
      return Configuration.configuration.isCompressionAvailable() &&
             session.isCompressionEnabled() && arg.contains(COMPRESS);
    }
  }

  /**
   * @param arg as the Format string where FIXED items will be
   *     replaced by
   *     context values and next using
   *     argFormat as format second argument; this arg comes from the
   *     rule
   *     itself
   * @param argFormat as format second argument; this argFormat comes
   *     from
   *     the transfer Information itself
   *
   * @return The string with replaced values from context and second argument
   */
  protected String getReplacedValue(final String arg,
                                    final Object[] argFormat) {
    final StringBuilder builder = new StringBuilder(arg);
    // check NOWAIT and LOCALEXEC and COMPRESS
    if (arg.contains(NOWAIT)) {
      waitForValidation = false;
      WaarpStringUtils.replaceAll(builder, NOWAIT, "");
    }
    if (arg.contains(LOCALEXEC)) {
      useLocalExec = true;
      WaarpStringUtils.replaceAll(builder, LOCALEXEC, "");
    }
    if (arg.contains(COMPRESS)) {
      WaarpStringUtils.replaceAll(builder, COMPRESS, "");
    }
    substituteFile(builder);
    final DbTaskRunner runner = substituteRunner(builder);
    substituteDate(builder);
    substituteHost(builder);
    WaarpStringUtils.replaceAll(builder, BLOCKSIZE,
                                Integer.toString(session.getBlockSize()));
    substitutePath(builder, runner);
    WaarpStringUtils.replaceAll(builder, HOMEPATH,
                                Configuration.configuration.getBaseDirectory());
    substituteErrorCode(builder);
    // finalname
    if (argFormat != null && argFormat.length > 0) {
      try {
        return String.format(builder.toString(), argFormat);
      } catch (final Exception e) {
        // ignored error since bad argument in static rule info
        logger.error("Bad format in Rule: {" + builder + "} " + e.getMessage());
      }
    }
    return builder.toString();
  }

  /**
   * Apply transferInfo substitutions
   *
   * @param line the line to format
   *
   * @return the line after substitutions
   */
  protected String applyTransferSubstitutions(final String line) {
    final Object[] argFormat = BLANK.split(argTransfer);
    if (argFormat != null && argFormat.length > 0) {
      try {
        return String.format(line, argFormat);
      } catch (final Exception e) {
        // ignored error since bad argument in static rule info
        logger.error("Bad format in Rule: {" + line + "} " + e.getMessage());
      }
    }
    return line;
  }

  private void substituteErrorCode(final StringBuilder builder) {
    if (session.getLocalChannelReference() == null) {
      WaarpStringUtils.replaceAll(builder, ERRORMSG, "NoError");
      WaarpStringUtils.replaceAll(builder, ERRORCODE, "-");
      WaarpStringUtils
          .replaceAll(builder, ERRORSTRCODE, ErrorCode.Unknown.name());
    } else {
      try {
        WaarpStringUtils.replaceAll(builder, ERRORMSG,
                                    session.getLocalChannelReference()
                                           .getErrorMessage());
      } catch (final NullPointerException e) {
        WaarpStringUtils.replaceAll(builder, ERRORMSG, "NoError");
      }
      try {
        WaarpStringUtils.replaceAll(builder, ERRORCODE,
                                    session.getLocalChannelReference()
                                           .getCurrentCode().getCode());
      } catch (final NullPointerException e) {
        WaarpStringUtils.replaceAll(builder, ERRORCODE, "-");
      }
      try {
        WaarpStringUtils.replaceAll(builder, ERRORSTRCODE,
                                    session.getLocalChannelReference()
                                           .getCurrentCode().name());
      } catch (final NullPointerException e) {
        WaarpStringUtils
            .replaceAll(builder, ERRORSTRCODE, ErrorCode.Unknown.name());
      }
    }
  }

  private void substitutePath(final StringBuilder builder,
                              final DbTaskRunner runner) {
    R66Dir dir = new R66Dir(session);
    if (runner != null) {
      if (runner.isRecvThrough() || runner.isSendThrough()) {
        try {
          dir.changeDirectoryNotChecked(runner.getRule().getRecvPath());
          WaarpStringUtils.replaceAll(builder, INPATH, dir.getFullPath());
        } catch (final CommandAbstractException ignored) {
          // nothing
        }
        dir = new R66Dir(session);
        try {
          dir.changeDirectoryNotChecked(runner.getRule().getSendPath());
          WaarpStringUtils.replaceAll(builder, OUTPATH, dir.getFullPath());
        } catch (final CommandAbstractException ignored) {
          // nothing
        }
        dir = new R66Dir(session);
        try {
          dir.changeDirectoryNotChecked(runner.getRule().getWorkPath());
          WaarpStringUtils.replaceAll(builder, WORKPATH, dir.getFullPath());
        } catch (final CommandAbstractException ignored) {
          // nothing
        }
        dir = new R66Dir(session);
        try {
          dir.changeDirectoryNotChecked(runner.getRule().getArchivePath());
          WaarpStringUtils.replaceAll(builder, ARCHPATH, dir.getFullPath());
        } catch (final CommandAbstractException ignored) {
          // nothing
        }
      } else {
        try {
          dir.changeDirectory(runner.getRule().getRecvPath());
          WaarpStringUtils.replaceAll(builder, INPATH, dir.getFullPath());
        } catch (final CommandAbstractException ignored) {
          // nothing
        }
        dir = new R66Dir(session);
        try {
          dir.changeDirectory(runner.getRule().getSendPath());
          WaarpStringUtils.replaceAll(builder, OUTPATH, dir.getFullPath());
        } catch (final CommandAbstractException ignored) {
          // nothing
        }
        dir = new R66Dir(session);
        try {
          dir.changeDirectory(runner.getRule().getWorkPath());
          WaarpStringUtils.replaceAll(builder, WORKPATH, dir.getFullPath());
        } catch (final CommandAbstractException ignored) {
          // nothing
        }
        dir = new R66Dir(session);
        try {
          dir.changeDirectory(runner.getRule().getArchivePath());
          WaarpStringUtils.replaceAll(builder, ARCHPATH, dir.getFullPath());
        } catch (final CommandAbstractException ignored) {
          // nothing
        }
      }
    } else {
      try {
        dir.changeDirectory(Configuration.configuration.getInPath());
        WaarpStringUtils.replaceAll(builder, INPATH, dir.getFullPath());
      } catch (final CommandAbstractException ignored) {
        // nothing
      }
      dir = new R66Dir(session);
      try {
        dir.changeDirectory(Configuration.configuration.getOutPath());
        WaarpStringUtils.replaceAll(builder, OUTPATH, dir.getFullPath());
      } catch (final CommandAbstractException ignored) {
        // nothing
      }
      dir = new R66Dir(session);
      try {
        dir.changeDirectory(Configuration.configuration.getWorkingPath());
        WaarpStringUtils.replaceAll(builder, WORKPATH, dir.getFullPath());
      } catch (final CommandAbstractException ignored) {
        // nothing
      }
      dir = new R66Dir(session);
      try {
        dir.changeDirectory(Configuration.configuration.getArchivePath());
        WaarpStringUtils.replaceAll(builder, ARCHPATH, dir.getFullPath());
      } catch (final CommandAbstractException ignored) {
        // nothing
      }
    }
  }

  private void substituteHost(final StringBuilder builder) {
    if (session.getAuth() != null) {
      WaarpStringUtils
          .replaceAll(builder, REMOTEHOST, session.getAuth().getUser());
      try {
        WaarpStringUtils.replaceAll(builder, LOCALHOST,
                                    Configuration.configuration
                                        .getHostId(session.getAuth().isSsl()));
      } catch (final OpenR66ProtocolNoSslException e) {
        // replace by standard name
        WaarpStringUtils.replaceAll(builder, LOCALHOST,
                                    Configuration.configuration.getHostId());
      }
    }
    if (session.getRemoteAddress() != null) {
      WaarpStringUtils.replaceAll(builder, REMOTEHOSTADDR,
                                  session.getRemoteAddress().toString());
      WaarpStringUtils.replaceAll(builder, LOCALHOSTADDR,
                                  session.getLocalAddress().toString());
    } else {
      WaarpStringUtils.replaceAll(builder, REMOTEHOSTADDR, "unknown");
      WaarpStringUtils.replaceAll(builder, LOCALHOSTADDR, "unknown");
    }
  }

  private void substituteDate(final StringBuilder builder) {
    DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
    final Date date = new Date();
    WaarpStringUtils.replaceAll(builder, DATE, dateFormat.format(date));
    dateFormat = new SimpleDateFormat("HHmmss");
    WaarpStringUtils.replaceAll(builder, HOUR, dateFormat.format(date));
  }

  private DbTaskRunner substituteRunner(final StringBuilder builder) {
    final DbTaskRunner runner = session.getRunner();
    if (runner != null) {
      WaarpStringUtils
          .replaceAll(builder, ORIGINALFULLPATH, runner.getOriginalFilename());
      WaarpStringUtils.replaceAll(builder, ORIGINALFILENAME, R66File
          .getBasename(runner.getOriginalFilename()));
      WaarpStringUtils.replaceAll(builder, RULE, runner.getRuleId());
      WaarpStringUtils.replaceAll(builder, TRANSFERID,
                                  Long.toString(runner.getSpecialId()));
      final String requester = runner.getRequester();
      WaarpStringUtils.replaceAll(builder, REQUESTERHOST, requester);
      final String requested = runner.getRequested();
      WaarpStringUtils.replaceAll(builder, REQUESTEDHOST, requested);
      WaarpStringUtils.replaceAll(builder, FULLTRANSFERID,
                                  runner.getSpecialId() + "_" + requester +
                                  '_' + requested);
      WaarpStringUtils.replaceAll(builder, RANKTRANSFER,
                                  Integer.toString(runner.getRank()));
    }
    return runner;
  }

  private void substituteFile(final StringBuilder builder) {
    File trueFile = null;
    if (session.getFile() != null) {
      trueFile = session.getFile().getTrueFile();
    }
    if (trueFile != null) {
      WaarpStringUtils
          .replaceAll(builder, TRUEFULLPATH, trueFile.getAbsolutePath());
      WaarpStringUtils.replaceAll(builder, TRUEFILENAME, R66Dir
          .getFinalUniqueFilename(session.getFile()));
      WaarpStringUtils
          .replaceAll(builder, FILESIZE, Long.toString(trueFile.length()));
    } else {
      WaarpStringUtils.replaceAll(builder, TRUEFULLPATH, "nofile");
      WaarpStringUtils.replaceAll(builder, TRUEFILENAME, "nofile");
      WaarpStringUtils.replaceAll(builder, FILESIZE, "0");
    }
  }

  // Using CommentLine format

  /**
   * Generates a substitution map as expected by Apache Commons Exec
   * CommandLine
   */
  protected Map<String, Object> getSubstitutionMap() {
    final Map<String, Object> rv = new HashMap<String, Object>();
    rv.put(
        COMPILE_HASH.matcher(NOWAIT).replaceAll(Matcher.quoteReplacement("")),
        "");
    rv.put(COMPILE_HASH.matcher(LOCALEXEC)
                       .replaceAll(Matcher.quoteReplacement("")), "");
    rv.put(
        COMPILE_HASH.matcher(COMPRESS).replaceAll(Matcher.quoteReplacement("")),
        "");

    substituteFile(rv);

    final DbTaskRunner runner = substituteRunner(rv);

    substituteDate(rv);

    substituteHost(rv);
    rv.put(COMPILE_HASH.matcher(BLOCKSIZE)
                       .replaceAll(Matcher.quoteReplacement("")),
           session.getBlockSize());

    final R66Dir dir = new R66Dir(session);
    substitutePath(rv, runner, dir);
    rv.put(
        COMPILE_HASH.matcher(HOMEPATH).replaceAll(Matcher.quoteReplacement("")),
        Configuration.configuration.getBaseDirectory());
    substituteErrorCode(rv);
    return rv;
  }

  private void substituteErrorCode(final Map<String, Object> rv) {
    if (session.getLocalChannelReference() == null) {
      rv.put(COMPILE_HASH.matcher(ERRORMSG)
                         .replaceAll(Matcher.quoteReplacement("")), "NoError");
      rv.put(COMPILE_HASH.matcher(ERRORCODE)
                         .replaceAll(Matcher.quoteReplacement("")), "-");
      rv.put(COMPILE_HASH.matcher(ERRORSTRCODE)
                         .replaceAll(Matcher.quoteReplacement("")),
             ErrorCode.Unknown.name());
    } else {
      try {
        rv.put(COMPILE_HASH.matcher(ERRORMSG)
                           .replaceAll(Matcher.quoteReplacement("")),
               session.getLocalChannelReference().getErrorMessage());
      } catch (final NullPointerException e) {
        rv.put(COMPILE_HASH.matcher(ERRORMSG)
                           .replaceAll(Matcher.quoteReplacement("")),
               "NoError");
      }
      try {
        rv.put(COMPILE_HASH.matcher(ERRORCODE)
                           .replaceAll(Matcher.quoteReplacement("")),
               session.getLocalChannelReference().getCurrentCode().getCode());
      } catch (final NullPointerException e) {
        rv.put(COMPILE_HASH.matcher(ERRORCODE)
                           .replaceAll(Matcher.quoteReplacement("")), "-");
      }
      try {
        rv.put(COMPILE_HASH.matcher(ERRORSTRCODE)
                           .replaceAll(Matcher.quoteReplacement("")),
               session.getLocalChannelReference().getCurrentCode().name());
      } catch (final NullPointerException e) {
        rv.put(COMPILE_HASH.matcher(ERRORSTRCODE)
                           .replaceAll(Matcher.quoteReplacement("")),
               ErrorCode.Unknown.name());
      }
    }
  }

  private void substitutePath(final Map<String, Object> rv,
                              final DbTaskRunner runner, R66Dir dir) {
    if (runner != null) {
      if (runner.isRecvThrough() || runner.isSendThrough()) {
        try {
          dir.changeDirectoryNotChecked(runner.getRule().getRecvPath());
          rv.put(COMPILE_HASH.matcher(INPATH)
                             .replaceAll(Matcher.quoteReplacement("")),
                 dir.getFullPath());
        } catch (final CommandAbstractException ignored) {
          // nothing
        }
        dir = new R66Dir(session);
        try {
          dir.changeDirectoryNotChecked(runner.getRule().getSendPath());
          rv.put(COMPILE_HASH.matcher(OUTPATH)
                             .replaceAll(Matcher.quoteReplacement("")),
                 dir.getFullPath());
        } catch (final CommandAbstractException ignored) {
          // nothing
        }
        dir = new R66Dir(session);
        try {
          dir.changeDirectoryNotChecked(runner.getRule().getWorkPath());
          rv.put(COMPILE_HASH.matcher(WORKPATH)
                             .replaceAll(Matcher.quoteReplacement("")),
                 dir.getFullPath());
        } catch (final CommandAbstractException ignored) {
          // nothing
        }
        dir = new R66Dir(session);
        try {
          dir.changeDirectoryNotChecked(runner.getRule().getArchivePath());
          rv.put(COMPILE_HASH.matcher(ARCHPATH)
                             .replaceAll(Matcher.quoteReplacement("")),
                 dir.getFullPath());
        } catch (final CommandAbstractException ignored) {
          // nothing
        }
      } else {
        try {
          dir.changeDirectory(runner.getRule().getRecvPath());
          rv.put(COMPILE_HASH.matcher(INPATH)
                             .replaceAll(Matcher.quoteReplacement("")),
                 dir.getFullPath());
        } catch (final CommandAbstractException ignored) {
          // nothing
        }
        dir = new R66Dir(session);
        try {
          dir.changeDirectory(runner.getRule().getSendPath());
          rv.put(COMPILE_HASH.matcher(OUTPATH)
                             .replaceAll(Matcher.quoteReplacement("")),
                 dir.getFullPath());
        } catch (final CommandAbstractException ignored) {
          // nothing
        }
        dir = new R66Dir(session);
        try {
          dir.changeDirectory(runner.getRule().getWorkPath());
          rv.put(COMPILE_HASH.matcher(WORKPATH)
                             .replaceAll(Matcher.quoteReplacement("")),
                 dir.getFullPath());
        } catch (final CommandAbstractException ignored) {
          // nothing
        }
        dir = new R66Dir(session);
        try {
          dir.changeDirectory(runner.getRule().getArchivePath());
          rv.put(COMPILE_HASH.matcher(ARCHPATH)
                             .replaceAll(Matcher.quoteReplacement("")),
                 dir.getFullPath());
        } catch (final CommandAbstractException ignored) {
          // nothing
        }
      }
    } else {
      try {
        dir.changeDirectory(Configuration.configuration.getInPath());
        rv.put(COMPILE_HASH.matcher(INPATH)
                           .replaceAll(Matcher.quoteReplacement("")),
               dir.getFullPath());
      } catch (final CommandAbstractException ignored) {
        // nothing
      }
      dir = new R66Dir(session);
      try {
        dir.changeDirectory(Configuration.configuration.getOutPath());
        rv.put(COMPILE_HASH.matcher(OUTPATH)
                           .replaceAll(Matcher.quoteReplacement("")),
               dir.getFullPath());
      } catch (final CommandAbstractException ignored) {
        // nothing
      }
      dir = new R66Dir(session);
      try {
        dir.changeDirectory(Configuration.configuration.getWorkingPath());
        rv.put(COMPILE_HASH.matcher(WORKPATH)
                           .replaceAll(Matcher.quoteReplacement("")),
               dir.getFullPath());
      } catch (final CommandAbstractException ignored) {
        // nothing
      }
      dir = new R66Dir(session);
      try {
        dir.changeDirectory(Configuration.configuration.getArchivePath());
        rv.put(COMPILE_HASH.matcher(ARCHPATH)
                           .replaceAll(Matcher.quoteReplacement("")),
               dir.getFullPath());
      } catch (final CommandAbstractException ignored) {
        // nothing
      }
    }
  }

  private void substituteHost(final Map<String, Object> rv) {
    if (session.getAuth() != null) {
      rv.put(COMPILE_HASH.matcher(REMOTEHOST)
                         .replaceAll(Matcher.quoteReplacement("")),
             session.getAuth().getUser());
      String localhost;
      try {
        localhost =
            Configuration.configuration.getHostId(session.getAuth().isSsl());
      } catch (final OpenR66ProtocolNoSslException e) {
        // replace by standard name
        localhost = Configuration.configuration.getHostId();
      }
      rv.put(COMPILE_HASH.matcher(LOCALHOST)
                         .replaceAll(Matcher.quoteReplacement("")), localhost);
    }
    if (session.getRemoteAddress() != null) {
      rv.put(COMPILE_HASH.matcher(REMOTEHOSTADDR)
                         .replaceAll(Matcher.quoteReplacement("")),
             session.getRemoteAddress().toString());
      rv.put(COMPILE_HASH.matcher(LOCALHOSTADDR)
                         .replaceAll(Matcher.quoteReplacement("")),
             session.getLocalAddress().toString());
    } else {
      rv.put(COMPILE_HASH.matcher(REMOTEHOSTADDR)
                         .replaceAll(Matcher.quoteReplacement("")), "unknown");
      rv.put(COMPILE_HASH.matcher(LOCALHOSTADDR)
                         .replaceAll(Matcher.quoteReplacement("")), "unknown");
    }
  }

  private void substituteDate(final Map<String, Object> rv) {
    DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
    final Date date = new Date();
    rv.put(COMPILE_HASH.matcher(DATE).replaceAll(Matcher.quoteReplacement("")),
           dateFormat.format(date));
    dateFormat = new SimpleDateFormat("HHmmss");
    rv.put(COMPILE_HASH.matcher(HOUR).replaceAll(Matcher.quoteReplacement("")),
           dateFormat.format(date));
  }

  private DbTaskRunner substituteRunner(final Map<String, Object> rv) {
    final DbTaskRunner runner = session.getRunner();
    if (runner != null) {
      rv.put(COMPILE_HASH.matcher(ORIGINALFULLPATH)
                         .replaceAll(Matcher.quoteReplacement("")),
             runner.getOriginalFilename());
      rv.put(COMPILE_HASH.matcher(ORIGINALFILENAME)
                         .replaceAll(Matcher.quoteReplacement("")),
             R66File.getBasename(runner.getOriginalFilename()));
      rv.put(
          COMPILE_HASH.matcher(RULE).replaceAll(Matcher.quoteReplacement("")),
          runner.getRuleId());
    }
    if (runner != null) {
      rv.put(COMPILE_HASH.matcher(TRANSFERID)
                         .replaceAll(Matcher.quoteReplacement("")),
             runner.getSpecialId());
      rv.put(COMPILE_HASH.matcher(REQUESTERHOST)
                         .replaceAll(Matcher.quoteReplacement("")),
             runner.getRequester());
      rv.put(COMPILE_HASH.matcher(REQUESTEDHOST)
                         .replaceAll(Matcher.quoteReplacement("")),
             runner.getRequested());
      rv.put(COMPILE_HASH.matcher(FULLTRANSFERID)
                         .replaceAll(Matcher.quoteReplacement("")),
             runner.getSpecialId() + "_" + runner.getRequester() + '_' +
             runner.getRequested());
      rv.put(COMPILE_HASH.matcher(RANKTRANSFER)
                         .replaceAll(Matcher.quoteReplacement("")),
             Integer.toString(runner.getRank()));
    }
    return runner;
  }

  private void substituteFile(final Map<String, Object> rv) {
    File trueFile = null;
    if (session.getFile() != null) {
      trueFile = session.getFile().getTrueFile();
    }
    if (trueFile != null) {
      rv.put(COMPILE_HASH.matcher(TRUEFULLPATH)
                         .replaceAll(Matcher.quoteReplacement("")),
             trueFile.getAbsolutePath());
      rv.put(COMPILE_HASH.matcher(TRUEFILENAME)
                         .replaceAll(Matcher.quoteReplacement("")),
             R66Dir.getFinalUniqueFilename(session.getFile()));
      rv.put(COMPILE_HASH.matcher(FILESIZE)
                         .replaceAll(Matcher.quoteReplacement("")),
             Long.toString(trueFile.length()));
    } else {
      rv.put(COMPILE_HASH.matcher(TRUEFULLPATH)
                         .replaceAll(Matcher.quoteReplacement("")), "nofile");
      rv.put(COMPILE_HASH.matcher(TRUEFILENAME)
                         .replaceAll(Matcher.quoteReplacement("")), "nofile");
      rv.put(COMPILE_HASH.matcher(FILESIZE)
                         .replaceAll(Matcher.quoteReplacement("")), "0");
    }
  }

}
