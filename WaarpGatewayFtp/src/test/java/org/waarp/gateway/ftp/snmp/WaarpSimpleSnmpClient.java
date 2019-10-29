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
package org.waarp.gateway.ftp.snmp;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TableEvent;
import org.snmp4j.util.TableUtils;
import org.waarp.common.logging.SysErrLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple SNMP Client (for testing purpose)
 */
public class WaarpSimpleSnmpClient {

  private final String address;

  private final int port;

  private Snmp snmp;

  /**
   * @param clientAddress
   * @param port
   */
  public WaarpSimpleSnmpClient(String clientAddress, int port) {
    address = clientAddress;
    this.port = port;
    try {
      start();
    } catch (final IOException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Start the Snmp session. If you forget the listen() method you will not
   * get any answers because the
   * communication is asynchronous and the listen() method listens for
   * answers.
   *
   * @throws IOException
   **/
  private void start() throws IOException {
    final UdpAddress upaddress = new UdpAddress(port);
    System.err.println("Listen: " + upaddress);
    final TransportMapping<UdpAddress> transport =
        new DefaultUdpTransportMapping(upaddress);
    snmp = new Snmp(transport);
    // Do not forget this line!
    transport.listen();
  }

  public static String extractSingleString(ResponseEvent event) {
    return event.getResponse().get(0).getVariable().toString();
  }

  // Since snmp4j relies on asynch req/resp we need a listener
  // for responses which should be closed
  public void stop() throws IOException {
    snmp.close();
  }

  /**
   * Method which takes a single OID and returns the response from the agent
   * as a String.
   *
   * @param oid
   *
   * @return String
   *
   * @throws IOException
   **/
  public String getAsString(OID oid) throws IOException {
    final ResponseEvent event = get(new OID[] { oid });
    return event.getResponse().get(0).getVariable().toString();
  }

  /**
   * This method is capable of handling multiple OIDs
   *
   * @param oids
   *
   * @return ResponseEvent
   *
   * @throws IOException
   **/
  public ResponseEvent get(OID oids[]) throws IOException {
    final ResponseEvent event = snmp.send(getPDU(oids), getTarget(), null);
    if (event != null) {
      return event;
    }
    throw new RuntimeException("GET timed out");
  }

  /**
   * @param oids
   *
   * @return A PDU from oids
   */
  private PDU getPDU(OID oids[]) {
    final PDU pdu = new PDU();
    for (final OID oid : oids) {
      pdu.add(new VariableBinding(oid));
    }

    pdu.setType(PDU.GET);
    return pdu;
  }

  /**
   * This method returns a Target, which contains information about where the
   * data should be fetched and how.
   *
   * @return
   **/
  private Target getTarget() {
    final Address targetAddress = GenericAddress.parse(address);
    final CommunityTarget target = new CommunityTarget();
    target.setCommunity(new OctetString("public"));
    target.setAddress(targetAddress);
    target.setRetries(2);
    target.setTimeout(1500);
    target.setVersion(SnmpConstants.version2c);
    return target;
  }

  /**
   * This method is capable of handling multiple OIDs linked with a listener
   *
   * @param oids
   * @param listener
   *
   * @throws IOException
   **/
  public void getAsString(OID oids, ResponseListener listener) {
    try {
      snmp.send(getPDU(new OID[] { oids }), getTarget(), null, listener);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Normally this would return domain objects or something else than this...
   */
  public List<List<String>> getTableAsStrings(OID[] oids) {
    final TableUtils tUtils = new TableUtils(snmp, new DefaultPDUFactory());

    final List<TableEvent> events =
        tUtils.getTable(getTarget(), oids, null, null);

    final List<List<String>> list = new ArrayList<List<String>>();
    for (final TableEvent event : events) {
      if (event.isError()) {
        System.err.println(event);
        continue;
        // throw new
        // RuntimeException(event.getErrorMessage(),event.getException());
      }
      final List<String> strList = new ArrayList<String>();
      list.add(strList);
      for (final VariableBinding vb : event.getColumns()) {
        strList.add(vb.getVariable().toString());
      }
    }
    return list;
  }

}
