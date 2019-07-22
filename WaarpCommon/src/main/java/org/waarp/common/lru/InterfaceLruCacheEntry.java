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
 * You should have received a copy of the GNU General Public License along with Waarp. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.waarp.common.lru;

/**
 * Cache Entry interface
 * 
 * @author Frederic Bregier
 * @author Damian Momot
 * 
 */
public interface InterfaceLruCacheEntry<V> {
    /**
     * Returns value stored in entry or null if entry is not valid
     * 
     * @return Value
     */
    public V getValue();

    /**
     * 
     * @param timeRef
     * @return True if this entry is still valid
     */
    public boolean isStillValid(long timeRef);

    /**
     * Reset the time of overtime
     * 
     * @return True if this entry has its time reset
     */
    public boolean resetTime(long ttl);
}
