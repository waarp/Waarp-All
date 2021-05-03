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

package org.waarp.openr66.protocol.http.rest.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.waarp.common.exception.InvalidArgumentException;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.gateway.kernel.rest.RestArgument;

import java.util.Iterator;
import java.util.Map.Entry;

/**
 * Utility class to check REST V1 arguments
 */
public class HttpRestV1Utils {
  private HttpRestV1Utils() {
    // Empty
  }

  /**
   * Check ObjectNode
   *
   * @param objectNode
   *
   * @throws InvalidArgumentException
   */
  public static void checkSanity(final ObjectNode objectNode)
      throws InvalidArgumentException {
    Iterator<Entry<String, JsonNode>> iterator = objectNode.fields();
    while (iterator.hasNext()) {
      Entry<String, JsonNode> next = iterator.next();
      ParametersChecker.checkSanityString(next.getValue().asText());
    }
  }

  /**
   * Check RestArgument
   *
   * @param argument
   *
   * @throws InvalidArgumentException
   */
  public static void checkSanity(final RestArgument argument)
      throws InvalidArgumentException {
    checkSanity(argument.getBody());
    checkSanity(argument.getUriArgs());
  }
}
