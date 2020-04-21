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

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import org.waarp.common.database.DbSession;
import org.waarp.common.guid.IntegerUuid;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.client.RecvThroughHandler;
import org.waarp.openr66.commander.ClientRunner;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.PartnerConfiguration;
import org.waarp.openr66.protocol.exception.OpenR66Exception;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoConnectionException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolRemoteShutdownException;
import org.waarp.openr66.protocol.networkhandler.NetworkChannelReference;
import org.waarp.openr66.protocol.networkhandler.NetworkServerHandler;
import org.waarp.openr66.protocol.networkhandler.NetworkServerInitializer;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.R66Future;
import org.waarp.openr66.protocol.utils.R66Versions;

import static org.waarp.common.database.DbConstant.*;

/**
 * Reference of one object using Local Channel localId and containing local
 * channel and network channel.
 */
public class LocalChannelReference {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(LocalChannelReference.class);

  /**
   * Network Channel Ref
   */
  private final NetworkChannelReference networkChannelRef;
  /**
   * Traffic handler associated if any
   */
  private final ChannelTrafficShapingHandler cts;

  /**
   * Network Server Handler
   */
  private final NetworkServerHandler networkServerHandler;

  /**
   * Server Actions handler
   */
  private final TransferActions serverHandler = new TransferActions();

  /**
   * Local Id
   */
  private final Integer localId;

  /**
   * Remote Id
   */
  private Integer remoteId;

  /**
   * Requested_requester_specialId
   */
  private String requestId;
  /**
   * Future on Global Request
   */
  private final R66Future futureRequest;

  /**
   * Future on Valid Starting Request
   */
  private final R66Future futureValidRequest = new R66Future(true);

  /**
   * Future on Transfer if any
   */
  private R66Future futureEndTransfer = new R66Future(true);

  /**
   * Future on Connection
   */
  private final R66Future futureConnection = new R66Future(true);

  /**
   * Future on Startup
   */
  private final R66Future futureStartup = new R66Future(true);

  /**
   * Session
   */
  private R66Session session;

  /**
   * Last error message
   */
  private String errorMessage = "NoError";

  /**
   * Last error code
   */
  private ErrorCode code = ErrorCode.Unknown;

  /**
   * RecvThroughHandler
   */
  private RecvThroughHandler recvThroughHandler;

  private boolean isSendThroughMode;
  /**
   * Thread for ClientRunner if any
   */
  private ClientRunner clientRunner;

  /**
   * To be able to check hash once all transfer is over once again
   */
  private String hashComputeDuringTransfer;
  /**
   * If partial hash, no global hash validation can be done
   */
  private boolean partialHash;

  /**
   * PartnerConfiguration
   */
  private volatile PartnerConfiguration partner;
  /**
   * DbSession for Database that do not support concurrency in access
   */
  private volatile DbSession noconcurrencyDbSession;

  /**
   * @param networkChannelRef
   * @param remoteId
   * @param futureRequest
   *
   * @throws OpenR66ProtocolRemoteShutdownException
   */
  public LocalChannelReference(NetworkChannelReference networkChannelRef,
                               Integer remoteId, R66Future futureRequest)
      throws OpenR66ProtocolRemoteShutdownException {
    this.networkChannelRef = networkChannelRef;
    networkServerHandler =
        (NetworkServerHandler) this.networkChannelRef.channel().pipeline().get(
            NetworkServerInitializer.NETWORK_HANDLER);
    localId = new IntegerUuid().getInt();
    this.remoteId = remoteId;
    if (futureRequest == null) {
      this.futureRequest = new R66Future(true);
    } else {
      if (futureRequest.isDone()) {
        futureRequest.reset();
      }
      this.futureRequest = futureRequest;
    }
    cts = (ChannelTrafficShapingHandler) networkChannelRef.channel().pipeline()
                                                          .get(
                                                              NetworkServerInitializer.LIMITCHANNEL);
    LocalServerHandler.channelActive(serverHandler);
    serverHandler.setLocalChannelReference(this);
    networkChannelRef.add(this);
  }

