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

import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.packet.BusinessRequestPacket;
import org.waarp.openr66.protocol.localhandler.packet.ErrorPacket;
import org.waarp.openr66.protocol.utils.ChannelUtils;

import java.util.regex.Pattern;

/**
 * Dummy Runnable Task that only logs
 */
public abstract class AbstractExecJavaTask implements R66Runnable {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(AbstractExecJavaTask.class);
  protected static final Pattern BLANK = WaarpStringUtils.BLANK;

  protected int delay;
  protected int status = -1;
  protected R66Session session;
  protected boolean waitForValidation;
  protected boolean useLocalExec;

  protected String classname;
  protected String fullarg;
  protected boolean isToValidate;
  protected boolean callFromBusiness;

  protected String finalInformation;

  /**
   * Server side method to validate the request
   *
   * @param packet
   */
  public void validate(BusinessRequestPacket packet) {
    status = 0;
    packet.validate();
    if (callFromBusiness) {
      final LocalChannelReference localChannelReference =
          session.getLocalChannelReference();
      if (localChannelReference != null) {
        final R66Result result =
            new R66Result(session, true, ErrorCode.CompleteOk, null);
        localChannelReference.validateRequest(result);
        try {
          ChannelUtils
              .writeAbstractLocalPacket(localChannelReference, packet, true);
        } catch (final OpenR66ProtocolPacketException ignored) {
          // nothing
        }
      } else {
        finalInformation = packet.toString();
      }
    }
  }

  /**
   * To be called by the requester when finished
   *
   * @param object special object to get back
   */
  public void finalValidate(Object object) {
    status = 0;
    if (callFromBusiness) {
      final LocalChannelReference localChannelReference =
          session.getLocalChannelReference();
      if (localChannelReference != null) {
        final R66Result result =
            new R66Result(session, true, ErrorCode.CompleteOk, null);
        result.setOther(object);
        localChannelReference.validateRequest(result);
        localChannelReference.close();
      } else {
        finalInformation = JsonHandler.writeAsString(object);
      }
    }
  }

  /**
   * To be used if abnormal usage is made of one Java Method
   */
  public void invalid() {
    status = 2;
    if (!callFromBusiness) {
      return;
    }
    final R66Result result =
        new R66Result(null, session, true, ErrorCode.Unimplemented,
                      session.getRunner());
    final LocalChannelReference localChannelReference =
        session.getLocalChannelReference();
    if (localChannelReference != null) {
      localChannelReference.sessionNewState(R66FiniteDualStates.ERROR);
      final ErrorPacket error = new ErrorPacket("Command Incompatible",
                                                ErrorCode.ExternalOp.getCode(),
                                                ErrorPacket.FORWARDCLOSECODE);
      try {
        ChannelUtils
            .writeAbstractLocalPacket(localChannelReference, error, true);
      } catch (final OpenR66ProtocolPacketException ignored) {
        // nothing
      }
      localChannelReference.invalidateRequest(result);
      localChannelReference.close();
    }
  }

  @Override
  public void run() {
    if (callFromBusiness) {
      // Business Request to validate?
      if (isToValidate) {
        final BusinessRequestPacket packet =
            new BusinessRequestPacket(classname + ' ' + fullarg, 0);
        validate(packet);
      }
    }
    final StringBuilder builder =
        new StringBuilder(getClass().getSimpleName()).append(':')
                                                     .append("args(")
                                                     .append(fullarg)
                                                     .append(')');
    logger.warn(builder.toString());
    status = 0;
  }

  @Override
  public void setArgs(R66Session session, boolean waitForValidation,
                      boolean useLocalExec, int delay, String classname,
                      String arg, boolean callFromBusiness,
                      boolean isToValidate) {
    this.session = session;
    this.waitForValidation = waitForValidation;
    this.useLocalExec = useLocalExec;
    this.delay = delay;
    this.classname = classname;
    this.callFromBusiness = callFromBusiness;
    fullarg = arg;
    this.isToValidate = isToValidate;
  }

  @Override
  public int getFinalStatus() {
    return status;
  }

  @Override
  public String toString() {
    if (status == -1 || finalInformation == null) {
      final StringBuilder builder =
          new StringBuilder(getClass().getSimpleName()).append(": [")
                                                       .append(fullarg)
                                                       .append(']');
      return builder.toString();
    } else {
      return finalInformation;
    }
  }

}
