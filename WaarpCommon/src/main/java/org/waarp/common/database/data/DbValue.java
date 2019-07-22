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
package org.waarp.common.database.data;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Date;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.utility.WaarpStringUtils;

/**
 * Database Value to help getting and setting value from and to database
 *
 * @author Frederic Bregier
 *
 */
public class DbValue {
    /**
     * Real value
     */
    private Object value;
    /**
     * Data Type
     */
    int type;
    /**
     * Column name
     */
    private String column;

    public DbValue(String value) {
        this.setValue(value);
        type = Types.VARCHAR;
    }

    public DbValue(String value, boolean LONG) {
        this.setValue(value);
        type = Types.LONGVARCHAR;
    }

    public DbValue(boolean value) {
        this.setValue(value);
        type = Types.BIT;
    }

    public DbValue(byte value) {
        this.setValue(value);
        type = Types.TINYINT;
    }

    public DbValue(short value) {
        this.setValue(value);
        type = Types.SMALLINT;
    }

    public DbValue(int value) {
        this.setValue(value);
        type = Types.INTEGER;
    }

    public DbValue(long value) {
        this.setValue(value);
        type = Types.BIGINT;
    }

    public DbValue(float value) {
        this.setValue(value);
        type = Types.REAL;
    }

    public DbValue(double value) {
        this.setValue(value);
        type = Types.DOUBLE;
    }

    public DbValue(byte[] value) {
        this.setValue(value);
        type = Types.VARBINARY;
    }

    public DbValue(Date value) {
        this.setValue(value);
        type = Types.DATE;
    }

    public DbValue(Timestamp value) {
        this.setValue(value);
        type = Types.TIMESTAMP;
    }

    public DbValue(java.util.Date value) {
        this.setValue(new Timestamp(value.getTime()));
        type = Types.TIMESTAMP;
    }

    public DbValue(String value, String name) {
        this.setValue(value);
        type = Types.VARCHAR;
        setColumn(name);
    }

    public DbValue(String value, String name, boolean LONG) {
        this.setValue(value);
        type = Types.LONGVARCHAR;
        setColumn(name);
    }

    public DbValue(boolean value, String name) {
        this.setValue(value);
        type = Types.BIT;
        setColumn(name);
    }

    public DbValue(byte value, String name) {
        this.setValue(value);
        type = Types.TINYINT;
        setColumn(name);
    }

    public DbValue(short value, String name) {
        this.setValue(value);
        type = Types.SMALLINT;
        setColumn(name);
    }

    public DbValue(int value, String name) {
        this.setValue(value);
        type = Types.INTEGER;
        setColumn(name);
    }

    public DbValue(long value, String name) {
        this.setValue(value);
        type = Types.BIGINT;
        setColumn(name);
    }

    public DbValue(float value, String name) {
        this.setValue(value);
        type = Types.REAL;
        setColumn(name);
    }

    public DbValue(double value, String name) {
        this.setValue(value);
        type = Types.DOUBLE;
        setColumn(name);
    }

    public DbValue(byte[] value, String name) {
        this.setValue(value);
        type = Types.VARBINARY;
        setColumn(name);
    }

    public DbValue(Date value, String name) {
        this.setValue(value);
        type = Types.DATE;
        setColumn(name);
    }

    public DbValue(Timestamp value, String name) {
        this.setValue(value);
        type = Types.TIMESTAMP;
        setColumn(name);
    }

    public DbValue(java.util.Date value, String name) {
        this.setValue(new Timestamp(value.getTime()));
        type = Types.TIMESTAMP;
        setColumn(name);
    }

    public DbValue(Reader value, String name) {
        this.setValue(value);
        type = Types.CLOB;
        setColumn(name);
    }

