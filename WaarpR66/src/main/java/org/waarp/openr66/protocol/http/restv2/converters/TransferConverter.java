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
import org.joda.time.DateTime;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.dao.HostDAO;
import org.waarp.openr66.dao.RuleDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.Rule;
import org.waarp.openr66.pojo.Transfer;
import org.waarp.openr66.pojo.UpdatedInfo;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.http.restv2.converters.RuleConverter.ModeTrans;
import org.waarp.openr66.protocol.http.restv2.errors.RestError;
import org.waarp.openr66.protocol.http.restv2.errors.RestErrorException;

import javax.ws.rs.InternalServerErrorException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.waarp.common.file.FileUtils.*;
import static org.waarp.openr66.dao.database.DBTransferDAO.*;
import static org.waarp.openr66.protocol.http.restv2.RestConstants.*;
import static org.waarp.openr66.protocol.http.restv2.RestConstants.TransferFields.*;
import static org.waarp.openr66.protocol.http.restv2.errors.RestErrors.*;

/**
 * A collection of utility methods to convert {@link Transfer} objects to their
 * corresponding
 * {@link ObjectNode} and vice-versa.
 */
public final class TransferConverter {

  /**
   * Makes the default constructor of this utility class inaccessible.
   */
  private TransferConverter() throws InstantiationException {
    throw new InstantiationException(
        getClass().getName() + " cannot be instantiated.");
  }

  // ########################### INNER CLASSES ################################

  /**
   * All the possible ways to order a list of transfer objects.
   */
  public enum Order {
    /**
     * By transferId, in ascending order.
     */
    ascId(ID_FIELD, true),
    /**
     * By transferId, in descending order.
     */
    descId(ID_FIELD, false),
    /**
     * By fileName, in ascending order.
     */
    ascFile(ORIGINAL_NAME_FIELD, true),
    /**
     * By fileName, in descending order.
     */
    descFile(ORIGINAL_NAME_FIELD, false),
    /**
     * By starting date, in ascending order.
     */
    ascStart(TRANSFER_START_FIELD, true),
    /**
     * By starting date, in descending order.
     */
    descStart(TRANSFER_START_FIELD, false),
    /**
     * By end date, in ascending order.
     */
    ascStop(TRANSFER_STOP_FIELD, true),
    /**
     * By end date, in descending order.
     */
    descStop(TRANSFER_STOP_FIELD, false);

    /**
     * The name of the database column used for sorting.
     */
    public final String column;
    /**
     * If the order is ascending or descending.
     */
    public final boolean ascend;

    Order(final String column, final boolean ascend) {
      this.column = column;
      this.ascend = ascend;
    }
  }

  // ########################## PUBLIC METHODS ################################

  /**
   * Returns an {@link ObjectNode} representing the {@link Transfer} object
   * given as parameter.
   *
   * @param transfer the Transfer object to serialize
   *
   * @return the corresponding ObjectNode
   */
  public static ObjectNode transferToNode(final Transfer transfer) {
    final ObjectNode node = JsonHandler.createObjectNode();
    node.put(TRANSFER_ID, transfer.getId());
    node.put(GLOBAL_STEP, transfer.getGlobalStep().toString());
    node.put(GLOBAL_LAST_STEP, transfer.getLastGlobalStep().toString());
    node.put(STEP, transfer.getStep());
    node.put(RANK, transfer.getRank());
    node.put(UPDATED_INFO, transfer.getUpdatedInfo().toString());
    node.put(STEP_STATUS, transfer.getStepStatus().toString());
    node.put(ERROR_CODE, transfer.getInfoStatus().code);
    node.put(ERROR_MESSAGE, transfer.getInfoStatus().getMesg());
    node.put(ORIGINAL_FILENAME, transfer.getOriginalName());
    node.put(FILENAME, transfer.getFilename());
    node.put(RULE, transfer.getRule());
    node.put(BLOCK_SIZE, transfer.getBlockSize());
    node.put(FILE_INFO, transfer.getFileInfo());
    node.put(TRANSFER_INFO, transfer.getTransferInfo());
    node.put(START, new DateTime(transfer.getStart()).toString());
    node.put(STOP, new DateTime(transfer.getStop()).toString());
    node.put(REQUESTED, transfer.getRequested());
    node.put(REQUESTER, transfer.getRequester());
    node.put(RETRIEVE, transfer.getRetrieveMode());

    return node;
  }

