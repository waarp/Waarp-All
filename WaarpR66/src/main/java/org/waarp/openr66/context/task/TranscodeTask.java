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

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.transcode.CharsetsUtil;
import org.waarp.common.utility.FileConvert;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;

import java.io.File;

/**
 * Transcode the current file from one Charset to another Charset as
 * specified<br>
 * <br>
 * Arguments are:<br>
 * -dos2unix or -unix2dos (only one) ; optional argument, but if present -from
 * and -to might be omitted;<br>
 * -from charset<br>
 * -to charset<br>
 * -newfile newfilename ; optional argument ; if not used, will be current
 * filename.extension ; if used,
 * extension is ignored<br>
 * -extension extension ; optional argument ; if not used, will be
 * filename.transcode<br>
 * <br>
 * A convenient method (from Waarp Common) allows to list in html (-html), csv
 * (-csv) or text format (-text)
 * all the supported Charsets from your JVM. To use it, run the following
 * command:<br>
 * java -cp WaarpCommon-1.2.7.jar org.waarp.common.transcode.CharsetsUtil [-csv
 * | -html | -text ]<br>
 * <br>
 * It could also be used as a test of transcode outside R66:<br>
 * java -cp WaarpCommon-1.2.7.jar org.waarp.common.transcode.CharsetsUtil -from
 * fromFilename fromCharset -to
 * toFilename toCharset<br>
 * <p>
 * The current file is not touched and is not marked as moved.
 */
public class TranscodeTask extends AbstractTask {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(TranscodeTask.class);

  /**
   * @param argRule
   * @param delay
   * @param argTransfer
   * @param session
   */
  public TranscodeTask(final String argRule, final int delay,
                       final String argTransfer, final R66Session session) {
    super(TaskType.TRANSCODE, delay, argRule, argTransfer, session);
  }

  @Override
  public void run() {
    final boolean success;
    final DbTaskRunner runner = session.getRunner();
    String arg = argRule;
    arg = getReplacedValue(arg, BLANK.split(argTransfer));
    final String[] args = BLANK.split(arg);
    boolean dos2unix = false;
    boolean unix2dos = false;
    String fromCharset = null;
    String toCharset = null;
    String newfilename = null;
    String extension = null;
    for (int i = 0; i < args.length; i++) {
      if ("-from".equalsIgnoreCase(args[i])) {
        i++;
        if (i < args.length) {
          fromCharset = args[i];
        }
      } else if ("-to".equalsIgnoreCase(args[i])) {
        i++;
        if (i < args.length) {
          toCharset = args[i];
        }
      } else if ("-newfile".equalsIgnoreCase(args[i])) {
        i++;
        if (i < args.length) {
          newfilename = args[i];
        }
      } else if ("-extension".equalsIgnoreCase(args[i])) {
        i++;
        if (i < args.length) {
          extension = args[i];
        }
      } else if ("-dos2unix".equalsIgnoreCase(args[i])) {
        dos2unix = true;
      } else if ("-unix2dos".equalsIgnoreCase(args[i])) {
        unix2dos = true;
      }
    }
    if (dos2unix && unix2dos) {
      final R66Result result =
          new R66Result(session, false, ErrorCode.Warning, runner);
      futureCompletion.setResult(result);
      logger.warn(
          "Dos2Unix and Unix2Dos cannot be used simultaneously in Transcode: " +
          runner.toShortString());
      futureCompletion.setFailure(new OpenR66ProtocolSystemException(
          "Dos2Unix and Unix2Dos cannot be used simultaneously in Transcode"));
      return;
    }
    if (fromCharset == null || toCharset == null) {
      if (!(dos2unix || unix2dos)) {
        final R66Result result =
            new R66Result(session, false, ErrorCode.Warning, runner);
        futureCompletion.setResult(result);
        logger.warn(
            "Not enough argument in Transcode: " + runner.toShortString());
        futureCompletion.setFailure(new OpenR66ProtocolSystemException(
            "Not enough argument in Transcode"));
      } else {
        // only 1 of Dos2Unix/Unix2Dos
        final FileConvert convert =
            new FileConvert(null, unix2dos, false, null);
        final File from = session.getFile().getTrueFile();
        if (convert.convert(from, unix2dos)) {
          futureCompletion.setSuccess();
        } else {
          final R66Result result =
              new R66Result(session, false, ErrorCode.Internal, runner);
          futureCompletion.setResult(result);
          logger.error(
              "Cannot Transcode " + argRule + ':' + argTransfer + " and " +
              session);
          futureCompletion.setFailure(
              new OpenR66ProtocolSystemException("Cannot Transcode file"));
        }
      }
      return;
    }
    final File from = session.getFile().getTrueFile();
    String finalname = newfilename;
    if (newfilename != null) {
      finalname = newfilename;
    } else if (extension != null) {
      finalname = from.getAbsolutePath() + '.' + extension;
    } else {
      finalname = from.getAbsolutePath() + ".transcode";
    }
    success = CharsetsUtil
        .transcode(from.getAbsolutePath(), fromCharset, finalname, toCharset,
                   Configuration.BUFFERSIZEDEFAULT);
    if (success && (dos2unix || unix2dos)) {
      // now convert it
      // only 1 of Dos2Unix/Unix2Dos
      final FileConvert convert = new FileConvert(null, unix2dos, false, null);
      final File to = new File(finalname);
      if (convert.convert(to, unix2dos)) {
        futureCompletion.setSuccess();
      } else {
        // only warning
        logger.warn("Cannot Unix/Dos Transcode " + to + " : " + argRule + ':' +
                    argTransfer + " and " + session);
        futureCompletion.setSuccess();
      }
      return;
    }
    if (success) {
      futureCompletion.setSuccess();
    } else {
      logger.error("Cannot Transcode from " + fromCharset + " to " + toCharset +
                   " with " + argRule + ':' + argTransfer + " and " + session);
      futureCompletion.setFailure(
          new OpenR66ProtocolSystemException("Cannot Transcode file"));
    }
  }

}
