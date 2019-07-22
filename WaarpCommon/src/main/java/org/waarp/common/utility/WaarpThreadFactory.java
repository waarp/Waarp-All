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
package org.waarp.common.utility;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread Factory that build named threads, by default daemon threads.
 * 
 * @author Frederic Bregier
 * 
 */
public class WaarpThreadFactory implements ThreadFactory {
    private String GlobalName;
    private AtomicLong counter = new AtomicLong();
    private boolean isDaemon = true;

    /**
     * Default is Daemon thread
     * 
     * @param globalName
     */
    public WaarpThreadFactory(String globalName) {
        GlobalName = globalName + "-";
    }

    /**
     * 
     * @param globalName
     * @param isDaemon
     *            (Default is True, meaning system can exit if only Daemon threads left)
     */
    public WaarpThreadFactory(String globalName, boolean isDaemon) {
        GlobalName = globalName + "-";
        this.isDaemon = isDaemon;
    }

    public Thread newThread(Runnable arg0) {
        Thread thread = new Thread(arg0, GlobalName + counter.incrementAndGet());
        thread.setDaemon(isDaemon);
        return thread;
    }
}
