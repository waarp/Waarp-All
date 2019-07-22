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
package org.waarp.common.filemonitor;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

/**
 * Example of FileFilter using regex on filename and possible Size
 * 
 * @author "Frederic Bregier"
 *
 */
public class RegexFileFilter implements FileFilter {
    public static final String REGEX_XML_EXTENSION = ".*\\.[xX][mM][lL]$";

    protected Pattern pattern;
    protected long minimalSize = 0;

    public RegexFileFilter(String regex) {
        pattern = Pattern.compile(regex);
    }

    public RegexFileFilter(String regex, long minimalSize) {
        pattern = Pattern.compile(regex);
        this.minimalSize = minimalSize;
    }

    public RegexFileFilter(long minimalSize) {
        pattern = null;
        this.minimalSize = minimalSize;
    }

    @Override
    public boolean accept(File pathname) {
        if (pathname.isFile()) {
            if (pattern != null) {
                return pattern.matcher(pathname.getPath()).matches()
                        && (minimalSize == 0 || pathname.length() >= minimalSize);
            }
            return minimalSize == 0 || pathname.length() >= minimalSize;
        }
        return false;
    }

}
