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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Static Shared values
 * 
 * @author Frederic Bregier
 * 
 */
class XmlStaticShared {

    protected static final TimeZone z = TimeZone.getTimeZone("GMT");

    /** parser for Date */
    protected static final DateFormat timeFormat = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss z");

    /** parser for SQL Date */
    protected static final DateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd z");

    /** parser for Timestamp */
    protected static final DateFormat timestampFormat = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss");
    static {
        timeFormat.setTimeZone(z);
        dateFormat.setTimeZone(z);
        timestampFormat.setTimeZone(z);
    }

}
