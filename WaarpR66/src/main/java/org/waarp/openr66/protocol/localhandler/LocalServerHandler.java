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
package org.waarp.openr66.protocol.localhandler;

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpShutdownHook;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.context.task.exception.OpenR66RunnerException;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.configuration.PartnerConfiguration;
import org.waarp.openr66.protocol.exception.OpenR66Exception;
import org.waarp.openr66.protocol.exception.OpenR66ExceptionTrappedFactory;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessCancelException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessNoWriteBackException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessQueryAlreadyFinishedException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessQueryStillRunningException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessRemoteFileNotFoundException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessStopException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNetworkException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoConnectionException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoDataException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNotAuthenticatedException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolRemoteShutdownException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolShutdownException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;
import org.waarp.openr66.protocol.localhandler.packet.AbstractLocalPacket;
import org.waarp.openr66.protocol.localhandler.packet.AuthentPacket;
import org.waarp.openr66.protocol.localhandler.packet.BlockRequestPacket;
import org.waarp.openr66.protocol.localhandler.packet.BusinessRequestPacket;
import org.waarp.openr66.protocol.localhandler.packet.ConnectionErrorPacket;
import org.waarp.openr66.protocol.localhandler.packet.DataPacket;
import org.waarp.openr66.protocol.localhandler.packet.EndRequestPacket;
import org.waarp.openr66.protocol.localhandler.packet.EndTransferPacket;
import org.waarp.openr66.protocol.localhandler.packet.ErrorPacket;
import org.waarp.openr66.protocol.localhandler.packet.InformationPacket;
import org.waarp.openr66.protocol.localhandler.packet.JsonCommandPacket;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketCodec;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;
import org.waarp.openr66.protocol.localhandler.packet.RequestPacket;
import org.waarp.openr66.protocol.localhandler.packet.ShutdownPacket;
import org.waarp.openr66.protocol.localhandler.packet.TestPacket;
import org.waarp.openr66.protocol.localhandler.packet.ValidPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.JsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.RequestJsonPacket;
import org.waarp.openr66.protocol.networkhandler.packet.NetworkPacket;
import org.waarp.openr66.protocol.utils.ChannelCloseTimer;
import org.waarp.openr66.protocol.utils.ChannelUtils;

import static org.waarp.openr66.context.R66FiniteDualStates.*;

/**
 * The local server handler handles real end file operations.
 */
public final class LocalServerHandler {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(LocalServerHandler.class);

  private LocalServerHandler() {
  }

  public static void channelInactive(final TransferActions serverHandler) {
    serverHandler.channelClosed();
  }

  public static void channelActive(final TransferActions serverHandler) {
    serverHandler.newSession();
  }

  /**
   * Replacement for local channels
   *
   * @param localChannelReference
   * @param networkPacket
   */
  public static void channelRead0(
      final LocalChannelReference localChannelReference,
      final NetworkPacket networkPacket) {
    try {
      final AbstractLocalPacket packet =
          LocalPacketCodec.decodeNetworkPacket(networkPacket.getBuffer());
      channelRead1(localChannelReference, packet);
    } catch (final OpenR66ProtocolShutdownException e) {
      logger.error(e.getMessage());
      exceptionCaught(localChannelReference.getServerHandler(), e);
    } catch (final Exception e) {
      exceptionCaught(localChannelReference.getServerHandler(), e);
    } finally {
      networkPacket.clear();
    }
  }

  public static void channelRead0(
      final LocalChannelReference localChannelReference,
      final AbstractLocalPacket msg) {
    try {
      channelRead1(localChannelReference, msg);
    } catch (final Exception e) {
      exceptionCaught(localChannelReference.getServerHandler(), e);
    }
  }

