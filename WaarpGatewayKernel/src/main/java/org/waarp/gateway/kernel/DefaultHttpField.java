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
public class DefaultHttpField extends AbstractHttpField {

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
  public DefaultHttpField(String fieldname, FieldRole fieldtype,
                          String fieldinfo, String fieldvalue,
                          boolean fieldvisibility, boolean fieldmandatory,
                          boolean fieldcookieset, boolean fieldtovalidate,
                          FieldPosition fieldposition, int fieldrank) {
    super(fieldname, fieldtype, fieldinfo, fieldvalue, fieldvisibility,
          fieldmandatory, fieldcookieset, fieldtovalidate, fieldposition,
          fieldrank);
  }

  @Override
  public String getHtmlFormField(HttpPage page)
      throws HttpIncorrectRequestException {
    final StringBuilder builder = new StringBuilder();
    switch (getFieldtype()) {
      case BUSINESS_INPUT_CHECKBOX:
      case BUSINESS_INPUT_RADIO: {
        builder.append(getFieldinfo());
        final AbstractHttpField source = page.getFields().get(getFieldname());
        final String[] values = source.fieldvalue.split(",");
        final String[] finalValues = fieldvalue.split(",");
        String inputtype;
        if (getFieldtype() == FieldRole.BUSINESS_INPUT_CHECKBOX) {
          inputtype = ": <INPUT type=CHECKBOX name=";
        } else {
          inputtype = ": <INPUT type=RADIO name=";
        }
        for (final String string : values) {
          builder.append(inputtype).append(getFieldname());
          if (fieldvalue != null && fieldvalue.length() > 0) {
            builder.append(" value=\"").append(string).append('"');
            if (finalValues != null) {
              for (final String value : finalValues) {
                if (value.equals(string)) {
                  builder.append(" checked");
                  break;
                }
              }
            }
          }
          builder.append('>').append(string).append("<BR>");
        }
        break;
      }
      case BUSINESS_INPUT_FILE:
      case BUSINESS_INPUT_HIDDEN:
      case BUSINESS_INPUT_PWD:
      case BUSINESS_INPUT_TEXT:
      case SUBMIT: {
        builder.append(getFieldinfo());
        switch (getFieldtype()) {
          case BUSINESS_INPUT_FILE:
            builder.append(": <INPUT type=FILE name=");
            break;
          case BUSINESS_INPUT_HIDDEN:
            builder.append(": <INPUT type=HIDDEN name=");
            break;
          case BUSINESS_INPUT_PWD:
            builder.append(": <INPUT type=PASSWORD name=");
            break;
          case BUSINESS_INPUT_TEXT:
            builder.append(": <INPUT type=TEXT name=");
            break;
          case SUBMIT:
            builder.append(": <INPUT type=SUBMIT name=");
            break;
          default:
            throw new HttpIncorrectRequestException(
                "Incorrect type: " + getFieldtype());
        }
        builder.append(getFieldname());
        if (fieldvalue != null && fieldvalue.length() > 0) {
          builder.append(" value=\"").append(fieldvalue).append('"');
        }
        builder.append('>');
        break;
      }
      case BUSINESS_INPUT_IMAGE: {
        builder.append(getFieldinfo()).append(": <INPUT type=IMAGE name=")
               .append(getFieldname());
        if (fieldvalue != null && fieldvalue.length() > 0) {
          builder.append(" src=\"").append(fieldvalue).append("\" ");
        }
        if (getFieldinfo() != null && getFieldinfo().length() > 0) {
          builder.append(" alt=\"").append(getFieldinfo()).append("\" ");
        }
        builder.append('>');
        break;
      }
      case BUSINESS_SELECT: {
        builder.append(getFieldinfo()).append("<BR><SELECT name=")
               .append(getFieldname()).append('>');
        final AbstractHttpField source = page.getFields().get(getFieldname());
        final String[] values = source.fieldvalue.split(",");
        for (final String string : values) {
          builder.append("<OPTION label=\"").append(string)
                 .append("\" value=\"").append(string);
          if (fieldvalue != null && fieldvalue.length() > 0 &&
              fieldvalue.equals(string)) {
            builder.append("\" selected>");
          } else {
            builder.append("\">");
          }
          builder.append(string).append("</OPTION>");
        }
        builder.append("</SELECT>");
        break;
      }
      case BUSINESS_TEXTAREA: {
        builder.append(getFieldinfo()).append("<BR><TEXTAREA name=")
               .append(getFieldname()).append('>');
        if (fieldvalue != null && fieldvalue.length() > 0) {
          builder.append(fieldvalue);
        }
        builder.append("</TEXTAREA>");
        break;
      }
      case BUSINESS_COOKIE:
        // no since Cookie
        break;
      default:
        throw new HttpIncorrectRequestException(
            "Incorrect type: " + getFieldtype());
    }
    return builder.toString();
  }

  @Override
  public String getHtmlTabField(HttpPage page)
      throws HttpIncorrectRequestException {
    final StringBuilder builder =
        new StringBuilder().append(getFieldinfo()).append("</TD><TD>");
    if (fieldvalue != null) {
      builder.append(fieldvalue);
    }
    return builder.toString();
  }

  @Override
  public DefaultHttpField clone() {
    return new DefaultHttpField(getFieldname(), getFieldtype(), getFieldinfo(),
                                fieldvalue, isFieldvisibility(),
                                isFieldmandatory(), isFieldcookieset(),
                                isFieldtovalidate(), getFieldposition(),
                                getFieldrank());
  }

  @Override
  public void setStringValue(String value)
      throws HttpIncorrectRequestException {
    switch (getFieldtype()) {
      case BUSINESS_INPUT_CHECKBOX:
        if (fieldvalue != null) {
          if (fieldvalue.length() > 0) {
            fieldvalue += ',' + value;
          } else {
            fieldvalue = value;
          }
        } else {
          fieldvalue = value;
        }
        setPresent(true);
        break;
      case BUSINESS_INPUT_FILE:
      case BUSINESS_INPUT_HIDDEN:
      case BUSINESS_INPUT_IMAGE:
      case BUSINESS_INPUT_PWD:
      case BUSINESS_INPUT_RADIO:
      case BUSINESS_INPUT_TEXT:
      case BUSINESS_SELECT:
      case BUSINESS_TEXTAREA:
      case BUSINESS_COOKIE:
        if (isPresent()) {
          // should not be
          throw new HttpIncorrectRequestException(
              "Field already filled: " + getFieldname());
        }
        fieldvalue = value;
        setPresent(true);
        break;
      default:
        break;
    }
  }

  @Override
  public void setFileUpload(FileUpload fileUpload)
      throws HttpIncorrectRequestException {
    if (getFieldtype() == FieldRole.BUSINESS_INPUT_FILE) {
      if (isPresent()) {
        // should not be
        throw new HttpIncorrectRequestException(
            "Field already filled: " + getFieldname());
      }
      this.fileUpload = fileUpload;
      fieldvalue = fileUpload.getFilename();
      setPresent(true);
    } else {
      // should not be
      throw new HttpIncorrectRequestException(
          "Field with wrong type (should be File): " + getFieldname());
    }
  }

}
