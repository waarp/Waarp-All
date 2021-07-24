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
package org.waarp.gateway.kernel;

import io.netty.handler.codec.http.multipart.FileUpload;
import org.waarp.gateway.kernel.exception.HttpIncorrectRequestException;

/**
 *
 */
public abstract class AbstractHttpField implements Cloneable {

  public enum FieldRole {
    BUSINESS_INPUT_TEXT, BUSINESS_INPUT_CHECKBOX, BUSINESS_INPUT_RADIO,
    BUSINESS_INPUT_HIDDEN, BUSINESS_INPUT_FILE, BUSINESS_INPUT_IMAGE,
    BUSINESS_INPUT_PWD, BUSINESS_TEXTAREA, BUSINESS_SELECT, SUBMIT,
    BUSINESS_COOKIE
  }

  public enum FieldPosition {
    URL, HEADER, COOKIE, BODY, ANY
  }

  /**
   * Special field name for Error page
   */
  public static final String ERRORINFO = "ERRORINFO";
  /*
   * fieldname, fieldtype, fieldinfo, fieldvalue, fieldvisibility, fieldmandatory, fieldcookieset,
   * fieldtovalidate, fieldposition, fieldrank
   */
  private String fieldname;
  private FieldRole fieldtype;
  private String fieldinfo;
  public String fieldvalue;
  private boolean fieldvisibility;
  private boolean fieldmandatory;
  private boolean fieldcookieset;
  private boolean fieldtovalidate;
  private FieldPosition fieldposition;
  private int fieldrank;
  private boolean present;
  FileUpload fileUpload;

  /**
   * @param fieldname
   * @param fieldtype
   * @param fieldinfo
   * @param fieldvalue
   * @param fieldvisibility
   * @param fieldmandatory
   * @param fieldcookieset
   * @param fieldtovalidate
   * @param fieldposition
   * @param fieldrank
   */
  protected AbstractHttpField(final String fieldname, final FieldRole fieldtype,
                              final String fieldinfo, final String fieldvalue,
                              final boolean fieldvisibility,
                              final boolean fieldmandatory,
                              final boolean fieldcookieset,
                              final boolean fieldtovalidate,
                              final FieldPosition fieldposition,
                              final int fieldrank) {
    setFieldname(fieldname);
    setFieldtype(fieldtype);
    setFieldinfo(fieldinfo);
    this.fieldvalue = fieldvalue;
    setFieldvisibility(fieldvisibility);
    setFieldmandatory(fieldmandatory);
    setFieldcookieset(fieldcookieset);
    setFieldtovalidate(fieldtovalidate);
    setFieldposition(fieldposition);
    setFieldrank(fieldrank);
  }

  /**
   * @param page source HttpPage
   *
   * @return the html form of a field according to its type and value
   */
  public abstract String getHtmlFormField(HttpPage page)
      throws HttpIncorrectRequestException;

  /**
   * @param page source HttpPage
   *
   * @return the html tab of a field according to its type and value
   */
  public abstract String getHtmlTabField(HttpPage page)
      throws HttpIncorrectRequestException;

  @Override
  public abstract AbstractHttpField clone();

  /**
   * Set the value
   *
   * @param value
   *
   * @throws HttpIncorrectRequestException if the value was already
   *     set
   */
  public abstract void setStringValue(String value)
      throws HttpIncorrectRequestException;

  /**
   * Set the fileUpload
   *
   * @param fileUpload
   *
   * @throws HttpIncorrectRequestException if the value was already
   *     set
   */
  public abstract void setFileUpload(FileUpload fileUpload)
      throws HttpIncorrectRequestException;

  /**
   * Clean method
   */
  public final void clean() {
    setFieldname(null);
    setFieldinfo(null);
    fieldvalue = null;
    setPresent(false);
    if (getFileUpload() != null) {
      getFileUpload().delete();
      fileUpload = null;
    }
  }

  /**
   * @return the fieldname
   */
  public final String getFieldname() {
    return fieldname;
  }

  /**
   * @param fieldname the fieldname to set
   */
  private void setFieldname(final String fieldname) {
    this.fieldname = fieldname;
  }

  /**
   * @return the fieldtype
   */
  public final FieldRole getFieldtype() {
    return fieldtype;
  }

  /**
   * @param fieldtype the fieldtype to set
   */
  private void setFieldtype(final FieldRole fieldtype) {
    this.fieldtype = fieldtype;
  }

  /**
   * @return the fieldinfo
   */
  public final String getFieldinfo() {
    return fieldinfo;
  }

  /**
   * @param fieldinfo the fieldinfo to set
   */
  private void setFieldinfo(final String fieldinfo) {
    this.fieldinfo = fieldinfo;
  }

  /**
   * @return the fieldvisibility
   */
  public final boolean isFieldvisibility() {
    return fieldvisibility;
  }

  /**
   * @param fieldvisibility the fieldvisibility to set
   */
  public final void setFieldvisibility(final boolean fieldvisibility) {
    this.fieldvisibility = fieldvisibility;
  }

  /**
   * @return the fieldmandatory
   */
  public final boolean isFieldmandatory() {
    return fieldmandatory;
  }

  /**
   * @param fieldmandatory the fieldmandatory to set
   */
  private void setFieldmandatory(final boolean fieldmandatory) {
    this.fieldmandatory = fieldmandatory;
  }

  /**
   * @return the fieldcookieset
   */
  public final boolean isFieldcookieset() {
    return fieldcookieset;
  }

  /**
   * @param fieldcookieset the fieldcookieset to set
   */
  private void setFieldcookieset(final boolean fieldcookieset) {
    this.fieldcookieset = fieldcookieset;
  }

  /**
   * @return the fieldtovalidate
   */
  public final boolean isFieldtovalidate() {
    return fieldtovalidate;
  }

  /**
   * @param fieldtovalidate the fieldtovalidate to set
   */
  private void setFieldtovalidate(final boolean fieldtovalidate) {
    this.fieldtovalidate = fieldtovalidate;
  }

  /**
   * @return the fieldposition
   */
  public final FieldPosition getFieldposition() {
    return fieldposition;
  }

  /**
   * @param fieldposition the fieldposition to set
   */
  private void setFieldposition(final FieldPosition fieldposition) {
    this.fieldposition = fieldposition;
  }

  /**
   * @return the fieldrank
   */
  public final int getFieldrank() {
    return fieldrank;
  }

  /**
   * @param fieldrank the fieldrank to set
   */
  private void setFieldrank(final int fieldrank) {
    this.fieldrank = fieldrank;
  }

  /**
   * @return the present
   */
  public final boolean isPresent() {
    return present;
  }

  /**
   * @param present the present to set
   */
  void setPresent(final boolean present) {
    this.present = present;
  }

  /**
   * @return the fileUpload
   */
  public final FileUpload getFileUpload() {
    return fileUpload;
  }
}
