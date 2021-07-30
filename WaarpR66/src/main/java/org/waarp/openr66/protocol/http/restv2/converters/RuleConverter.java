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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.common.xml.XmlUtil;
import org.waarp.openr66.context.task.TaskType;
import org.waarp.openr66.pojo.Rule;
import org.waarp.openr66.pojo.RuleTask;
import org.waarp.openr66.protocol.http.restv2.errors.RestError;
import org.waarp.openr66.protocol.http.restv2.errors.RestErrorException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.waarp.openr66.protocol.http.restv2.RestConstants.RuleFields.*;
import static org.waarp.openr66.protocol.http.restv2.errors.RestErrors.*;

/**
 * A collection of utility methods to convert {@link Rule} objects to their
 * corresponding {@link ObjectNode}
 * and vice-versa.
 */
public final class RuleConverter {

  /**
   * Makes the default constructor of this utility class inaccessible.
   */
  private RuleConverter() throws InstantiationException {
    throw new InstantiationException(
        getClass().getName() + " cannot be instantiated.");
  }

  // ########################### INNER CLASSES ################################

  /**
   * All the possible ways to sort a list of rule objects.
   */
  public enum Order {
    /**
     * By ruleID, in ascending order.
     */
    ascName(new Comparator<Rule>() {
      @Override
      public final int compare(final Rule t1, final Rule t2) {
        return t1.getName().compareTo(t2.getName());
      }
    }),
    /**
     * By ruleID, in descending order.
     */
    descName(new Comparator<Rule>() {
      @Override
      public final int compare(final Rule t1, final Rule t2) {
        return -t1.getName().compareTo(t2.getName());//NOSONAR
      }
    });

    /**
     * The {@link Comparator} used to sort a list of {@link Rule}.
     */
    public final Comparator<Rule> comparator;

    Order(final Comparator<Rule> comparator) {
      this.comparator = comparator;
    }
  }

  /**
   * The different modes of file transfer.
   */
  public enum ModeTrans {
    /**
     * From requester to requested.
     */
    send(1),
    /**
     * From requested to requester.
     */
    receive(2),
    /**
     * From requester to requested, with MD5 checksum verification.
     */
    sendMD5(3),
    /**
     * From requested to requester, with MD5 checksum verification.
     */
    receiveMD5(4),
    /**
     * From requester to requested, using Through mode.
     */
    SENDTHROUGHMODE(5),
    /**
     * From requested to requester, using Through mode.
     */
    RECVTHROUGHMODE(6),
    /**
     * From requester to requested, with MD5 checksum verification and
     * Through mode.
     */
    SENDMD5THROUGHMODE(7),
    /**
     * From requested to requester, with MD5 checksum verification and
     * Through mode.
     */
    RECVMD5THROUGHMODE(8);

    /**
     * The database code of the transfer mode.
     */
    public final int code;

    ModeTrans(final int code) {
      this.code = code;
    }

    /**
     * Returns the ModeTrans value corresponding to the give code.
     *
     * @param code the desired code
     *
     * @return the corresponding ModeTrans value
     *
     * @throws IllegalArgumentException if the code does not
     *     correspond to a ModeTrans value
     */
    public static ModeTrans fromCode(final int code) {
      for (final ModeTrans mode : values()) {
        if (mode.code == code) {
          return mode;
        }
      }
      throw new IllegalArgumentException("Code unknown: " + code);
    }
  }

  // ########################### PUBLIC METHODS ###############################

  /**
   * Converts the given {@link Rule} object into an {@link ObjectNode}.
   *
   * @param rule the host to convert
   *
   * @return the converted ObjectNode
   */
  public static ObjectNode ruleToNode(final Rule rule) {
    final ObjectNode node = JsonHandler.createObjectNode();
    node.put(RULE_NAME, rule.getName());
    node.putArray(HOST_IDS).addAll(getHostIdsArray(rule.getHostids()));
    node.put(MODE_TRANS, ModeTrans.fromCode(rule.getMode()).name());
    node.put(RECV_PATH, rule.getRecvPath());
    node.put(SEND_PATH, rule.getSendPath());
    node.put(ARCHIVE_PATH, rule.getArchivePath());
    node.put(WORK_PATH, rule.getWorkPath());
    node.set(R_PRE_TASKS, getTaskArray(rule.getRPreTasks()));
    node.set(R_POST_TASKS, getTaskArray(rule.getRPostTasks()));
    node.set(R_ERROR_TASKS, getTaskArray(rule.getRErrorTasks()));
    node.set(S_PRE_TASKS, getTaskArray(rule.getSPreTasks()));
    node.set(S_POST_TASKS, getTaskArray(rule.getSPostTasks()));
    node.set(S_ERROR_TASKS, getTaskArray(rule.getSErrorTasks()));

    return node;
  }

