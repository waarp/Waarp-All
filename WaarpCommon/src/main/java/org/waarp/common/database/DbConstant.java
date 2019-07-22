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
package org.waarp.common.database;

/**
 * Constants value for database
 * 
 * @author Frederic Bregier
 * 
 */
public class DbConstant {
    /**
     * Illegal value as SpecialId of transfer (any value above is valid)
     */
    public static final long ILLEGALVALUE = Long.MIN_VALUE;
    /**
     * The current DbAdmin object
     */
    public static DbAdmin admin = null;
    /**
     * The no-commit DbAdmin object
     */
    public static DbAdmin noCommitAdmin = null;
    /**
     * How long to wait in second for a validation of connection (isValid(time))
     */
    public static int VALIDTESTDURATION = 2;
    /**
     * Number of Database max connection (if pooled)
     */
    public static int MAXCONNECTION = 5000;
    /**
     * Delay in second to try to connect
     */
    public static int DELAYMAXCONNECTION = 30;

}