    public DbValue(InputStream value, String name) {
        this.setValue(value);
        type = Types.BLOB;
        setColumn(name);
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    public void setValue(byte value) {
        this.value = value;
    }

    public void setValue(short value) {
        this.value = value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    public void setValue(Date value) {
        this.value = value;
    }

    public void setValue(Timestamp value) {
        this.value = value;
    }

    public void setValue(java.util.Date value) {
        this.value = new Timestamp(value.getTime());
    }

    public void setValue(Reader value) {
        this.value = value;
    }

    public void setValue(InputStream value) {
        this.value = value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    /**
     *
     * @return the type in full string format
     */
    public String getType() {
        switch (type) {
            case Types.VARCHAR:
                return "VARCHAR";
            case Types.LONGVARCHAR:
                return "LONGVARCHAR";
            case Types.BIT:
                return "BIT";
            case Types.TINYINT:
                return "TINYINT";
            case Types.SMALLINT:
                return "SMALLINT";
            case Types.INTEGER:
                return "INTEGER";
            case Types.BIGINT:
                return "BIGINT";
            case Types.REAL:
                return "REAL";
            case Types.DOUBLE:
                return "DOUBLE";
            case Types.VARBINARY:
                return "VARBINARY";
            case Types.DATE:
                return "DATE";
            case Types.TIMESTAMP:
                return "TIMESTAMP";
            case Types.CLOB:
                return "CLOB";
            case Types.BLOB:
                return "BLOB";
            default:
                return "UNKNOWN:" + type;
        }
    }

    public Object getValue() throws IllegalAccessError {
        switch (type) {
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.BIT:
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
            case Types.BIGINT:
            case Types.REAL:
            case Types.DOUBLE:
            case Types.VARBINARY:
            case Types.DATE:
            case Types.TIMESTAMP:
            case Types.CLOB:
            case Types.BLOB:
                return value;
            default:
                throw new IllegalAccessError("Type unknown: " + type);
        }
    }

    public String getValueAsString() throws WaarpDatabaseSqlException {
        switch (type) {
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
                return (String) getValue();
            case Types.BIT:
                return ((Boolean) getValue()).toString();
            case Types.TINYINT:
                return ((Byte) getValue()).toString();
            case Types.SMALLINT:
                return ((Short) getValue()).toString();
            case Types.INTEGER:
                return ((Integer) getValue()).toString();
            case Types.BIGINT:
                return ((Long) getValue()).toString();
            case Types.REAL:
                return ((Float) getValue()).toString();
            case Types.DOUBLE:
                return ((Double) getValue()).toString();
            case Types.VARBINARY:
                return new String((byte[]) getValue(), WaarpStringUtils.UTF8);
            case Types.DATE:
                return ((Date) getValue()).toString();
            case Types.TIMESTAMP:
                return ((Timestamp) getValue()).toString();
            case Types.CLOB: {
                StringBuilder sBuilder = new StringBuilder();
                Reader reader = ((Reader) getValue());
                char[] cbuf = new char[4096];
                int len;
                try {
                    len = reader.read(cbuf);
                    while (len > 0) {
                        sBuilder.append(cbuf, 0, len);
                        len = reader.read(cbuf);
                    }
                } catch (IOException e) {
                    throw new WaarpDatabaseSqlException("Error while reading Clob as String", e);
                }
                return sBuilder.toString();
            }
            case Types.BLOB: {
                StringBuilder sBuilder = new StringBuilder();
                Reader reader = ((Reader) getValue());
                char[] cbuf = new char[4096];
                int len;
                try {
                    len = reader.read(cbuf);
                    while (len > 0) {
                        sBuilder.append(cbuf, 0, len);
                        len = reader.read(cbuf);
                    }
                } catch (IOException e) {
                    throw new WaarpDatabaseSqlException("Error while reading Clob as String", e);
                }
                return sBuilder.toString();
            }
            default:
                throw new WaarpDatabaseSqlException("Type unknown: " + type);
        }
    }

    public void setValueFromString(String svalue) throws WaarpDatabaseSqlException {
        switch (type) {
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
                setValue(svalue);
                break;
            case Types.BIT:
                setValue(Boolean.parseBoolean(svalue));
                break;
            case Types.TINYINT:
                setValue(Byte.parseByte(svalue));
                break;
            case Types.SMALLINT:
                setValue(Short.parseShort(svalue));
                break;
            case Types.INTEGER:
                setValue(Integer.parseInt(svalue));
                break;
            case Types.BIGINT:
                setValue(Long.parseLong(svalue));
                break;
            case Types.REAL:
                setValue(Float.parseFloat(svalue));
                break;
            case Types.DOUBLE:
                setValue(Double.parseDouble(svalue));
                break;
            case Types.VARBINARY:
                setValue(svalue.getBytes(WaarpStringUtils.UTF8));
                break;
            case Types.DATE:
                try {
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                    setValue(format.parse(svalue));
                } catch (ParseException e) {
                    try {
                        setValue(DateFormat.getDateTimeInstance().parse(svalue));
                    } catch (ParseException e1) {
                        throw new WaarpDatabaseSqlException("Error in Date: " + svalue, e);
                    }
                }
                break;
            case Types.TIMESTAMP:
                try {
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                    setValue(new Timestamp(format.parse(svalue).getTime()));
                } catch (ParseException e) {
                    try {
                        setValue(new Timestamp(DateFormat.getDateTimeInstance().parse(svalue).getTime()));
                    } catch (ParseException e1) {
                        throw new WaarpDatabaseSqlException("Error in Timestamp: " + svalue, e);
                    }
                }
                break;
            case Types.CLOB:
                try {
                    setValue(new InputStreamReader(new FileInputStream(svalue), WaarpStringUtils.UTF8));
                } catch (FileNotFoundException e) {
                    throw new WaarpDatabaseSqlException("Error in CLOB: " + svalue, e);
                }
                break;
            case Types.BLOB:
                try {
                    setValue(new FileInputStream(svalue));
                } catch (FileNotFoundException e) {
                    throw new WaarpDatabaseSqlException("Error in BLOB: " + svalue, e);
                }
                break;
            default:
                throw new WaarpDatabaseSqlException("Type unknown: " + type);
        }
    }

    /**
     * @return the column
     */
    public String getColumn() {
        return column;
    }

    /**
     * @param column the column to set
     */
    private void setColumn(String column) {
        this.column = column;
    }
}
