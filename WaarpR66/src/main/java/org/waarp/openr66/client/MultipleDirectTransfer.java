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
package org.waarp.openr66.client;

import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.openr66.client.utils.OutputFormat;
import org.waarp.openr66.client.utils.OutputFormat.FIELDS;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.database.DbConstant;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.R66Future;

import java.util.ArrayList;
import java.util.List;

/**
 * Direct Transfer from a client with or without database connection to transfer
 * for multiple files to
 * multiple hosts at once.<br>
 * Files will have to be separated by ','.<br>
 * Hosts will have to be separated by ','.<br>
 * <br>
 * For instance: -to host1,host2,host3 -file file1,file2 <br>
 * Will generate: <br>
 * -to host1 -file file1<br>
 * -to host1 -file file2<br>
 * -to host2 -file file1<br>
 * -to host2 -file file2<br>
 * -to host3 -file file1<br>
 * -to host3 -file file2<br>
 *
 *
 */
public class MultipleDirectTransfer extends DirectTransfer {
  private int errorMultiple = 0;
  private int doneMultiple = 0;
  private final List<OutputFormat> results = new ArrayList<OutputFormat>();

  public MultipleDirectTransfer(R66Future future, String remoteHost,
                                String filename, String rulename,
                                String fileinfo, boolean isMD5, int blocksize,
                                long id,
                                NetworkTransaction networkTransaction) {
    // no starttime since it is direct (blocking request, no delay)
    super(future, remoteHost, filename, rulename, fileinfo, isMD5, blocksize,
          id, networkTransaction);
  }

