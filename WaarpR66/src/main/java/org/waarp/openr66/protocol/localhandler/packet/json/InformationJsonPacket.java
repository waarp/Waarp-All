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
package org.waarp.openr66.protocol.localhandler.packet.json;

import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;

/**
 * Information (on request or on filesystem) JSON packet
 */
public class InformationJsonPacket extends JsonPacket {

  protected boolean isIdRequest;
  protected long id;
  protected boolean isTo;
  protected byte request;
  protected String rulename;
  protected String filename;

  /**
   * Empty constructor
   */
  public InformationJsonPacket() {

  }

  /**
   * Constructor for Transfer Request Information
   *
   * @param id
   * @param isTo
   * @param remoteHost
   */
  public InformationJsonPacket(long id, boolean isTo, String remoteHost) {
    setId(id);
    setTo(isTo);
    setRulename(remoteHost);
    setIdRequest(true);
    setRequestUserPacket();
  }

  /**
   * Constructor for File information
   *
   * @param request InformationPacket.ASKENUM ordinal (converted to
   *     byte)
   * @param rulename
   * @param filename
   */
  public InformationJsonPacket(byte request, String rulename, String filename) {
    setRequest(request);
    setFilename(filename);
    setRulename(rulename);
    setIdRequest(false);
    setRequestUserPacket();
  }

  /**
   * @return the isIdRequest
   */
  public boolean isIdRequest() {
    return isIdRequest;
  }

  /**
   * @param isIdRequest the isIdRequest to True for Transfer Request,
   *     else
   *     for File listing
   */
  public void setIdRequest(boolean isIdRequest) {
    this.isIdRequest = isIdRequest;
  }

  /**
   * @return the id
   */
  public long getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(long id) {
    this.id = id;
  }

  /**
   * @return the isTo for Transfer, determine the way of the transfer as
   *     requester/requested (isTo true/false)
   */
  public boolean isTo() {
    return isTo;
  }

  /**
   * @param isTo the isTo to set
   */
  public void setTo(boolean isTo) {
    this.isTo = isTo;
  }

  /**
   * @return the request
   */
  public byte getRequest() {
    return request;
  }

  /**
   * @param request the request to set
   */
  public void setRequest(byte request) {
    this.request = request;
  }

  /**
   * @return the rulename
   */
  public String getRulename() {
    return rulename;
  }

  /**
   * @param rulename the rulename to set (if Transfer and Json
   *     requester/requested (isTo true/false))
   */
  public void setRulename(String rulename) {
    this.rulename = rulename;
  }

  /**
   * @return the filename
   */
  public String getFilename() {
    return filename;
  }

  /**
   * @param filename the filename to set
   */
  public void setFilename(String filename) {
    this.filename = filename;
  }

  @Override
  public void setRequestUserPacket() {
    setRequestUserPacket(LocalPacketFactory.INFORMATIONPACKET);
  }
}
