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
 * Import Configuration response JSON packet
 */
public class ConfigImportResponseJsonPacket extends ConfigImportJsonPacket {

  protected byte command;
  protected boolean purgedhost;
  protected boolean purgedrule;
  protected boolean purgedbusiness;
  protected boolean purgedalias;
  protected boolean purgedroles;
  protected boolean importedhost;
  protected boolean importedrule;
  protected boolean importedbusiness;
  protected boolean importedalias;
  protected boolean importedroles;

  /**
   * @return the command
   */
  public byte getCommand() {
    return command;
  }

  /**
   * @param command the command to set
   */
  public void setCommand(final byte command) {
    this.command = command;
  }

  /**
   * @return the purgedhost
   */
  public boolean isPurgedhost() {
    return purgedhost;
  }

  /**
   * @param purgedhost the purgedhost to set
   */
  public void setPurgedhost(final boolean purgedhost) {
    this.purgedhost = purgedhost;
  }

  /**
   * @return the purgedrule
   */
  public boolean isPurgedrule() {
    return purgedrule;
  }

  /**
   * @param purgedrule the purgedrule to set
   */
  public void setPurgedrule(final boolean purgedrule) {
    this.purgedrule = purgedrule;
  }

  /**
   * @return the purgedbusiness
   */
  public boolean isPurgedbusiness() {
    return purgedbusiness;
  }

  /**
   * @param purgedbusiness the purgedbusiness to set
   */
  public void setPurgedbusiness(final boolean purgedbusiness) {
    this.purgedbusiness = purgedbusiness;
  }

  /**
   * @return the purgedalias
   */
  public boolean isPurgedalias() {
    return purgedalias;
  }

  /**
   * @param purgedalias the purgedalias to set
   */
  public void setPurgedalias(final boolean purgedalias) {
    this.purgedalias = purgedalias;
  }

  /**
   * @return the purgedroles
   */
  public boolean isPurgedroles() {
    return purgedroles;
  }

  /**
   * @param purgedroles the purgedroles to set
   */
  public void setPurgedroles(final boolean purgedroles) {
    this.purgedroles = purgedroles;
  }

  /**
   * @return the importedhost
   */
  public boolean isImportedhost() {
    return importedhost;
  }

  /**
   * @param importedhost the importedhost to set
   */
  public void setImportedhost(final boolean importedhost) {
    this.importedhost = importedhost;
  }

  /**
   * @return the importedrule
   */
  public boolean isImportedrule() {
    return importedrule;
  }

  /**
   * @param importedrule the importedrule to set
   */
  public void setImportedrule(final boolean importedrule) {
    this.importedrule = importedrule;
  }

  /**
   * @return the importedbusiness
   */
  public boolean isImportedbusiness() {
    return importedbusiness;
  }

  /**
   * @param importedbusiness the importedbusiness to set
   */
  public void setImportedbusiness(final boolean importedbusiness) {
    this.importedbusiness = importedbusiness;
  }

  /**
   * @return the importedalias
   */
  public boolean isImportedalias() {
    return importedalias;
  }

  /**
   * @param importedalias the importedalias to set
   */
  public void setImportedalias(final boolean importedalias) {
    this.importedalias = importedalias;
  }

  /**
   * @return the importedroles
   */
  public boolean isImportedroles() {
    return importedroles;
  }

  /**
   * @param importedroles the importedroles to set
   */
  public void setImportedroles(final boolean importedroles) {
    this.importedroles = importedroles;
  }

  @Override
  public void setRequestUserPacket() {
    setRequestUserPacket(LocalPacketFactory.CONFIMPORTPACKET);
  }
}
