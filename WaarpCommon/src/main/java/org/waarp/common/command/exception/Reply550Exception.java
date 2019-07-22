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
package org.waarp.common.command.exception;

import org.waarp.common.command.ReplyCode;

/**
 * 550 Requested action not taken. File unavailable (e.g., file not found, no access).
 * 
 * @author Frederic Bregier
 * 
 */
public class Reply550Exception extends CommandAbstractException {

    /**
     * serialVersionUID of long:
     */
    private static final long serialVersionUID = 550L;

    /**
     * 550 Requested action not taken. File unavailable (e.g., file not found, no access).
     * 
     * @param message
     */
    public Reply550Exception(String message) {
        super(ReplyCode.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, message);
    }

    /**
     * 550 Requested action not taken. File unavailable (e.g., file not found, no access).
     *
     * @param message
     * @param e
     */
    public Reply550Exception(String message, Throwable e) {
        super(ReplyCode.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, message, e);
    }

}