  @Override
  public void run() {
    final String[] localfilenames = filename.split(",");
    final String[] rhosts = remoteHost.split(",");
    boolean inError = false;
    R66Result resultError = null;
    // first check if filenames contains wildcards
    DbRule dbrule = null;
    try {
      dbrule = new DbRule(rulename);
    } catch (final WaarpDatabaseException e1) {
      logger.error(Messages.getString("Transfer.18"), e1); //$NON-NLS-1$
      future.setFailure(e1);
      return;
    }
    List<String> files = null;
    if (dbrule.isSendMode()) {
      files = getLocalFiles(dbrule, localfilenames);
    }
    for (String host : rhosts) {
      host = host.trim();
      if (host != null && !host.isEmpty()) {
        if (dbrule.isRecvMode()) {
          files =
              getRemoteFiles(dbrule, localfilenames, host, networkTransaction);
        }
        for (String filename : files) {
          filename = filename.trim();
          if (filename != null && !filename.isEmpty()) {
            logger
                .info("Launch transfer to " + host + " with file " + filename);
            final long time1 = System.currentTimeMillis();
            final R66Future future = new R66Future(true);
            final DirectTransfer transaction =
                new DirectTransfer(future, host, filename, rulename, fileinfo,
                                   isMD5, blocksize, id, networkTransaction);
            transaction.normalInfoAsWarn = normalInfoAsWarn;
            logger.debug("rhost: " + host + ":" + transaction.remoteHost);
            transaction.run();
            future.awaitOrInterruptible();
            final long time2 = System.currentTimeMillis();
            logger.debug("finish transfer: " + future.isSuccess());
            final long delay = time2 - time1;
            final R66Result result = future.getResult();
            final OutputFormat outputFormat = new OutputFormat(
                "Unique " + MultipleDirectTransfer.class.getSimpleName(), null);
            if (future.isSuccess()) {
              if (result.getRunner().getErrorInfo() == ErrorCode.Warning) {
                outputFormat.setValue(FIELDS.status.name(), 1);
                outputFormat.setValue(FIELDS.statusTxt.name(),
                                      Messages.getString("Transfer.Status") +
                                      Messages.getString(
                                          "RequestInformation.Warned")); //$NON-NLS-1$
              } else {
                outputFormat.setValue(FIELDS.status.name(), 0);
                outputFormat.setValue(FIELDS.statusTxt.name(),
                                      Messages.getString("Transfer.Status") +
                                      Messages.getString(
                                          "RequestInformation.Success")); //$NON-NLS-1$
              }
              outputFormat.setValue(FIELDS.remote.name(), host);
              outputFormat.setValueString(result.getRunner().getJson());
              outputFormat.setValue("filefinal", (result.getFile() != null?
                  result.getFile().toString() : "no file"));
              outputFormat.setValue("delay", delay);
              getResults().add(outputFormat);
              setDoneMultiple(getDoneMultiple() + 1);
              if (transaction.normalInfoAsWarn) {
                logger.warn(outputFormat.loggerOut());
              } else {
                logger.info(outputFormat.loggerOut());
              }
              if (nolog || result.getRunner().shallIgnoreSave()) {
                // In case of success, delete the runner
                try {
                  result.getRunner().delete();
                } catch (final WaarpDatabaseException e) {
                  logger.warn("Cannot apply nolog to     " +
                              result.getRunner().toShortString(), e);
                }
              }
            } else {
              if (result == null || result.getRunner() == null) {
                outputFormat.setValue(FIELDS.status.name(), 2);
                outputFormat.setValue(FIELDS.statusTxt.name(), Messages
                    .getString("Transfer.FailedNoId")); //$NON-NLS-1$
                outputFormat.setValue(FIELDS.remote.name(), host);
                logger.error(outputFormat.loggerOut(), future.getCause());
                outputFormat.setValue(FIELDS.error.name(),
                                      future.getCause().getMessage());
                outputFormat.sysout();
                if (DetectionUtils.isJunit()) {
                  return;
                }
                networkTransaction.closeAll();
                System.exit(ErrorCode.Unknown.ordinal());
              }
              if (result.getRunner().getErrorInfo() == ErrorCode.Warning) {
                outputFormat.setValue(FIELDS.status.name(), 1);
                outputFormat.setValue(FIELDS.statusTxt.name(),
                                      Messages.getString("Transfer.Status") +
                                      Messages.getString(
                                          "RequestInformation.Warned")); //$NON-NLS-1$
              } else {
                outputFormat.setValue(FIELDS.status.name(), 2);
                outputFormat.setValue(FIELDS.statusTxt.name(),
                                      Messages.getString("Transfer.Status") +
                                      Messages.getString(
                                          "RequestInformation.Failure")); //$NON-NLS-1$
              }
              outputFormat.setValue(FIELDS.remote.name(), host);
              outputFormat.setValueString(result.getRunner().getJson());
              if (result.getRunner().getErrorInfo() == ErrorCode.Warning) {
                logger.warn(outputFormat.loggerOut(), future.getCause());
              } else {
                logger.error(outputFormat.loggerOut(), future.getCause());
              }
              outputFormat.setValue(FIELDS.error.name(),
                                    future.getCause().getMessage());
              getResults().add(outputFormat);
              setErrorMultiple(getErrorMultiple() + 1);
              inError = true;
              if (result != null) {
                inError = true;
                resultError = result;
              }
            }
          }
        }
      }
    }
    if (inError) {
      if (resultError != null) {
        future.setResult(resultError);
      }
      future.cancel();
    } else {
      future.setSuccess();
    }
  }

  public static void main(String[] args) {
    WaarpLoggerFactory.setDefaultFactory(new WaarpSlf4JLoggerFactory(null));
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(MultipleDirectTransfer.class);
    }
    if (!getParams(args, false)) {
      logger.error(Messages.getString("Configuration.WrongInit")); //$NON-NLS-1$
      if (!OutputFormat.isQuiet()) {
        System.out.println(
            Messages.getString("Configuration.WrongInit")); //$NON-NLS-1$
      }
      if (DbConstant.admin != null) {
        DbConstant.admin.close();
      }
      if (DetectionUtils.isJunit()) {
        return;
      }
      ChannelUtils.stopLogger();
      System.exit(2);
    }

