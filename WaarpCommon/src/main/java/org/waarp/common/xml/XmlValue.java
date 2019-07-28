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

import org.waarp.common.exception.InvalidArgumentException;

import java.io.InvalidObjectException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * XmlValue base element
 */
public class XmlValue {

  private static final String CAN_NOT_CONVERT_VALUE = "Can not convert value ";

  private static final String TO_TYPE = " to type ";

  private static final String CAN_NOT_CONVERT_VALUE_FROM =
      "Can not convert value from ";

  private final XmlDecl decl;

  private Object value;

  private List<?> values;

  private XmlValue[] subXml;

  public XmlValue(XmlDecl decl) {
    this.decl = decl;
    if (this.decl.isSubXml()) {
      if (this.decl.isMultiple()) {
        value = null;
        values = new ArrayList<XmlValue>();
        subXml = null;
        return;
      }
      final int len = this.decl.getSubXmlSize();
      final XmlDecl[] newDecls = this.decl.getSubXml();
      subXml = new XmlValue[len];
      for (int i = 0; i < len; i++) {
        subXml[i] = new XmlValue(newDecls[i]);
      }
      value = null;
      values = null;
      return;
    }
    if (this.decl.isMultiple()) {
      value = null;
      switch (getType()) {
        case BOOLEAN:
        case STRING:
        case TIMESTAMP:
        case SQLDATE:
        case SHORT:
        case DOUBLE:
        case LONG:
        case BYTE:
        case CHARACTER:
        case FLOAT:
        case INTEGER:
          values = new ArrayList<Boolean>();
          break;
        case XVAL:
        case EMPTY:
        default:
          break;
      }
    }
  }

  @SuppressWarnings("unchecked")
  public XmlValue(XmlValue from) {
    this(from.decl);
    if (decl.isSubXml()) {
      if (decl.isMultiple()) {
        final List<XmlValue[]> subvalues = (List<XmlValue[]>) from.values;
        for (final XmlValue[] xmlValues : subvalues) {
          final XmlValue[] newValues = new XmlValue[xmlValues.length];
          for (int i = 0; i < xmlValues.length; i++) {
            newValues[i] = new XmlValue(xmlValues[i]);
          }
          ((Collection<XmlValue[]>) values).add(newValues);
        }
      } else {
        for (int i = 0; i < from.subXml.length; i++) {
          subXml[i] = new XmlValue(from.subXml[i]);
        }
      }
    } else if (decl.isMultiple()) {
      final List<Object> subvalues = (List<Object>) from.values;
      for (final Object object : subvalues) {
        try {
          addValue(getCloneValue(getType(), object));
        } catch (final InvalidObjectException e) {
          // nothing
        }
      }
    } else {
      try {
        setValue(from.getCloneValue());
      } catch (final InvalidObjectException e) {
        // Nothing
      }
    }
  }

  /**
   * @return the decl
   */
  public XmlDecl getDecl() {
    return decl;
  }

  /**
   * Get Java field name
   *
   * @return the field name
   */
  public String getName() {
    return decl.getName();
  }

  /**
   * @return the type
   */
  public Class<?> getClassType() {
    return decl.getClassType();
  }

  /**
   * @return the type
   */
  public XmlType getType() {
    return decl.getType();
  }

  /**
   * @return the xmlPath
   */
  public String getXmlPath() {
    return decl.getXmlPath();
  }

  /**
   * @return True if this Value is a subXml
   */
  public boolean isSubXml() {
    return decl.isSubXml();
  }

  /**
   * @return the associated SubXML with the XmlValue (might be null if
   *     singleton
   *     or Multiple)
   */
  public XmlValue[] getSubXml() {
    return subXml;
  }

  /**
   * @return True if the Value are list of values
   */
  public boolean isMultiple() {
    return decl.isMultiple();
  }

  /**
   * @return the associated list with the XmlValues (might be null if
   *     singleton
   *     or SubXml)
   */
  public List<?> getList() {
    return values;
  }

