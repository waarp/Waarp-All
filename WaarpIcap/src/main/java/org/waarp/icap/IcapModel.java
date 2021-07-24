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

package org.waarp.icap;

/**
 * Model parameters for ICAP
 */
public enum IcapModel {
  /**
   * Default model (usable with ClamAV)
   */
  DEFAULT_MODEL(new String[] {
      IcapScanFile.SERVICE_ARG, "avscan"
  }),
  /**
   * ICAP generic model (as default, usable with ClamAV)
   */
  ICAP_AVSCAN(new String[] {
      IcapScanFile.SERVICE_ARG, "avscan"
  }),
  /**
   * ICAP ClamAV model (less generic)
   */
  ICAP_CLAMAV(new String[] {
      IcapScanFile.SERVICE_ARG, "srv_clamav"
  }),
  /**
   * ICAP Virus_scan model (less generic)
   */
  ICAP_VIRUS_SCAN(new String[] {
      IcapScanFile.SERVICE_ARG, "virus_scan"
  });

  private final String[] defaultArgs;

  IcapModel(final String[] args) {
    defaultArgs = args;
  }

  /**
   * @return the associated default parameters
   */
  public final String[] getDefaultArgs() {
    return defaultArgs;
  }
}
