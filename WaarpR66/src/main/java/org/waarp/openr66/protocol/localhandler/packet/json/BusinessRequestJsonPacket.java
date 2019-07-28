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
 * Business Request JSON packet
 */
public class BusinessRequestJsonPacket extends JsonPacket {

  protected boolean isToApplied;
  protected String className;
  protected String arguments;
  protected String extraArguments;
  protected int delay;
  protected boolean isValidated;

  /**
   * @return the isValidated
   */
  public boolean isValidated() {
    return isValidated;
  }

  /**
   * @param isValidated the isValidated to set
   */
  public void setValidated(boolean isValidated) {
    this.isValidated = isValidated;
  }

  /**
   * @return the isToApplied
   */
  public boolean isToApplied() {
    return isToApplied;
  }

  /**
   * @param isToApplied the isToApplied to set
   */
  public void setToApplied(boolean isToApplied) {
    this.isToApplied = isToApplied;
  }

  /**
   * @return the className
   */
  public String getClassName() {
    return className;
  }

  /**
   * @param className the className to set
   */
  public void setClassName(String className) {
    this.className = className;
  }

  /**
   * @return the arguments
   */
  public String getArguments() {
    return arguments;
  }

  /**
   * @param arguments the arguments to set
   */
  public void setArguments(String arguments) {
    this.arguments = arguments;
  }

  /**
   * @return the extraArguments
   */
  public String getExtraArguments() {
    return extraArguments;
  }

  /**
   * @param extraArguments the extraArguments to set
   */
  public void setExtraArguments(String extraArguments) {
    this.extraArguments = extraArguments;
  }

  /**
   * @return the delay
   */
  public int getDelay() {
    return delay;
  }

  /**
   * @param delay the delay to set
   */
  public void setDelay(int delay) {
    this.delay = delay;
  }

  @Override
  public void setRequestUserPacket() {
    setRequestUserPacket(LocalPacketFactory.BUSINESSREQUESTPACKET);
  }
}
