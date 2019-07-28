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
package org.waarp.gateway.kernel.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.QueryStringEncoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.waarp.common.crypto.HmacSha256;
import org.waarp.common.exception.InvalidArgumentException;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.role.RoleDefault;
import org.waarp.common.role.RoleDefault.ROLE;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.gateway.kernel.exception.HttpIncorrectRequestException;
import org.waarp.gateway.kernel.exception.HttpInvalidAuthenticationException;
import org.waarp.gateway.kernel.rest.DataModelRestMethodHandler.COMMAND_TYPE;
import org.waarp.gateway.kernel.rest.HttpRestHandler.METHOD;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * Rest object that contains all arguments or answers once all tasks are
 * over:</br>
 * - ARG_HASBODY, ARG_METHOD, ARG_PATH, ARG_BASEPATH, ARGS_SUBPATH,
 * ARG_X_AUTH_KEY, ARG_X_AUTH_USER,
 * ARG_X_AUTH_TIMESTAMP, ARG_X_AUTH_ROLE: root elements (query only)</br>
 * - ARG_METHOD, ARG_PATH, ARG_BASEPATH, ARGS_SUBPATH, ARG_X_AUTH_USER,
 * JSON_STATUSMESSAGE, JSON_STATUSCODE,
 * JSON_COMMAND: root elements (answer only)</br>
 * - ARGS_URI: uri elements (query only)</br>
 * - ARGS_HEADER: header elements (query only)</br>
 * - ARG_COOKIE: cookie elements</br>
 * - ARGS_BODY: body elements (query only)</br>
 * - ARGS_ANSWER: answer part (answer only)</br>
 */
public class RestArgument {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(RestArgument.class);

  public enum REST_GROUP {
    /**
     * arguments.path(ARGS_URI) main entry for URI arguments
     */
    ARGS_URI("uri"),
    /**
     * arguments.path(ARGS_HEADER) main entry for HEADER arguments
     */
    ARGS_HEADER("header"),
    /**
     * arguments.path(ARGS_COOKIE) main entry for COOKIE arguments
     */
    ARGS_COOKIE("cookie"),
    /**
     * arguments.path(ARGS_BODY) main entry for BODY arguments
     */
    ARGS_BODY("body"),
    /**
     * arguments.path(ARGS_ANSWER) main entry for ANSWER arguments
     */
    ARGS_ANSWER("answer");

    public final String group;

    REST_GROUP(String group) {
      this.group = group;
    }
  }

  public enum REST_ROOT_FIELD {
    /**
     * arguments.path(ARG_PATH) = uri path
     */
    ARG_PATH("path"),
    /**
     * arguments.path(ARG_BASEPATH).asText() = uri base path
     */
    ARG_BASEPATH("base"),
    /**
     * arguments.path(ARGS_SUBPATH) main entry for SUB-PATH arguments<br>
     * arguments.path(ARGS_SUBPATH).elements() for an iterator or .get(x)
     * for
     * xth SUB-PATH argument
     */
    ARGS_SUBPATH("subpath"),
    /**
     * arguments.path(ARG_METHOD).asText() = method identified
     */
    ARG_METHOD("X-method"),
    /**
     * arguments.path(ARG_HASBODY).asBoolean() = true if the body has
     * content
     */
    ARG_HASBODY("hasBody"),
    /**
     * arguments.path(ARG_X_AUTH_KEY).asText() = Key used
     */
    ARG_X_AUTH_KEY("X-Auth-Key"),
    /**
     * arguments.path(ARG_X_AUTH_KEY).asText() = Key used
     */
    ARG_X_AUTH_USER("X-Auth-User"),
    /**
     * Internal Key used (not to be passed through wire)
     */
    ARG_X_AUTH_INTERNALKEY("X-Auth-InternalKey"),
    /**
     * arguments.path(ARG_X_AUTH_TIMESTAMP).asText() = Timestamp in ISO 8601
     * format
     */
    ARG_X_AUTH_TIMESTAMP("X-Auth-Timestamp"),
    /**
     * arguments.path(ARG_X_AUTH_ROLE).asInt() = Role used
     */
    ARG_X_AUTH_ROLE("X-Auth-Role"), JSON_STATUSCODE("code"),
    JSON_STATUSMESSAGE("message"), JSON_COMMAND("command"),
    JSON_DETAIL("detail");

    public final String field;

    REST_ROOT_FIELD(String field) {
      this.field = field;
    }
  }

