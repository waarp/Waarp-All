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
package org.waarp.snmp;

import org.snmp4j.agent.MOAccess;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.smi.Counter32;
import org.snmp4j.smi.Counter64;
import org.snmp4j.smi.Gauge32;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Opaque;
import org.snmp4j.smi.SMIConstants;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.Variable;
import org.waarp.snmp.interf.WaarpInterfaceVariableFactory;
import org.waarp.snmp.utils.WaarpDefaultVariableFactory;
import org.waarp.snmp.utils.WaarpMORow;
import org.waarp.snmp.utils.WaarpMOScalar;

/**
 * This class creates and returns ManagedObjects
 */
public final class WaarpMOFactory {
  /**
   * To be setup to default Factory to be used or kept as null for default one
   */
  private static WaarpInterfaceVariableFactory factory;

  /**
   * Default one
   */
  private static final WaarpInterfaceVariableFactory defaultFactory =
      new WaarpDefaultVariableFactory();

  private WaarpMOFactory() {
  }

  /**
   * @param oid
   * @param value
   * @param type
   *
   * @return an MOScalar according to the argument
   */
  public static WaarpMOScalar createReadOnly(final OID oid, final Object value,
                                             final int type,
                                             final WaarpMORow row,
                                             final int mibLevel,
                                             final int entry) {
    return new WaarpMOScalar(oid, MOAccessImpl.ACCESS_READ_ONLY,
                             getVariable(oid, value, type, mibLevel, entry),
                             row);
  }

  /**
   * @param oid
   * @param value
   * @param type
   * @param access
   *
   * @return an MOScalar according to the argument
   */
  public static WaarpMOScalar create(final OID oid, final Object value,
                                     final int type, final MOAccess access,
                                     final WaarpMORow row, final int mibLevel,
                                     final int entry) {
    return new WaarpMOScalar(oid, access,
                             getVariable(oid, value, type, mibLevel, entry),
                             row);
  }

  /**
   * Create a Variable using the arguments
   *
   * @param oid
   * @param value
   * @param type
   * @param mibLevel
   * @param entry
   *
   * @return a Variable using the arguments
   */
  public static Variable getVariable(final OID oid, final Object value,
                                     final int type, final int mibLevel,
                                     final int entry) {
    final Variable var;
    final WaarpInterfaceVariableFactory vf;
    if (getFactory() == null) {
      vf = defaultFactory;
    } else {
      vf = getFactory();
    }
    var = vf.getVariable(oid, type, mibLevel, entry);
    if (value != null) {
      switch (type) {
        case SMIConstants.SYNTAX_INTEGER:
          // case SMIConstants.SYNTAX_INTEGER32:
          ((Integer32) var).setValue((Integer) value);
          break;
        case SMIConstants.SYNTAX_OCTET_STRING:
          // case SMIConstants.SYNTAX_BITS:
          ((OctetString) var).setValue(value.toString());
          break;
        case SMIConstants.SYNTAX_NULL:
          break;
        case SMIConstants.SYNTAX_OBJECT_IDENTIFIER:
          ((OID) var).setValue(value.toString());
          break;
        case SMIConstants.SYNTAX_IPADDRESS:
          ((IpAddress) var).setValue(value.toString());
          break;
        case SMIConstants.SYNTAX_COUNTER32:
          ((Counter32) var).setValue((Long) value);
          break;
        case SMIConstants.SYNTAX_GAUGE32:
          // case SMIConstants.SYNTAX_UNSIGNED_INTEGER32:
          ((Gauge32) var).setValue((Long) value);
          break;
        case SMIConstants.SYNTAX_TIMETICKS:
          if (value instanceof TimeTicks) {
            ((TimeTicks) var).setValue(value.toString());
          } else {
            ((TimeTicks) var).setValue((Long) value);
          }
          break;
        case SMIConstants.SYNTAX_OPAQUE:
          ((Opaque) var).setValue((byte[]) value);
          break;
        case SMIConstants.SYNTAX_COUNTER64:
          ((Counter64) var).setValue((Long) value);
          break;
        default:
          throw new IllegalArgumentException(
              "Unmanaged Type: " + value.getClass());
      }
    }
    return var;
  }

  /**
   * Set a Variable value
   *
   * @param var
   * @param value
   * @param type
   */
  public static void setVariable(final Variable var, final Object value,
                                 final int type) {
    if (value != null) {
      switch (type) {
        case SMIConstants.SYNTAX_INTEGER:
          // case SMIConstants.SYNTAX_INTEGER32:
          ((Integer32) var).setValue((Integer) value);
          break;
        case SMIConstants.SYNTAX_OCTET_STRING:
          // case SMIConstants.SYNTAX_BITS:
          ((OctetString) var).setValue(value.toString());
          break;
        case SMIConstants.SYNTAX_NULL:
          break;
        case SMIConstants.SYNTAX_OBJECT_IDENTIFIER:
          ((OID) var).setValue(value.toString());
          break;
        case SMIConstants.SYNTAX_IPADDRESS:
          ((IpAddress) var).setValue(value.toString());
          break;
        case SMIConstants.SYNTAX_COUNTER32:
          ((Counter32) var).setValue((Long) value);
          break;
        case SMIConstants.SYNTAX_GAUGE32:
          // case SMIConstants.SYNTAX_UNSIGNED_INTEGER32:
          ((Gauge32) var).setValue((Long) value);
          break;
        case SMIConstants.SYNTAX_TIMETICKS:
          if (value instanceof TimeTicks) {
            ((TimeTicks) var).setValue(value.toString());
          } else {
            ((TimeTicks) var).setValue((Long) value);
          }
          break;
        case SMIConstants.SYNTAX_OPAQUE:
          ((Opaque) var).setValue((byte[]) value);
          break;
        case SMIConstants.SYNTAX_COUNTER64:
          ((Counter64) var).setValue((Long) value);
          break;
        default:
          throw new IllegalArgumentException(
              "Unmanaged Type: " + value.getClass());
      }
    }
  }

  /**
   * @return the factory
   */
  public static WaarpInterfaceVariableFactory getFactory() {
    return factory;
  }

  /**
   * @param factory the factory to set
   */
  public static void setFactory(final WaarpInterfaceVariableFactory factory) {
    WaarpMOFactory.factory = factory;
  }
}
