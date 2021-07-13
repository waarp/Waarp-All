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
package org.waarp.openr66.thrift;

import org.apache.thrift.TException;
import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.database.data.AbstractDbData;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.client.AbstractTransfer;
import org.waarp.openr66.commander.ClientRunner;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.filesystem.R66File;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.PartnerConfiguration;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.LocalServerHandler;
import org.waarp.openr66.protocol.localhandler.packet.ErrorPacket;
import org.waarp.openr66.protocol.localhandler.packet.RequestPacket;
import org.waarp.openr66.protocol.utils.TransferUtils;
import org.waarp.thrift.r66.Action;
import org.waarp.thrift.r66.ErrorCode;
import org.waarp.thrift.r66.R66Request;
import org.waarp.thrift.r66.R66Result;
import org.waarp.thrift.r66.R66Service;
import org.waarp.thrift.r66.RequestMode;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.waarp.common.database.DbConstant.*;

/**
 * Embedded service attached with the Thrift service
 */
public class R66EmbeddedServiceImpl implements R66Service.Iface {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(R66EmbeddedServiceImpl.class);

  private DbTaskRunner initRequest(final R66Request request) {
    Timestamp ttimestart = null;
    if (request.isSetStart()) {
      final Date date;
      try {
        final SimpleDateFormat dateFormat =
            new SimpleDateFormat(AbstractTransfer.TIMESTAMP_FORMAT);
        date = dateFormat.parse(request.getStart());
        ttimestart = new Timestamp(date.getTime());
      } catch (final ParseException ignored) {
        // nothing
      }
    } else if (request.isSetDelay()) {
      if (request.getDelay().charAt(0) == '+') {
        ttimestart = new Timestamp(System.currentTimeMillis() + Long.parseLong(
            request.getDelay().substring(1)));
      } else {
        ttimestart = new Timestamp(Long.parseLong(request.getDelay()));
      }
    }
    final DbRule rule;
    try {
      rule = new DbRule(request.getRule());
    } catch (final WaarpDatabaseException e) {
      logger.warn("Cannot get Rule: " + request.getRule() + " : {}",
                  e.getMessage());
      return null;
    }
    int mode = rule.getMode();
    if (request.isMd5()) {
      mode = RequestPacket.getModeMD5(mode);
    }
    final DbTaskRunner taskRunner;
    long tid = ILLEGALVALUE;
    if (request.isSetTid()) {
      tid = request.getTid();
    }
    if (tid != ILLEGALVALUE) {
      try {
        taskRunner = new DbTaskRunner(tid, request.getDestuid());
        // requested
        taskRunner.setSenderByRequestToValidate(true);
      } catch (final WaarpDatabaseException e) {
        logger.warn("Cannot get task" + " : {}", e.getMessage());
        return null;
      }
    } else {
      final String sep =
          PartnerConfiguration.getSeparator(request.getDestuid());
      final RequestPacket requestPacket =
          new RequestPacket(request.getRule(), mode, request.getFile(),
                            request.getBlocksize(), 0, tid, request.getInfo(),
                            -1, sep);
      // Not isRecv since it is the requester, so send => isRetrieve is true
      final boolean isRetrieve =
          !RequestPacket.isRecvMode(requestPacket.getMode());
      try {
        taskRunner = new DbTaskRunner(rule, isRetrieve, requestPacket,
                                      request.getDestuid(), ttimestart);
      } catch (final WaarpDatabaseException e) {
        logger.warn("Cannot get task" + " : {}", e.getMessage());
        return null;
      }
    }
    return taskRunner;
  }