  /**
   * Converts the given {@link ObjectNode} into a {@link Rule} object.
   *
   * @param object the ObjectNode to convert
   *
   * @return the corresponding Rule object
   *
   * @throws RestErrorException If the given ObjectNode does not
   *     represent a Rule object.
   */
  public static Rule nodeToNewRule(final ObjectNode object) {
    Rule emptyRule = null;
    try {
      emptyRule = new Rule(null, -1, new ArrayList<String>(), "", "", "", "",
                           new ArrayList<RuleTask>(), new ArrayList<RuleTask>(),
                           new ArrayList<RuleTask>(), new ArrayList<RuleTask>(),
                           new ArrayList<RuleTask>(),
                           new ArrayList<RuleTask>());
    } catch (final WaarpDatabaseSqlException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
    }

    return nodeToUpdatedRule(object, emptyRule);
  }

  /**
   * Returns the given {@link Rule} object updated with the values defined in
   * the {@link ObjectNode} parameter.
   * All fields missing in the JSON object will stay unchanged in the updated
   * host entry.
   *
   * @param object the ObjectNode to convert.
   * @param oldRule the rule entry to update.
   *
   * @return the updated host entry
   *
   * @throws RestErrorException if the given ObjectNode does not
   *     represent a Rule object
   */
  public static Rule nodeToUpdatedRule(final ObjectNode object,
                                       final Rule oldRule) {
    final List<RestError> errors = new ArrayList<RestError>();

    final Iterator<Map.Entry<String, JsonNode>> fields = object.fields();
    while (fields.hasNext()) {
      final Map.Entry<String, JsonNode> field = fields.next();
      final String name = field.getKey();
      final JsonNode value = field.getValue();

      if (name.equalsIgnoreCase(RULE_NAME)) {
        if (value.isTextual()) {
          if (oldRule.getName() == null) {
            oldRule.setName(XmlUtil.getExtraTrimed(value.asText()));
          } else if (!oldRule.getName()
                             .equals(XmlUtil.getExtraTrimed(value.asText()))) {
            errors.add(FIELD_NOT_ALLOWED(RULE_NAME));
          }
        } else {
          errors.add(ILLEGAL_FIELD_VALUE(RULE_NAME, XmlUtil.getExtraTrimed(
              value.toString())));
        }
      } else if (name.equalsIgnoreCase(HOST_IDS)) {
        if (value.isArray()) {
          final List<String> hosts = new ArrayList<String>();
          final Iterator<JsonNode> elements = value.elements();
          while (elements.hasNext()) {
            final JsonNode element = elements.next();
            if (element.isTextual()) {
              hosts.add(XmlUtil.getExtraTrimed(element.asText()));
            } else {
              errors.add(ILLEGAL_FIELD_VALUE(HOST_IDS, XmlUtil.getExtraTrimed(
                  value.toString())));
            }
          }
          oldRule.setHostids(hosts);
        } else {
          errors.add(ILLEGAL_FIELD_VALUE(HOST_IDS, XmlUtil.getExtraTrimed(
              value.toString())));
        }
      } else if (name.equalsIgnoreCase(MODE_TRANS)) {
        if (value.isTextual()) {
          try {
            final ModeTrans modeTrans =
                ModeTrans.valueOf(XmlUtil.getExtraTrimed(value.asText()));
            oldRule.setMode(modeTrans.code);
          } catch (final IllegalArgumentException e) {
            errors.add(ILLEGAL_FIELD_VALUE(MODE_TRANS, XmlUtil.getExtraTrimed(
                value.toString())));
          }
        } else {
          errors.add(ILLEGAL_FIELD_VALUE(MODE_TRANS, XmlUtil.getExtraTrimed(
              value.toString())));
        }
      } else if (name.equalsIgnoreCase(RECV_PATH)) {
        if (value.isTextual()) {
          oldRule.setRecvPath(XmlUtil.getExtraTrimed(value.asText()));
        } else {
          errors.add(ILLEGAL_FIELD_VALUE(RECV_PATH, XmlUtil.getExtraTrimed(
              value.toString())));
        }
      } else if (name.equalsIgnoreCase(SEND_PATH)) {
        if (value.isTextual()) {
          oldRule.setSendPath(XmlUtil.getExtraTrimed(value.asText()));
        } else {
          errors.add(ILLEGAL_FIELD_VALUE(SEND_PATH, XmlUtil.getExtraTrimed(
              value.toString())));
        }
      } else if (name.equalsIgnoreCase(ARCHIVE_PATH)) {
        if (value.isTextual()) {
          oldRule.setArchivePath(XmlUtil.getExtraTrimed(value.asText()));
        } else {
          errors.add(ILLEGAL_FIELD_VALUE(ARCHIVE_PATH, XmlUtil.getExtraTrimed(
              value.toString())));
        }
      } else if (name.equalsIgnoreCase(WORK_PATH)) {
        if (value.isTextual()) {
          oldRule.setWorkPath(XmlUtil.getExtraTrimed(value.asText()));
        } else {
          errors.add(ILLEGAL_FIELD_VALUE(WORK_PATH, XmlUtil.getExtraTrimed(
              value.toString())));
        }
      } else if (name.equalsIgnoreCase(R_PRE_TASKS)) {
        if (value.isArray()) {
          oldRule.setRPreTasks(parseTasks((ArrayNode) value, R_PRE_TASKS));
        } else {
          errors.add(ILLEGAL_FIELD_VALUE(R_PRE_TASKS, XmlUtil.getExtraTrimed(
              value.toString())));
        }
      } else if (name.equalsIgnoreCase(R_POST_TASKS)) {
        if (value.isArray()) {
          oldRule.setRPostTasks(parseTasks((ArrayNode) value, R_POST_TASKS));
        } else {
          errors.add(ILLEGAL_FIELD_VALUE(R_POST_TASKS, XmlUtil.getExtraTrimed(
              value.toString())));
        }
      } else if (name.equalsIgnoreCase(R_ERROR_TASKS)) {
        if (value.isArray()) {
          oldRule.setRErrorTasks(parseTasks((ArrayNode) value, R_ERROR_TASKS));
        } else {
          errors.add(ILLEGAL_FIELD_VALUE(R_ERROR_TASKS, XmlUtil.getExtraTrimed(
              value.toString())));
        }
      } else if (name.equalsIgnoreCase(S_PRE_TASKS)) {
        if (value.isArray()) {
          oldRule.setSPreTasks(parseTasks((ArrayNode) value, S_PRE_TASKS));
        } else {
          errors.add(ILLEGAL_FIELD_VALUE(S_PRE_TASKS, XmlUtil.getExtraTrimed(
              value.toString())));
        }
      } else if (name.equalsIgnoreCase(S_POST_TASKS)) {
        if (value.isArray()) {
          oldRule.setSPostTasks(parseTasks((ArrayNode) value, S_POST_TASKS));
        } else {
          errors.add(ILLEGAL_FIELD_VALUE(S_POST_TASKS, XmlUtil.getExtraTrimed(
              value.toString())));
        }
      } else if (name.equalsIgnoreCase(S_ERROR_TASKS)) {
        if (value.isArray()) {
          oldRule.setSErrorTasks(parseTasks((ArrayNode) value, S_ERROR_TASKS));
        } else {
          errors.add(ILLEGAL_FIELD_VALUE(S_ERROR_TASKS, XmlUtil.getExtraTrimed(
              value.toString())));
        }
      } else {
        errors.add(UNKNOWN_FIELD(name));
      }
    }

    errors.addAll(checkRequiredFields(oldRule));

    if (errors.isEmpty()) {
      return oldRule;
    } else {
      throw new RestErrorException(errors);
    }
  }

