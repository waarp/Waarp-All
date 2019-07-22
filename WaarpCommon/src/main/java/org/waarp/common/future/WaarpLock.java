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
package org.waarp.common.future;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ReentrantLock with a timeout in locking without exception and no exception on unlock if the thread is not locking it.
 * 
 * @author "Frederic Bregier"
 *
 */
@SuppressWarnings("serial")
public class WaarpLock extends ReentrantLock {
    /**
     * 
     */
    public WaarpLock() {
    }

    /**
     * @param fair
     */
    public WaarpLock(boolean fair) {
        super(fair);
    }

    /**
     * Try to lock within the given limit, but do not raized any exception if not locked.
     * A complete lock shall be done using other calls, like simple lock() method.
     * 
     * @param timeout
     * @param timeUnit
     */
    public void lock(long timeout, TimeUnit timeUnit) {
        try {
            tryLock(timeout, timeUnit);
        } catch (InterruptedException e) {
        }
    }

    @Override
    public void unlock() {
        try {
            super.unlock();
        } catch (IllegalMonitorStateException e) {
        }
    }

}