  /**
   * Add a value into the Multiple values from the String (not compatible with
   * subXml)
   *
   * @param valueOrig
   *
   * @throws InvalidObjectException
   * @throws InvalidArgumentException
   */
  @SuppressWarnings("unchecked")
  public void addFromString(String valueOrig)
      throws InvalidObjectException, InvalidArgumentException {
    final String valueNew = XmlUtil.getExtraTrimed(valueOrig);
    switch (getType()) {
      case BOOLEAN:
        ((Collection<Boolean>) values)
            .add((Boolean) convert(getClassType(), valueNew));
        break;
      case INTEGER:
        ((Collection<Integer>) values)
            .add((Integer) convert(getClassType(), valueNew));
        break;
      case FLOAT:
        ((Collection<Float>) values)
            .add((Float) convert(getClassType(), valueNew));
        break;
      case CHARACTER:
        ((Collection<Character>) values)
            .add((Character) convert(getClassType(), valueNew));
        break;
      case BYTE:
        ((Collection<Byte>) values)
            .add((Byte) convert(getClassType(), valueNew));
        break;
      case LONG:
        ((Collection<Long>) values)
            .add((Long) convert(getClassType(), valueNew));
        break;
      case DOUBLE:
        ((Collection<Double>) values)
            .add((Double) convert(getClassType(), valueNew));
        break;
      case SHORT:
        ((Collection<Short>) values)
            .add((Short) convert(getClassType(), valueNew));
        break;
      case SQLDATE:
        ((Collection<Date>) values)
            .add((Date) convert(getClassType(), valueNew));
        break;
      case TIMESTAMP:
        ((Collection<Timestamp>) values)
            .add((Timestamp) convert(getClassType(), valueNew));
        break;
      case STRING:
        ((Collection<String>) values)
            .add((String) convert(getClassType(), valueNew));
        break;
      case XVAL:
        throw new InvalidObjectException(
            "XVAL cannot be assigned from String directly");
        // ((List<XmlValue>) this.values).add((XmlValue) value)
      case EMPTY:
        throw new InvalidObjectException("EMPTY cannot be assigned");
    }
  }

  /**
   * Add a value into the Multiple values from the Object
   *
   * @param value
   *
   * @throws InvalidObjectException
   */
  @SuppressWarnings("unchecked")
  public void addValue(Object value) throws InvalidObjectException {
    if (getType().isNativelyCompatible(value)) {
      switch (getType()) {
        case BOOLEAN:
          ((Collection<Boolean>) values).add((Boolean) value);
          break;
        case INTEGER:
          ((Collection<Integer>) values).add((Integer) value);
          break;
        case FLOAT:
          ((Collection<Float>) values).add((Float) value);
          break;
        case CHARACTER:
          ((Collection<Character>) values).add((Character) value);
          break;
        case BYTE:
          ((Collection<Byte>) values).add((Byte) value);
          break;
        case LONG:
          ((Collection<Long>) values).add((Long) value);
          break;
        case DOUBLE:
          ((Collection<Double>) values).add((Double) value);
          break;
        case SHORT:
          ((Collection<Short>) values).add((Short) value);
          break;
        case SQLDATE:
          if (Date.class.isAssignableFrom(value.getClass())) {
            ((Collection<Date>) values).add((Date) value);
          } else if (java.util.Date.class.isAssignableFrom(value.getClass())) {
            ((Collection<Date>) values)
                .add(new Date(((java.util.Date) value).getTime()));
          }
          break;
        case TIMESTAMP:
          ((Collection<Timestamp>) values).add((Timestamp) value);
          break;
        case STRING:
          ((Collection<String>) values)
              .add(XmlUtil.getExtraTrimed((String) value));
          break;
        case XVAL:
          ((Collection<XmlValue[]>) values).add((XmlValue[]) value);
          break;
        default:
          throw new InvalidObjectException(
              CAN_NOT_CONVERT_VALUE_FROM + value.getClass() + TO_TYPE +
              getClassType());
      }
    } else {
      throw new InvalidObjectException(
          CAN_NOT_CONVERT_VALUE_FROM + value.getClass() + TO_TYPE +
          getClassType());
    }
  }

  /**
   * @return the value as Object (might be null if multiple)
   */
  public Object getValue() {
    return value;
  }

