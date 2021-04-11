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

import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.openr66.client.utils.OutputFormat;
import org.waarp.openr66.client.utils.OutputFormat.FIELDS;
import org.waarp.openr66.configuration.FileBasedConfiguration;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.authentication.R66Auth;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.packet.InformationPacket;
import org.waarp.openr66.protocol.localhandler.packet.ValidPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.R66Future;

import java.net.SocketAddress;

import static org.waarp.common.database.DbConstant.*;

/**
 * Class to request information on remote files
 */
public class RequestInformation implements Runnable {
  /**
   * Internal Logger
   */
  static volatile WaarpLogger logger;

  public static final byte REQUEST_CHECK = -1;

  protected static String infoArgs =
      Messages.getString("RequestInformation.0") //$NON-NLS-1$
      + Messages.getString("Message.OutputFormat");

  protected final NetworkTransaction networkTransaction;
  final R66Future future;
  final String requested;
  final String filename;
  final String rulename;
  final byte code;
  long id = ILLEGALVALUE;
  boolean isTo = true;
  boolean normalInfoAsWarn = true;

  static String srequested;
  static String sfilename;
  static String srulename;
  static byte scode = REQUEST_CHECK;
  static long sid = ILLEGALVALUE;
  static boolean sisTo = true;
  protected static boolean snormalInfoAsWarn = true;

  /**
   * Parse the parameter and set current values
   *
   * @param args
   *
   * @return True if all parameters were found and correct
   */
  protected static boolean getParams(final String[] args) {
    infoArgs = Messages.getString("RequestInformation.0") +
               Messages.getString("Message.OutputFormat"); //$NON-NLS-1$
    if (args.length < 5) {
      logger.error(infoArgs);
      return false;
    }
    if (!FileBasedConfiguration
        .setClientConfigurationFromXml(Configuration.configuration, args[0])) {
      logger.error(
          Messages.getString("Configuration.NeedCorrectConfig")); //$NON-NLS-1$
      return false;
    }
    for (int i = 1; i < args.length; i++) {
      if ("-to".equalsIgnoreCase(args[i])) {
        i++;
        srequested = args[i];
        if (Configuration.configuration.getAliases().containsKey(srequested)) {
          srequested = Configuration.configuration.getAliases().get(srequested);
        }
      } else if ("-file".equalsIgnoreCase(args[i])) {
        i++;
        sfilename = args[i];
        sfilename = sfilename.replace('§', '*');
      } else if ("-rule".equalsIgnoreCase(args[i])) {
        i++;
        srulename = args[i];
      } else if ("-logWarn".equalsIgnoreCase(args[i])) {
        snormalInfoAsWarn = true;
      } else if ("-notlogWarn".equalsIgnoreCase(args[i])) {
        snormalInfoAsWarn = false;
      } else if ("-exist".equalsIgnoreCase(args[i])) {
        scode = (byte) InformationPacket.ASKENUM.ASKEXIST.ordinal();
      } else if ("-detail".equalsIgnoreCase(args[i])) {
        scode = (byte) InformationPacket.ASKENUM.ASKMLSDETAIL.ordinal();
      } else if ("-list".equalsIgnoreCase(args[i])) {
        scode = (byte) InformationPacket.ASKENUM.ASKLIST.ordinal();
      } else if ("-mlsx".equalsIgnoreCase(args[i])) {
        scode = (byte) InformationPacket.ASKENUM.ASKMLSLIST.ordinal();
      } else if ("-id".equalsIgnoreCase(args[i])) {
        i++;
        sid = Long.parseLong(args[i]);
      } else if ("-reqfrom".equalsIgnoreCase(args[i])) {
        sisTo = true;
      } else if ("-reqto".equalsIgnoreCase(args[i])) {
        sisTo = false;
      }
    }
    OutputFormat.getParams(args);
    if (sfilename != null && scode == REQUEST_CHECK) {
      scode = (byte) InformationPacket.ASKENUM.ASKEXIST.ordinal();
    }
    if (srulename == null && scode != REQUEST_CHECK || srequested == null) {
      logger.error(
          Messages.getString("RequestInformation.12") + infoArgs); //$NON-NLS-1$
      return false;
    }
    if (scode != REQUEST_CHECK && sid != ILLEGALVALUE) {
      logger.error(
          Messages.getString("RequestInformation.13") + infoArgs); //$NON-NLS-1$
      return false;
    }
    return true;
  }

