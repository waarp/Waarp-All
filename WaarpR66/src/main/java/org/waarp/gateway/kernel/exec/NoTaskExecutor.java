/**
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation; either version 3.0 of the
 * License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this
 * software; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.waarp.gateway.kernel.exec;

import org.waarp.common.command.exception.Reply421Exception;
import org.waarp.common.future.WaarpFuture;

/**
 * NoTaskExecutor class. No action will be taken.
 * 
 * 
 * 
 * @author Frederic Bregier
 * 
 */
public class NoTaskExecutor extends AbstractExecutor {
    private final WaarpFuture futureCompletion;

    /**
     * 
     * @param command
     * @param delay
     * @param futureCompletion
     */
    public NoTaskExecutor(String command, long delay, WaarpFuture futureCompletion) {
        this.futureCompletion = futureCompletion;
    }

    public void run() throws Reply421Exception {
        futureCompletion.setSuccess();
    }
}