  public static void channelRead1(
      final LocalChannelReference localChannelReference,
      final AbstractLocalPacket packet) throws Exception {
    // action as requested and answer if necessary
    final TransferActions serverHandler =
        localChannelReference.getServerHandler();
    if (packet.getType() == LocalPacketFactory.STARTUPPACKET) {
      logger.warn("Error in the protocol: {}", packet.toString());
    } else {
      if (serverHandler.getLocalChannelReference() == null) {
        logger.error(
            "No LocalChannelReference at " + packet.getClass().getName());
        serverHandler.getSession().newState(ERROR);
        final ErrorPacket errorPacket = new ErrorPacket(
            "No LocalChannelReference at " + packet.getClass().getName(),
            ErrorCode.ConnectionImpossible.getCode(),
            ErrorPacket.FORWARDCLOSECODE);
        ChannelUtils
            .writeAbstractLocalPacket(localChannelReference, errorPacket, true);
        if (Configuration.configuration.getR66Mib() != null) {
          Configuration.configuration.getR66Mib()
                                     .notifyWarning("No LocalChannelReference",
                                                    packet.getClass()
                                                          .getSimpleName());
        }
        packet.clear();
        return;
      }
      switch (packet.getType()) {
        case LocalPacketFactory.AUTHENTPACKET: {
          serverHandler.authent((AuthentPacket) packet,
                                localChannelReference.getNetworkChannelObject()
                                                     .isSSL());
          break;
        }
        // Already done case LocalPacketFactory.STARTUPPACKET:
        case LocalPacketFactory.DATAPACKET: {
          if (((DataPacket) packet).getPacketRank() % 100 == 1 ||
              serverHandler.getSession().getState() != DATAR) {
            serverHandler.getSession().newState(DATAR);
            logger.debug("DATA RANK: {} : {}",
                         ((DataPacket) packet).getPacketRank(),
                         serverHandler.getSession().getRunner().getRank());
          }
          serverHandler.data((DataPacket) packet);
          break;
        }
        case LocalPacketFactory.VALIDPACKET: {
          // SHUTDOWNPACKET does not need authentication
          if (((ValidPacket) packet).getTypeValid() !=
              LocalPacketFactory.SHUTDOWNPACKET &&
              !serverHandler.getSession().isAuthenticated()) {
            logger.warn("Valid packet received while not authenticated: {} {}",
                        packet, serverHandler.getSession());
            serverHandler.getSession().newState(ERROR);
            packet.clear();
            throw new OpenR66ProtocolNotAuthenticatedException(
                "Not authenticated while Valid received");
          }
          if (((ValidPacket) packet).getTypeValid() ==
              LocalPacketFactory.REQUESTPACKET) {
            final String[] fields = ((ValidPacket) packet).getSmiddle().split(
                PartnerConfiguration.BAR_SEPARATOR_FIELD);
            String newfilename = fields[0];
            // potential file size changed
            long newSize = -1;
            String newFileInfo = null;
            if (fields.length > 1) {
              try {
                newSize = Long.parseLong(fields[1]);
                // potential fileInfo changed
                if (fields.length > 2) {
                  newFileInfo = fields[2];
                }
              } catch (final NumberFormatException e2) {
                newfilename +=
                    PartnerConfiguration.BAR_SEPARATOR_FIELD + fields[1];
                newSize = -1;
              }
            }
            if (newFileInfo != null && !newFileInfo.equals(
                serverHandler.getSession().getRunner().getFileInformation())) {
              serverHandler.requestChangeFileInfo(newFileInfo);
            }
            serverHandler.requestChangeNameSize(newfilename, newSize);
            packet.clear();
          } else {
            serverHandler.valid((ValidPacket) packet);
          }
          break;
        }
        case LocalPacketFactory.ERRORPACKET: {
          serverHandler.getSession().newState(ERROR);
          serverHandler.errorMesg((ErrorPacket) packet);
          break;
        }
        case LocalPacketFactory.CONNECTERRORPACKET: {
          serverHandler.connectionError((ConnectionErrorPacket) packet);
          break;
        }
        case LocalPacketFactory.REQUESTPACKET: {
          serverHandler.request((RequestPacket) packet);
          break;
        }
        case LocalPacketFactory.SHUTDOWNPACKET: {
          serverHandler.getSession().newState(SHUTDOWN);
          serverHandler.shutdown((ShutdownPacket) packet);
          break;
        }
        case LocalPacketFactory.STOPPACKET:
        case LocalPacketFactory.CANCELPACKET:
        case LocalPacketFactory.CONFIMPORTPACKET:
        case LocalPacketFactory.CONFEXPORTPACKET:
        case LocalPacketFactory.BANDWIDTHPACKET: {
          logger.error("Unimplemented Mesg: " + packet.getClass().getName());
          serverHandler.getSession().newState(ERROR);
          serverHandler.getLocalChannelReference().invalidateRequest(
              new R66Result(
                  new OpenR66ProtocolSystemException("Not implemented"),
                  serverHandler.getSession(), true, ErrorCode.Unimplemented,
                  null));
          final ErrorPacket errorPacket = new ErrorPacket(
              "Unimplemented Mesg: " + packet.getClass().getName(),
              ErrorCode.Unimplemented.getCode(), ErrorPacket.FORWARDCLOSECODE);
          ChannelUtils.writeAbstractLocalPacket(
              serverHandler.getLocalChannelReference(), errorPacket, true);
          packet.clear();
          break;
        }
        case LocalPacketFactory.TESTPACKET: {
          serverHandler.getSession().newState(TEST);
          serverHandler.test((TestPacket) packet);
          break;
        }
        case LocalPacketFactory.ENDTRANSFERPACKET: {
          serverHandler.endTransfer((EndTransferPacket) packet);
          break;
        }
        case LocalPacketFactory.INFORMATIONPACKET: {
          serverHandler.getSession().newState(INFORMATION);
          serverHandler.information((InformationPacket) packet);
          break;
        }
        case LocalPacketFactory.ENDREQUESTPACKET: {
          serverHandler.endRequest((EndRequestPacket) packet);
          break;
        }
        case LocalPacketFactory.BUSINESSREQUESTPACKET: {
          serverHandler.businessRequest((BusinessRequestPacket) packet);
          break;
        }
        case LocalPacketFactory.BLOCKREQUESTPACKET: {
          serverHandler.blockRequest((BlockRequestPacket) packet);
          break;
        }
        case LocalPacketFactory.JSONREQUESTPACKET: {
          if (!serverHandler.getSession().isAuthenticated()) {
            logger.warn(
                "JsonCommand packet received while not authenticated: {} {}",
                packet, serverHandler.getSession());
            serverHandler.getSession().newState(ERROR);
            throw new OpenR66ProtocolNotAuthenticatedException(
                "Not authenticated while Valid received");
          }
          JsonPacket json = ((JsonCommandPacket) packet).getJsonRequest();
          if (json == null) {
            final ErrorCode code = ErrorCode.CommandNotFound;
            final R66Result resulttest =
                new R66Result(serverHandler.getSession(), true, code,
                              serverHandler.getSession().getRunner());
            json = new JsonPacket();
            json.setComment("Invalid command");
            json.setRequestUserPacket(
                ((JsonCommandPacket) packet).getTypeValid());
            final JsonCommandPacket valid =
                new JsonCommandPacket(json, resulttest.getCode().getCode(),
                                      LocalPacketFactory.REQUESTUSERPACKET);
            resulttest.setOther(packet);
            serverHandler.getLocalChannelReference()
                         .validateRequest(resulttest);
            try {
              ChannelUtils.writeAbstractLocalPacket(
                  serverHandler.getLocalChannelReference(), valid, true);
            } catch (final OpenR66ProtocolPacketException ignored) {
              // ignore
            }
            serverHandler.getSession().setStatus(99);
            localChannelReference.close();
            return;
          }
          json.setRequestUserPacket(
              ((JsonCommandPacket) packet).getTypeValid());
          if (((JsonCommandPacket) packet).getTypeValid() ==
              LocalPacketFactory.REQUESTPACKET) {
            final RequestJsonPacket node = (RequestJsonPacket) json;
            final String newfilename = node.getFilename();
            if (newfilename == null) {
              // error so ignore
              return;
            }
            final long newSize = node.getFilesize();
            final String newFileInfo = node.getFileInfo();
            logger.debug("NewSize {} NewName {}", newSize, newfilename);
            // potential fileInfo changed
            if (newFileInfo != null && !newFileInfo.equals(
                serverHandler.getSession().getRunner().getFileInformation())) {
              logger.debug("NewSize {} NewName {} newFileInfo {}", newSize,
                           newfilename, newFileInfo);
              serverHandler.requestChangeFileInfo(newFileInfo);
            }
            // potential file size changed
            serverHandler.requestChangeNameSize(newfilename, newSize);
          } else {
            serverHandler.jsonCommand((JsonCommandPacket) packet);
          }
          break;
        }
        default: {
          logger.error("Unknown Mesg: " + packet.getClass().getName());
          serverHandler.getSession().newState(ERROR);
          serverHandler.getLocalChannelReference().invalidateRequest(
              new R66Result(
                  new OpenR66ProtocolSystemException("Unknown Message"),
                  serverHandler.getSession(), true, ErrorCode.Unimplemented,
                  null));
          final ErrorPacket errorPacket =
              new ErrorPacket("Unkown Mesg: " + packet.getClass().getName(),
                              ErrorCode.Unimplemented.getCode(),
                              ErrorPacket.FORWARDCLOSECODE);
          ChannelUtils.writeAbstractLocalPacket(
              serverHandler.getLocalChannelReference(), errorPacket, true);
          packet.clear();
        }
      }
    }
  }

