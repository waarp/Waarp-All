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

package org.waarp.openr66.protocol.http.restv2.resthandlers;

import io.cdap.http.HandlerHook;
import io.cdap.http.HttpResponder;
import io.cdap.http.internal.HandlerInfo;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.joda.time.DateTime;
import org.waarp.common.crypto.HmacSha256;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.role.RoleDefault;
import org.waarp.common.role.RoleDefault.ROLE;
import org.waarp.common.utility.BaseXx;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.dao.HostDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.pojo.Host;
import org.waarp.openr66.protocol.http.restv2.RestServiceInitializer;
import org.waarp.openr66.protocol.http.restv2.converters.HostConfigConverter;
import org.waarp.openr66.protocol.http.restv2.dbhandlers.AbstractRestDbHandler;
import org.waarp.openr66.protocol.http.restv2.dbhandlers.RequiredRole;

import javax.ws.rs.Consumes;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static javax.ws.rs.core.HttpHeaders.*;
import static javax.ws.rs.core.MediaType.*;
import static org.glassfish.jersey.message.internal.HttpHeaderReader.*;
import static org.glassfish.jersey.message.internal.MediaTypes.*;
import static org.waarp.common.role.RoleDefault.ROLE.*;
import static org.waarp.openr66.protocol.configuration.Configuration.*;
import static org.waarp.openr66.protocol.http.restv2.RestConstants.*;

/**
 * This class defines hooks called before and after the corresponding {@link
 * AbstractRestDbHandler} when a
 * request is made. These hooks check the user authentication and privileges, as
 * well as the request content
 * type.
 */
public class RestHandlerHook implements HandlerHook {

  /**
   * Tells if the REST request authentication is activated.
   */
  private final boolean authenticated;

  /**
   * Stores the key used for HMAC authentication.
   */
  private final HmacSha256 hmac;

  /**
   * The time (in ms) for which a HMAC signed request is valid.
   */
  private final long delay;

  /**
   * The logger for all events.
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(RestHandlerHook.class);

  /**
   * Hook called before a request handler is called. Checks if the REST method
   * is active in the CRUD
   * configuration, checks the request's content type, and finally checks the
   * user authentication (if
   * activated).
   *
   * @param request the HttpRequest currently being processed
   * @param responder the HttpResponder sending the response
   * @param handlerInfo the information about the handler to which the
   *     request will be sent for processing
   *
   * @return {@code true} if the request can be handed to the handler, or
   *     {@code false} if an error occurred and
   *     a response must be sent immediately.
   */
  @Override
  public boolean preCall(final HttpRequest request,
                         final HttpResponder responder,
                         final HandlerInfo handlerInfo) {

    try {
      final AbstractRestDbHandler handler = getHandler(handlerInfo);
      if (!handler.checkCRUD(request)) {
        responder.sendStatus(METHOD_NOT_ALLOWED);
        return false;
      }

      final Method handleMethod = getMethod(handler, handlerInfo);
      if (authenticated && !request.method().equals(OPTIONS)) {
        final String user = checkCredentials(request);
        if (!checkAuthorization(user, handleMethod)) {
          responder.sendStatus(FORBIDDEN);
          return false;
        }
      }

      final List<MediaType> expectedTypes = getExpectedMediaTypes(handleMethod);
      if (!checkContentType(request, expectedTypes)) {
        final DefaultHttpHeaders headers = new DefaultHttpHeaders();
        headers.add(ACCEPT, convertToString(expectedTypes));
        responder.sendStatus(UNSUPPORTED_MEDIA_TYPE, headers);
        return false;
      }

      return true;
    } catch (final NotAllowedException e) {
      logger.info(e.getMessage());
      final DefaultHttpHeaders headers = new DefaultHttpHeaders();
      headers.add(WWW_AUTHENTICATE, "Basic, HMAC");
      responder.sendStatus(UNAUTHORIZED, headers);
    } catch (final InternalServerErrorException e) {
      logger.error(e);
      responder.sendStatus(INTERNAL_SERVER_ERROR);
    } catch (final Throwable t) {
      logger.error("RESTv2 Unexpected exception caught ->", t);
      responder.sendStatus(INTERNAL_SERVER_ERROR);
    }
    return false;
  }

  /**
   * Returns the {@link AbstractRestDbHandler} instance corresponding to the
   * info given as parameter.
   *
   * @param handlerInfo the information about the handler
   *
   * @return the corresponding AbstractRestDbHandler
   *
   * @throws IllegalArgumentException if the given handler does not
   *     exist.
   */
  private AbstractRestDbHandler getHandler(final HandlerInfo handlerInfo) {
    for (final AbstractRestDbHandler h : RestServiceInitializer.handlers) {
      if (h.getClass().getName().equals(handlerInfo.getHandlerName())) {
        return h;
      }
    }
    throw new IllegalArgumentException(
        "The handler " + handlerInfo.getHandlerName() + " does not exist.");
  }