  // ########################## PRIVATE METHODS ###############################

  /**
   * Converts the given List of host names into an {@link ArrayNode}.
   *
   * @param hostIds the list to convert
   *
   * @return the corresponding ArrayNode
   */
  private static ArrayNode getHostIdsArray(final List<String> hostIds) {
    final ArrayNode array = JsonHandler.createArrayNode();
    for (final String host : hostIds) {
      array.add(host);
    }
    return array;
  }

  /**
   * Converts the given List of {@link RuleTask} into an {@link ArrayNode}.
   *
   * @param tasks the list to convert
   *
   * @return the corresponding ArrayNode
   */
  private static ArrayNode getTaskArray(final List<RuleTask> tasks) {
    final ArrayNode array = JsonHandler.createArrayNode();
    for (final RuleTask task : tasks) {
      final ObjectNode object = JsonHandler.createObjectNode();
      object.put(TASK_TYPE, task.getType());
      object.put(TASK_ARGUMENTS, task.getPath());
      object.put(TASK_DELAY, task.getDelay());
      array.add(object);
    }
    return array;
  }

  /**
   * List all missing required fields. This method returns a list of {@link
   * RestError} representing all the
   * errors encountered when checking the given host's required fields. If all
   * required fields have indeed been
   * initialized, an empty list is returned.
   *
   * @param rule the host entry to check
   *
   * @return the list of encountered errors
   */
  private static List<RestError> checkRequiredFields(final Rule rule) {
    final List<RestError> errors = new ArrayList<RestError>();
    if (ParametersChecker.isEmpty(rule.getName())) {
      errors.add(MISSING_FIELD(RULE_NAME));
    }
    if (rule.getMode() == -1) {
      errors.add(MISSING_FIELD(MODE_TRANS));
    }
    return errors;
  }

