/*
 * This file is part of Waarp Project (named also Waarp or GG).
 *
 *  Copyright (c) 2019, Waarp SAS, and individual contributors by the @author
 *  tags. See the COPYRIGHT.txt in the distribution for a full listing of
 * individual contributors.
 *
 *  All Waarp Project is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 * Waarp . If not, see <http://www.gnu.org/licenses/>.
 */
package org.waarp.common.xml;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;

/**
 * XmlHash Hashtable for XmlValue utility. Hash all values (including subXml)
 * but only root for Multiple
 */
public class XmlHash {
  final Hashtable<String, XmlValue> hashtable;

  XmlHash() {
    hashtable = new Hashtable<String, XmlValue>();
  }

  public XmlHash(final XmlValue[] values) {
    hashtable = new Hashtable<String, XmlValue>();
    for (final XmlValue xmlValue : values) {
      if (xmlValue.isMultiple()) {
        hashtable.put(xmlValue.getName(), xmlValue);
      } else if (xmlValue.isSubXml()) {
        put(xmlValue);
      } else {
        hashtable.put(xmlValue.getName(), xmlValue);
      }
    }
  }

  public XmlHash(final XmlValue value) {
    hashtable = new Hashtable<String, XmlValue>();
    if (value == null) {
      return;
    }
    if (value.isMultiple()) {
      hashtable.put(value.getName(), value);
    } else if (value.isSubXml()) {
      put(value);
    } else {
      hashtable.put(value.getName(), value);
    }
  }

  public XmlValue get(final String name) {
    return hashtable.get(name);
  }

  public XmlValue put(final XmlValue value) {
    if (value.isMultiple()) {
      return hashtable.put(value.getName(), value);
    } else if (value.isSubXml()) {
      final XmlValue ret = hashtable.put(value.getName(), value);
      if (!value.isEmpty()) {
        for (final XmlValue subvalue : value.getSubXml()) {
          if (subvalue != null) {
            put(subvalue);
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

  public boolean contains(final XmlValue value) {
    return hashtable.contains(value);
  }

  public boolean containsValue(final XmlValue value) {
    return hashtable.containsValue(value);
  }

  public boolean containsKey(final String key) {
    return hashtable.containsKey(key);
  }

  public XmlValue remove(final String key) {
    return hashtable.remove(key);
  }

  public void clear() {
    hashtable.clear();
  }

  public Set<String> keySet() {
    return hashtable.keySet();
  }

}
