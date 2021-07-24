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

package org.waarp.openr66.protocol.http.restv2.dbhandlers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.cdap.http.AbstractHttpHandler;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import org.waarp.common.exception.InvalidArgumentException;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.gateway.kernel.rest.RestConfiguration.CRUD;
import org.waarp.openr66.protocol.http.restv2.errors.RestError;
import org.waarp.openr66.protocol.http.restv2.errors.RestErrorException;

import java.util.ArrayList;
import java.util.List;

import static io.netty.handler.codec.http.HttpMethod.*;
import static org.waarp.openr66.protocol.http.restv2.errors.RestErrors.*;

/**
 * This abstract class represents a handler for the WaarpR66 REST API. A handler
 * can handle one or multiple
 * entry points with multiple methods each.
 * <p>
 * To assign an entry point to a handler, use the {@link javax.ws.rs.Path}
 * annotation with the URI as value.
 * This annotation can also be used on methods of the handler to create entry
 * points below the handler's one.
 * <p>
 * For each entry point, all the desired HTTP methods must be assigned a
 * corresponding Java method using the
 * appropriate {@link javax.ws.rs.HttpMethod}. These methods must all have an
 * {@link HttpRequest} and a
 * {@link io.cdap.http.HttpResponder} as their first 2 parameters, which
 * correspond respectively to the
 * request and its reply. The methods may also have other parameters marked with
 * the
 * {@link javax.ws.rs.PathParam} annotation, meaning that these parameters
 * correspond to the {id} given in the
 * URI of the request. Some parameters may also be marked with the {@link
 * javax.ws.rs.QueryParam} an
 * annotation, in which case the parameter marked is referring to the query
 * parameter whose name is given in
 * the annotation's parameter. A query parameter can be given a default value
 * with the
 * {@link javax.ws.rs.DefaultValue} which will be assigned to the parameter when
 * the request does not contain
 * a value for it.
 * <p>
 * These methods should also be annotated with the {@link javax.ws.rs.Consumes}
 * annotation to indicate the
 * expected content type of the request. Finally, each method should also be
 * annotated with the
 * {@link RequiredRole} annotation to make the method only accessible to users
 * with the specified
 * {@link org.waarp.common.role.RoleDefault.ROLE} or higher.
 */
public abstract class AbstractRestDbHandler extends AbstractHttpHandler {

  /**
   * A byte mask defining which HTTP methods are allowed on the handler.
   */
  protected final byte crud;

  /**
   * Initializes the handler with the given CRUD mask.
   *
   * @param crud the CRUD mask for this handler
   */
  protected AbstractRestDbHandler(final byte crud) {
    this.crud = crud;
  }

  /**
   * Checks whether the {@link HttpRequest} given can be made on the handler
   * in accordance with the handler's
   * CRUD configuration.
   *
   * @param request the HTTP request to check
   *
   * @return {@code true} if the request is active, {@code false} otherwise.
   */
  public boolean checkCRUD(final HttpRequest request) {
    final HttpMethod method = request.method();
    if (method.equals(GET)) {
      return CRUD.READ.isValid(crud);
    } else if (method.equals(POST)) {
      return CRUD.CREATE.isValid(crud);
    } else if (method.equals(DELETE)) {
      return CRUD.DELETE.isValid(crud);
    } else if (method.equals(PUT)) {
      return CRUD.UPDATE.isValid(crud);
    } else {
      return method.equals(OPTIONS);
    }
  }

  protected final void checkSanity(final String... strings) {
    try {
      ParametersChecker.checkSanityString(strings);
    } catch (final InvalidArgumentException e) {
      final List<RestError> errors = new ArrayList<RestError>(1);
      errors.add(ILLEGAL_FIELD_VALUE("argument", "Content Not Allowed"));
      throw new RestErrorException(errors);
    }
  }

  protected final void checkSanity(final ObjectNode objectNode) {
    try {
      ParametersChecker.checkSanityString(
          JsonHandler.writeAsString(objectNode));
    } catch (final InvalidArgumentException e) {
      final List<RestError> errors = new ArrayList<RestError>(1);
      errors.add(ILLEGAL_FIELD_VALUE("argument", "Content Not Allowed"));
      throw new RestErrorException(errors);
    }
  }
}