  /**
   * Converts the given {@link ArrayNode} into a List of {@link RuleTask}.
   *
   * @param array the ArrayNode to convert
   * @param fieldName the name of the field containing the ArrayNode
   *
   * @return the extracted list of RuleTask
   *
   * @throws RestErrorException ff the ArrayNode does not represent a
   *     list of RuleTask objects
   */
  private static List<RuleTask> parseTasks(final ArrayNode array,
                                           final String fieldName) {
    final List<RuleTask> result = new ArrayList<RuleTask>();
    final List<RestError> errors = new ArrayList<RestError>();

    final Iterator<JsonNode> elements = array.elements();
    while (elements.hasNext()) {
      final JsonNode element = elements.next();

      if (element.isObject()) {
        final RuleTask task = new RuleTask("", "", 0);
        final Iterator<Map.Entry<String, JsonNode>> fields = element.fields();
        while (fields.hasNext()) {
          final Map.Entry<String, JsonNode> field = fields.next();
          final String name = field.getKey();
          final JsonNode value = field.getValue();

          if (name.equalsIgnoreCase(TASK_TYPE)) {
            if (value.isTextual()) {
              final String taskType = XmlUtil.getExtraTrimed(value.asText());
              if (TaskType.isValidTask(taskType)) {
                task.setType(taskType);
              } else {
                errors.add(ILLEGAL_FIELD_VALUE(TASK_TYPE, taskType));
              }
            } else {
              errors.add(ILLEGAL_FIELD_VALUE(TASK_TYPE, XmlUtil.getExtraTrimed(
                  value.toString())));
            }
          } else if (name.equalsIgnoreCase(TASK_ARGUMENTS)) {
            if (value.isTextual()) {
              task.setPath(XmlUtil.getExtraTrimed(value.asText()));
            } else {
              errors.add(ILLEGAL_FIELD_VALUE(TASK_ARGUMENTS,
                                             XmlUtil.getExtraTrimed(
                                                 value.toString())));
            }
          } else if (name.equalsIgnoreCase(TASK_DELAY)) {
            if (value.canConvertToInt() && value.asInt() >= 0) {
              task.setDelay(value.asInt());
            } else {
              errors.add(ILLEGAL_FIELD_VALUE(TASK_DELAY, XmlUtil.getExtraTrimed(
                  value.toString())));
            }
          } else {
            errors.add(UNKNOWN_FIELD(name));
          }
        }
        if (task.getType().isEmpty()) {
          errors.add(MISSING_FIELD(TASK_TYPE));
        } else {
          result.add(task);
        }
      } else {
        errors.add(ILLEGAL_FIELD_VALUE(fieldName, XmlUtil.getExtraTrimed(
            element.toString())));
      }
    }

    if (errors.isEmpty()) {
      return result;
    } else {
      throw new RestErrorException(errors);
    }
  }

}