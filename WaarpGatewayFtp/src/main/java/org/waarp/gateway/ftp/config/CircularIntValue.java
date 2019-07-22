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
package org.waarp.gateway.ftp.config;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Circular Value used by passive connections to find the next valid port to propose to the client.
 * 
 * @author Frederic Bregier
 * 
 */
class CircularIntValue {
    /**
     * Min value
     */
    private final int min;

    /**
     * Max value
     */
    private final int max;

    /**
     * Current Value
     */
    private AtomicInteger current;

    /**
     * Create a circular range of values
     * 
     * @param min
     * @param max
     */
    public CircularIntValue(int min, int max) {
        this.min = min;
        this.max = max;
        current = new AtomicInteger(this.min - 1);
    }

    /**
     * Get the next value
     * 
     * @return the next value
     */
    public int getNext() {
        synchronized (current) {
            if (!current.compareAndSet(max, min)) {
                current.incrementAndGet();
            }
            return current.get();
        }
    }
}
