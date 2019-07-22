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
package org.waarp.openr66.context.task;

import java.io.File;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.filesystem.R66Dir;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;
import org.waarp.openr66.protocol.utils.FileUtils;

/**
 * Move the file (without renaming it)
 * 
 * @author Frederic Bregier
 * 
 */
public class MoveTask extends AbstractTask {
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(MoveTask.class);

    /**
     * @param argRule
     * @param delay
     * @param argTransfer
     * @param session
     */
    public MoveTask(String argRule, int delay, String argTransfer,
            R66Session session) {
        super(TaskType.MOVE, delay, argRule, argTransfer, session);
    }

    @Override
    public void run() {
        logger.info("Move with " + argRule + ":" + argTransfer + " and {}",
                session);
        String directory = argRule;
        directory = getReplacedValue(directory, argTransfer.split(" ")).trim().replace('\\', '/');
        String finalname = directory + R66Dir.SEPARATOR + session.getFile().getBasename();
        File from = session.getFile().getTrueFile();
        File to = new File(finalname);
        try {
            FileUtils.copy(from, to, true, false);
        } catch (OpenR66ProtocolSystemException e) {
            logger.error("Move with " + argRule + ":" + argTransfer + " to " + finalname + " and " +
                    session, e);
            futureCompletion.setFailure(e);
            return;
        }
        session.getRunner().setFileMoved(finalname, true);
        futureCompletion.setSuccess();

    }

}
