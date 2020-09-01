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

package org.waarp.xample;

/*
 * Copyright (c) 2002 Felix Golubov
 */

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A helper class for the XAmple application which holds pathes of accessed XSD
 * and XML files.
 *
 * @author Felix Golubov
 * @version 1.0
 */

public class History {
  public static final int MAX_ITEM_COUNT = 20;
  public static final int MAX_LABEL_LENGTH = 70;

  ArrayList<History> items;
  final String path;
  final String label;

  public History(final String path) {
    this.path = path.replace('\\', '/');
    label = abbreviatePath(this.path, MAX_LABEL_LENGTH);
  }

  public static String abbreviatePath(final String path,
                                      final int limitLength) {
    if (path.length() <= limitLength) {
      return path;
    }
    int k = path.indexOf('/', 1);
    final String substring =
        "..." + path.substring(path.length() - limitLength + 3);
    if (k < 0) {
      return substring;
    }
    k = path.indexOf('/', k + 1);
    if (k < 0) {
      return substring;
    }
    final String prefix = path.substring(0, k + 1);
    String suffix = path.substring(k + 1);
    k = suffix.indexOf('/', 1);
    if (k < 0) {
      return substring;
    }
    final int limit = MAX_LABEL_LENGTH - prefix.length() - 3;
    while (k >= 0 && suffix.length() > limit) {
      suffix = suffix.substring(k);
      k = suffix.indexOf('/', 1);
    }
    if (suffix.length() <= limit) {
      return prefix + "..." + suffix;
    } else {
      return substring;
    }
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof History)) {
      return false;
    }
    final History history = (History) obj;
    if (path == null) {
      return history.path == null;
    }
    return path.equals(history.path);
  }

  @Override
  public String toString() {
    return label;
  }

  public History put(final String childPath) {
    if (items == null) {
      items = new ArrayList<History>();
    }
    History child = new History(childPath);
    final int index = items.indexOf(child);
    if (index >= 0) {
      child = items.remove(index);
    }
    items.add(0, child);
    if (items.size() > MAX_ITEM_COUNT) {
      items.remove(items.size() - 1);
    }
    return child;
  }

  public History getFirstChild() {
    if (items == null && items.isEmpty()) {
      return null;
    }
    return items.get(0);
  }

  public void remove(final String childPath) {
    if (items == null) {
      items = new ArrayList<History>();
    }
    final History child = new History(childPath);
    final int index = items.indexOf(child);
    if (index >= 0) {
      items.remove(index);
    }
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(new Object[] { items, path, label });
  }
}