  public static void exceptionCaught(final TransferActions serverHandler,
                                     final Throwable cause) {
    // inform clients
    logger.debug("Exception and isFinished: {}",
                 (serverHandler.getLocalChannelReference() != null &&
                  serverHandler.getLocalChannelReference().getFutureRequest()
                               .isDone()), cause);
    if (serverHandler.getLocalChannelReference() != null &&
        serverHandler.getLocalChannelReference().getFutureRequest().isDone()) {
      ChannelCloseTimer.closeFutureTransaction(serverHandler);
      return;
    }
    final OpenR66Exception exception = OpenR66ExceptionTrappedFactory
        .getExceptionFromTrappedException(
            serverHandler.getLocalChannelReference() != null?
                serverHandler.getLocalChannelReference().getNetworkChannel() :
                null, cause);
    ErrorCode code;
    if (exception != null) {
      serverHandler.getSession().newState(ERROR);
      boolean isAnswered = false;
      if (exception instanceof OpenR66ProtocolShutdownException) {
        shutdownFromException(serverHandler, exception);
        return;
      } else {
        if (serverHandler.getLocalChannelReference() != null &&
            serverHandler.getLocalChannelReference().getFutureRequest() !=
            null) {
          if (serverHandler.getLocalChannelReference().getFutureRequest()
                           .isDone()) {
            final R66Result result =
                serverHandler.getLocalChannelReference().getFutureRequest()
                             .getResult();
            if (result != null) {
              isAnswered = result.isAnswered();
            }
          }
        }
        final DbTaskRunner runner = serverHandler.getSession().getRunner();
        if (exception instanceof OpenR66ProtocolNoConnectionException) {
          code = ErrorCode.ConnectionImpossible;
          if (runner != null) {
            runner.stopOrCancelRunner(code);
          }
        } else if (exception instanceof OpenR66ProtocolBusinessCancelException) {
          code = ErrorCode.CanceledTransfer;
          if (runner != null) {
            runner.stopOrCancelRunner(code);
          }
        } else if (exception instanceof OpenR66ProtocolBusinessStopException) {
          code = ErrorCode.StoppedTransfer;
          if (runner != null) {
            runner.stopOrCancelRunner(code);
          }
        } else if (exception instanceof OpenR66ProtocolBusinessQueryAlreadyFinishedException) {
          code = ErrorCode.QueryAlreadyFinished;
          try {
            serverHandler.tryFinalizeRequest(
                new R66Result(serverHandler.getSession(), true, code, runner));
            ChannelCloseTimer.closeFutureTransaction(serverHandler);
            return;
          } catch (final OpenR66RunnerErrorException ignored) {
            // nothing
          } catch (final OpenR66ProtocolSystemException ignored) {
            // nothing
          }
        } else if (exception instanceof OpenR66ProtocolBusinessQueryStillRunningException) {
          code = ErrorCode.QueryStillRunning;
          // nothing is to be done
          logger.error("Will close channel since ", exception);
          serverHandler.getLocalChannelReference().close();
          serverHandler.getSession().setStatus(56);
          return;
        } else if (exception instanceof OpenR66ProtocolBusinessRemoteFileNotFoundException) {
          code = ErrorCode.FileNotFound;
        } else if (exception instanceof OpenR66RunnerException) {
          code = ErrorCode.ExternalOp;
        } else if (exception instanceof OpenR66RunnerErrorException) {
          code = ErrorCode.ExternalOp;
        } else if (exception instanceof OpenR66ProtocolNotAuthenticatedException) {
          code = ErrorCode.BadAuthent;
          isAnswered = true;
        } else if (exception instanceof OpenR66ProtocolNetworkException) {
          code = ErrorCode.Disconnection;
          if (runner != null) {
            final R66Result finalValue = new R66Result(
                new OpenR66ProtocolSystemException(
                    Messages.getString("LocalServerHandler.2")),
                //$NON-NLS-1$
                serverHandler.getSession(), true, code, runner);
            try {
              serverHandler.tryFinalizeRequest(finalValue);
            } catch (final OpenR66Exception ignored) {
              // nothing
            }
          }
        } else if (exception instanceof OpenR66ProtocolRemoteShutdownException) {
          code = ErrorCode.RemoteShutdown;
          if (runner != null) {
            runner.stopOrCancelRunner(code);
          }
        } else if (exception instanceof OpenR66ProtocolNoDataException) {
          code = ErrorCode.FileNotFound;
        } else {
          if (runner != null) {
            switch (runner.getErrorInfo()) {
              case InitOk:
              case PostProcessingOk:
              case PreProcessingOk:
              case Running:
              case TransferOk:
                code = ErrorCode.Internal;
                break;
              default:
                code = runner.getErrorInfo();
            }
          } else {
            code = ErrorCode.Internal;
          }
        }
        if (!isAnswered &&
            !(exception instanceof OpenR66ProtocolBusinessNoWriteBackException) &&
            !(exception instanceof OpenR66ProtocolNoConnectionException)) {
          if (code == null || code == ErrorCode.Internal) {
            code = ErrorCode.RemoteError;
          }
          final ErrorPacket errorPacket =
              new ErrorPacket(exception.getMessage(), code.getCode(),
                              ErrorPacket.FORWARDCLOSECODE);
          try {
            if (serverHandler.getLocalChannelReference() != null) {
              ChannelUtils.writeAbstractLocalPacket(
                  serverHandler.getLocalChannelReference(), errorPacket, true);
            }
          } catch (final OpenR66ProtocolPacketException e1) {
            // should not be
          }
        }
        final R66Result finalValue =
            new R66Result(exception, serverHandler.getSession(), true, code,
                          runner);
        try {
          serverHandler.getSession().setFinalizeTransfer(false, finalValue);
          if (serverHandler.getLocalChannelReference() != null) {
            serverHandler.getLocalChannelReference()
                         .invalidateRequest(finalValue);
          }
        } catch (final OpenR66RunnerErrorException e1) {
          if (serverHandler.getLocalChannelReference() != null) {
            serverHandler.getLocalChannelReference()
                         .invalidateRequest(finalValue);
          }
        } catch (final OpenR66ProtocolSystemException e1) {
          if (serverHandler.getLocalChannelReference() != null) {
            serverHandler.getLocalChannelReference()
                         .invalidateRequest(finalValue);
          }
        }
      }
      if (exception instanceof OpenR66ProtocolBusinessNoWriteBackException) {
        logger.error("Will close channel {}", exception.getMessage());
        ChannelCloseTimer.closeFutureTransaction(serverHandler);
        serverHandler.getSession().setStatus(56);
        return;
      } else if (exception instanceof OpenR66ProtocolNoConnectionException) {
        logger.error("Will close channel {}", exception.getMessage());
        ChannelCloseTimer.closeFutureTransaction(serverHandler);
        serverHandler.getSession().setStatus(57);
        return;
      }
      serverHandler.getSession().setStatus(58);
      ChannelCloseTimer.closeFutureTransaction(serverHandler);
    } else {
      // Nothing to do
      serverHandler.getSession().setStatus(59);
    }
  }

