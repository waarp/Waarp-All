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
package org.waarp.common.file;

/**
 * Interface for Option support (mainly inspired for MLSx FTP command)
 */
public interface OptsMLSxInterface {
  /**
   * @return the optsCharset
   */
  byte getOptsCharset();

  /**
   * @param optsCharset the optsCharset to set
   */
  void setOptsCharset(byte optsCharset);

  /**
   * @return the optsCreate
   */
  byte getOptsCreate();

  /**
   * @param optsCreate the optsCreate to set
   */
  void setOptsCreate(byte optsCreate);

  /**
   * @return the optsLang
   */
  byte getOptsLang();

  /**
   * @param optsLang the optsLang to set
   */
  void setOptsLang(byte optsLang);

  /**
   * @return the optsMediaType
   */
  byte getOptsMediaType();

  /**
   * @param optsMediaType the optsMediaType to set
   */
  void setOptsMediaType(byte optsMediaType);

  /**
   * @return the optsModify
   */
  byte getOptsModify();

  /**
   * @param optsModify the optsModify to set
   */
  void setOptsModify(byte optsModify);

  /**
   * @return the optsPerm
   */
  byte getOptsPerm();

  /**
   * @param optsPerm the optsPerm to set
   */
  void setOptsPerm(byte optsPerm);

  /**
   * @return the optsSize
   */
  byte getOptsSize();

  /**
   * @param optsSize the optsSize to set
   */
  void setOptsSize(byte optsSize);

  /**
   * @return the optsType
   */
  byte getOptsType();

  /**
   * @param optsType the optsType to set
   */
  void setOptsType(byte optsType);

  /**
   * @return the optsUnique
   */
  byte getOptsUnique();

  /**
   * @param optsUnique the optsUnique to set
   */
  void setOptsUnique(byte optsUnique);

  /**
   * @return the String associated to the feature for MLSx
   */
  String getFeat();
}