  /**
   * Initialize a {@link Transfer} object using the values of the given {@link
   * ObjectNode}.
   *
   * @param object the ObjectNode to convert
   *
   * @return the new Transfer object
   *
   * @throws RestErrorException if the given ObjectNode does not
   *     represent a Transfer object
   * @throws InternalServerErrorException if an unexpected error
   *     occurred
   */
  public static Transfer nodeToNewTransfer(final ObjectNode object) {
    Transfer defaultTransfer = null;
    try {
      defaultTransfer =
          new Transfer(null, null, -1, false, null, null, ZERO_COPY_CHUNK_SIZE);
    } catch (WaarpDatabaseSqlException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      throw new IllegalArgumentException(e);
    }
    defaultTransfer.setRequester(serverName());
    defaultTransfer.setOwnerRequest(serverName());
    defaultTransfer.setBlockSize(Configuration.configuration.getBlockSize());
    defaultTransfer.setTransferInfo("{}");
    defaultTransfer.setStart(new Timestamp(DateTime.now().getMillis()));
    final Transfer transfer = parseNode(object, defaultTransfer);

    ModeTrans mode;
    RuleDAO ruleDAO = null;
    try {
      ruleDAO = DAO_FACTORY.getRuleDAO(true);
      final Rule rule = ruleDAO.select(transfer.getRule());
      mode = ModeTrans.fromCode(rule.getMode());
    } catch (final DAOConnectionException e) {
      throw new InternalServerErrorException(e);
    } catch (final DAONoDataException e) {
      throw new InternalServerErrorException(e);
    } finally {
      DAOFactory.closeDAO(ruleDAO);
    }

    transfer.setRetrieveMode(
        mode == ModeTrans.receive || mode == ModeTrans.receiveMD5);
    transfer.setTransferMode(mode.code);
    transfer.setStop(transfer.getStart());
    transfer.setUpdatedInfo(UpdatedInfo.TOSUBMIT);

    return transfer;
  }

  // ######################### PRIVATE METHODS ################################

  /**
   * Tells if the given rule exists in the database.
   *
   * @param rule the name of the rule
   *
   * @return {@code true} if the rule exists, {@code false} otherwise.
   */
  private static boolean ruleExists(final String rule) {
    RuleDAO ruleDAO = null;
    try {
      ruleDAO = DAO_FACTORY.getRuleDAO(true);
      return ruleDAO.exist(rule);
    } catch (final DAOConnectionException e) {
      throw new InternalServerErrorException(e);
    } finally {
      DAOFactory.closeDAO(ruleDAO);
    }
  }

  /**
   * Tells if the given host exists in the database.
   *
   * @param host the name of the host
   *
   * @return {@code true} if the host exists, {@code false} otherwise.
   */
  private static boolean hostExists(final String host) {
    HostDAO hostDAO = null;
    try {
      hostDAO = DAO_FACTORY.getHostDAO(true);
      return hostDAO.exist(host);
    } catch (final DAOConnectionException e) {
      throw new InternalServerErrorException(e);
    } finally {
      DAOFactory.closeDAO(hostDAO);
    }
  }

  /**
   * Tells if the given host is allowed to use given rule.
   *
   * @param host the name of the host
   * @param rule the name of the rule
   *
   * @return {@code true} if the host is allowed to use the rule, {@code
   *     false} otherwise
   */
  private static boolean canUseRule(final String host, final String rule) {
    RuleDAO ruleDAO = null;
    try {
      ruleDAO = DAO_FACTORY.getRuleDAO(true);
      final List<String> hostIds = ruleDAO.select(rule).getHostids();
      return !hostIds.isEmpty() && !hostIds.contains(host);
    } catch (final DAOConnectionException e) {
      throw new InternalServerErrorException(e);
    } catch (final DAONoDataException e) {
      throw new InternalServerErrorException(e);
    } finally {
      DAOFactory.closeDAO(ruleDAO);
    }
  }

  /**
   * Returns a list of {@link RestError} corresponding to all the fields
   * required to initialize a transfer that
   * are missing from the given {@link Transfer} object. If no fields are
   * missing, an empty list is returned.
   *
   * @param transfer the Transfer object to check.
   *
   * @return the list of all missing fields
   */
  private static List<RestError> checkRequiredFields(final Transfer transfer) {
    final List<RestError> errors = new ArrayList<RestError>();
    if (ParametersChecker.isEmpty(transfer.getRule())) {
      errors.add(MISSING_FIELD(RULE));
    }
    if (ParametersChecker.isEmpty(transfer.getOriginalName())) {
      errors.add(MISSING_FIELD(FILENAME));
    }
    if (ParametersChecker.isEmpty(transfer.getRequested())) {
      errors.add(MISSING_FIELD(REQUESTED));
    }

    return errors;
  }