  /**
   * Special empty LCR constructor
   */
  public LocalChannelReference() {
    networkChannelRef = null;
    networkServerHandler = null;
    localId = 0;
    futureRequest = new R66Future(true);
    cts = null;
    serverHandler.localChannelReference = this;
  }

  /**
   * Close the localChannelReference
   */
  public void close() {
    LocalServerHandler.channelInactive(serverHandler);
    if (networkChannelRef != null) {
      networkChannelRef.remove(this);
    }
    LocalTransaction lt = Configuration.configuration.getLocalTransaction();
    if (lt != null) {
      lt.remove(this);
    }
  }

  /**
   * @return the networkChannelRef
   */
  public Channel getNetworkChannel() {
    return networkChannelRef.channel();
  }

  /**
   * @return the id
   */
  public Integer getLocalId() {
    return localId;
  }

  /**
   * @return the remoteId
   */
  public Integer getRemoteId() {
    return remoteId;
  }

  /**
   * @return the ChannelTrafficShapingHandler
   */
  public ChannelTrafficShapingHandler getChannelTrafficShapingHandler() {
    return cts;
  }

  /**
   * @return the networkChannelObject
   */
  public NetworkChannelReference getNetworkChannelObject() {
    return networkChannelRef;
  }

  /**
   * @return the networkServerHandler
   */
  public NetworkServerHandler getNetworkServerHandler() {
    return networkServerHandler;
  }

  /**
   * @return the serverHandler
   */
  public TransferActions getServerHandler() {
    return serverHandler;
  }

  /**
   * @return the channelHandlerContextNetwork
   */
  public ChannelHandlerContext getChannelHandlerContextNetwork() {
    return networkChannelRef.channel().pipeline()
                            .context(NetworkServerInitializer.NETWORK_HANDLER);
  }

  /**
   * @return the actual dbSession
   */
  public DbSession getDbSession() {
    if (noconcurrencyDbSession != null) {
      return noconcurrencyDbSession;
    }
    if (networkServerHandler != null) {
      return networkServerHandler.getDbSession();
    }
    logger.info("SHOULD NOT BE");
    return admin.getSession();
  }

  /**
   * @param remoteId the remoteId to set
   */
  public void setRemoteId(Integer remoteId) {
    this.remoteId = remoteId;
  }

  /**
   * @return the session
   */
  public R66Session getSession() {
    return session;
  }

  /**
   * @param session the session to set
   */
  public void setSession(R66Session session) {
    this.session = session;
  }

  /**
   * @return the current errorMessage
   */
  public String getErrorMessage() {
    return errorMessage;
  }

  /**
   * @param errorMessage the errorMessage to set
   */
  public void setErrorMessage(String errorMessage, ErrorCode code) {
    this.errorMessage = errorMessage;
    this.code = code;
  }

  /**
   * @return the code
   */
  public ErrorCode getCurrentCode() {
    return code;
  }

  /**
   * Validate or not the Startup (before connection)
   *
   * @param validate
   */
  public void validateStartup(boolean validate) {
    if (futureStartup.isDone()) {
      return;
    }
    if (validate) {
      futureStartup.setSuccess();
    } else {
      futureStartup.cancel();
    }
  }

  /**
   * @return the futureValidateStartup
   */
  public R66Future getFutureValidateStartup() {
    if (!futureStartup.awaitOrInterruptible()) {
      validateStartup(false);
      return futureStartup;
    }
    return futureStartup;
  }

  /**
   * @return True if the connection is validated (in OK or KO status)
   */
  public boolean isConnectionValidate() {
    return futureConnection.isDone();
  }