  /**
   * Returns the {@link Method} object corresponding to the handler method
   * chosen to process the request. This
   * is needed to check for the annotations present on the method.
   *
   * @param handler the handler chosen to process the request
   * @param handlerInfo the information about the handler
   *
   * @return the corresponding Method object
   *
   * @throws IllegalArgumentException if the given method name does
   *     not exist
   */
  private Method getMethod(final AbstractRestDbHandler handler,
                           final HandlerInfo handlerInfo) {
    Method method = null;
    for (final Method m : handler.getClass().getMethods()) {//NOSONAR
      if (m.getName().equals(handlerInfo.getMethodName()) &&
          m.getParameterTypes()[0] == HttpRequest.class &&
          m.getParameterTypes()[1] == HttpResponder.class) {
        method = m;
        break;
      }
    }
    if (method == null) {
      throw new IllegalArgumentException(
          "The handler " + handlerInfo.getHandlerName() +
          " does not have a method " + handlerInfo.getMethodName());
    }
    return method;
  }

  /**
   * Return a List of all the {@link MediaType} accepted by the given {@link
   * Method}. This list is based on the
   * types indicated by the method's {@link Consumes} annotation. If the
   * annotation is absent, the method will
   * be assumed to accept any type.
   *
   * @param method the Method to inspect
   *
   * @return the list of all acceptable MediaType
   */
  private List<MediaType> getExpectedMediaTypes(final Method method) {
    List<MediaType> consumedTypes = WILDCARD_TYPE_SINGLETON_LIST;

    if (method.isAnnotationPresent(Consumes.class)) {
      consumedTypes = createFrom(method.getAnnotation(Consumes.class));
    } else {
      logger.warn(String.format(
          "[RESTv2] The method %s of handler %s is missing " +
          "a '%s' annotation for the expected request content type, " +
          "the default value '%s' was given instead.", method.getName(),
          method.getDeclaringClass().getSimpleName(),
          Consumes.class.getSimpleName(), WILDCARD));
    }

    return consumedTypes;
  }

