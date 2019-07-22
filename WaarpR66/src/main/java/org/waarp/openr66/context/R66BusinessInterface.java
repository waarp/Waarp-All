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
package org.waarp.openr66.context;

import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;

/**
 * @author Frederic Bregier
 * 
 */
public interface R66BusinessInterface {
    /**
     * Called once the connection is opened
     * @param session
     * @throws OpenR66RunnerErrorException
     */
    public void checkAtConnection(R66Session session) throws OpenR66RunnerErrorException;

    /**
     * Called once the authentication is done
     * @param session
     * @throws OpenR66RunnerErrorException
     */
    public void checkAtAuthentication(R66Session session) throws OpenR66RunnerErrorException;

    /**
     * Called once the DbTaskRunner is created/loaded and before pre tasks
     * @param session
     * @throws OpenR66RunnerErrorException
     */
    public void checkAtStartup(R66Session session) throws OpenR66RunnerErrorException;

    /**
     * Called once the pre tasks are over in success
     * @param session
     * @throws OpenR66RunnerErrorException
     */
    public void checkAfterPreCommand(R66Session session) throws OpenR66RunnerErrorException;

    /**
     * Called once the transfer is done in success but before the post tasks
     * @param session
     * @throws OpenR66RunnerErrorException
     */
    public void checkAfterTransfer(R66Session session) throws OpenR66RunnerErrorException;

    /**
     * Called once the post tasks are over in success
     * @param session
     * @throws OpenR66RunnerErrorException
     */
    public void checkAfterPost(R66Session session) throws OpenR66RunnerErrorException;

    /**
     * Called once any error occurs
     * @param session
     * @throws OpenR66RunnerErrorException
     */
    public void checkAtError(R66Session session);

    /**
     * Called once after pre tasks, while filename could be changed (or not)
     * @param session
     * @throws OpenR66RunnerErrorException
     */
    public void checkAtChangeFilename(R66Session session) throws OpenR66RunnerErrorException;

    /**
     * Called when the session is to be closed in order to release resources
     * @param session
     */
    public void releaseResources(R66Session session);

    /**
     * Called to get the current extra "Information" from Business side to transmit, 
     * valid in extended protocol (> 2.4)
     * @param session
     * @return the current "session" info
     */
    public String getInfo(R66Session session);

    /**
     * Set optional info to be set within extra information of PacketValid as "Info" 
     * or R66Result as "Other", valid in extended protocol (> 2.4)
     * @param session
     * @param info
     */
    public void setInfo(R66Session session, String info);

}
