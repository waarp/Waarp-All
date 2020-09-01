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

/**
 * XmlDecl to declare types, path and name for values from/to XML file/document
 */
public class XmlDecl {
  private final String name;

  private final XmlType type;

  private final String xmlPath;

  private final XmlDecl[] subXml;

  private final boolean isMultiple;

  public XmlDecl(final String name, final String xmlPath) {
    this.name = name;
    type = XmlType.EMPTY;
    this.xmlPath = xmlPath;
    isMultiple = false;
    subXml = null;
  }

  public XmlDecl(final XmlType type, final String xmlPath) {
    name = xmlPath;
    this.type = type;
    this.xmlPath = xmlPath;
    isMultiple = false;
    subXml = null;
  }

  public XmlDecl(final String name, final XmlType type, final String xmlPath) {
    this.name = name;
    this.type = type;
    this.xmlPath = xmlPath;
    isMultiple = false;
    subXml = null;
  }

  public XmlDecl(final String name, final XmlType type, final String xmlPath,
                 final boolean isMultiple) {
    this.name = name;
    this.type = type;
    this.xmlPath = xmlPath;
    this.isMultiple = isMultiple;
    subXml = null;
  }

  public XmlDecl(final String name, final XmlType type, final String xmlPath,
                 final XmlDecl[] decls, final boolean isMultiple) {
    this.name = name;
    this.type = type;
    this.xmlPath = xmlPath;
    this.isMultiple = isMultiple;
    subXml = decls;
  }

  /**
   * Get Java field name
   *
   * @return the field name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the class type
   */
  public Class<?> getClassType() {
    return type.getClassType();
  }

  /**
   * @return the internal type
   */
  public XmlType getType() {
    return type;
  }

  /**
   * @return the xmlPath
   */
  public String getXmlPath() {
    return xmlPath;
  }

  /**
   * @return True if this Decl is a subXml
   */
  public boolean isSubXml() {
    return subXml != null;
  }

  /**
   * @return the subXml
   */
  public XmlDecl[] getSubXml() {
    return subXml;
  }

  /**
   * @return the subXml size
   */
  public int getSubXmlSize() {
    if (subXml == null) {
      return 0;
    }
    return subXml.length;
  }

  /**
   * @return the isMultiple
   */
  public boolean isMultiple() {
    return isMultiple;
  }

  /**
   * Check if two XmlDecl are compatible
   *
   * @param xmlDecl
   *
   * @return True if compatible
   */
  public boolean isCompatible(final XmlDecl xmlDecl) {
    if ((isMultiple && xmlDecl.isMultiple ||
         !isMultiple && !xmlDecl.isMultiple) &&
        (isSubXml() && xmlDecl.isSubXml() ||
         !isSubXml() && !xmlDecl.isSubXml())) {
      if (!isSubXml()) {
        return type == xmlDecl.type;
      }
      if (subXml == null || xmlDecl.subXml == null ||
          subXml.length != xmlDecl.subXml.length) {
        return false;
      }
      for (int i = 0; i < subXml.length; i++) {
        if (!subXml[i].isCompatible(xmlDecl.subXml[i])) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public String toString() {
    return "Decl: " + name + " Type: " + type.name() + " XmlPath: " + xmlPath +
           " isMultiple: " + isMultiple + " isSubXml: " + isSubXml();
  }
}