  /**
   * Validate or Invalidate the connection (authentication)
   *
   * @param validate
   */
  public void validateConnection(boolean validate, R66Result result) {
    if (futureConnection.isDone()) {
      logger.debug("LocalChannelReference already validated: " +
                   futureConnection.isSuccess());
      return;
    }
    logger.debug("Validation of connection {}", validate);
    if (validate) {
      futureConnection.setResult(result);
      futureConnection.setSuccess();
    } else {
      futureConnection.setResult(result);
      setErrorMessage(result.getMessage(), result.getCode());
      futureConnection.cancel();
    }
  }

  /**
   * @return the futureValidateConnection
   */
  public R66Future getFutureValidateConnection() {
    R66Result result;
    final Channel channel = networkChannelRef.channel();
    if (channel != null && channel.isActive()) {
      if (!futureConnection.awaitOrInterruptible()) {
        if (futureConnection.isDone()) {
          return futureConnection;
        } else {
          logger.warn("Cannot get Connection due to out of Time: {}", this);
          result = new R66Result(
              new OpenR66ProtocolNoConnectionException("Out of time"), session,
              false, ErrorCode.ConnectionImpossible, null);
          validateConnection(false, result);
          return futureConnection;
        }
      } else {
        return futureConnection;
      }
    }
    if (futureConnection.isDone()) {
      return futureConnection;
    }

    logger.info("Cannot get Connection due to out of Time: {}", this);
    result =
        new R66Result(new OpenR66ProtocolNoConnectionException("Out of time"),
                      session, false, ErrorCode.ConnectionImpossible, null);
    validateConnection(false, result);
    return futureConnection;
  }

  /**
   * Validate the End of a Transfer
   *
   * @param finalValue
   */
  public void validateEndTransfer(R66Result finalValue) {
    if (!futureEndTransfer.isDone()) {
      futureEndTransfer.setResult(finalValue);
      futureEndTransfer.setSuccess();
    } else {
      logger.debug("Could not validate since Already validated: " +
                   futureEndTransfer.isSuccess() + ' ' + finalValue);
      if (!futureEndTransfer.getResult().isAnswered()) {
        futureEndTransfer.getResult().setAnswered(finalValue.isAnswered());
      }
    }
  }

  /**
   * @return the futureEndTransfer
   */
  public R66Future getFutureEndTransfer() {
    return futureEndTransfer;
  }

  /**
   * Special waiter for Send Through method. It reset the EndTransfer future.
   *
   * @throws OpenR66Exception
   */
  public void waitReadyForSendThrough() throws OpenR66Exception {
    logger.debug("Wait for End of Prepare Transfer");
    futureEndTransfer.awaitOrInterruptible();
    if (futureEndTransfer.isSuccess()) {
      // reset since transfer will start now
      futureEndTransfer = new R66Future(true);
    } else {
      if (futureEndTransfer.getResult() != null &&
          futureEndTransfer.getResult().getException() != null) {
        throw futureEndTransfer.getResult().getException();
      } else if (futureEndTransfer.getCause() != null) {
        throw new OpenR66RunnerErrorException(futureEndTransfer.getCause());
      } else {
        throw new OpenR66RunnerErrorException("Unknown reason");
      }
    }
  }

  /**
   * @return the futureValidRequest
   */
  public R66Future getFutureValidRequest() {
    return futureValidRequest;
  }

  /**
   * @return the futureRequest
   */
  public R66Future getFutureRequest() {
    return futureRequest;
  }

