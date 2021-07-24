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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.PartnerConfiguration;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;

import java.util.Date;

/**
 * Transfer request JSON packet
 */
@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.ANY, setterVisibility = Visibility.ANY)
public class TransferRequestJsonPacket extends JsonPacket {

  protected static final byte REQVALIDATE = 0;

  protected static final byte REQANSWERVALIDATE = 1;

  protected String rulename;

  protected int mode;

  protected String filename;

  protected String requested;

  protected int blocksize;

  protected int rank;

  protected long specialId;

  protected byte validate;

  protected long originalSize;

  protected String fileInformation = "";

  protected String separator = PartnerConfiguration.BAR_JSON_FIELD;

  protected Date start;

  protected boolean isAdditionalDelay;

  protected long delay;

  /**
   * @return the isAdditionalDelay
   */
  public final boolean isAdditionalDelay() {
    return isAdditionalDelay;
  }

  /**
   * @param isAdditionalDelay the isAdditionalDelay to set
   */
  public final void setAdditionalDelay(final boolean isAdditionalDelay) {
    this.isAdditionalDelay = isAdditionalDelay;
  }

  /**
   * @return the delay
   */
  public final long getDelay() {
    return delay;
  }

  /**
   * @param delay the delay to set
   */
  public final void setDelay(final long delay) {
    this.delay = delay;
  }

  /**
   * @return the start
   */
  public final Date getStart() {
    return start;
  }

  /**
   * @param start the start to set
   */
  public final void setStart(final Date start) {
    this.start = start;
  }

  /**
   * @return the requested
   */
  public final String getRequested() {
    return requested;
  }

  /**
   * @param requested the requested to set
   */
  public final void setRequested(final String requested) {
    this.requested = requested;
  }

  /**
   * @return the rulename
   */
  public final String getRulename() {
    return rulename;
  }

  /**
   * @param rulename the rulename to set
   */
  public final void setRulename(final String rulename) {
    this.rulename = rulename;
  }

  /**
   * @return the mode
   */
  public final int getMode() {
    return mode;
  }

  /**
   * @param mode the mode to set
   */
  public final void setMode(final int mode) {
    this.mode = mode;
  }

  /**
   * @return the filename
   */
  public final String getFilename() {
    return filename;
  }

  /**
   * @param filename the filename to set
   */
  public final void setFilename(final String filename) {
    this.filename = filename;
  }

  /**
   * @return the blocksize
   */
  public final int getBlocksize() {
    return blocksize;
  }

  /**
   * @param blocksize the blocksize to set
   */
  public final void setBlocksize(final int blocksize) {
    this.blocksize = blocksize;
  }

  /**
   * @return the rank
   */
  public final int getRank() {
    return rank;
  }

  /**
   * @param rank the rank to set
   */
  public final void setRank(final int rank) {
    this.rank = rank;
  }

  /**
   * @return the specialId
   */
  public final long getSpecialId() {
    return specialId;
  }

  /**
   * @param specialId the specialId to set
   */
  public final void setSpecialId(final long specialId) {
    this.specialId = specialId;
  }

  /**
   * @return the validate
   */
  public final byte getValidate() {
    return validate;
  }

  /**
   * @return True if is to validate
   */
  public final boolean isToValidate() {
    return validate == REQVALIDATE;
  }

  /**
   * @param validate the validate to set
   */
  public final void setValidate(final byte validate) {
    this.validate = validate;
  }

  /**
   * Validate the request
   */
  public final void validate() {
    validate = REQANSWERVALIDATE;
  }

  /**
   * @return the originalSize
   */
  public final long getOriginalSize() {
    return originalSize;
  }

  /**
   * @param originalSize the originalSize to set
   */
  public final void setOriginalSize(final long originalSize) {
    this.originalSize = originalSize;
  }

  /**
   * @return the fileInformation
   */
  public final String getFileInformation() {
    return fileInformation;
  }

  /**
   * @param fileInformation the fileInformation to set
   */
  public final void setFileInformation(String fileInformation) {
    if (fileInformation == null) {
      fileInformation = "";
    }
    this.fileInformation = fileInformation;
  }

  /**
   * @return the separator
   */
  public final String getSeparator() {
    return separator;
  }

  /**
   * @param separator the separator to set
   */
  public final void setSeparator(final String separator) {
    this.separator = separator;
  }

  /**
   * Update the JsonPacket from runner (blocksize, rank, specialid)
   *
   * @param runner
   */
  @JsonIgnore
  public final void setFromDbTaskRunner(final DbTaskRunner runner) {
    blocksize = runner.getBlocksize();
    rank = runner.getRank();
    specialId = runner.getSpecialId();
    originalSize = runner.getOriginalSize();
  }

  @Override
  public final void setRequestUserPacket() {
    setRequestUserPacket(LocalPacketFactory.REQUESTPACKET);
  }
}