  public enum REST_FIELD {
    JSON_RESULT("result"), JSON_PATH("path"), JSON_JSON("body"),
    X_DETAILED_ALLOW("DetailedAllow"), X_ALLOW_URIS("UriAllowed"),
    JSON_ID("_id");

    public final String field;

    REST_FIELD(String field) {
      this.field = field;
    }
  }

  public enum DATAMODEL {
    JSON_COUNT("count"), JSON_RESULTS("results"), JSON_FILTER("filter"),
    JSON_LIMIT("limit");

    public final String field;

    DATAMODEL(String field) {
      this.field = field;
    }
  }

  final ObjectNode arguments;

  /**
   * Create a RestArgument
   *
   * @param emptyArgument might be null, but might be also already
   *     initialized with some values
   */
  public RestArgument(ObjectNode emptyArgument) {
    if (emptyArgument == null) {
      arguments = JsonHandler.createObjectNode();
    } else {
      arguments = emptyArgument;
    }
  }

  /**
   * Clean all internal values
   */
  public void clean() {
    arguments.removeAll();
  }

  /**
   * Set values according to the request URI
   *
   * @param request
   */
  public void setRequest(HttpRequest request) {
    arguments.put(REST_ROOT_FIELD.ARG_HASBODY.field,
                  request instanceof FullHttpRequest &&
                  ((FullHttpRequest) request).content() !=
                  Unpooled.EMPTY_BUFFER);
    arguments.put(REST_ROOT_FIELD.ARG_METHOD.field, request.method().name());
    final QueryStringDecoder decoderQuery =
        new QueryStringDecoder(request.uri());
    final String path = decoderQuery.path();
    arguments.put(REST_ROOT_FIELD.ARG_PATH.field, path);
    // compute path main uri
    String basepath = path;
    int pos = basepath.indexOf('/');
    if (pos >= 0) {
      if (pos == 0) {
        final int pos2 = basepath.indexOf('/', 1);
        if (pos2 < 0) {
          basepath = basepath.substring(1);
        } else {
          basepath = basepath.substring(1, pos2);
        }
      } else {
        basepath = basepath.substring(0, pos);
      }
    }
    arguments.put(REST_ROOT_FIELD.ARG_BASEPATH.field, basepath);
    // compute sub path args
    if (pos == 0) {
      pos = path.indexOf('/', 1);
    }
    if (pos >= 0) {
      int pos2 = path.indexOf('/', pos + 1);
      if (pos2 > 0) {
        final ArrayNode array =
            arguments.putArray(REST_ROOT_FIELD.ARGS_SUBPATH.field);
        while (pos2 > 0) {
          array.add(path.substring(pos + 1, pos2));
          pos = pos2;
          pos2 = path.indexOf('/', pos + 1);
        }
      }
      pos2 = path.indexOf('?', pos + 1);
      if (pos2 > 0 && pos2 > pos + 1) {
        ArrayNode array =
            (ArrayNode) arguments.get(REST_ROOT_FIELD.ARGS_SUBPATH.field);
        if (array == null) {
          array = arguments.putArray(REST_ROOT_FIELD.ARGS_SUBPATH.field);
        }
        array.add(path.substring(pos + 1, pos2));
      } else {
        final String last = path.substring(pos + 1);
        if (!last.isEmpty()) {
          ArrayNode array =
              (ArrayNode) arguments.get(REST_ROOT_FIELD.ARGS_SUBPATH.field);
          if (array == null) {
            array = arguments.putArray(REST_ROOT_FIELD.ARGS_SUBPATH.field);
          }
          array.add(last);
        }
      }
    }
    final Map<String, List<String>> map = decoderQuery.parameters();
    final ObjectNode node = arguments.putObject(REST_GROUP.ARGS_URI.group);
    for (final Entry<String, List<String>> entry : map.entrySet()) {
      final String key = entry.getKey();
      try {
        ParametersChecker.checkSanityString(
            entry.getValue().toArray(ParametersChecker.ZERO_ARRAY_STRING));
        if (key.equalsIgnoreCase(REST_ROOT_FIELD.ARG_X_AUTH_KEY.field)) {
          arguments.put(REST_ROOT_FIELD.ARG_X_AUTH_KEY.field,
                        entry.getValue().get(0));
          continue;
        }
        if (key.equalsIgnoreCase(REST_ROOT_FIELD.ARG_X_AUTH_USER.field)) {
          arguments.put(REST_ROOT_FIELD.ARG_X_AUTH_USER.field,
                        entry.getValue().get(0));
          continue;
        }
        if (key.equalsIgnoreCase(REST_ROOT_FIELD.ARG_X_AUTH_TIMESTAMP.field)) {
          arguments.put(REST_ROOT_FIELD.ARG_X_AUTH_TIMESTAMP.field,
                        entry.getValue().get(0));
          continue;
        }
        final List<String> list = entry.getValue();
        if (list.size() > 1) {
          final ArrayNode array = node.putArray(key);
          for (final String val : entry.getValue()) {
            array.add(val);
          }
        } else if (list.isEmpty()) {
          node.putNull(key);
        } else {
          // 1
          node.put(key, list.get(0));
        }
      } catch (InvalidArgumentException e) {
        logger.error("Arguments incompatible with Security: " + entry.getKey(),
                     e);
      }
    }
  }

