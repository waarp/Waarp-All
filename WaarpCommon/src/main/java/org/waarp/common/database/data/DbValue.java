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
package org.waarp.common.database.data;

import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.utility.WaarpStringUtils;

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
import java.util.TimeZone;

/**
 * Database Value to help getting and setting value from and to database
 */
public class DbValue {
  private static final String TYPE_UNKNOWN = "Type unknown: ";
  /**
   * Real value
   */
  private Object value;
  /**
   * Data Type
   */
  final int type;
  /**
   * Column name
   */
  private String column;

  public DbValue(final String value) {
    setValue(value);
    type = Types.VARCHAR;
  }

  public DbValue(final String value, final boolean LONG) {
    setValue(value);
    type = Types.LONGVARCHAR;
  }

  public DbValue(final boolean value) {
    setValue(value);
    type = Types.BIT;
  }

  public DbValue(final byte value) {
    setValue(value);
    type = Types.TINYINT;
  }

  public DbValue(final short value) {
    setValue(value);
    type = Types.SMALLINT;
  }

  public DbValue(final int value) {
    setValue(value);
    type = Types.INTEGER;
  }

  public DbValue(final long value) {
    setValue(value);
    type = Types.BIGINT;
  }

  public DbValue(final float value) {
    setValue(value);
    type = Types.REAL;
  }

  public DbValue(final double value) {
    setValue(value);
    type = Types.DOUBLE;
  }

  public DbValue(final byte[] value) {
    setValue(value);
    type = Types.VARBINARY;
  }

  public DbValue(final Date value) {
    setValue(value);
    type = Types.DATE;
  }

  public DbValue(final Timestamp value) {
    setValue(value);
    type = Types.TIMESTAMP;
  }

  public DbValue(final java.util.Date value) {
    setValue(new Timestamp(value.getTime()));
    type = Types.TIMESTAMP;
  }

  public DbValue(final String value, final String name) {
    setValue(value);
    type = Types.VARCHAR;
    setColumn(name);
  }

  public DbValue(final String value, final String name,
                 final boolean trueLongOrFalseKey) {
    setValue(value);
    if (trueLongOrFalseKey) {
      type = Types.LONGVARCHAR;
    } else {
      type = Types.NVARCHAR;
    }
    setColumn(name);
  }

  public DbValue(final boolean value, final String name) {
    setValue(value);
    type = Types.BIT;
    setColumn(name);
  }

  public DbValue(final byte value, final String name) {
    setValue(value);
    type = Types.TINYINT;
    setColumn(name);
  }

  public DbValue(final short value, final String name) {
    setValue(value);
    type = Types.SMALLINT;
    setColumn(name);
  }

  public DbValue(final int value, final String name) {
    setValue(value);
    type = Types.INTEGER;
    setColumn(name);
  }

  public DbValue(final long value, final String name) {
    setValue(value);
    type = Types.BIGINT;
    setColumn(name);
  }

  public DbValue(final float value, final String name) {
    setValue(value);
    type = Types.REAL;
    setColumn(name);
  }

  public DbValue(final double value, final String name) {
    setValue(value);
    type = Types.DOUBLE;
    setColumn(name);
  }

  public DbValue(final byte[] value, final String name) {
    setValue(value);
    type = Types.VARBINARY;
    setColumn(name);
  }

  public DbValue(final Date value, final String name) {
    setValue(value);
    type = Types.DATE;
    setColumn(name);
  }

  public DbValue(final Timestamp value, final String name) {
    setValue(value);
    type = Types.TIMESTAMP;
    setColumn(name);
  }

  public DbValue(final java.util.Date value, final String name) {
    setValue(new Timestamp(value.getTime()));
    type = Types.TIMESTAMP;
    setColumn(name);
  }

  public DbValue(final Reader value, final String name) {
    setValue(value);
    type = Types.CLOB;
    setColumn(name);
  }

  public DbValue(final InputStream value, final String name) {
    setValue(value);
    type = Types.BLOB;
    setColumn(name);
  }

  public void setValue(final String value) {
    this.value = value;
  }

  public void setValue(final boolean value) {
    this.value = value;
  }

  public void setValue(final byte value) {
    this.value = value;
  }

  public void setValue(final short value) {
    this.value = value;
  }

  public void setValue(final int value) {
    this.value = value;
  }

  public void setValue(final long value) {
    this.value = value;
  }

  public void setValue(final float value) {
    this.value = value;
  }

  public void setValue(final double value) {
    this.value = value;
  }

  public void setValue(final byte[] value) {
    this.value = value;
  }