  /**
   * Invalidate the current request
   *
   * @param finalvalue
   */
  public void invalidateRequest(R66Result finalvalue) {
    R66Result finalValue = finalvalue;
    if (finalValue == null) {
      finalValue =
          new R66Result(session, false, ErrorCode.Unknown, session.getRunner());
    }
    logger.debug("FST: " + futureStartup.isDone() + ":" + futureStartup.isSuccess() +
    		     " FCT: " + futureConnection.isDone() + ':' +
    		     futureConnection.isSuccess() + 
    		     " FET: " + futureEndTransfer.isDone() + ':' +
                 futureEndTransfer.isSuccess() + " FVR: " +
                 futureValidRequest.isDone() + ':' +
                 futureValidRequest.isSuccess() + " FR: " +
                 futureRequest.isDone() + ':' + futureRequest.isSuccess() +
                 ' ' + finalValue.getMessage());
    if (!futureStartup.isDone()) {
    	futureStartup.setResult(finalValue);
        if (finalValue.getException() != null) {
        	futureStartup.setFailure(finalValue.getException());
        } else {
        	futureStartup.cancel();
        }
      }
    if (!futureConnection.isDone()) {
      futureConnection.setResult(finalValue);
      if (finalValue.getException() != null) {
        futureConnection.setFailure(finalValue.getException());
      } else {
        futureConnection.cancel();
      }
    }
    if (!futureEndTransfer.isDone()) {
      futureEndTransfer.setResult(finalValue);
      if (finalValue.getException() != null) {
        futureEndTransfer.setFailure(finalValue.getException());
      } else {
        futureEndTransfer.cancel();
      }
    }
    if (!futureValidRequest.isDone()) {
      futureValidRequest.setResult(finalValue);
      if (finalValue.getException() != null) {
        futureValidRequest.setFailure(finalValue.getException());
      } else {
        futureValidRequest.cancel();
      }
    }
    logger.trace("Invalidate Request",
                 new Exception("DEBUG Trace for " + "Invalidation"));
    if (finalValue.getCode() != ErrorCode.ServerOverloaded) {
      if (!futureRequest.isDone()) {
        setErrorMessage(finalValue.getMessage(), finalValue.getCode());
        futureRequest.setResult(finalValue);
        if (finalValue.getException() != null) {
          futureRequest.setFailure(finalValue.getException());
        } else {
          futureRequest.cancel();
        }
      } else {
        logger.debug("Could not invalidate since Already finished: " +
                     futureEndTransfer.getResult());
      }
    } else {
      setErrorMessage(finalValue.getMessage(), finalValue.getCode());
      logger.debug("Overloaded");
    }
    if (session != null) {
      final DbTaskRunner runner = session.getRunner();
      if (runner != null && runner.isSender()) {
        NetworkTransaction.stopRetrieve(this);
      }
    }
  }

  /**
   * Validate the current Request
   *
   * @param finalValue
   */
  public void validateRequest(R66Result finalValue) {
    setErrorMessage("NoError", null);
    if (!futureEndTransfer.isDone()) {
      logger.debug("Will validate EndTransfer");
      validateEndTransfer(finalValue);
    }
    if (!futureValidRequest.isDone()) {
      futureValidRequest.setResult(finalValue);
      futureValidRequest.setSuccess();
    }
    logger.debug("Validate Request");
    if (!futureRequest.isDone()) {
      if (finalValue.getOther() == null &&
          session.getBusinessObject() != null &&
          session.getBusinessObject().getInfo(session) != null) {
        finalValue.setOther(session.getBusinessObject().getInfo(session));
      }
      futureRequest.setResult(finalValue);
      futureRequest.setSuccess();
    } else {
      logger.info(
          "Already validated: " + futureRequest.isSuccess() + ' ' + finalValue);
      if (!futureRequest.getResult().isAnswered()) {
        futureRequest.getResult().setAnswered(finalValue.isAnswered());
      }
    }
  }

  private long getMinLimit(long a, long b) {
    long res = a;
    if (a <= 0) {
      res = b;
    } else if (b > 0 && b < a) {
      res = b;
    }
    return res;
  }

  public void setChannelLimit(boolean isSender, long limit) {
    final ChannelTrafficShapingHandler limitHandler =
        (ChannelTrafficShapingHandler) networkChannelRef.channel().pipeline()
                                                        .get(
                                                            NetworkServerInitializer.LIMITCHANNEL);
    if (isSender) {
      limitHandler.setWriteLimit(limit);
      logger.info("Will write at {} Bytes/sec", limit);
    } else {
      limitHandler.setReadLimit(limit);
      logger.info("Will read at {} Bytes/sec", limit);
    }
  }

