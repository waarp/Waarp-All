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

package org.waarp.openr66.protocol.http.restv2.converters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.openr66.pojo.Host;
import org.waarp.openr66.protocol.http.restv2.errors.RestError;
import org.waarp.openr66.protocol.http.restv2.errors.RestErrorException;

import javax.ws.rs.InternalServerErrorException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.waarp.common.file.FileUtils.*;
import static org.waarp.openr66.protocol.configuration.Configuration.*;
import static org.waarp.openr66.protocol.http.restv2.RestConstants.HostFields.*;
import static org.waarp.openr66.protocol.http.restv2.errors.RestErrors.*;

/**
 * A collection of utility methods to convert {@link Host} objects to their
 * corresponding {@link ObjectNode}
 * and vice-versa.
 */
public final class HostConverter {

  /**
   * Makes the default constructor of this utility class inaccessible.
   */
  private HostConverter() throws InstantiationException {
    throw new InstantiationException(
        getClass().getName() + " cannot be instantiated.");
  }

  // ########################### INNER CLASSES ################################

  /**
   * Represents all the possible ways to sort a list of host objects.
   */
  public enum Order {
    /**
     * By hostID, in ascending order.
     */
    ascId(new Comparator<Host>() {
      @Override
      public int compare(final Host t1, final Host t2) {
        return t1.getHostid().compareTo(t2.getHostid());
      }
    }),
    /**
     * By hostID, in descending order.
     */
    descId(new Comparator<Host>() {
      @Override
      public int compare(final Host t1, final Host t2) {
        return -t1.getHostid().compareTo(t2.getHostid());//NOSONAR
      }
    }),
    /**
     * By address, in ascending order.
     */
    ascAddress(new Comparator<Host>() {
      @Override
      public int compare(final Host t1, final Host t2) {
        return t1.getAddress().compareTo(t2.getAddress());//NOSONAR
      }
    }),
    /**
     * By address, in descending order.
     */
    descAddress(new Comparator<Host>() {
      @Override
      public int compare(final Host t1, final Host t2) {
        return -t1.getAddress().compareTo(t2.getAddress());//NOSONAR
      }
    });

    /**
     * The comparator used to sort the list of RestHost objects.
     */
    public final Comparator<Host> comparator;

    Order(final Comparator<Host> comparator) {
      this.comparator = comparator;
    }
  }

  // ########################### PUBLIC METHODS ###############################

  /**
   * Converts the given {@link Host} object into an {@link ObjectNode}.
   *
   * @param host the host to convert
   *
   * @return the converted ObjectNode
   */
  public static ObjectNode hostToNode(final Host host) {
    final ObjectNode node = JsonHandler.createObjectNode();
    node.put(HOST_NAME, host.getHostid());
    node.put(ADDRESS, host.getAddress());
    node.put(PORT, host.getPort());
    node.put(PASSWORD, host.getHostkey());
    node.put(IS_SSL, host.isSSL());
    node.put(IS_CLIENT, host.isClient());
    node.put(IS_ADMIN, host.isAdmin());
    node.put(IS_ACTIVE, host.isActive());
    node.put(IS_PROXY, host.isProxified());

    return node;
  }

  /**
   * Converts the given {@link ObjectNode} into a {@link Host} object.
   *
   * @param object the ObjectNode to convert
   *
   * @return the corresponding Host object
   *
   * @throws RestErrorException if the given ObjectNode does not
   *     represent a Host object
   * @throws InternalServerErrorException if an unexpected error
   *     occurred
   */
  public static Host nodeToNewHost(final ObjectNode object) {
    Host emptyHost = null;
    try {
      emptyHost =
          new Host(null, null, -1, null, false, false, false, false, true);
    } catch (WaarpDatabaseSqlException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
    }

    return nodeToUpdatedHost(object, emptyHost);
  }