  public void setValue(final Date value) {
    this.value = value;
  }

  public void setValue(final Timestamp value) {
    this.value = value;
  }

  public void setValue(final java.util.Date value) {
    this.value = new Timestamp(value.getTime());
  }

  public void setValue(final Reader value) {
    this.value = value;
  }

  public void setValue(final InputStream value) {
    this.value = value;
  }

  public void setValue(final Object value) {
    this.value = value;
  }

  /**
   * @return the type in full string format
   */
  public String getType() {
    switch (type) {
      case Types.VARCHAR:
        return "VARCHAR";
      case Types.NVARCHAR:
        return "NVARCHAR";
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
      case Types.NVARCHAR:
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
        throw new IllegalAccessError(TYPE_UNKNOWN + type);
    }
  }

  public String getValueAsString() throws WaarpDatabaseSqlException {
    switch (type) {
      case Types.VARCHAR:
      case Types.NVARCHAR:
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
      case Types.DOUBLE:
      case Types.DATE:
      case Types.TIMESTAMP:
        return getValue().toString();
      case Types.VARBINARY:
        return new String((byte[]) getValue(), WaarpStringUtils.UTF8);
      case Types.CLOB:
        return getClob();
      case Types.BLOB:
        return getBlob();
      default:
        throw new WaarpDatabaseSqlException(TYPE_UNKNOWN + type);
    }
  }

  private String getBlob() throws WaarpDatabaseSqlException {
    final StringBuilder sBuilder = new StringBuilder();
    final InputStream inputStream = (InputStream) getValue();
    final byte[] cbuf = new byte[4096];
    int len;
    try {
      len = inputStream.read(cbuf);
      while (len > 0) {
        sBuilder.append(new String(cbuf, 0, len));
        len = inputStream.read(cbuf);
      }
    } catch (final IOException e) {
      throw new WaarpDatabaseSqlException("Error while reading Blob as String",
                                          e);
    }
    return sBuilder.toString();
  }

  private String getClob() throws WaarpDatabaseSqlException {
    final StringBuilder sBuilder = new StringBuilder();
    final Reader reader = (Reader) getValue();
    final char[] cbuf = new char[4096];
    int len;
    try {
      len = reader.read(cbuf);
      while (len > 0) {
        sBuilder.append(cbuf, 0, len);
        len = reader.read(cbuf);
      }
    } catch (final IOException e) {
      throw new WaarpDatabaseSqlException("Error while reading Clob as String",
                                          e);
    }
    return sBuilder.toString();
  }

  public void setValueFromString(final String svalue)
      throws WaarpDatabaseSqlException {
    switch (type) {
      case Types.VARCHAR:
      case Types.NVARCHAR:
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
          final DateFormat format = WaarpStringUtils.getDateFormat();
          setValue(format.parse(svalue));
        } catch (final ParseException e) {
          try {
            final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            setValue(format.parse(svalue));
          } catch (final ParseException e1) {
            try {
              setValue(DateFormat.getDateTimeInstance().parse(svalue));
            } catch (final ParseException e2) {
              throw new WaarpDatabaseSqlException("Error in Date: " + svalue,
                                                  e);
            }
          }
        }
        break;
      case Types.TIMESTAMP:
        try {
          final DateFormat format =
              new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
          format.setTimeZone(TimeZone.getTimeZone("GMT"));
          setValue(new Timestamp(format.parse(svalue).getTime()));
        } catch (final ParseException e) {
          try {
            final SimpleDateFormat format =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            setValue(new Timestamp(format.parse(svalue).getTime()));
          } catch (final ParseException e1) {
            try {
              setValue(new Timestamp(
                  DateFormat.getDateTimeInstance().parse(svalue).getTime()));
            } catch (final ParseException e2) {
              throw new WaarpDatabaseSqlException(
                  "Error in Timestamp: " + svalue, e);
            }
          }
        }
        break;
      case Types.CLOB:
        try {
          setValue(new InputStreamReader(new FileInputStream(svalue),
                                         WaarpStringUtils.UTF8));
        } catch (final FileNotFoundException e) {
          throw new WaarpDatabaseSqlException("Error in CLOB: " + svalue, e);
        }
        break;
      case Types.BLOB:
        try {
          setValue(new FileInputStream(svalue));
        } catch (final FileNotFoundException e) {
          throw new WaarpDatabaseSqlException("Error in BLOB: " + svalue, e);
        }
        break;
      default:
        throw new WaarpDatabaseSqlException(TYPE_UNKNOWN + type);
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
  private void setColumn(final String column) {
    this.column = column;
  }
}
