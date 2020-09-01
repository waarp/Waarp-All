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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.waarp.openr66.pojo.Limit;
import org.waarp.openr66.protocol.http.restv2.errors.RestError;
import org.waarp.openr66.protocol.http.restv2.errors.RestErrorException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.waarp.openr66.protocol.http.restv2.RestConstants.LimitsFields.*;
import static org.waarp.openr66.protocol.http.restv2.RestConstants.*;
import static org.waarp.openr66.protocol.http.restv2.errors.RestErrors.*;

/**
 * A collection of utility methods to convert {@link Limit} objects to their
 * corresponding {@link ObjectNode}
 * and vice-versa.
 */
public final class LimitsConverter {

  /**
   * Makes the default constructor of this utility class inaccessible.
   */
  private LimitsConverter() throws InstantiationException {
    throw new InstantiationException(
        getClass().getName() + " cannot be instantiated.");
  }

  // ########################### PUBLIC METHODS ###############################

  /**
   * Converts the given {@link Limit} object into an {@link ObjectNode}.
   *
   * @param limits the limits object to convert
   *
   * @return the converted ObjectNode
   */
  public static ObjectNode limitToNode(final Limit limits) {
    final ObjectNode node = new ObjectNode(JsonNodeFactory.instance);
    node.put(READ_GLOBAL_LIMIT, limits.getReadGlobalLimit());
    node.put(WRITE_GLOBAL_LIMIT, limits.getWriteGlobalLimit());
    node.put(READ_SESSION_LIMIT, limits.getReadSessionLimit());
    node.put(WRITE_SESSION_LIMIT, limits.getWriteSessionLimit());
    node.put(DELAY_LIMIT, limits.getDelayLimit());

    return node;
  }

  /**
   * Converts the given {@link ObjectNode} into a {@link Limit} object.
   *
   * @param object the ObjectNode to convert
   *
   * @return the corresponding Limit object
   *
   * @throws RestErrorException if the given ObjectNode does not
   *     represent a Limit object
   */
  public static Limit nodeToNewLimit(final ObjectNode object) {
    final Limit emptyLimits = new Limit(serverName(), 0, 0, 0, 0, 0);
    return nodeToUpdatedLimit(object, emptyLimits);
  }

  /**
   * Returns the given {@link Limit} object updated with the values defined in
   * the {@link ObjectNode} parameter.
   * All fields missing in the JSON object will stay unchanged in the updated
   * limit object.
   *
   * @param object the ObjectNode to convert
   * @param oldLimits the Limit object to update
   *
   * @return the updated Limit object
   *
   * @throws RestErrorException if the given ObjectNode does not
   *     represent a Limit object
   */
  public static Limit nodeToUpdatedLimit(final ObjectNode object,
                                         final Limit oldLimits) {
    final List<RestError> errors = new ArrayList<RestError>();

    while (object.fields().hasNext()) {
      final Map.Entry<String, JsonNode> field = object.fields().next();
      final String name = field.getKey();
      final JsonNode value = field.getValue();

      if (name.equalsIgnoreCase(READ_GLOBAL_LIMIT)) {
        if (value.canConvertToLong() && value.asLong() >= 0) {
          oldLimits.setReadGlobalLimit(value.asLong());
        } else {
          errors.add(ILLEGAL_PARAMETER_VALUE(name, value.toString()));
        }
      } else if (name.equalsIgnoreCase(WRITE_GLOBAL_LIMIT)) {
        if (value.canConvertToLong() && value.asLong() >= 0) {
          oldLimits.setWriteGlobalLimit(value.asLong());
        } else {
          errors.add(ILLEGAL_PARAMETER_VALUE(name, value.toString()));
        }
      } else if (name.equalsIgnoreCase(READ_SESSION_LIMIT)) {
        if (value.canConvertToLong() && value.asLong() >= 0) {
          oldLimits.setReadSessionLimit(value.asLong());
        } else {
          errors.add(ILLEGAL_PARAMETER_VALUE(name, value.toString()));
        }
      } else if (name.equalsIgnoreCase(WRITE_SESSION_LIMIT)) {
        if (value.canConvertToLong() && value.asLong() >= 0) {
          oldLimits.setWriteSessionLimit(value.asLong());
        } else {
          errors.add(ILLEGAL_PARAMETER_VALUE(name, value.toString()));
        }
      } else if (name.equalsIgnoreCase(DELAY_LIMIT)) {
        if (value.canConvertToLong() && value.asLong() >= 0) {
          oldLimits.setDelayLimit(value.asLong());
        } else {
          errors.add(ILLEGAL_PARAMETER_VALUE(name, value.toString()));
        }
      } else {
        errors.add(UNKNOWN_FIELD(name));
      }
    }

    if (errors.isEmpty()) {
      return oldLimits;
    } else {
      throw new RestErrorException(errors);
    }
  }
}