  /**
   * Utility function to get a clone of a value
   *
   * @param type
   * @param value
   *
   * @return the clone Object
   *
   * @throws InvalidObjectException
   */
  public static Object getCloneValue(XmlType type, Object value)
      throws InvalidObjectException {
    if (value == null) {
      throw new InvalidObjectException(
          "Can not convert value from null to type " + type.classType);
    }
    switch (type) {
      case BOOLEAN:
        return new Boolean((Boolean) value);
      case INTEGER:
        return new Integer((Integer) value);
      case FLOAT:
        return new Float((Float) value);
      case CHARACTER:
        return new Character((Character) value);
      case BYTE:
        return new Byte((Byte) value);
      case LONG:
        return new Long((Long) value);
      case DOUBLE:
        return new Double((Double) value);
      case SHORT:
        return new Short((Short) value);
      case SQLDATE:
        return new Date(((Date) value).getTime());
      case TIMESTAMP:
        return new Timestamp(((Timestamp) value).getTime());
      case STRING:
        return value;
      case XVAL:
        return new XmlValue((XmlValue) value);
      case EMPTY:
      default:
        throw new InvalidObjectException(
            CAN_NOT_CONVERT_VALUE_FROM + value.getClass() + TO_TYPE +
            type.classType);
    }
  }

  /**
   * @return a clone of the value as Object (might be null if multiple)
   *
   * @throws InvalidObjectException
   */
  public Object getCloneValue() throws InvalidObjectException {
    if (getType() == XmlType.EMPTY) {
      return new XmlValue(decl);
    }
    return getCloneValue(getType(), value);
  }

  /**
   * @return the value as a string
   */
  public String getString() {
    if (getType().isString()) {
      return XmlUtil.getExtraTrimed((String) value);
    }
    throw new IllegalArgumentException(
        CAN_NOT_CONVERT_VALUE_FROM + decl.getClassType() + " to type String");
  }

  /**
   * @return the value as an integer
   */
  public int getInteger() {
    if (getType().isInteger()) {
      return (Integer) value;
    }
    throw new IllegalArgumentException(
        CAN_NOT_CONVERT_VALUE_FROM + decl.getClassType() + " to type Integer");
  }

  /**
   * @return the value as a boolean
   */
  public boolean getBoolean() {
    if (getType().isBoolean()) {
      return (Boolean) value;
    }
    throw new IllegalArgumentException(
        CAN_NOT_CONVERT_VALUE_FROM + decl.getClassType() + " to type Boolean");
  }

  /**
   * @return the value as a long
   */
  public long getLong() {
    if (getType().isLong()) {
      return (Long) value;
    }
    throw new IllegalArgumentException(
        CAN_NOT_CONVERT_VALUE_FROM + decl.getClassType() + " to type Long");
  }

  /**
   * @return the value as a float
   */
  public float getFloat() {
    if (getType().isFloat()) {
      return (Float) value;
    }
    throw new IllegalArgumentException(
        CAN_NOT_CONVERT_VALUE_FROM + decl.getClassType() + " to type Float");
  }

  /**
   * @return the value as a float
   */
  public char getCharacter() {
    if (getType().isCharacter()) {
      return (Character) value;
    }
    throw new IllegalArgumentException(
        CAN_NOT_CONVERT_VALUE_FROM + decl.getClassType() +
        " to type Character");
  }

  /**
   * @return the value as a float
   */
  public byte getByte() {
    if (getType().isByte()) {
      return (Byte) value;
    }
    throw new IllegalArgumentException(
        CAN_NOT_CONVERT_VALUE_FROM + decl.getClassType() + " to type Byte");
  }

  /**
   * @return the value as a float
   */
  public double getDouble() {
    if (getType().isDouble()) {
      return (Double) value;
    }
    throw new IllegalArgumentException(
        CAN_NOT_CONVERT_VALUE_FROM + decl.getClassType() + " to type Double");
  }

  /**
   * @return the value as a float
   */
  public short getShort() {
    if (getType().isShort()) {
      return (Short) value;
    }
    throw new IllegalArgumentException(
        CAN_NOT_CONVERT_VALUE_FROM + decl.getClassType() + " to type Short");
  }

  /**
   * @return the value as a float
   */
  public Date getDate() {
    if (getType().isDate()) {
      return (Date) value;
    }
    throw new IllegalArgumentException(
        CAN_NOT_CONVERT_VALUE_FROM + decl.getClassType() + " to type Date");
  }

  /**
   * @return the value as a float
   */
  public Timestamp getTimestamp() {
    if (getType().isTimestamp()) {
      return (Timestamp) value;
    }
    throw new IllegalArgumentException(
        CAN_NOT_CONVERT_VALUE_FROM + decl.getClassType() +
        " to type Timestamp");
  }