  @Override
  public R66Result transferRequestQuery(final R66Request request)
      throws TException {
    final DbTaskRunner runner = initRequest(request);
    if (runner != null) {
      runner.changeUpdatedInfo(AbstractDbData.UpdatedInfo.TOSUBMIT);
      final boolean isSender = runner.isSender();
      if (!runner.forceSaveStatus()) {
        logger.warn("Cannot prepare task");
        return new R66Result(request.getMode(), ErrorCode.CommandNotFound,
                             "ERROR: Cannot prepare transfer");
      }
      final R66Result result =
          new R66Result(request.getMode(), ErrorCode.InitOk,
                        "Transfer Scheduled");
      if (request.getMode() == RequestMode.SYNCTRANSFER) {
        // now need to wait but first, reload the runner
        try {
          runner.select();
          while (!runner.isFinished()) {
            try {
              Thread.sleep(1000);
              runner.select();
            } catch (final InterruptedException e) {//NOSONAR
              SysErrLogger.FAKE_LOGGER.ignoreLog(e);
              break;
            }
          }
          runner.setSender(isSender);
        } catch (final WaarpDatabaseException ignored) {
          // nothing
        }
        setResultFromRunner(runner, result);
        if (runner.isAllDone()) {
          result.setCode(ErrorCode.CompleteOk);
          result.setResultinfo("Transfer Done");
        } else {
          result.setCode(ErrorCode.valueOf(runner.getErrorInfo().name()));
          result.setResultinfo(runner.getErrorInfo().getMesg());
        }
      } else {
        try {
          runner.select();
        } catch (final WaarpDatabaseException ignored) {
          // nothing
        }
        runner.setSender(isSender);
        setResultFromRunner(runner, result);
      }
      return result;
    } else {
      logger.warn("ERROR: Transfer NOT scheduled");
      return new R66Result(request.getMode(), ErrorCode.Internal,
                           "ERROR: Transfer NOT scheduled");
    }
  }

  private void setResultFromRunner(final DbTaskRunner runner,
                                   final R66Result result) {
    result.setDestuid(runner.getRequested());
    result.setFromuid(runner.getRequester());
    result.setTid(runner.getSpecialId());
    result.setRule(runner.getRuleId());
    result.setBlocksize(runner.getBlocksize());
    result.setFile(runner.getFilename());
    result.setOriginalfilename(runner.getOriginalFilename());
    result.setIsmoved(runner.isFileMoved());
    result.setModetransfer(runner.getMode());
    result.setRetrievemode(runner.isSender());
    result.setStep(runner.getStep());
    result.setGloballaststep(runner.getGloballaststep());
    result.setRank(runner.getRank());
    result.setStart(runner.getStart().toString());
    result.setStop(runner.getStop().toString());
    result.setResultinfo(runner.getFileInformation());
  }

  private void setResultFromLCR(final LocalChannelReference lcr,
                                final R66Result result) {
    final R66Session session = lcr.getSession();
    DbTaskRunner runner = null;
    if (session != null) {
      runner = session.getRunner();
    } else {
      final ClientRunner run = lcr.getClientRunner();
      if (run != null) {
        runner = run.getTaskRunner();
      }
    }
    if (runner != null) {
      setResultFromRunner(runner, result);
    }
  }

  private int stopOrCancelRunner(final long id, final String reqd,
                                 final String reqr,
                                 final org.waarp.openr66.context.ErrorCode code) {
    try {
      final DbTaskRunner taskRunner =
          new DbTaskRunner(null, null, id, reqr, reqd);
      return taskRunner.stopOrCancelRunner(code)? 1 : 0;
    } catch (final WaarpDatabaseException ignored) {
      // nothing
    }
    logger
        .warn("Cannot accomplished action on task: " + id + ' ' + code.name());
    return -1;
  }

