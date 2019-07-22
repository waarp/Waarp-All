/**
   This file is part of Waarp Project.

   Copyright 2009, Frederic Bregier, and individual contributors by the @author
   tags. See the COPYRIGHT.txt in the distribution for a full listing of
   individual contributors.

   All Waarp Project is free software: you can redistribute it and/or 
   modify it under the terms of the GNU General Public License as published 
   by the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   Waarp is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Waarp .  If not, see <http://www.gnu.org/licenses/>.
 */
package org.waarp.common.lru;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Simple LRU Cache
 * 
 * @author "Frederic Bregier"
 *
 */
public class SimpleLRUCache<K, V> extends LinkedHashMap<K, V> {
    private static final long serialVersionUID = -3505777964745783339L;
    private int capacity; // Maximum number of items in the cache.

    public static <K, V> Map<K, V> create(int capacity) {
        return Collections.synchronizedMap(new SimpleLRUCache<K, V>(capacity));
    }

    public SimpleLRUCache(int capacity) {
        super(capacity + 1, 1.0f, true); // Pass 'true' for accessOrder.
        this.capacity = capacity;
    }

    protected boolean removeEldestEntry(Entry<K, V> entry) {
        return (size() > this.capacity);
    }
}
