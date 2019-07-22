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
 * Abstract class for exception in commands
 * 
 * @author Frederic Bregier
 * 
 */
@SuppressWarnings("serial")
public abstract class CommandAbstractException extends Exception {
    /**
     * Associated code
     */
    public ReplyCode code = null;

    /**
     * Associated Message if any
     */
    public String message = null;

    /**
     * Simplest constructor
     * 
     * @param code
     * @param message
     */
    public CommandAbstractException(ReplyCode code, String message) {
        super(code.getMesg());
        this.code = code;
        this.message = message;
    }

    /**
     * Constructor with Throwable
     *
     * @param code
     * @param message
     * @param e
     */
    public CommandAbstractException(ReplyCode code, String message, Throwable e) {
        super(code.getMesg(), e);
        this.code = code;
        this.message = message;
    }

    /**
	 *
	 */
    @Override
    public String toString() {
        return "Code: " + code.name() + " Mesg: " +
                (message != null ? message : "no specific message");
    }

    @Override
    public String getMessage() {
        return toString();
    }
}
