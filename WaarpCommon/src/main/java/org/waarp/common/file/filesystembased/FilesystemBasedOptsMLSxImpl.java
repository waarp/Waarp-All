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
package org.waarp.common.file.filesystembased;

import org.waarp.common.file.OptsMLSxInterface;

/**
 * Class that implements Opts command for MLSx operations. (-1) means not
 * supported, 0 supported but not
 * active, 1 supported and active
 */
public class FilesystemBasedOptsMLSxImpl implements OptsMLSxInterface {
  /**
   * Size option
   */
  private byte optsSize = -1;

  /**
   * Modify option
   */
  private byte optsModify = -1;

  /**
   * Type option
   */
  private byte optsType = -1;

  /**
   * Perm option
   */
  private byte optsPerm = -1;

  /**
   * Create option
   */
  private byte optsCreate = -1;

  /**
   * Unique option
   */
  private byte optsUnique = -1;

  /**
   * Lang option
   */
  private byte optsLang = -1;

  /**
   * Media-Type option
   */
  private byte optsMediaType = -1;

  /**
   * Charset option
   */
  private byte optsCharset = -1;

  /**
   * Default empty constructor: no support at all of MLSx function
   */
  public FilesystemBasedOptsMLSxImpl() {
  }

  /**
   * (-1) means not supported, 0 supported but not active, 1 supported and
   * active
   *
   * @param optsSize
   * @param optsModify
   * @param optsType
   * @param optsPerm
   * @param optsCreate
   * @param optsUnique
   * @param optsLang
   * @param optsMediaType
   * @param optsCharset
   */
  public FilesystemBasedOptsMLSxImpl(final byte optsSize, final byte optsModify,
                                     final byte optsType, final byte optsPerm,
                                     final byte optsCreate,
                                     final byte optsUnique, final byte optsLang,
                                     final byte optsMediaType,
                                     final byte optsCharset) {
    this.optsSize = optsSize;
    this.optsModify = optsModify;
    this.optsType = optsType;
    this.optsPerm = optsPerm;
    this.optsCreate = optsCreate;
    this.optsUnique = optsUnique;
    this.optsLang = optsLang;
    this.optsMediaType = optsMediaType;
    this.optsCharset = optsCharset;
  }

  /**
   * @return the optsCharset
   */
  @Override
  public byte getOptsCharset() {
    return optsCharset;
  }

  /**
   * @param optsCharset the optsCharset to set
   */
  @Override
  public void setOptsCharset(final byte optsCharset) {
    this.optsCharset = optsCharset;
  }

  /**
   * @return the optsCreate
   */
  @Override
  public byte getOptsCreate() {
    return optsCreate;
  }

  /**
   * @param optsCreate the optsCreate to set
   */
  @Override
  public void setOptsCreate(final byte optsCreate) {
    this.optsCreate = optsCreate;
  }

  /**
   * @return the optsLang
   */
  @Override
  public byte getOptsLang() {
    return optsLang;
  }

  /**
   * @param optsLang the optsLang to set
   */
  @Override
  public void setOptsLang(final byte optsLang) {
    this.optsLang = optsLang;
  }

  /**
   * @return the optsMediaType
   */
  @Override
  public byte getOptsMediaType() {
    return optsMediaType;
  }

  /**
   * @param optsMediaType the optsMediaType to set
   */
  @Override
  public void setOptsMediaType(final byte optsMediaType) {
    this.optsMediaType = optsMediaType;
  }

  /**
   * @return the optsModify
   */
  @Override
  public byte getOptsModify() {
    return optsModify;
  }

  /**
   * @param optsModify the optsModify to set
   */
  @Override
  public void setOptsModify(final byte optsModify) {
    this.optsModify = optsModify;
  }

  /**
   * @return the optsPerm
   */
  @Override
  public byte getOptsPerm() {
    return optsPerm;
  }

  /**
   * @param optsPerm the optsPerm to set
   */
  @Override
  public void setOptsPerm(final byte optsPerm) {
    this.optsPerm = optsPerm;
  }

  /**
   * @return the optsSize
   */
  @Override
  public byte getOptsSize() {
    return optsSize;
  }

  /**
   * @param optsSize the optsSize to set
   */
  @Override
  public void setOptsSize(final byte optsSize) {
    this.optsSize = optsSize;
  }

  /**
   * @return the optsType
   */
  @Override
  public byte getOptsType() {
    return optsType;
  }

  /**
   * @param optsType the optsType to set
   */
  @Override
  public void setOptsType(final byte optsType) {
    this.optsType = optsType;
  }

  /**
   * @return the optsUnique
   */
  @Override
  public byte getOptsUnique() {
    return optsUnique;
  }

  /**
   * @param optsUnique the optsUnique to set
   */
  @Override
  public void setOptsUnique(final byte optsUnique) {
    this.optsUnique = optsUnique;
  }

  /**
   * @return the String associated to the feature for MLSx
   */
  @Override
  public String getFeat() {
    final StringBuilder builder = new StringBuilder();
    builder.append(' ');
    if (optsSize >= 0) {
      builder.append("Size");
      if (optsSize > 0) {
        builder.append("*;");
      } else {
        builder.append(';');
      }
    }
    if (optsModify >= 0) {
      builder.append("Modify");
      if (optsModify > 0) {
        builder.append("*;");
      } else {
        builder.append(';');
      }
    }
    if (optsType >= 0) {
      builder.append("Type");
      if (optsType > 0) {
        builder.append("*;");
      } else {
        builder.append(';');
      }
    }
    if (optsPerm >= 0) {
      builder.append("Perm");
      if (optsPerm > 0) {
        builder.append("*;");
      } else {
        builder.append(';');
      }
    }
    if (optsCreate >= 0) {
      builder.append("Create");
      if (optsCreate > 0) {
        builder.append("*;");
      } else {
        builder.append(';');
      }
    }
    if (optsUnique >= 0) {
      builder.append("Unique");
      if (optsUnique > 0) {
        builder.append("*;");
      } else {
        builder.append(';');
      }
    }
    if (optsLang >= 0) {
      builder.append("Lang");
      if (optsLang > 0) {
        builder.append("*;");
      } else {
        builder.append(';');
      }
    }
    if (optsMediaType >= 0) {
      builder.append("Media-Type");
      if (optsMediaType > 0) {
        builder.append("*;");
      } else {
        builder.append(';');
      }
    }
    if (optsCharset >= 0) {
      builder.append("Charset");
      if (optsCharset > 0) {
        builder.append("*;");
      } else {
        builder.append(';');
      }
    }
    builder.append("UNIX.mode;");
    return builder.toString();
  }
}
