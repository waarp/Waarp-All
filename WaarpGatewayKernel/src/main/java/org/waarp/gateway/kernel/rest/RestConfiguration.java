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
package org.waarp.gateway.kernel.rest;

import org.waarp.common.crypto.HmacSha256;
import org.waarp.common.exception.CryptoException;
import org.waarp.common.utility.WaarpStringUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * General RestConfiguration model
 */
public class RestConfiguration {
  public enum CRUD {
    CREATE(0x01), READ(0x02), UPDATE(0x04), DELETE(0x08), ALL(0x0F);

    public final byte mask;

    CRUD(int mask) {
      this.mask = (byte) mask;
    }

    public boolean isValid(byte tocheck) {
      return (tocheck & mask) != 0;
    }
  }

  /**
   * SERVER REST interface using explicit address (null means all available)
   */
  private String restAddress;
  /**
   * Http REST port (SSL or not SSL)
   */
  private int restPort = -1;
  /**
   * SERVER REST interface using SSL
   */
  private boolean restSsl;
  /**
   * SERVER REST interface using time limit (default: no limit <= 0)
   */
  private long restTimeLimit = -1;
  /**
   * SERVER REST interface using authentication
   */
  private boolean restAuthenticated;
  /**
   * SERVER REST interface using signature
   */
  private boolean restSignature = true;
  /**
   * Key for signature in SHA-256
   */
  private HmacSha256 hmacSha256;
  /**
   * SERVER REST interface allowing one Handler and associated CRUD (or
   * equivalent POST, GET, PUT, DELETE)
   * methods (2^0 for active, 2^1 as Create/POST, 2^2 as Read/GET, 2^3 as
   * Update/PUT, 2^4 as Delete/DELETE)
   */
  private byte[] resthandlersCrud;

  /**
   * Associated RestMethod Handlers
   */
  public final HashMap<String, RestMethodHandler> restHashMap =
      new HashMap<String, RestMethodHandler>();

  /**
   * Set Key from String directly
   *
   * @param authentKey
   */
  public void initializeKey(String authentKey) {
    setHmacSha256(new HmacSha256());
    getHmacSha256().setSecretKey(authentKey.getBytes(WaarpStringUtils.UTF8));
  }

  /**
   * Set Key from file
   *
   * @param authentKey
   *
   * @throws CryptoException
   * @throws IOException
   */
  public void initializeKey(File authentKey)
      throws CryptoException, IOException {
    setHmacSha256(new HmacSha256());
    getHmacSha256().setSecretKey(authentKey);
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder(
        "{address: " + getRestAddress() + ", port: " + getRestPort() +
        ", ssl: " + isRestSsl() + ", time: " + getRestTimeLimit() +
        ", authent:" + isRestAuthenticated() + ", signature: " +
        isRestSignature() + ", handlers: [");
    for (final Entry<String, RestMethodHandler> elt : restHashMap.entrySet()) {
      result.append(elt.getKey()).append('=').append(elt.getValue().methods)
            .append(", ");
    }
    result.append("], crud: [");
    for (final byte crud : getResthandlersCrud()) {
      result.append(crud).append(", ");
    }
    result.append("] }");
    return result.toString();
  }

  /**
   * @return the restAddress
   */
  public String getRestAddress() {
    return restAddress;
  }

  /**
   * @param restAddress the restAddress to set
   */
  public void setRestAddress(String restAddress) {
    this.restAddress = restAddress;
  }

  /**
   * @return the restPort
   */
  public int getRestPort() {
    return restPort;
  }

  /**
   * @param restPort the restPort to set
   */
  public void setRestPort(int restPort) {
    this.restPort = restPort;
  }

  /**
   * @return the restSsl
   */
  public boolean isRestSsl() {
    return restSsl;
  }

  /**
   * @param restSsl the restSsl to set
   */
  public void setRestSsl(boolean restSsl) {
    this.restSsl = restSsl;
  }

  /**
   * @return the restTimeLimit
   */
  public long getRestTimeLimit() {
    return restTimeLimit;
  }

  /**
   * @param restTimeLimit the restTimeLimit to set
   */
  public void setRestTimeLimit(long restTimeLimit) {
    this.restTimeLimit = restTimeLimit;
  }

  /**
   * @return the restAuthenticated
   */
  public boolean isRestAuthenticated() {
    return restAuthenticated;
  }

  /**
   * @param restAuthenticated the restAuthenticated to set
   */
  public void setRestAuthenticated(boolean restAuthenticated) {
    this.restAuthenticated = restAuthenticated;
  }

  /**
   * @return the restSignature
   */
  public boolean isRestSignature() {
    return restSignature;
  }

  /**
   * @param restSignature the restSignature to set
   */
  public void setRestSignature(boolean restSignature) {
    this.restSignature = restSignature;
  }

  /**
   * @return the hmacSha256
   */
  public HmacSha256 getHmacSha256() {
    return hmacSha256;
  }

  /**
   * @param hmacSha256 the hmacSha256 to set
   */
  public void setHmacSha256(HmacSha256 hmacSha256) {
    this.hmacSha256 = hmacSha256;
  }

  /**
   * @return the resthandlersCrud
   */
  public byte[] getResthandlersCrud() {
    return resthandlersCrud;
  }

  /**
   * @param resthandlersCrud the resthandlersCrud to set
   */
  public void setResthandlersCrud(byte[] resthandlersCrud) {
    this.resthandlersCrud = resthandlersCrud;
  }
}
