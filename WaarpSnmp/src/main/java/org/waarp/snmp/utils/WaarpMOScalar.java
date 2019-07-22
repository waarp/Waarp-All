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
package org.waarp.snmp.utils;

import org.snmp4j.agent.MOAccess;
import org.snmp4j.agent.MOScope;
import org.snmp4j.agent.mo.MOScalar;
import org.snmp4j.agent.request.SubRequest;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;

/**
 * GoldenGate MOScalar implementation
 *
 *
 */
public class WaarpMOScalar extends MOScalar<Variable> {
  WaarpMORow row;

  /**
   * @param id
   * @param access
   * @param value
   */
  public WaarpMOScalar(OID id, MOAccess access, Variable value,
                       WaarpMORow row) {
    super(id, access, value);
    this.row = row;
    setVolatile(true);
  }

  /**
   * Called when a direct external access is done to this scalar. Therefore
   * this
   * function can be override to
   * host update check.
   *
   * @see org.snmp4j.agent.mo.MOScalar#get(org.snmp4j.agent.request.SubRequest)
   */
  @Override
  public void get(SubRequest request) {
    row.mib.updateServices(this);
    super.get(request);
  }

  /**
   * Called when a multiple external access is done to this scalar and the
   * following. Therefore this function
   * can be override to host update check.
   *
   * @see org.snmp4j.agent.mo.MOScalar#find(org.snmp4j.agent.MOScope)
   */
  @Override
  public OID find(MOScope range) {
    row.mib.updateServices(range);
    return super.find(range);
  }

}