  /**
   * Set X_AUTH_USER, Method, Path, Basepath and Cookie from source
   *
   * @param source
   */
  public void setFromArgument(RestArgument source) {
    if (source.arguments.has(REST_ROOT_FIELD.ARG_X_AUTH_USER.field)) {
      arguments.put(REST_ROOT_FIELD.ARG_X_AUTH_USER.field,
                    source.arguments.get(REST_ROOT_FIELD.ARG_X_AUTH_USER.field)
                                    .asText());
    }
    if (source.arguments.has(REST_ROOT_FIELD.ARG_METHOD.field)) {
      arguments.put(REST_ROOT_FIELD.ARG_METHOD.field,
                    source.arguments.get(REST_ROOT_FIELD.ARG_METHOD.field)
                                    .asText());
    }
    if (source.arguments.has(REST_ROOT_FIELD.ARG_PATH.field)) {
      arguments.put(REST_ROOT_FIELD.ARG_PATH.field,
                    source.arguments.get(REST_ROOT_FIELD.ARG_PATH.field)
                                    .asText());
    }
    if (source.arguments.has(REST_ROOT_FIELD.ARG_BASEPATH.field)) {
      arguments.put(REST_ROOT_FIELD.ARG_BASEPATH.field,
                    source.arguments.get(REST_ROOT_FIELD.ARG_BASEPATH.field)
                                    .asText());
    }
    if (source.arguments.has(REST_ROOT_FIELD.ARGS_SUBPATH.field)) {
      arguments.putArray(REST_ROOT_FIELD.ARGS_SUBPATH.field).addAll(
          (ArrayNode) source.arguments.get(REST_ROOT_FIELD.ARGS_SUBPATH.field));
    }
    if (source.arguments.has(REST_GROUP.ARGS_URI.group)) {
      arguments.putObject(REST_GROUP.ARGS_URI.group).setAll(
          (ObjectNode) source.arguments.get(REST_GROUP.ARGS_URI.group));
    }
    if (source.arguments.has(REST_GROUP.ARGS_COOKIE.group)) {
      arguments.putObject(REST_GROUP.ARGS_COOKIE.group).setAll(
          (ObjectNode) source.arguments.get(REST_GROUP.ARGS_COOKIE.group));
    }

    logger.debug("DEBUG: {}\n {}", arguments, source);
  }

  /**
   * @return the full Path of the URI
   */
  public String getUri() {
    return arguments.path(REST_ROOT_FIELD.ARG_PATH.field).asText();
  }

  /**
   * @return the base Path of the URI (first item between '/')
   */
  public String getBaseUri() {
    return arguments.path(REST_ROOT_FIELD.ARG_BASEPATH.field).asText();
  }

  /**
   * @return An iterator of JsonNode, which values can be retrieved by
   *     item.asText()
   */
  public Iterator<JsonNode> getSubUri() {
    return arguments.path(REST_ROOT_FIELD.ARGS_SUBPATH.field).elements();
  }

  public int getSubUriSize() {
    return arguments.path(REST_ROOT_FIELD.ARGS_SUBPATH.field).size();
  }

  public void addSubUriToUriArgs(String name, int rank) {
    final ObjectNode node = getUriArgs();
    final JsonNode elt =
        arguments.path(REST_ROOT_FIELD.ARGS_SUBPATH.field).get(rank);
    if (elt != null) {
      node.set(name, elt);
    }
  }