  private R66Result stopOrCancel(final R66Request request,
                                 final LocalChannelReference lcr,
                                 final org.waarp.openr66.context.ErrorCode r66code) {
    // stop the current transfer
    final R66Result resulttest;
    if (lcr != null) {
      int rank = 0;
      if (r66code == org.waarp.openr66.context.ErrorCode.StoppedTransfer &&
          lcr.getSession() != null) {
        final DbTaskRunner taskRunner = lcr.getSession().getRunner();
        if (taskRunner != null) {
          rank = taskRunner.getRank();
        }
      }
      final ErrorPacket error =
          new ErrorPacket(r66code.name() + ' ' + rank, r66code.getCode(),
                          ErrorPacket.FORWARDCLOSECODE);
      try {
        // inform local instead of remote
        LocalServerHandler.channelRead0(lcr, error);
      } catch (final Exception ignored) {
        // nothing
      }
      resulttest = new R66Result(request.getMode(), ErrorCode.CompleteOk,
                                 r66code.name());
      setResultFromLCR(lcr, resulttest);
    } else {
      // Transfer is not running
      // but maybe need action on database
      final int test =
          stopOrCancelRunner(request.getTid(), request.getDestuid(),
                             request.getFromuid(), r66code);
      if (test > 0) {
        resulttest = new R66Result(request.getMode(), ErrorCode.CompleteOk,
                                   r66code.name());
      } else if (test == 0) {
        resulttest = new R66Result(request.getMode(), ErrorCode.TransferOk,
                                   r66code.name());
      } else {
        resulttest = new R66Result(request.getMode(), ErrorCode.CommandNotFound,
                                   "Error: cannot accomplished task on transfer");
      }
    }
    return resulttest;
  }

  private R66Result restart(final R66Request request,
                            final LocalChannelReference lcr) {
    // Try to validate a restarting transfer
    // validLimit on requested side
    if (Configuration.configuration.getConstraintLimitHandler()
                                   .checkConstraints()) {
      logger
          .warn("Limit exceeded {} while asking to relaunch a task " + request,
                Configuration.configuration
                    .getConstraintLimitHandler().lastAlert);
      return new R66Result(request.getMode(), ErrorCode.ServerOverloaded,
                           "Limit exceeded while asking to relaunch a task");
    }
    // Try to validate a restarting transfer
    // header = ?; middle = requested+blank+requester+blank+specialId
    final DbTaskRunner taskRunner;
    try {
      taskRunner =
          new DbTaskRunner(null, null, request.getTid(), request.getFromuid(),
                           request.getDestuid());
      final org.waarp.openr66.context.R66Result resulttest =
          TransferUtils.restartTransfer(taskRunner, lcr);
      return new R66Result(request.getMode(),
                           ErrorCode.valueOf(resulttest.getCode().name()),
                           resulttest.getMessage());
    } catch (final WaarpDatabaseException e1) {
      logger.warn("Exception while trying to restart transfer" + " : {}",
                  e1.getMessage());
      return new R66Result(request.getMode(), ErrorCode.Internal,
                           "Exception while trying to restart transfer");
    }
  }

  @Override
  public R66Result infoTransferQuery(final R66Request request)
      throws TException {
    final RequestMode mode = request.getMode();
    if (mode != RequestMode.INFOREQUEST) {
      // error
      logger.warn("Mode is uncompatible with infoTransferQuery");
      return new R66Result(request.getMode(), ErrorCode.Unimplemented,
                           "Mode is uncompatible with infoTransferQuery");
    }
    // now check if enough arguments are provided
    if (!request.isSetTid() ||
        !request.isSetDestuid() && !request.isSetFromuid() ||
        !request.isSetAction()) {
      // error
      logger.warn("Not enough arguments");
      return new R66Result(request.getMode(), ErrorCode.RemoteError,
                           "Not enough arguments");
    }
    // requested+blank+requester+blank+specialId
    final LocalChannelReference lcr =
        Configuration.configuration.getLocalTransaction().getFromRequest(
            request.getDestuid() + ' ' + request.getFromuid() + ' ' +
            request.getTid());
    final org.waarp.openr66.context.ErrorCode r66code;
    switch (request.getAction()) {
      case Detail: {
        final R66Result result =
            new R66Result(request.getMode(), ErrorCode.CompleteOk,
                          "Existence test OK");
        result.setAction(Action.Exist);
        result.setDestuid(request.getDestuid());
        result.setFromuid(request.getFromuid());
        result.setTid(request.getTid());
        if (lcr != null) {
          setResultFromLCR(lcr, result);
        } else {
          try {
            final DbTaskRunner runner =
                new DbTaskRunner(null, null, request.getTid(),
                                 request.getFromuid(), request.getDestuid());
            setResultFromRunner(runner, result);
          } catch (final WaarpDatabaseException e) {
            result.setCode(ErrorCode.FileNotFound);
          }
        }
        return result;
      }
      case Restart:
        return restart(request, lcr);
      case Cancel:
        r66code = org.waarp.openr66.context.ErrorCode.CanceledTransfer;
        return stopOrCancel(request, lcr, r66code);
      case Stop:
        r66code = org.waarp.openr66.context.ErrorCode.StoppedTransfer;
        return stopOrCancel(request, lcr, r66code);
      default:
        logger.warn("Uncompatible with " + request.getAction().name());
        return new R66Result(request.getMode(), ErrorCode.Unimplemented,
                             "Uncompatible with " + request.getAction().name());
    }
  }

