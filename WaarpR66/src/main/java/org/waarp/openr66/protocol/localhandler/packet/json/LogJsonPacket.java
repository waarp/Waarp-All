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

import java.util.Date;

/**
 * Export Log JSON packet
 */
public class LogJsonPacket extends JsonPacket {
  protected boolean purge;
  protected boolean clean;
  protected boolean statuspending;
  protected boolean statustransfer;
  protected boolean statusdone;
  protected boolean statuserror;
  protected String rule, request;
  protected Date start;
  protected Date stop;
  protected String startid;
  protected String stopid;

  /**
   * @return the purge
   */
  public boolean isPurge() {
    return purge;
  }

  /**
   * @param purge the purge to set
   */
  public void setPurge(boolean purge) {
    this.purge = purge;
  }

  /**
   * @return the clean
   */
  public boolean isClean() {
    return clean;
  }

  /**
   * @param clean the clean to set
   */
  public void setClean(boolean clean) {
    this.clean = clean;
  }

  /**
   * @return the statuspending
   */
  public boolean isStatuspending() {
    return statuspending;
  }

  /**
   * @param statuspending the statuspending to set
   */
  public void setStatuspending(boolean statuspending) {
    this.statuspending = statuspending;
  }

  /**
   * @return the statustransfer
   */
  public boolean isStatustransfer() {
    return statustransfer;
  }

  /**
   * @param statustransfer the statustransfer to set
   */
  public void setStatustransfer(boolean statustransfer) {
    this.statustransfer = statustransfer;
  }

  /**
   * @return the statusdone
   */
  public boolean isStatusdone() {
    return statusdone;
  }

  /**
   * @param statusdone the statusdone to set
   */
  public void setStatusdone(boolean statusdone) {
    this.statusdone = statusdone;
  }

  /**
   * @return the statuserror
   */
  public boolean isStatuserror() {
    return statuserror;
  }

  /**
   * @param statuserror the statuserror to set
   */
  public void setStatuserror(boolean statuserror) {
    this.statuserror = statuserror;
  }

  /**
   * @return the rule
   */
  public String getRule() {
    return rule;
  }

  /**
   * @param rule the rule to set
   */
  public void setRule(String rule) {
    this.rule = rule;
  }

  /**
   * @return the request
   */
  public String getRequest() {
    return request;
  }

  /**
   * @param request the request to set
   */
  public void setRequest(String request) {
    this.request = request;
  }

  /**
   * @return the start
   */
  public Date getStart() {
    return start;
  }

  /**
   * @param start the start to set
   */
  public void setStart(Date start) {
    this.start = start;
  }

  /**
   * @return the stop
   */
  public Date getStop() {
    return stop;
  }

  /**
   * @param stop the stop to set
   */
  public void setStop(Date stop) {
    this.stop = stop;
  }

  /**
   * @return the startid
   */
  public String getStartid() {
    return startid;
  }

  /**
   * @param startid the startid to set
   */
  public void setStartid(String startid) {
    this.startid = startid;
  }

  /**
   * @return the stopid
   */
  public String getStopid() {
    return stopid;
  }

  /**
   * @param stopid the stopid to set
   */
  public void setStopid(String stopid) {
    this.stopid = stopid;
  }

  @Override
  public void fromJson(JsonPacket other) {
    super.fromJson(other);
    if (other instanceof LogJsonPacket) {
      final LogJsonPacket other2 = (LogJsonPacket) other;
      purge = other2.purge;
      clean = other2.clean;
      statuspending = other2.statuspending;
      statustransfer = other2.statustransfer;
      statusdone = other2.statusdone;
      statuserror = other2.statuserror;
      rule = other2.rule;
      request = other2.request;
      start = other2.start;
      stop = other2.stop;
      startid = other2.startid;
      stopid = other2.stopid;
    }
  }

  @Override
  public void setRequestUserPacket() {
    setRequestUserPacket(LocalPacketFactory.LOGPACKET);
  }
}