  private static void shutdownFromException(final TransferActions serverHandler,
                                            final OpenR66Exception exception) {
    WaarpShutdownHook.shutdownWillStart();
    logger.warn(Messages.getString("LocalServerHandler.0") +
                //$NON-NLS-1$
                serverHandler.getSession().getAuth().getUser());
    if (serverHandler.getLocalChannelReference() != null) {
      final R66Result finalValue =
          new R66Result(exception, serverHandler.getSession(), true,
                        ErrorCode.Shutdown,
                        serverHandler.getSession().getRunner());
      try {
        serverHandler.tryFinalizeRequest(finalValue);
      } catch (final OpenR66RunnerErrorException ignored) {
        // ignore
      } catch (final OpenR66ProtocolSystemException ignored) {
        // ignore
      }
      if (!serverHandler.getLocalChannelReference().getFutureRequest()
                        .isDone()) {
        try {
          serverHandler.getSession().setFinalizeTransfer(false, finalValue);
        } catch (final OpenR66RunnerErrorException e1) {
          serverHandler.getLocalChannelReference()
                       .invalidateRequest(finalValue);
        } catch (final OpenR66ProtocolSystemException e1) {
          serverHandler.getLocalChannelReference()
                       .invalidateRequest(finalValue);
        }
      }
    }
    // dont'close, thread will do
    ChannelUtils.startShutdown();
    // set global shutdown info and before close, send a valid
    // shutdown to all
    serverHandler.getSession().setStatus(54);
  }
}
