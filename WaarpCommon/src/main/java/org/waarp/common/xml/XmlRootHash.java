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
package org.waarp.common.xml;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;

/**
 * XmlHash Hashtable for XmlValue utility. Hash all values (except subXml and root for
 * Multiple)
 * 
 * @author Frederic Bregier
 * 
 */
public class XmlRootHash {
    private final Hashtable<String, XmlValue> hashtable;

    public XmlRootHash(XmlValue[] values) {
        hashtable = new Hashtable<String, XmlValue>();
        for (XmlValue xmlValue : values) {
            if (xmlValue.isMultiple()) {
                hashtable.put(xmlValue.getName(), xmlValue);
            } else if (xmlValue.isSubXml()) {
                hashtable.put(xmlValue.getName(), xmlValue);
            } else {
                hashtable.put(xmlValue.getName(), xmlValue);
            }
        }
    }

    public XmlValue get(String name) {
        return hashtable.get(name);
    }

    public XmlValue put(XmlValue value) {
        if (value.isMultiple()) {
            return hashtable.put(value.getName(), value);
        } else if (value.isSubXml()) {
            XmlValue ret = hashtable.put(value.getName(), value);
            if (!value.isEmpty()) {
                for (XmlValue subvalue : value.getSubXml()) {
                    if (subvalue != null) {
                        this.put(subvalue);
                    }
                }
            }
            return ret;
        } else {
            return hashtable.put(value.getName(), value);
        }
    }

    public int size() {
        return hashtable.size();
    }

    public boolean isEmpty() {
        return hashtable.isEmpty();
    }

    public Enumeration<String> keys() {
        return hashtable.keys();
    }

    public Enumeration<XmlValue> elements() {
        return hashtable.elements();
    }

    public boolean contains(XmlValue value) {
        return hashtable.contains(value);
    }

    public boolean containsValue(XmlValue value) {
        return hashtable.containsValue(value);
    }

    public boolean containsKey(String key) {
        return hashtable.containsKey(key);
    }

    public XmlValue remove(String key) {
        return hashtable.remove(key);
    }

    public void clear() {
        hashtable.clear();
    }

    public Set<String> keySet() {
        return hashtable.keySet();
    }

}