  public void addIdToUriArgs() {
    addSubUriToUriArgs(REST_FIELD.JSON_ID.field, 0);
  }

  public JsonNode getId() {
    return getUriArgs().path(REST_FIELD.JSON_ID.field);
  }

  public static JsonNode getId(ObjectNode node) {
    return node.path(REST_FIELD.JSON_ID.field);
  }

  public long getLimitFromUri() {
    return getUriArgs().path(DATAMODEL.JSON_LIMIT.field).asLong(100);
  }

  public String getXAuthKey() {
    return arguments.path(REST_ROOT_FIELD.ARG_X_AUTH_KEY.field).asText();
  }

  public String getXAuthUser() {
    return arguments.path(REST_ROOT_FIELD.ARG_X_AUTH_USER.field).asText();
  }

  public String getXAuthTimestamp() {
    return arguments.path(REST_ROOT_FIELD.ARG_X_AUTH_TIMESTAMP.field).asText();
  }

  public void setXAuthRole(RoleDefault role) {
    arguments.put(REST_ROOT_FIELD.ARG_X_AUTH_ROLE.field, role.getRoleAsByte());
  }

  public ROLE getXAuthRole() {
    final byte role =
        (byte) arguments.get(REST_ROOT_FIELD.ARG_X_AUTH_ROLE.field).asInt();
    return ROLE.fromByte(role);
  }

  /**
   * @return The ObjectNode containing all couples key/value
   */
  public ObjectNode getUriArgs() {
    JsonNode node = arguments.path(REST_GROUP.ARGS_URI.group);
    if (node == null || node.isMissingNode()) {
      node = arguments.putObject(REST_GROUP.ARGS_URI.group);
    }
    return (ObjectNode) node;
  }

  /**
   * @return the method or null
   */
  public METHOD getMethod() {
    final String text =
        arguments.path(REST_ROOT_FIELD.ARG_METHOD.field).asText();
    if (text == null || text.isEmpty()) {
      return METHOD.TRACE;
    }
    try {
      return METHOD.valueOf(text);
    } catch (final Exception e) {
      return METHOD.TRACE;
    }
  }

  /**
   * set values from Header into arguments.path(ARGS_HEADER)
   *
   * @throws HttpIncorrectRequestException
   */
  public void setHeaderArgs(List<Entry<String, String>> list) {
    ObjectNode node = (ObjectNode) arguments.get(REST_GROUP.ARGS_HEADER.group);
    if (node == null || node.isMissingNode()) {
      node = arguments.putObject(REST_GROUP.ARGS_HEADER.group);
    }
    for (final Entry<String, String> entry : list) {
      try {
        ParametersChecker.checkSanityString(entry.getValue());
        final String key = entry.getKey();
        if (!key.equals(HttpHeaderNames.COOKIE.toString())) {
          if (key.equalsIgnoreCase(REST_ROOT_FIELD.ARG_X_AUTH_KEY.field)) {
            arguments
                .put(REST_ROOT_FIELD.ARG_X_AUTH_KEY.field, entry.getValue());
            continue;
          }
          if (key.equalsIgnoreCase(REST_ROOT_FIELD.ARG_X_AUTH_USER.field)) {
            arguments
                .put(REST_ROOT_FIELD.ARG_X_AUTH_USER.field, entry.getValue());
            continue;
          }
          if (key
              .equalsIgnoreCase(REST_ROOT_FIELD.ARG_X_AUTH_TIMESTAMP.field)) {
            arguments.put(REST_ROOT_FIELD.ARG_X_AUTH_TIMESTAMP.field,
                          entry.getValue());
            continue;
          }
          node.put(key, entry.getValue());
        }
      } catch (InvalidArgumentException e) {
        logger.error("Arguments incompatible with Security: " + entry.getKey(),
                     e);
      }
    }
  }

