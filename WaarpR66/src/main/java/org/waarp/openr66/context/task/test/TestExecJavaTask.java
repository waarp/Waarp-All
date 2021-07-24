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
package org.waarp.openr66.context.task.test;

import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.context.filesystem.R66File;
import org.waarp.openr66.context.task.AbstractExecJavaTask;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.packet.BusinessRequestPacket;
import org.waarp.openr66.protocol.utils.ChannelUtils;

/**
 * Example of Java Task for ExecJava
 * <p>
 * 2nd argument is a numerical rank. When rank > 100 stops, else increment rank.
 */
public class TestExecJavaTask extends AbstractExecJavaTask {

  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(TestExecJavaTask.class);

  @Override
  public void run() {
    if (callFromBusiness) {
      // Business Request to validate?
      if (isToValidate) {
        final String[] args = BLANK.split(fullarg);
        int rank = Integer.parseInt(args[1]);
        rank++;
        final BusinessRequestPacket packet = new BusinessRequestPacket(
            getClass().getName() + " business " + rank + " final return", 0);
        if (rank > 100) {
          validate(packet);
          logger.info("Will NOT close the channel: {}", rank);
        } else {
          logger.info("Continue: {}", rank);
          if (session.getLocalChannelReference() != null) {
            try {
              ChannelUtils.writeAbstractLocalPacket(
                  session.getLocalChannelReference(), packet, true);
            } catch (final OpenR66ProtocolPacketException ignored) {
              // nothing
            }
          }
        }
        status = 0;
        return;
      }
      finalValidate("Validated");
    } else {
      // Rule EXECJAVA based
      final R66File file = session.getFile();
      final DbTaskRunner runner = session.getRunner();
      if (file == null) {
        logger.info("TestExecJavaTask No File");
      } else {
        try {
          logger.info("TestExecJavaTask File: {}", file.getFile());
        } catch (final CommandAbstractException ignored) {
          // nothing
        }
      }
      if (runner == null) {
        logger.warn("TestExecJavaTask No Runner: " + fullarg);
      } else {
        logger.warn("TestExecJavaTask Runner: " + runner.toShortString());
      }
      status = 0;
    }
  }
}
