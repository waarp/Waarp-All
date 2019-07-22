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
package org.waarp.common.database.model;

/**
 * Type of Database supported
 * 
 * @author Frederic Bregier
 * 
 */
public enum DbType {
    Oracle, MySQL, PostGreSQL, H2, MariaDB, none;

    public static DbType getFromDriver(String driver) {
        if (driver.contains("oracle")) {
            return DbType.Oracle;
        } else if (driver.contains("mysql")) {
            return DbType.MySQL;
        } else if (driver.contains("postgresql")) {
            return DbType.PostGreSQL;
        } else if (driver.contains("h2")) {
            return DbType.H2;
        } else if (driver.contains("mariadb")) {
            return DbType.MariaDB;
        }
        return none;
    }
}