  /**
   * set values from Header into arguments.path(ARGS_HEADER)
   *
   * @throws HttpIncorrectRequestException
   */
  public void setHeaderArgs(
      Iterator<Entry<CharSequence, CharSequence>> iterator) {
    ObjectNode node = (ObjectNode) arguments.get(REST_GROUP.ARGS_HEADER.group);
    if (node == null || node.isMissingNode()) {
      node = arguments.putObject(REST_GROUP.ARGS_HEADER.group);
    }
    while (iterator.hasNext()) {
      final Entry<CharSequence, CharSequence> entry = iterator.next();
      try {
        ParametersChecker.checkSanityString(entry.getValue().toString());
        final String key = entry.getKey().toString();
        if (!key.equals(HttpHeaderNames.COOKIE.toString())) {
          if (key.equalsIgnoreCase(REST_ROOT_FIELD.ARG_X_AUTH_KEY.field)) {
            arguments.put(REST_ROOT_FIELD.ARG_X_AUTH_KEY.field,
                          entry.getValue().toString());
            continue;
          }
          if (key.equalsIgnoreCase(REST_ROOT_FIELD.ARG_X_AUTH_USER.field)) {
            arguments.put(REST_ROOT_FIELD.ARG_X_AUTH_USER.field,
                          entry.getValue().toString());
            continue;
          }
          if (key
              .equalsIgnoreCase(REST_ROOT_FIELD.ARG_X_AUTH_TIMESTAMP.field)) {
            arguments.put(REST_ROOT_FIELD.ARG_X_AUTH_TIMESTAMP.field,
                          entry.getValue().toString());
            continue;
          }
          node.put(key, entry.getValue().toString());
        }
      } catch (InvalidArgumentException e) {
        logger.error("Arguments incompatible with Security: " + entry.getKey(),
                     e);
      }
    }
  }

  /**
   * @return The ObjectNode containing all couples key/value
   */
  public ObjectNode getHeaderArgs() {
    JsonNode node = arguments.path(REST_GROUP.ARGS_HEADER.group);
    if (node == null || node.isMissingNode()) {
      node = arguments.putObject(REST_GROUP.ARGS_HEADER.group);
    }
    return (ObjectNode) node;
  }

  /**
   * set method From URI
   */
  public void methodFromUri() {
    final JsonNode node = arguments.path(REST_GROUP.ARGS_URI.group)
                                   .path(REST_ROOT_FIELD.ARG_METHOD.field);
    if (!node.isMissingNode()) {
      // override
      arguments.put(REST_ROOT_FIELD.ARG_METHOD.field, node.asText());
    }
  }

  /**
   * set method From Header
   */
  public void methodFromHeader() {
    final JsonNode node = arguments.path(REST_GROUP.ARGS_HEADER.group)
                                   .path(REST_ROOT_FIELD.ARG_METHOD.field);
    if (!node.isMissingNode()) {
      // override
      arguments.put(REST_ROOT_FIELD.ARG_METHOD.field, node.asText());
    }
  }

  /**
   * set values from Cookies into arguments.path(ARGS_COOKIE)
   */
  public void setCookieArgs(String cookieString) {
    Set<Cookie> cookies;
    if (cookieString == null) {
      cookies = Collections.emptySet();
    } else {
      cookies = ServerCookieDecoder.LAX.decode(cookieString);
    }
    if (!cookies.isEmpty()) {
      final ObjectNode node = arguments.putObject(REST_GROUP.ARGS_COOKIE.group);
      for (final Cookie cookie : cookies) {
        try {
          ParametersChecker.checkSanityString(cookie.value());
          node.put(cookie.name(), cookie.value());
        } catch (InvalidArgumentException e) {
          logger.error("Arguments incompatible with Security: " + cookie.name(),
                       e);
        }
      }
    }
  }

  /**
   * @return The ObjectNode containing all couples key/value
   */
  public ObjectNode getCookieArgs() {
    JsonNode node = arguments.path(REST_GROUP.ARGS_COOKIE.group);
    if (node == null || node.isMissingNode()) {
      node = arguments.putObject(REST_GROUP.ARGS_COOKIE.group);
    }
    return (ObjectNode) node;
  }

  /**
   * @return The ObjectNode containing all couples key/value
   */
  public ObjectNode getBody() {
    JsonNode node = arguments.path(REST_GROUP.ARGS_BODY.group);
    if (node == null || node.isMissingNode()) {
      node = arguments.putObject(REST_GROUP.ARGS_BODY.group);
    }
    return (ObjectNode) node;
  }

  /**
   * @return The ObjectNode containing all couples key/value
   */
  public ObjectNode getAnswer() {
    JsonNode node = arguments.path(REST_GROUP.ARGS_ANSWER.group);
    if (node == null || node.isMissingNode()) {
      node = arguments.putObject(REST_GROUP.ARGS_ANSWER.group);
    }
    return (ObjectNode) node;
  }

  public void addAnswer(ObjectNode node) {
    getAnswer().setAll(node);
  }