  /**
   * @param future
   * @param requested
   * @param rulename
   * @param filename
   * @param request
   * @param id Id of the request
   * @param isTo request is To remote Host (true), or From remote host
   *     (false)
   * @param networkTransaction
   */
  public RequestInformation(final R66Future future, final String requested,
                            final String rulename, final String filename,
                            final byte request, final long id,
                            final boolean isTo,
                            final NetworkTransaction networkTransaction) {
    this.future = future;
    this.rulename = rulename;
    this.requested = requested;
    this.filename = filename;
    code = request;
    this.id = id;
    this.isTo = isTo;
    this.networkTransaction = networkTransaction;
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(RequestInformation.class);
    }
  }

  @Override
  public void run() {
    final InformationPacket request;
    if (code != REQUEST_CHECK) {
      request = new InformationPacket(rulename, code, filename);
    } else {
      request =
          new InformationPacket(String.valueOf(id), code, isTo? "1" : "0");
    }

    // Connection
    final DbHostAuth host = R66Auth.getServerAuth(requested);
    if (host == null) {
      logger.error(
          Messages.getString("Message.HostNotFound") + requested); //$NON-NLS-1$
      final R66Result result =
          new R66Result(null, true, ErrorCode.ConnectionImpossible, null);
      future.setResult(result);
      future.cancel();
      return;
    }
    if (host.isClient()) {
      logger.error(
          Messages.getString("Message.HostIsClient") + requested); //$NON-NLS-1$
      final R66Result result =
          new R66Result(null, true, ErrorCode.ConnectionImpossible, null);
      future.setResult(result);
      future.cancel();
      return;
    }
    final SocketAddress socketAddress = host.getSocketAddress();
    final boolean isSSL = host.isSsl();

    final LocalChannelReference localChannelReference = networkTransaction
        .createConnectionWithRetry(socketAddress, isSSL, future);
    if (localChannelReference == null) {
      logger.error(Messages.getString("AdminR66OperationsGui.188") +
                   requested); //$NON-NLS-1$
      final R66Result result =
          new R66Result(null, true, ErrorCode.ConnectionImpossible, null);
      future.setResult(result);
      future.cancel();
      return;
    }
    localChannelReference.sessionNewState(R66FiniteDualStates.INFORMATION);
    try {
      ChannelUtils
          .writeAbstractLocalPacket(localChannelReference, request, false);
    } catch (final OpenR66ProtocolPacketException e) {
      logger.error(Messages.getString("RequestInformation.20")); //$NON-NLS-1$
      final R66Result result =
          new R66Result(null, true, ErrorCode.TransferError, null);
      future.setResult(result);
      future.cancel();
      return;
    }
    localChannelReference.getFutureRequest().awaitOrInterruptible();
  }

  /**
   * @param args
   */
  public static void main(final String[] args) {
    WaarpLoggerFactory
        .setDefaultFactoryIfNotSame(new WaarpSlf4JLoggerFactory(null));
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(RequestInformation.class);
    }
    if (!getParams(args)) {
      logger.error(Messages.getString("Configuration.WrongInit")); //$NON-NLS-1$
      if (!OutputFormat.isQuiet()) {
        SysErrLogger.FAKE_LOGGER.sysout(
            Messages.getString("Configuration.WrongInit")); //$NON-NLS-1$
      }
      if (admin != null) {
        admin.close();
      }
      WaarpSystemUtil.systemExit(1);
      return;
    }
    NetworkTransaction networkTransaction = null;
    int value = 3;
    try {
      Configuration.configuration.pipelineInit();
      networkTransaction = new NetworkTransaction();
      final R66Future result = new R66Future(true);
      final RequestInformation requestInformation =
          new RequestInformation(result, srequested, srulename, sfilename,
                                 scode, sid, sisTo, networkTransaction);
      requestInformation.normalInfoAsWarn = snormalInfoAsWarn;
      requestInformation.run();
      result.awaitOrInterruptible();
      // if transfer information request (code = -1 = REQUEST_CHECK) => middle empty and
      // header = Runner as XML
      // if listing request => middle = nb of files, header = list of files in native/list/mlsx/exist (true/false)
      // format, 1 file per line
      final OutputFormat outputFormat =
          new OutputFormat(RequestInformation.class.getSimpleName(), args);
      if (result.isSuccess()) {
        value = 0;
        final R66Result r66result = result.getResult();
        final ValidPacket info = (ValidPacket) r66result.getOther();
        outputFormat.setValue(FIELDS.status.name(), 0);
        outputFormat.setValue(FIELDS.statusTxt.name(), Messages
            .getString("RequestInformation.Success")); //$NON-NLS-1$
        outputFormat.setValue(FIELDS.remote.name(), srequested);
        if (requestInformation.code != REQUEST_CHECK) {
          outputFormat.setValue("nb", Integer.parseInt(info.getSmiddle()));
          final String[] files = info.getSheader().split("\n");
          int i = 0;
          for (final String file : files) {
            i++;
            outputFormat.setValue("file" + i, file);
          }
        } else {
          try {
            final DbTaskRunner runner =
                DbTaskRunner.fromStringXml(info.getSheader(), false);
            outputFormat.setValueString(runner.getJson());
          } catch (final OpenR66ProtocolBusinessException e) {
            outputFormat.setValue("Id", requestInformation.id);
            outputFormat.setValue(FIELDS.transfer.name(), info.getSheader());
          }
        }
        if (requestInformation.normalInfoAsWarn) {
          logger.warn(outputFormat.loggerOut());
        } else if (logger.isInfoEnabled()) {
          logger.info(outputFormat.loggerOut());
        }
        if (!OutputFormat.isQuiet()) {
          outputFormat.sysout();
        }
      } else {
        value = 2;
        outputFormat.setValue(FIELDS.status.name(), 2);
        outputFormat.setValue(FIELDS.statusTxt.name(), Messages
            .getString("RequestInformation.Failure")); //$NON-NLS-1$
        outputFormat.setValue(FIELDS.remote.name(), srequested);
        outputFormat
            .setValue(FIELDS.error.name(), result.getResult().toString());
        logger.error(outputFormat.loggerOut());
        if (!OutputFormat.isQuiet()) {
          outputFormat.sysout();
        }
      }
    } catch (final Throwable e) {
      logger.error("Exception", e);
    } finally {
      if (!WaarpSystemUtil.isJunit()) {
        if (networkTransaction != null) {
          networkTransaction.closeAll();
        }
        if (admin != null) {
          admin.close();
        }
        WaarpSystemUtil.systemExit(value);
      }
    }
  }

}