  /**
   * Fills the fields of the given {@link Transfer} object with the values
   * extracted from the {@link ObjectNode}
   * parameter, and returns the result.
   *
   * @param object the ObjectNode from which the values should be
   *     extracted
   * @param transfer the Transfer object whose fields will be filled
   *
   * @return the filled Transfer object
   *
   * @throws RestErrorException if the given ObjectNode does not
   *     represent a Transfer object.
   */
  private static Transfer parseNode(final ObjectNode object,
                                    final Transfer transfer) {
    final List<RestError> errors = new ArrayList<RestError>();

    final Iterator<Map.Entry<String, JsonNode>> fields = object.fields();
    while (fields.hasNext()) {
      final Map.Entry<String, JsonNode> field = fields.next();
      final String name = field.getKey();
      final JsonNode value = field.getValue();

      if (name.equalsIgnoreCase(RULE)) {
        if (value.isTextual()) {
          if (ruleExists(value.asText())) {
            transfer.setRule(value.asText());
          } else {
            errors.add(UNKNOWN_RULE(value.asText()));
          }
        } else {
          errors.add(ILLEGAL_FIELD_VALUE(name, value.toString()));
        }
      } else if (name.equalsIgnoreCase(FILENAME)) {
        if (value.isTextual()) {
          transfer.setOriginalName(value.asText());
          transfer.setFilename(value.asText());
        } else {
          errors.add(ILLEGAL_FIELD_VALUE(name, value.toString()));
        }
      } else if (name.equalsIgnoreCase(REQUESTED)) {
        if (value.isTextual()) {
          if (hostExists(value.asText())) {
            transfer.setRequested(value.asText());
            try {
              transfer.setRequester(
                  Configuration.configuration.getHostId(value.asText()));
            } catch (final WaarpDatabaseException e) {
              // Ignore !!
            }
          } else {
            errors.add(UNKNOWN_HOST(value.asText()));
          }
        } else {
          errors.add(ILLEGAL_FIELD_VALUE(name, value.toString()));
        }
      } else if (name.equalsIgnoreCase(BLOCK_SIZE)) {
        if (value.canConvertToInt() && value.asInt() > 0) {
          transfer.setBlockSize(value.asInt());
        } else {
          errors.add(ILLEGAL_FIELD_VALUE(name, value.toString()));
        }
      } else if (name.equalsIgnoreCase(FILE_INFO)) {
        if (value.isTextual()) {
          transfer.setFileInfo(value.asText());
        } else {
          errors.add(ILLEGAL_FIELD_VALUE(name, value.toString()));
        }
      } else if (name.equalsIgnoreCase(TRANSFER_INFO)) {
        if (value.isTextual()) {
          transfer.setTransferInfo(value.asText());
        } else {
          errors.add(ILLEGAL_FIELD_VALUE(name, value.toString()));
        }
      } else if (name.equalsIgnoreCase(START)) {
        if (value.isTextual()) {
          try {
            final DateTime start = DateTime.parse(value.asText());
            if (start.isBeforeNow()) {
              errors.add(ILLEGAL_FIELD_VALUE(name, value.asText()));
            } else {
              transfer.setStart(new Timestamp(start.getMillis()));
            }
          } catch (final IllegalArgumentException e) {
            errors.add(ILLEGAL_FIELD_VALUE(name, value.asText()));
          }
        } else {
          errors.add(ILLEGAL_FIELD_VALUE(name, value.toString()));
        }
      }
    }

    // check that both hosts are allowed to use the transfer rule
    final String rule = transfer.getRule();
    final String requested = transfer.getRequested();
    final String requester = transfer.getRequester();
    if (rule != null && !requested.isEmpty() && canUseRule(requested, rule)) {
      errors.add(RULE_NOT_ALLOWED(requested, rule));
    }
    if (rule != null && !requester.isEmpty() && canUseRule(requester, rule)) {
      errors.add(RULE_NOT_ALLOWED(requester, rule));
    }

    errors.addAll(checkRequiredFields(transfer));

    if (errors.isEmpty()) {
      return transfer;
    } else {
      throw new RestErrorException(errors);
    }
  }
}