  public void setResult(HttpResponseStatus status) {
    arguments
        .put(REST_ROOT_FIELD.JSON_STATUSMESSAGE.field, status.reasonPhrase());
    arguments.put(REST_ROOT_FIELD.JSON_STATUSCODE.field, status.code());
  }

  /**
   * @return the Http Status code
   */
  public int getStatusCode() {
    return arguments.path(REST_ROOT_FIELD.JSON_STATUSCODE.field).asInt();
  }

  /**
   * @return the Http Status message according to the Http Status code
   */
  public String getStatusMessage() {
    return arguments.path(REST_ROOT_FIELD.JSON_STATUSMESSAGE.field).asText();
  }

  public void setDetail(String detail) {
    arguments.put(REST_ROOT_FIELD.JSON_DETAIL.field, detail);
  }

  /**
   * @return the detail information on error (mainly)
   */
  public String getDetail() {
    return arguments.path(REST_ROOT_FIELD.JSON_DETAIL.field).asText();
  }

  public void setCommand(COMMAND_TYPE command) {
    arguments.put(REST_ROOT_FIELD.JSON_COMMAND.field, command.name());
  }

  public void setCommand(String cmd) {
    arguments.put(REST_ROOT_FIELD.JSON_COMMAND.field, cmd);
  }

  /**
   * @return the COMMAND field, to be transformed either into COMMAND_TYPE or
   *     ACTIONS_TYPE
   */
  public String getCommandField() {
    return arguments.path(REST_ROOT_FIELD.JSON_COMMAND.field).asText();
  }

  /**
   * @return the COMMAND_TYPE but might be null if not found or if of
   *     ACTIONS_TYPE
   */
  public COMMAND_TYPE getCommand() {
    final String cmd =
        arguments.path(REST_ROOT_FIELD.JSON_COMMAND.field).asText();
    if (cmd != null && !cmd.isEmpty()) {
      try {
        return COMMAND_TYPE.valueOf(cmd);
      } catch (final Exception e) {
        return null;
      }
    } else {
      return null;
    }
  }

  /**
   * @param filter the filter used in multi get
   */
  public void addFilter(ObjectNode filter) {
    if (filter == null) {
      filter = JsonHandler.createObjectNode();
    }
    getAnswer().putObject(DATAMODEL.JSON_FILTER.field).setAll(filter);
  }

  /**
   * @return the filter used in multi get
   */
  public ObjectNode getFilter() {
    return (ObjectNode) getAnswer().path(DATAMODEL.JSON_FILTER.field);
  }

  /**
   * @return the array of results (in DataModel multi get)
   */
  public ArrayNode getResults() {
    JsonNode node = getAnswer().path(DATAMODEL.JSON_RESULTS.field);
    if (node == null || node.isMissingNode()) {
      node = getAnswer().putArray(DATAMODEL.JSON_RESULTS.field);
    }
    return (ArrayNode) node;
  }

  /**
   * @param result added to the array of results (in DataModel multi
   *     get)
   */
  public void addResult(ObjectNode result) {
    getResults().add(result);
  }

  /**
   * @param count added to answer if > 0
   * @param limit added to answer
   */
  public void addCountLimit(long count, long limit) {
    final ObjectNode node = getAnswer();
    if (count >= 0) {
      node.put(DATAMODEL.JSON_COUNT.field, count);
    }
    node.put(DATAMODEL.JSON_LIMIT.field, limit);
  }

  /**
   * @return the count of element (-1 if not found)
   */
  public long getCount() {
    return getAnswer().path(DATAMODEL.JSON_COUNT.field).asLong(-1);
  }

  public long getLimit() {
    return getAnswer().path(DATAMODEL.JSON_LIMIT.field).asLong(100);
  }

  /**
   * Add options in answer
   *
   * @param allow
   * @param path
   * @param detailedAllow
   */
  public void addOptions(String allow, String path, ArrayNode detailedAllow) {
    final ObjectNode node = getAnswer();
    node.put(HttpHeaderNames.ALLOW.toString(), allow);
    node.put(REST_FIELD.X_ALLOW_URIS.field, path);
    if (detailedAllow != null) {
      node.putArray(REST_FIELD.X_DETAILED_ALLOW.field).addAll(detailedAllow);
    }
  }

  public String getAllowOption() {
    return getAnswer().path(HttpHeaderNames.ALLOW.toString()).asText();
  }