  /**
   * Returns the given {@link Host} object updated with the values defined in
   * the {@link ObjectNode} parameter.
   * All fields missing in the JSON object will stay unchanged in the updated
   * host entry.
   *
   * @param object the ObjectNode to convert.
   * @param oldHost the host entry to update.
   *
   * @return the updated host entry
   *
   * @throws RestErrorException if the given ObjectNode does not
   *     represent a Host object
   * @throws InternalServerErrorException if an unexpected error
   *     occurred
   */
  public static Host nodeToUpdatedHost(final ObjectNode object,
                                       final Host oldHost) {

    final List<RestError> errors = new ArrayList<RestError>();

    final Iterator<Map.Entry<String, JsonNode>> fields = object.fields();
    while (fields.hasNext()) {
      final Map.Entry<String, JsonNode> field = fields.next();
      final String name = field.getKey();
      final JsonNode value = field.getValue();

      if (name.equalsIgnoreCase(HOST_NAME)) {
        if (value.isTextual()) {
          if (oldHost.getHostid() == null) {
            oldHost.setHostid(value.asText());
          } else if (!oldHost.getHostid().equals(value.asText())) {
            errors.add(FIELD_NOT_ALLOWED(HOST_NAME));
          }
        } else {
          errors.add(ILLEGAL_FIELD_VALUE(name, value.toString()));
        }
      } else if (name.equalsIgnoreCase(ADDRESS)) {
        if (value.isTextual()) {
          oldHost.setAddress(value.asText());
        } else {
          errors.add(ILLEGAL_FIELD_VALUE(name, value.toString()));
        }
      } else if (name.equalsIgnoreCase(PORT)) {
        if (value.canConvertToInt() && value.asInt() >= 0 &&
            value.asInt() < ZERO_COPY_CHUNK_SIZE) {
          oldHost.setPort(value.asInt());
        } else {
          errors.add(ILLEGAL_FIELD_VALUE(name, value.toString()));
        }
      } else if (name.equalsIgnoreCase(PASSWORD)) {
        if (value.isTextual()) {
          oldHost.setHostkey(encryptPassword(value.asText()));
        } else {
          errors.add(ILLEGAL_FIELD_VALUE(name, value.toString()));
        }
      } else if (name.equalsIgnoreCase(IS_SSL)) {
        if (value.isBoolean()) {
          oldHost.setSSL(value.asBoolean());
        } else {
          errors.add(ILLEGAL_FIELD_VALUE(name, value.toString()));
        }
      } else if (name.equalsIgnoreCase(IS_CLIENT)) {
        if (value.isBoolean()) {
          oldHost.setClient(value.asBoolean());
        } else {
          errors.add(ILLEGAL_FIELD_VALUE(name, value.toString()));
        }
      } else if (name.equalsIgnoreCase(IS_ADMIN)) {
        if (value.isBoolean()) {
          oldHost.setAdmin(value.asBoolean());
        } else {
          errors.add(ILLEGAL_FIELD_VALUE(name, value.toString()));
        }
      } else if (name.equalsIgnoreCase(IS_ACTIVE)) {
        if (value.isBoolean()) {
          oldHost.setActive(value.asBoolean());
        } else {
          errors.add(ILLEGAL_FIELD_VALUE(name, value.toString()));
        }
      } else if (name.equalsIgnoreCase(IS_PROXY)) {
        if (value.isBoolean()) {
          oldHost.setProxified(value.asBoolean());
        } else {
          errors.add(ILLEGAL_FIELD_VALUE(name, value.toString()));
        }
      } else {
        errors.add(UNKNOWN_FIELD(name));
      }
    }

    errors.addAll(checkRequiredFields(oldHost));

    if (errors.isEmpty()) {
      return oldHost;
    } else {
      throw new RestErrorException(errors);
    }
  }

  // ########################## PRIVATE METHODS ###############################

  /**
   * Encrypts the given password String using the server's cryptographic key.
   *
   * @param password the password to encrypt
   *
   * @return the encrypted password
   *
   * @throws InternalServerErrorException If an error occurred when
   *     encrypting the password.
   */
  private static byte[] encryptPassword(final String password) {
    try {
      return configuration.getCryptoKey().cryptToHex(password).getBytes();
    } catch (final Exception e) {
      throw new InternalServerErrorException(
          "Failed to encrypt the host password", e);
    }
  }

  /**
   * List all missing required fields. This method returns a list of {@link
   * RestError} representing all the
   * errors encountered when checking the given host's required fields. If all
   * required fields have indeed been
   * initialized, an empty list is returned.
   *
   * @param host the host entry to check
   *
   * @return the list of encountered errors
   */
  private static List<RestError> checkRequiredFields(final Host host) {
    final List<RestError> errors = new ArrayList<RestError>();
    if (ParametersChecker.isEmpty(host.getHostid())) {
      errors.add(MISSING_FIELD(HOST_NAME));
    }
    if (ParametersChecker.isEmpty(host.getAddress())) {
      errors.add(MISSING_FIELD(ADDRESS));
    }
    if (host.getPort() == -1) {
      errors.add(MISSING_FIELD(PORT));
    }
    if (host.getHostkey() == null || host.getHostkey().length == 0) {
      errors.add(MISSING_FIELD(PASSWORD));
    }

    return errors;
  }
}