  /**
   * Set a value from String
   *
   * @param value
   *
   * @throws InvalidArgumentException
   */
  public void setFromString(String value) throws InvalidArgumentException {
    this.value = convert(getClassType(), XmlUtil.getExtraTrimed(value));
  }

  /**
   * Test if the Value is empty. If it is a SubXml or isMultiple, check if
   * subnodes are present but not if those
   * nodes are empty.
   *
   * @return True if the Value is Empty
   */
  public boolean isEmpty() {
    if (isSubXml()) {
      if (isMultiple()) {
        return values.isEmpty();
      } else {
        return subXml.length == 0;
      }
    }
    if (isMultiple()) {
      return values.isEmpty();
    } else {
      return value == null;
    }
  }

  /**
   * Get a value into a String
   *
   * @return the value in String format
   */
  public String getIntoString() {
    if (!isMultiple() && !isSubXml()) {
      if (value != null) {
        return value.toString();
      } else {
        return "";
      }
    } else {
      throw new IllegalArgumentException(
          "Cannot convert Multiple values to single String");
    }
  }

  /**
   * @param value the value to set
   *
   * @throws InvalidObjectException
   * @throws NumberFormatException
   */
  @SuppressWarnings("unchecked")
  public void setValue(Object value) throws InvalidObjectException {
    if (getType().isNativelyCompatible(value)) {
      switch (getType()) {
        case BOOLEAN:
        case TIMESTAMP:
        case SHORT:
        case DOUBLE:
        case LONG:
        case BYTE:
        case CHARACTER:
        case FLOAT:
        case INTEGER:
          this.value = value;
          break;
        case SQLDATE:
          if (Date.class.isAssignableFrom(value.getClass())) {
            this.value = value;
          } else if (java.util.Date.class.isAssignableFrom(value.getClass())) {
            this.value = new Date(((java.util.Date) value).getTime());
          }
          break;
        case STRING:
          this.value = XmlUtil.getExtraTrimed((String) value);
          break;
        case XVAL:
          final XmlValue[] newValue = (XmlValue[]) value;
          if (isSubXml()) {
            // FIXME should check also internal XmlDecl equality but
            // can only check size
            if (decl.getSubXmlSize() != newValue.length) {
              throw new InvalidObjectException(
                  "XmlDecl are not compatible from Array of XmlValue" +
                  TO_TYPE + getClassType());
            }
            if (isMultiple()) {
              ((List<XmlValue[]>) values).add(newValue);
            } else {
              subXml = newValue;
            }
          } else {
            throw new InvalidObjectException(
                "Can not convert value from Array of XmlValue" + TO_TYPE +
                getClassType());
          }
          break;
        default:
          throw new InvalidObjectException(
              CAN_NOT_CONVERT_VALUE_FROM + value.getClass() + TO_TYPE +
              getClassType());
      }
    } else {
      throw new InvalidObjectException(
          CAN_NOT_CONVERT_VALUE_FROM + value.getClass() + TO_TYPE +
          getClassType());
    }
  }

