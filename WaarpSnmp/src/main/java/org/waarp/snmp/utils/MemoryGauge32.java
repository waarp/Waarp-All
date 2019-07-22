/**
 * This file is part of Waarp Project.
 * 
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author
 * tags. See the COPYRIGHT.txt in the distribution for a full listing of
 * individual contributors.
 * 
 * All Waarp Project is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Waarp. If not, see <http://www.gnu.org/licenses/>.
 */
package org.waarp.snmp.utils;

import org.waarp.snmp.interf.WaarpGauge32;

/**
 * Specific Value for Gauge32 for Memory usage
 * 
 * @author Frederic Bregier
 * 
 */
@SuppressWarnings("serial")
public class MemoryGauge32 extends WaarpGauge32 {
    /**
     * The different Type of Memory Gauge32 elements
     * 
     * @author Frederic Bregier
     *
     */
    public static enum MemoryType {
        TotalMemory, FreeMemory, UsedMemory
    }

    /**
     * Runtime for Memory
     */
    protected Runtime runtime = Runtime.getRuntime();
    /**
     * Type of MemoryType used
     */
    protected MemoryType type = null;

    protected void setInternalValue() {
        if (type == null)
            return;
        long mem;
        switch (type) {
            case TotalMemory:
                mem = runtime.totalMemory();
                setValue(mem >> 10);
                return;
            case FreeMemory:
                mem = runtime.freeMemory();
                setValue(mem >> 10);
                return;
            case UsedMemory:
                mem = runtime.totalMemory() - runtime.freeMemory();
                setValue(mem >> 10);
                return;
        }
    }

    protected void setInternalValue(long value) {
        // ignored
        setInternalValue();
    }

    /**
     * 
     * @param type
     *            the type of MemoryType used
     */
    public MemoryGauge32(MemoryType type) {
        this.type = type;
        setInternalValue();
    }
}
