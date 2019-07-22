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

import java.io.InvalidObjectException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.waarp.common.exception.InvalidArgumentException;

/**
 * XmlValue base element
 * 
 * @author Frederic Bregier
 * 
 */
public class XmlValue {

    private XmlDecl decl;

    private Object value;

    private List<?> values;

    private XmlValue[] subXml;

    public XmlValue(XmlDecl decl) {
        this.decl = decl;
        if (this.decl.isSubXml()) {
            if (this.decl.isMultiple()) {
                value = null;
                values = new ArrayList<XmlValue>();
                this.subXml = null;
                return;
            }
            int len = this.decl.getSubXmlSize();
            XmlDecl[] newDecls = this.decl.getSubXml();
            this.subXml = new XmlValue[len];
            for (int i = 0; i < len; i++) {
                this.subXml[i] = new XmlValue(newDecls[i]);
            }
            value = null;
            values = null;
            return;
        }
        if (this.decl.isMultiple()) {
            value = null;
            switch (this.getType()) {
                case BOOLEAN:
                    values = new ArrayList<Boolean>();
                    break;
                case INTEGER:
                    values = new ArrayList<Integer>();
                    break;
                case FLOAT:
                    values = new ArrayList<Float>();
                    break;
                case CHARACTER:
                    values = new ArrayList<Character>();
                    break;
                case BYTE:
                    values = new ArrayList<Byte>();
                    break;
                case LONG:
                    values = new ArrayList<Long>();
                    break;
                case DOUBLE:
                    values = new ArrayList<Double>();
                    break;
                case SHORT:
                    values = new ArrayList<Short>();
                    break;
                case SQLDATE:
                    values = new ArrayList<Date>();
                    break;
                case TIMESTAMP:
                    values = new ArrayList<Timestamp>();
                    break;
                case STRING:
                    values = new ArrayList<String>();
                    break;
                case EMPTY:
                    break;
                case XVAL:
                    break;
                default:
                    break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public XmlValue(XmlValue from) {
        this(from.decl);
        if (this.decl.isSubXml()) {
            if (this.decl.isMultiple()) {
                List<XmlValue[]> subvalues = (List<XmlValue[]>) from.values;
                for (XmlValue[] xmlValues : subvalues) {
                    XmlValue[] newValues = new XmlValue[xmlValues.length];
                    for (int i = 0; i < xmlValues.length; i++) {
                        newValues[i] = new XmlValue(xmlValues[i]);
                    }
                    ((List<XmlValue[]>) this.values).add(newValues);
                }
            } else {
                for (int i = 0; i < from.subXml.length; i++) {
                    this.subXml[i] = new XmlValue(from.subXml[i]);
                }
            }
        } else if (this.decl.isMultiple()) {
            List<Object> subvalues = (List<Object>) from.values;
            for (Object object : subvalues) {
                try {
                    this.addValue(getCloneValue(getType(), object));
                } catch (InvalidObjectException e) {
                    continue;
                }
            }
        } else {
            try {
                this.setValue(from.getCloneValue());
            } catch (InvalidObjectException e) {
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
        return this.decl.getName();
    }

    /**
     * @return the type
     */
    public Class<?> getClassType() {
        return this.decl.getClassType();
    }

    /**
     * @return the type
     */
    public XmlType getType() {
        return this.decl.getType();
    }

    /**
     * @return the xmlPath
     */
    public String getXmlPath() {
        return this.decl.getXmlPath();
    }

    /**
     * 
     * @return True if this Value is a subXml
     */
    public boolean isSubXml() {
        return this.decl.isSubXml();
    }

    /**
     * 
     * @return the associated SubXML with the XmlValue (might be null if singleton or Multiple)
     */
    public XmlValue[] getSubXml() {
        return this.subXml;
    }

    /**
     * 
     * @return True if the Value are list of values
     */
    public boolean isMultiple() {
        return this.decl.isMultiple();
    }

    /**
     * 
     * @return the associated list with the XmlValues (might be null if singleton or SubXml)
     */
    public List<?> getList() {
        return this.values;
    }

    /**
     * Add a value into the Multiple values from the String (not compatible with subXml)
     * 
     * @param value
     * @throws InvalidObjectException
     * @throws InvalidArgumentException
     */
    @SuppressWarnings("unchecked")
    public void addFromString(String value) throws InvalidObjectException, InvalidArgumentException {
        switch (this.getType()) {
            case BOOLEAN:
                ((List<Boolean>) this.values).add((Boolean) convert(
                        this.getClassType(), value));
                break;
            case INTEGER:
                ((List<Integer>) this.values).add((Integer) convert(
                        this.getClassType(), value));
                break;
            case FLOAT:
                ((List<Float>) this.values).add((Float) convert(
                        this.getClassType(), value));
                break;
            case CHARACTER:
                ((List<Character>) this.values).add((Character) convert(
                        this.getClassType(), value));
                break;
            case BYTE:
                ((List<Byte>) this.values).add((Byte) convert(
                        this.getClassType(), value));
                break;
            case LONG:
                ((List<Long>) this.values).add((Long) convert(
                        this.getClassType(), value));
                break;
            case DOUBLE:
                ((List<Double>) this.values).add((Double) convert(
                        this.getClassType(), value));
                break;
            case SHORT:
                ((List<Short>) this.values).add((Short) convert(
                        this.getClassType(), value));
                break;
            case SQLDATE:
                ((List<java.sql.Date>) this.values)
                        .add((java.sql.Date) convert(this.getClassType(), value));
                break;
            case TIMESTAMP:
                ((List<Timestamp>) this.values).add((Timestamp) convert(
                        this.getClassType(), value));
                break;
            case STRING:
                ((List<String>) this.values).add((String) convert(
                        this.getClassType(), value));
                break;
            case XVAL:
                throw new InvalidObjectException(
                        "XVAL cannot be assigned from String directly");
                // ((List<XmlValue>) this.values).add((XmlValue) value);
            case EMPTY:
                throw new InvalidObjectException(
                        "EMPTY cannot be assigned");
        }
    }

    /**
     * Add a value into the Multiple values from the Object
     * 
     * @param value
     * @throws InvalidObjectException
     */
    @SuppressWarnings("unchecked")
    public void addValue(Object value) throws InvalidObjectException {
        if (this.getType().isNativelyCompatible(value)) {
            switch (this.getType()) {
                case BOOLEAN:
                    ((List<Boolean>) this.values).add((Boolean) value);
                    break;
                case INTEGER:
                    ((List<Integer>) this.values).add((Integer) value);
                    break;
                case FLOAT:
                    ((List<Float>) this.values).add((Float) value);
                    break;
                case CHARACTER:
                    ((List<Character>) this.values).add((Character) value);
                    break;
                case BYTE:
                    ((List<Byte>) this.values).add((Byte) value);
                    break;
                case LONG:
                    ((List<Long>) this.values).add((Long) value);
                    break;
                case DOUBLE:
                    ((List<Double>) this.values).add((Double) value);
                    break;
                case SHORT:
                    ((List<Short>) this.values).add((Short) value);
                    break;
                case SQLDATE:
                    if (java.sql.Date.class.isAssignableFrom(value.getClass())) {
                        ((List<java.sql.Date>) this.values)
                                .add((java.sql.Date) value);
                    } else if (java.util.Date.class.isAssignableFrom(value
                            .getClass())) {
                        ((List<java.sql.Date>) this.values)
                                .add(new java.sql.Date(((java.util.Date) value)
                                        .getTime()));
                    }
                    break;
                case TIMESTAMP:
                    ((List<Timestamp>) this.values).add((Timestamp) value);
                    break;
                case STRING:
                    ((List<String>) this.values).add((String) value);
                    break;
                case XVAL:
                    ((List<XmlValue[]>) this.values).add((XmlValue[]) value);
                    break;
                default:
                    throw new InvalidObjectException(
                            "Can not convert value from " + value.getClass() +
                                    " to type " + this.getClassType());
            }
        } else {
            throw new InvalidObjectException("Can not convert value from " +
                    value.getClass() + " to type " + this.getClassType());
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
     * @return the clone Object
     * @throws InvalidObjectException
     */
    public static Object getCloneValue(XmlType type, Object value) throws InvalidObjectException {
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
                return new java.sql.Date(((java.sql.Date) value).getTime());
            case TIMESTAMP:
                return new Timestamp(((Timestamp) value).getTime());
            case STRING:
                return new String((String) value);
            case XVAL:
                return new XmlValue((XmlValue) value);
            case EMPTY:
            default:
                throw new InvalidObjectException(
                        "Can not convert value from " + value.getClass() +
                                " to type " + type.classType);
        }
    }

    /**
     * @return a clone of the value as Object (might be null if multiple)
     * @throws InvalidObjectException
     */
    public Object getCloneValue() throws InvalidObjectException {
        if (getType() == XmlType.EMPTY) {
            return new XmlValue(this.decl);
        }
        return getCloneValue(getType(), value);
    }

    /**
     * 
     * @return the value as a string
     */
    public String getString() {
        if (this.getType().isString()) {
            return (String) value;
        }
        throw new IllegalArgumentException("Can not convert value from " +
                this.decl.getClassType() + " to type String");
    }

    /**
     * 
     * @return the value as an integer
     */
    public int getInteger() {
        if (this.getType().isInteger()) {
            return (Integer) value;
        }
        throw new IllegalArgumentException("Can not convert value from " +
                this.decl.getClassType() + " to type Integer");
    }

    /**
     * 
     * @return the value as a boolean
     */
    public boolean getBoolean() {
        if (this.getType().isBoolean()) {
            return (Boolean) value;
        }
        throw new IllegalArgumentException("Can not convert value from " +
                this.decl.getClassType() + " to type Boolean");
    }

    /**
     * 
     * @return the value as a long
     */
    public long getLong() {
        if (this.getType().isLong()) {
            return (Long) value;
        }
        throw new IllegalArgumentException("Can not convert value from " +
                this.decl.getClassType() + " to type Long");
    }

    /**
     * 
     * @return the value as a float
     */
    public float getFloat() {
        if (this.getType().isFloat()) {
            return (Float) value;
        }
        throw new IllegalArgumentException("Can not convert value from " +
                this.decl.getClassType() + " to type Float");
    }

    /**
     * 
     * @return the value as a float
     */
    public char getCharacter() {
        if (this.getType().isCharacter()) {
            return (Character) value;
        }
        throw new IllegalArgumentException("Can not convert value from " +
                this.decl.getClassType() + " to type Character");
    }

    /**
     * 
     * @return the value as a float
     */
    public byte getByte() {
        if (this.getType().isByte()) {
            return (Byte) value;
        }
        throw new IllegalArgumentException("Can not convert value from " +
                this.decl.getClassType() + " to type Byte");
    }

    /**
     * 
     * @return the value as a float
     */
    public double getDouble() {
        if (this.getType().isDouble()) {
            return (Double) value;
        }
        throw new IllegalArgumentException("Can not convert value from " +
                this.decl.getClassType() + " to type Double");
    }

    /**
     * 
     * @return the value as a float
     */
    public short getShort() {
        if (this.getType().isShort()) {
            return (Short) value;
        }
        throw new IllegalArgumentException("Can not convert value from " +
                this.decl.getClassType() + " to type Short");
    }

    /**
     * 
     * @return the value as a float
     */
    public Date getDate() {
        if (this.getType().isDate()) {
            return (Date) value;
        }
        throw new IllegalArgumentException("Can not convert value from " +
                this.decl.getClassType() + " to type Date");
    }

    /**
     * 
     * @return the value as a float
     */
    public Timestamp getTimestamp() {
        if (this.getType().isTimestamp()) {
            return (Timestamp) value;
        }
        throw new IllegalArgumentException("Can not convert value from " +
                this.decl.getClassType() + " to type Timestamp");
    }

    /**
     * Set a value from String
     * 
     * @param value
     * @throws InvalidArgumentException
     */
    public void setFromString(String value) throws InvalidArgumentException {
        this.value = convert(this.getClassType(), value);
    }

    /**
     * Test if the Value is empty. If it is a SubXml or isMultiple, check if subnodes are present
     * but not if those nodes are empty.
     * 
     * @return True if the Value is Empty
     */
    public boolean isEmpty() {
        if (isSubXml()) {
            if (isMultiple()) {
                return (this.values.isEmpty());
            } else {
                return (this.subXml.length == 0);
            }
        }
        if (isMultiple()) {
            return (this.values.isEmpty());
        } else {
            return (this.value == null);
        }
    }

    /**
     * Get a value into a String
     * 
     * @return the value in String format
     */
    public String getIntoString() {
        if ((!isMultiple()) && (!isSubXml())) {
            if (this.value != null) {
                return this.value.toString();
            } else {
                return "";
            }
        } else {
            throw new IllegalArgumentException(
                    "Cannot convert Multiple values to single String");
        }
    }

    /**
     * @param value
     *            the value to set
     * @throws InvalidObjectException
     * @exception NumberFormatException
     */
    @SuppressWarnings("unchecked")
    public void setValue(Object value) throws InvalidObjectException {
        if (this.getType().isNativelyCompatible(value)) {
            switch (this.getType()) {
                case BOOLEAN:
                    this.value = (Boolean) value;
                    break;
                case INTEGER:
                    this.value = (Integer) value;
                    break;
                case FLOAT:
                    this.value = (Float) value;
                    break;
                case CHARACTER:
                    this.value = (Character) value;
                    break;
                case BYTE:
                    this.value = (Byte) value;
                    break;
                case LONG:
                    this.value = (Long) value;
                    break;
                case DOUBLE:
                    this.value = (Double) value;
                    break;
                case SHORT:
                    this.value = (Short) value;
                    break;
                case SQLDATE:
                    if (java.sql.Date.class.isAssignableFrom(value.getClass())) {
                        this.value = (java.sql.Date) value;
                    } else if (java.util.Date.class.isAssignableFrom(value
                            .getClass())) {
                        this.value = new java.sql.Date(
                                ((java.util.Date) value).getTime());
                    }
                    break;
                case TIMESTAMP:
                    this.value = (Timestamp) value;
                    break;
                case STRING:
                    this.value = (String) value;
                    break;
                case XVAL:
                    XmlValue[] newValue = ((XmlValue[]) value);
                    if (this.isSubXml()) {
                        // FIXME should check also internal XmlDecl equality but
                        // can only check size
                        if (this.decl.getSubXmlSize() != newValue.length) {
                            throw new InvalidObjectException(
                                    "XmlDecl are not compatible from Array of XmlValue" +
                                            " to type " + this.getClassType());
                        }
                        if (this.isMultiple()) {
                            ((List<XmlValue[]>) this.values).add(newValue);
                        } else {
                            this.subXml = newValue;
                        }
                    } else {
                        throw new InvalidObjectException(
                                "Can not convert value from Array of XmlValue" +
                                        " to type " + this.getClassType());
                    }
                    break;
                default:
                    throw new InvalidObjectException(
                            "Can not convert value from " + value.getClass() +
                                    " to type " + this.getClassType());
            }
        } else {
            throw new InvalidObjectException("Can not convert value from " +
                    value.getClass() + " to type " + this.getClassType());
        }
    }

    /**
     * Convert String value to the specified type. Throws InvalidArgumentException if type is
     * unrecognized.
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
                if (value.equals("1")) {
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
                            "Can not convert value " + value + " to type " +
                                    type);
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
                    throw new IllegalArgumentException("Can not use type " +
                            type);
                } else if (BigInteger.class.isAssignableFrom(type)) {
                    throw new IllegalArgumentException("Can not use type " +
                            type);
                } else {
                    throw new IllegalArgumentException(
                            "Can not convert value " + value + " to type " +
                                    type);
                }
            }
            //
            // Time and date. We stick close to the JDBC representations
            // for time and date, but add the "GMT" timezone so XML files
            // can be transferred across timezones without ambiguity. See
            // java.sql.Date.toString() and java.sql.Timestamp.toString().
            //
            else if (java.sql.Date.class.isAssignableFrom(type)) {
                return new java.sql.Date(XmlStaticShared.dateFormat.parse(value).getTime());
            } else if (Timestamp.class.isAssignableFrom(type)) {
                int dotIndex = value.indexOf('.');
                int spaceIndex = value.indexOf(' ', dotIndex);
                if (dotIndex < 0 || spaceIndex < 0) {
                    throw new IllegalArgumentException(
                            "Can not convert value " + value + " to type " +
                                    type);
                }
                Timestamp ts = new Timestamp(XmlStaticShared.timestampFormat.parse(
                        value.substring(0, dotIndex)).getTime());
                int nanos = Integer.parseInt(value.substring(dotIndex + 1,
                        spaceIndex));
                ts.setNanos(nanos);

                return ts;
            } else if (java.util.Date.class.isAssignableFrom(type)) {
                // Should not be
                return new java.sql.Date(XmlStaticShared.timeFormat.parse(value).getTime());
            } else {
                throw new IllegalArgumentException("Can not convert value " +
                        value + " to type " + type);
            }
        } catch (NumberFormatException e) {
            throw new InvalidArgumentException("Can not convert value " +
                    value + " to type " + type);
        } catch (IllegalArgumentException e) {
            throw new InvalidArgumentException("Can not convert value " +
                    value + " to type " + type, e);
        } catch (ParseException e) {
            throw new InvalidArgumentException("Can not convert value " +
                    value + " to type " + type);
        }
    }

    public String toString() {
        return "Val: " +
                (isMultiple() ? (values.size() + " elements")
                        : (value != null ? value.toString()
                                : (subXml != null ? "subXml" : "no value"))) +
                " " + decl.toString();
    }

    public String toFullString() {
        String detail = "Val: " +
                (isMultiple() ? (values.size() + " elements")
                        : (value != null ? value.toString()
                                : (subXml != null ? "subXml" : "no value"))) +
                " " + decl.toString();
        if (this.decl.isSubXml()) {
            if (isMultiple()) {
                detail += "[";
                for (Object obj : values) {
                    if (obj instanceof XmlValue) {
                        detail += ((XmlValue) obj).toFullString() + ", ";
                    } else {
                        detail += "[";
                        for (XmlValue obj2 : (XmlValue[]) obj) {
                            detail += obj2.toFullString() + ", ";
                        }
                        detail += "], ";
                    }
                }
                detail += "]";
            } else {
                detail += "[";
                for (XmlValue obj : this.subXml) {
                    detail += obj.toFullString() + ", ";
                }
                detail += "]";
            }
        }
        return detail;
    }
}