  public String getAllowUrisOption() {
    return getAnswer().path(REST_FIELD.X_ALLOW_URIS.field).asText();
  }

  public ArrayNode getDetailedAllowOption() {
    final JsonNode node = getAnswer().path(REST_FIELD.X_DETAILED_ALLOW.field);
    if (node.isMissingNode()) {
      return JsonHandler.createArrayNode();
    } else {
      return (ArrayNode) node;
    }
  }

  /**
   * The encoder is completed with extra necessary URI part containing
   * ARG_X_AUTH_TIMESTAMP & ARG_X_AUTH_KEY
   *
   * @param hmacSha256 SHA-256 key to create the signature
   * @param encoder
   * @param user might be null
   * @param extraKey might be null
   *
   * @return an array of 2 value in order ARG_X_AUTH_TIMESTAMP and
   *     ARG_X_AUTH_KEY
   *
   * @throws HttpInvalidAuthenticationException if the computation of
   *     the
   *     authentication failed
   */
  public static String[] getBaseAuthent(HmacSha256 hmacSha256,
                                        QueryStringEncoder encoder, String user,
                                        String extraKey)
      throws HttpInvalidAuthenticationException {
    final QueryStringDecoder decoderQuery =
        new QueryStringDecoder(encoder.toString());
    final Map<String, List<String>> map = decoderQuery.parameters();
    final TreeMap<String, String> treeMap = new TreeMap<String, String>();
    for (final Entry<String, List<String>> entry : map.entrySet()) {
      final String keylower = entry.getKey().toLowerCase();
      final List<String> values = entry.getValue();
      if (values != null && !values.isEmpty()) {
        final String last = values.get(values.size() - 1);
        treeMap.put(keylower, last);
      }
    }
    final DateTime date = new DateTime();
    treeMap.put(REST_ROOT_FIELD.ARG_X_AUTH_TIMESTAMP.field.toLowerCase(),
                date.toString());
    if (user != null) {
      treeMap.put(REST_ROOT_FIELD.ARG_X_AUTH_USER.field.toLowerCase(), user);
    }
    try {
      final String key =
          computeKey(hmacSha256, extraKey, treeMap, decoderQuery.path());
      return new String[] { date.toString(), key };
    } catch (final Exception e) {
      throw new HttpInvalidAuthenticationException(e);
    }

  }

  /**
   * Check Time only (no signature)
   *
   * @param maxInterval
   *
   * @throws HttpInvalidAuthenticationException
   */
  public void checkTime(long maxInterval)
      throws HttpInvalidAuthenticationException {
    final DateTime dateTime = new DateTime();
    final String date = getXAuthTimestamp();
    if (date != null && !date.isEmpty()) {
      final DateTime received = DateTime.parse(date);
      if (maxInterval > 0) {
        final Duration duration = new Duration(received, dateTime);
        if (Math.abs(duration.getMillis()) >= maxInterval) {
          throw new HttpInvalidAuthenticationException(
              "timestamp is not compatible with the maximum delay allowed");
        }
      }
    } else if (maxInterval > 0) {
      throw new HttpInvalidAuthenticationException(
          "timestamp absent while required");
    }
  }