  /**
   * Checks if the content type of the request is compatible with the expected
   * content type of the method
   * called. If no content type header can be found, the request will be
   * assumed to have a correct content type.
   *
   * @param request the HttpRequest sent by the user
   * @param consumedTypes a list of the acceptable MediaType for the
   *     request
   *
   * @return {@code true} if the request content type is acceptable, {@code
   *     false} otherwise.
   */
  private boolean checkContentType(final HttpRequest request,
                                   final List<MediaType> consumedTypes) {

    final String contentTypeHeader = request.headers().get(CONTENT_TYPE);
    if (ParametersChecker.isEmpty(contentTypeHeader)) {
      return true;
    }

    final MediaType requestType;
    try {
      requestType = readAcceptMediaType(contentTypeHeader).get(0);
    } catch (final ParseException e) {
      return false;
    }
    for (final MediaType consumedType : consumedTypes) {
      if (requestType.isCompatible(consumedType)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if the user making the request does exist. If the user does exist,
   * this method returns the user's
   * name, otherwise throws a {@link NotAllowedException}.
   *
   * @param request the request currently being processed
   *
   * @return the user's name
   *
   * @throws InternalServerErrorException if an unexpected error
   *     occurred
   * @throws NotAllowedException if the user making the request does
   *     not exist
   */
  protected String checkCredentials(final HttpRequest request) {

    final String authorization = request.headers().get(AUTHORIZATION);

    if (authorization == null) {
      throw new NotAllowedException("Missing header for authentication.");
    }

    final Pattern basicPattern = Pattern.compile("(Basic) (\\w+=*)");
    final Matcher basicMatcher = basicPattern.matcher(authorization);

    if (basicMatcher.find()) {

      final String[] credentials;
      credentials = new String(BaseXx.getFromBase64(basicMatcher.group(2)),
                               WaarpStringUtils.UTF8).split(":", 2);
      if (credentials.length != 2) {
        throw new NotAllowedException(
            "Invalid header for Basic authentication.");
      }
      final String user = credentials[0];
      final String pswd = credentials[1];

      HostDAO hostDAO = null;
      Host host;
      try {
        hostDAO = DAO_FACTORY.getHostDAO();
        if (!hostDAO.exist(user)) {
          throw new NotAllowedException("User does not exist.");
        }
        host = hostDAO.select(user);
      } catch (final DAOConnectionException e) {
        throw new InternalServerErrorException(e);
      } catch (final DAONoDataException e) {
        throw new InternalServerErrorException(e);
      } finally {
        DAOFactory.closeDAO(hostDAO);
      }

      final String key;
      try {
        key = configuration.getCryptoKey().cryptToHex(pswd);
      } catch (final Exception e) {
        throw new InternalServerErrorException(
            "An error occurred when encrypting the password", e);
      }
      if (!Arrays
          .equals(host.getHostkey(), key.getBytes(WaarpStringUtils.UTF8))) {
        throw new NotAllowedException("Invalid password.");
      }

      return user;
    }

    final String authUser = request.headers().get(AUTH_USER);
    final String authDate = request.headers().get(AUTH_TIMESTAMP);

    final Pattern hmacPattern = Pattern.compile("(HMAC) (\\w+)");
    final Matcher hmacMatcher = hmacPattern.matcher(authorization);

    if (hmacMatcher.find() && authUser != null && authDate != null) {

      final String authKey = hmacMatcher.group(2);
      final DateTime requestDate;
      try {
        requestDate = DateTime.parse(authDate);
      } catch (final IllegalArgumentException e) {
        throw new NotAllowedException("Invalid authentication timestamp.");
      }
      final DateTime limitTime = requestDate.plus(delay);
      if (DateTime.now().isAfter(limitTime)) {
        throw new NotAllowedException("Authentication expired.");
      }

      HostDAO hostDAO = null;
      Host host;
      try {
        hostDAO = DAO_FACTORY.getHostDAO();
        if (!hostDAO.exist(authUser)) {
          throw new NotAllowedException("User does not exist.");
        }
        host = hostDAO.select(authUser);
      } catch (final DAOConnectionException e) {
        throw new InternalServerErrorException(e);
      } catch (final DAONoDataException e) {
        throw new InternalServerErrorException(e);
      } finally {
        DAOFactory.closeDAO(hostDAO);
      }

      validateHMACCredentials(host, authDate, authUser, authKey);

      return authUser;
    }

    throw new NotAllowedException("Missing credentials.");
  }

  protected void validateHMACCredentials(final Host host, final String authDate,
                                         final String authUser,
                                         final String authKey)
      throws InternalServerErrorException {
    final String pswd;
    try {
      pswd = configuration.getCryptoKey().decryptHexInString(host.getHostkey());
    } catch (final Exception e) {
      throw new InternalServerErrorException(
          "An error occurred when decrypting the password", e);
    }

    final String key;
    try {
      key = hmac.cryptToHex(authDate + authUser + pswd);
    } catch (final Exception e) {
      throw new InternalServerErrorException(
          "An error occurred when hashing the key", e);
    }

    if (!key.equals(authKey)) {
      throw new NotAllowedException("Invalid password.");
    }
  }

  /**
   * Checks if the user given as argument is authorized to call the given
   * method.
   *
   * @param user the name of the user making the request
   * @param method the method called by the request
   *
   * @return {@code true} if the user is authorized to make the request,
   *     {@code false} otherwise.
   */
  protected boolean checkAuthorization(final String user, final Method method) {
    try {
      final DbHostAuth hostAuth = new DbHostAuth(user);
      if (hostAuth.isAdminrole()) {
        return true;
      }
    } catch (final WaarpDatabaseException e) {
      // ignore and continue
    }

    ROLE requiredRole = NOACCESS;
    if (method.isAnnotationPresent(RequiredRole.class)) {
      requiredRole = method.getAnnotation(RequiredRole.class).value();
    } else {
      logger.warn(String.format("[RESTv2] The method %s of handler %s is " +
                                "missing a '%s' annotation for the minimum required role, " +
                                "the default value '%s' was given instead.",
                                method.getName(),
                                method.getDeclaringClass().getSimpleName(),
                                RequiredRole.class.getSimpleName(), NOACCESS));
    }
    if (requiredRole == NOACCESS) {
      return true;
    }

    final List<ROLE> roles = HostConfigConverter.getRoles(user);
    if (roles != null) {
      final RoleDefault roleDefault = new RoleDefault();
      for (final ROLE roleType : roles) {
        roleDefault.addRole(roleType);
      }
      return roleDefault.isContaining(requiredRole);
    }
    return false;
  }

  /**
   * Hook called after a request handler is called.
   *
   * @param httpRequest the request currently being processed
   * @param httpResponseStatus the status of the http response
   *     generated by the request handler
   * @param handlerInfo information about the handler to which the
   *     request was sent
   */
  @Override
  public void postCall(final HttpRequest httpRequest,
                       final HttpResponseStatus httpResponseStatus,
                       final HandlerInfo handlerInfo) {
    // ignore
  }

  /**
   * Creates a HandlerHook which will check for authentication and signature
   * on incoming request depending on
   * the parameters.
   *
   * @param authenticated specifies if the HandlerHook will check
   *     authentication
   * @param hmac the key used for HMAC authentication
   * @param delay the delay for which a HMAC signed request is valid
   */
  public RestHandlerHook(final boolean authenticated, final HmacSha256 hmac,
                         final long delay) {
    this.authenticated = authenticated;
    this.hmac = hmac;
    this.delay = delay;
  }
}