  /**
   * Convert String value to the specified type. Throws
   * InvalidArgumentException
   * if type is unrecognized.
   *
   * @throws InvalidArgumentException
   */
  protected static Object convert(Class<?> type, String value)
      throws InvalidArgumentException {
    try {
      // test from specific to general
      //
      if (String.class.isAssignableFrom(type)) {
        return value;
      }
      // primitives
      //
      else if (type.equals(Boolean.TYPE)) {
        if ("1".equals(value)) {
          return Boolean.TRUE;
        }
        return Boolean.valueOf(value);
      } else if (type.equals(Integer.TYPE)) {
        return Integer.valueOf(value);
      } else if (type.equals(Float.TYPE)) {
        return Float.valueOf(value);
      } else if (type.equals(Character.TYPE)) {
        return Character.valueOf(value.charAt(0));
      } else if (type.equals(Byte.TYPE)) {
        return Byte.valueOf(value);
      } else if (type.equals(Long.TYPE)) {
        return Long.valueOf(value);
      } else if (type.equals(Double.TYPE)) {
        return Double.valueOf(value);
      } else if (type.equals(Short.TYPE)) {
        return Short.valueOf(value);
      }
      // primitive wrappers
      //
      else if (Boolean.class.isAssignableFrom(type)) {
        if ("true".equalsIgnoreCase(value)) {
          return Boolean.TRUE;
        } else {
          return Boolean.FALSE;
        }
      } else if (Character.class.isAssignableFrom(type)) {
        if (value.length() == 1) {
          return Character.valueOf(value.charAt(0));
        } else {
          throw new IllegalArgumentException(
              CAN_NOT_CONVERT_VALUE + value + TO_TYPE + type);
        }
      } else if (Number.class.isAssignableFrom(type)) {
        if (Double.class.isAssignableFrom(type)) {
          return new Double(value);
        } else if (Float.class.isAssignableFrom(type)) {
          return new Float(value);
        } else if (Integer.class.isAssignableFrom(type)) {
          return new Integer(value);
        } else if (Long.class.isAssignableFrom(type)) {
          return new Long(value);
        } else if (Short.class.isAssignableFrom(type)) {
          return new Short(value);
        }
        // other primitive-like classes
        //
        else if (BigDecimal.class.isAssignableFrom(type)) {
          throw new IllegalArgumentException("Can not use type " + type);
        } else if (BigInteger.class.isAssignableFrom(type)) {
          throw new IllegalArgumentException("Can not use type " + type);
        } else {
          throw new IllegalArgumentException(
              CAN_NOT_CONVERT_VALUE + value + TO_TYPE + type);
        }
      }
      //
      // Time and date. We stick close to the JDBC representations
      // for time and date, but add the "GMT" timezone so XML files
      // can be transferred across timezones without ambiguity. See
      // java.sql.Date.toString() and java.sql.Timestamp.toString().
      //
      else if (Date.class.isAssignableFrom(type)) {
        return new Date(XmlStaticShared.dateFormat.parse(value).getTime());
      } else if (Timestamp.class.isAssignableFrom(type)) {
        final int dotIndex = value.indexOf('.');
        final int spaceIndex = value.indexOf(' ', dotIndex);
        if (dotIndex < 0 || spaceIndex < 0) {
          throw new IllegalArgumentException(
              CAN_NOT_CONVERT_VALUE + value + TO_TYPE + type);
        }
        final Timestamp ts = new Timestamp(
            XmlStaticShared.timestampFormat.parse(value.substring(0, dotIndex))
                                           .getTime());
        final int nanos =
            Integer.parseInt(value.substring(dotIndex + 1, spaceIndex));
        ts.setNanos(nanos);

        return ts;
      } else if (java.util.Date.class.isAssignableFrom(type)) {
        // Should not be
        return new Date(XmlStaticShared.timeFormat.parse(value).getTime());
      } else {
        throw new IllegalArgumentException(
            CAN_NOT_CONVERT_VALUE + value + TO_TYPE + type);
      }
    } catch (final NumberFormatException e) {
      throw new InvalidArgumentException(
          CAN_NOT_CONVERT_VALUE + value + TO_TYPE + type);
    } catch (final IllegalArgumentException e) {
      throw new InvalidArgumentException(
          CAN_NOT_CONVERT_VALUE + value + TO_TYPE + type, e);
    } catch (final ParseException e) {
      throw new InvalidArgumentException(
          CAN_NOT_CONVERT_VALUE + value + TO_TYPE + type);
    }
  }

  @Override
  public String toString() {
    return "Val: " + (isMultiple()? values.size() + " elements" :
        value != null? value.toString() :
            subXml != null? "subXml" : "no value") + ' ' + decl;
  }

  public String toFullString() {
    StringBuilder detail = new StringBuilder("Val: " + (isMultiple()?
        values.size() + " elements" : value != null? value.toString() :
        subXml != null? "subXml" : "no value") + ' ' + decl);
    if (decl.isSubXml()) {
      if (isMultiple()) {
        detail.append('[');
        for (final Object obj : values) {
          if (obj instanceof XmlValue) {
            detail.append(((XmlValue) obj).toFullString()).append(", ");
          } else {
            detail.append('[');
            for (final XmlValue obj2 : (XmlValue[]) obj) {
              detail.append(obj2.toFullString()).append(", ");
            }
            detail.append("], ");
          }
        }
        detail.append(']');
      } else {
        detail.append('[');
        for (final XmlValue obj : subXml) {
          detail.append(obj.toFullString()).append(", ");
        }
        detail.append(']');
      }
    }
    return detail.toString();
  }
}