  @Override
  public boolean isStillRunning(final String fromuid, final String touid,
                                final long tid) throws TException {
    // now check if enough arguments are provided
    if (fromuid == null || touid == null || tid == ILLEGALVALUE) {
      // error
      logger.warn("Not enough arguments");
      return false;
    }
    // header = ?; middle = requested+blank+requester+blank+specialId
    final LocalChannelReference lcr =
        Configuration.configuration.getLocalTransaction().getFromRequest(
            touid + ' ' + fromuid + ' ' + tid);
    return lcr != null;
  }

  @Override
  public List<String> infoListQuery(final R66Request request)
      throws TException {
    List<String> list = new ArrayList<String>();
    final RequestMode mode = request.getMode();
    if (mode != RequestMode.INFOFILE) {
      // error
      logger.warn("Not correct mode for infoListQuery");
      list.add("Not correct mode for infoListQuery");
      return list;
    }
    // now check if enough arguments are provided
    if (!request.isSetRule() || !request.isSetAction()) {
      // error
      logger.warn("Not enough arguments");
      list.add("Not enough arguments");
      return list;
    }
    final R66Session session = new R66Session(false);
    session.getAuth().specialNoSessionAuth(false, Configuration.configuration
        .getHostId());
    final DbRule rule;
    try {
      rule = new DbRule(request.getRule());
    } catch (final WaarpDatabaseException e) {
      logger.warn("Rule is unknown: " + request.getRule());
      list.add("Rule is unknown: " + request.getRule());
      return list;
    }
    try {
      if (RequestPacket.isRecvMode(rule.getMode())) {
        session.getDir().changeDirectory(rule.getWorkPath());
      } else {
        session.getDir().changeDirectory(rule.getSendPath());
      }

      if (request.getAction() == Action.List ||
          request.getAction() == Action.Mlsx) {
        // ls or mls from current directory
        if (request.getAction() == Action.List) {
          list = session.getDir().list(request.getFile());
        } else {
          list = session.getDir().listFull(request.getFile(), false);
        }
      } else {
        // ls pr mls from current directory and filename
        if (!request.isSetFile()) {
          logger.warn("File missing");
          list.add("File missing");
          return list;
        }
        final R66File file =
            (R66File) session.getDir().setFile(request.getFile(), false);
        String sresult;
        if (request.getAction() == Action.Exist) {
          sresult = String.valueOf(file.exists());
          list.add(sresult);
        } else if (request.getAction() == Action.Detail) {
          sresult = session.getDir().fileFull(request.getFile(), false);
          final String[] slist = sresult.split("\n");
          sresult = slist[1];
          list.add(sresult);
        }
      }
      return list;
    } catch (final CommandAbstractException e) {
      logger.warn("Error occurs during: " + request + " : {}", e.getMessage());
      list.add("Error occurs during: " + request);
      return list;
    }
  }

}
