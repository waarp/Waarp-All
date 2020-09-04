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
package org.waarp.common.filemonitor;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

/**
 * Example of FileFilter using regex on filename and possible Size
 */
public class RegexFileFilter implements FileFilter {
  public static final String REGEX_XML_EXTENSION = ".*\\.[xX][mM][lL]$";

  protected final Pattern pattern;
  protected long minimalSize;

  public RegexFileFilter(final String regex) {
    pattern = Pattern.compile(regex);
  }

  public RegexFileFilter(final String regex, final long minimalSize) {
    pattern = Pattern.compile(regex);
    this.minimalSize = minimalSize;
  }

  public RegexFileFilter(final long minimalSize) {
    pattern = null;
    this.minimalSize = minimalSize;
  }

  @Override
  public boolean accept(final File pathname) {
    if (pathname.isFile()) {
      if (pattern != null) {
        return pattern.matcher(pathname.getPath()).find() &&
               (minimalSize == 0 || pathname.length() >= minimalSize);
      }
      return minimalSize == 0 || pathname.length() >= minimalSize;
    }
    return false;
  }

}