  /**
   * This implementation of authentication is as follow: if X_AUTH is included
   * in the URI or Header<br>
   * 0) Check that timestamp is correct (|curtime - timestamp| < maxinterval)
   * from ARG_X_AUTH_TIMESTAMP, if
   * maxInterval is 0, not mandatory<br>
   * 1) Get all URI args (except ARG_X_AUTH_KEY itself, but including
   * timestamp), lowered case, in alphabetic
   * order<br>
   * 2) Add an extra Key if not null (from ARG_X_AUTH_INTERNALKEY)<br>
   * 3) Compute an hash (SHA-1 or SHA-256)<br>
   * 4) Compare this hash with ARG_X_AUTH_KEY<br>
   *
   * @param hmacSha256 SHA-256 key to create the signature
   * @param extraKey will be added as ARG_X_AUTH_INTERNALKEY might be
   *     null
   * @param maxInterval ARG_X_AUTH_TIMESTAMP will be tested if value >
   *     0
   *
   * @throws HttpInvalidAuthenticationException if the authentication
   *     failed
   */
  public void checkBaseAuthent(HmacSha256 hmacSha256, String extraKey,
                               long maxInterval)
      throws HttpInvalidAuthenticationException {
    final TreeMap<String, String> treeMap = new TreeMap<String, String>();
    final String argPath = getUri();
    final ObjectNode arguri = getUriArgs();
    if (arguri == null) {
      throw new HttpInvalidAuthenticationException("Not enough argument");
    }
    final Iterator<Entry<String, JsonNode>> iterator = arguri.fields();
    final DateTime dateTime = new DateTime();
    DateTime received = null;
    while (iterator.hasNext()) {
      final Entry<String, JsonNode> entry = iterator.next();
      final String key = entry.getKey();
      if (key.equalsIgnoreCase(REST_ROOT_FIELD.ARG_X_AUTH_KEY.field)) {
        continue;
      }
      final JsonNode values = entry.getValue();
      if (key.equalsIgnoreCase(REST_ROOT_FIELD.ARG_X_AUTH_TIMESTAMP.field)) {
        received = DateTime.parse(values.asText());
      }
      final String keylower = key.toLowerCase();
      if (values != null) {
        String val;
        if (values.isArray()) {
          final JsonNode jsonNode = values.get(values.size() - 1);
          val = jsonNode.asText();
        } else {
          val = values.asText();
        }
        treeMap.put(keylower, val);
      }
    }
    if (received == null) {
      final String date = getXAuthTimestamp();
      received = DateTime.parse(date);
      treeMap
          .put(REST_ROOT_FIELD.ARG_X_AUTH_TIMESTAMP.field.toLowerCase(), date);
    }
    final String user = getXAuthUser();
    if (user != null && !user.isEmpty()) {
      treeMap.put(REST_ROOT_FIELD.ARG_X_AUTH_USER.field.toLowerCase(), user);
    }
    if (maxInterval > 0 && received != null) {
      final Duration duration = new Duration(received, dateTime);
      if (Math.abs(duration.getMillis()) >= maxInterval) {
        throw new HttpInvalidAuthenticationException(
            "timestamp is not compatible with the maximum delay allowed");
      }
    } else if (maxInterval > 0) {
      throw new HttpInvalidAuthenticationException(
          "timestamp absent while required");
    }
    final String key = computeKey(hmacSha256, extraKey, treeMap, argPath);
    if (!key.equalsIgnoreCase(getXAuthKey())) {
      throw new HttpInvalidAuthenticationException(
          "Invalid Authentication Key");
    }

  }

  /**
   * @param hmacSha256 SHA-256 key to create the signature
   * @param extraKey might be null
   * @param treeMap
   * @param argPath
   *
   * @throws HttpInvalidAuthenticationException
   */
  protected static String computeKey(HmacSha256 hmacSha256, String extraKey,
                                     TreeMap<String, String> treeMap,
                                     String argPath)
      throws HttpInvalidAuthenticationException {
    final Set<String> keys = treeMap.keySet();
    final StringBuilder builder = new StringBuilder(argPath);
    if (!keys.isEmpty() || extraKey != null) {
      builder.append('?');
    }
    boolean first = true;
    for (final String keylower : keys) {
      if (first) {
        first = false;
      } else {
        builder.append('&');
      }
      builder.append(keylower).append('=').append(treeMap.get(keylower));
    }
    if (extraKey != null) {
      if (!keys.isEmpty()) {
        builder.append('&');
      }
      builder.append(REST_ROOT_FIELD.ARG_X_AUTH_INTERNALKEY.field).append('=')
             .append(extraKey);
    }
    try {
      return hmacSha256.cryptToHex(builder.toString());
    } catch (final Exception e) {
      throw new HttpInvalidAuthenticationException(e);
    }
  }

  @Override
  public String toString() {
    return JsonHandler.writeAsString(arguments);
  }

  public String prettyPrint() {
    return JsonHandler.prettyPrint(arguments);
  }

  public static ObjectNode fillDetailedAllow(METHOD method, String path,
                                             String command, ObjectNode body,
                                             JsonNode result) {
    final ObjectNode node = JsonHandler.createObjectNode();
    final ObjectNode node2 = node.putObject(method.name());
    node2.put(REST_FIELD.JSON_PATH.field, '/' + path);
    node2.put(REST_ROOT_FIELD.JSON_COMMAND.field, command);
    if (body != null) {
      node2.set(REST_FIELD.JSON_JSON.field, body);
    }
    if (result != null) {
      node2.set(REST_GROUP.ARGS_ANSWER.group, result);
    }
    return node;
  }
}