    Configuration.configuration.pipelineInit();
    final NetworkTransaction networkTransaction = new NetworkTransaction();
    try {
      final R66Future future = new R66Future(true);
      final long time1 = System.currentTimeMillis();
      final MultipleDirectTransfer multipleDirectTransfer =
          new MultipleDirectTransfer(future, rhost, localFilename, rule,
                                     fileInfo, ismd5, block, idt,
                                     networkTransaction);
      multipleDirectTransfer.normalInfoAsWarn = snormalInfoAsWarn;
      multipleDirectTransfer.run();
      future.awaitOrInterruptible();
      final long time2 = System.currentTimeMillis();
      logger.debug("finish all transfers: " + future.isSuccess());
      final long delay = time2 - time1;
      final OutputFormat outputFormat =
          new OutputFormat(MultipleDirectTransfer.class.getSimpleName(), args);
      if (future.isSuccess()) {
        outputFormat.setValue(FIELDS.status.name(), 0);
        outputFormat.setValue(FIELDS.statusTxt.name(), "Multiple " + Messages
            .getString("Transfer.Status") +
                                                       Messages.getString(
                                                           "RequestInformation.Success")); //$NON-NLS-1$
        outputFormat.setValue(FIELDS.remote.name(), rhost);
        outputFormat.setValue("ok", multipleDirectTransfer.getDoneMultiple());
        outputFormat.setValue("delay", delay);
        if (multipleDirectTransfer.normalInfoAsWarn) {
          logger.warn(outputFormat.loggerOut());
        } else {
          logger.info(outputFormat.loggerOut());
        }
        if (!OutputFormat.isQuiet()) {
          outputFormat.sysout();
          for (final OutputFormat result : multipleDirectTransfer
              .getResults()) {
            System.out.println();
            result.sysout();
          }
        }
      } else {
        outputFormat.setValue(FIELDS.status.name(), 2);
        outputFormat.setValue(FIELDS.statusTxt.name(), "Multiple " + Messages
            .getString("Transfer.Status") +
                                                       Messages.getString(
                                                           "RequestInformation.Failure")); //$NON-NLS-1$
        outputFormat.setValue(FIELDS.remote.name(), rhost);
        outputFormat.setValue("ok", multipleDirectTransfer.getDoneMultiple());
        outputFormat.setValue("ko", multipleDirectTransfer.getErrorMultiple());
        outputFormat.setValue("delay", delay);
        logger.error(outputFormat.loggerOut());
        if (!OutputFormat.isQuiet()) {
          outputFormat.sysout();
          for (final OutputFormat result : multipleDirectTransfer
              .getResults()) {
            System.out.println();
            result.sysout();
          }
        }
        if (DetectionUtils.isJunit()) {
          return;
        }
        networkTransaction.closeAll();
        System.exit(multipleDirectTransfer.getErrorMultiple());
      }
    } catch (final Throwable e) {
      logger.error("Exception", e);
    } finally {
      if (DetectionUtils.isJunit()) {
        return;
      }
      networkTransaction.closeAll();
      System.exit(0);
    }
  }

  /**
   * @return the errorMultiple
   */
  public int getErrorMultiple() {
    return errorMultiple;
  }

  /**
   * @param errorMultiple the errorMultiple to set
   */
  private void setErrorMultiple(int errorMultiple) {
    this.errorMultiple = errorMultiple;
  }

  /**
   * @return the doneMultiple
   */
  public int getDoneMultiple() {
    return doneMultiple;
  }

  /**
   * @param doneMultiple the doneMultiple to set
   */
  private void setDoneMultiple(int doneMultiple) {
    this.doneMultiple = doneMultiple;
  }

  /**
   * @return the results
   */
  public List<OutputFormat> getResults() {
    return results;
  }
}
