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
package org.waarp.gateway.kernel.exec;

/**
 * Runnable interface for tasks
 * 
 * @author Frederic Bregier
 * 
 */
public interface GatewayRunnable extends Runnable {

    /**
     * The way the parameter will be set
     * 
     * @param waitForValidation
     *            True if the caller will wait up to delay time in ms
     * @param useLocalExec
     *            True if currently is configured to use LocalExec (may be ignored)
     * @param delay
     *            Delay in ms used only if waitForValidation is True
     * @param args
     *            First arg is the Class name used
     */
    public void setArgs(boolean waitForValidation,
            boolean useLocalExec, int delay, String[] args);

    /**
     * 
     * @return the final status where 0 is OK, 1 is Warning, 2 is Error
     */
    public int getFinalStatus();

    /**
     * 
     * @return Information on task
     */
    public String toString();
}
