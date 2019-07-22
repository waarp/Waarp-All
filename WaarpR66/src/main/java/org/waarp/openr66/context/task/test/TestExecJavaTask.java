/**
 * This file is part of Waarp Project.
 * 
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual contributors.
 * 
 * All Waarp Project is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Waarp . If not, see
 * <http://www.gnu.org/licenses/>.
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
 * 
 * 2nd argument is a numerical rank. When rank > 100 stops, else increment rank.
 * 
 * @author Frederic Bregier
 * 
 */
public class TestExecJavaTask extends AbstractExecJavaTask {

    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(TestExecJavaTask.class);

    @Override
    public void run() {
        if (callFromBusiness) {
            // Business Request to validate?
            if (isToValidate) {
                String[] args = fullarg.split(" ");
                int rank = Integer.parseInt(args[1]);
                rank++;
                BusinessRequestPacket packet =
                        new BusinessRequestPacket(this.getClass().getName() + " business " + rank
                                + " final return", 0);
                if (rank > 100) {
                    validate(packet);
                    logger.info("Will NOT close the channel: " + rank);
                } else {
                    logger.info("Continue: " + rank);
                    if (session.getLocalChannelReference() != null) {
                        try {
                            ChannelUtils.writeAbstractLocalPacket(session.getLocalChannelReference(),
                                    packet, true);
                        } catch (OpenR66ProtocolPacketException e) {
                        }
                    }
                }
                this.status = 0;
                return;
            }
            finalValidate("Validated");
            return;
        } else {
            // Rule EXECJAVA based
            R66File file = session.getFile();
            DbTaskRunner runner = session.getRunner();
            if (file == null) {
                logger.info("TestExecJavaTask No File");
            } else {
                try {
                    logger.info("TestExecJavaTask File: " + file.getFile());
                } catch (CommandAbstractException e) {
                }
            }
            if (runner == null) {
                logger.warn("TestExecJavaTask No Runner: " + fullarg);
            } else {
                logger.warn("TestExecJavaTask Runner: " + runner.toShortString());
            }
            this.status = 0;
        }
    }
}