  public long getChannelLimit(boolean isSender) {
    long global;
    long channel;
    if (isSender) {
      global = Configuration.configuration.getServerGlobalWriteLimit();
      channel = Configuration.configuration.getServerChannelWriteLimit();
    } else {
      global = Configuration.configuration.getServerGlobalReadLimit();
      channel = Configuration.configuration.getServerChannelReadLimit();
    }
    return getMinLimit(global, channel);
  }

  @Override
  public String toString() {
    return "LCR: L: " + localId + " R: " + remoteId + " Startup[" +
           futureStartup + "] Conn[" + futureConnection +
           "] ValidRequestRequest[" + futureValidRequest + "] EndTransfer[" +
           (futureEndTransfer != null? futureEndTransfer : "noEndTransfer") +
           "] Request[" + (futureRequest != null? futureRequest : "noRequest") +
           ']';
  }

  /**
   * @return the recvThroughHandler
   */
  public RecvThroughHandler getRecvThroughHandler() {
    return recvThroughHandler;
  }

  /**
   * @return True if in RecvThrough Mode
   */
  public boolean isRecvThroughMode() {
    return recvThroughHandler != null;
  }

  /**
   * @param recvThroughHandler the recvThroughHandler to set
   */
  public void setRecvThroughHandler(RecvThroughHandler recvThroughHandler) {
    this.recvThroughHandler = recvThroughHandler;
  }

  /**
   * @return True if in SendThrough Mode
   */
  public boolean isSendThroughMode() {
    return isSendThroughMode;
  }

  /**
   * @param isSendThroughMode the isSendThroughMode to set
   */
  public void setSendThroughMode(boolean isSendThroughMode) {
    this.isSendThroughMode = isSendThroughMode;
  }

  /**
   * @return the clientRunner
   */
  public ClientRunner getClientRunner() {
    return clientRunner;
  }

  /**
   * @param clientRunner the clientRunner to set
   */
  public void setClientRunner(ClientRunner clientRunner) {
    this.clientRunner = clientRunner;
  }

  /**
   * Shortcut to set a new state in Session
   *
   * @param desiredState
   */
  public void sessionNewState(R66FiniteDualStates desiredState) {
    if (session != null) {
      session.newState(desiredState);
    }
  }

  /**
   * @return the current state or TEST if no session exists
   */
  public R66FiniteDualStates getSessionState() {
    if (session != null) {
      return session.getState();
    }
    return R66FiniteDualStates.TEST;
  }

  /**
   * @return the hashComputeDuringTransfer
   */
  public String getHashComputeDuringTransfer() {
    return hashComputeDuringTransfer;
  }

  /**
   * @param hashComputeDuringTransfer the hashComputeDuringTransfer to
   *     set
   */
  public void setHashComputeDuringTransfer(String hashComputeDuringTransfer) {
    this.hashComputeDuringTransfer = hashComputeDuringTransfer;
  }

  public void setPartialHash() {
    partialHash = true;
  }

  public boolean isPartialHash() {
    return partialHash;
  }

  /**
   * @return the partner
   */
  public PartnerConfiguration getPartner() {
    return partner;
  }

  /**
   * @param hostId the partner to set
   */
  public void setPartner(String hostId) {
    logger.debug("host:" + hostId);
    partner = Configuration.configuration.getVersions().get(hostId);
    if (partner == null) {
      partner =
          new PartnerConfiguration(hostId, R66Versions.V2_4_12.getVersion());
    }
  }

  /**
   * @return the requestId
   */
  public String getRequestId() {
    return requestId;
  }

  /**
   * @param requestId the requestId to set
   */
  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

}
