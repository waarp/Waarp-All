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
package org.waarp.openr66.database.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseNoDataException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.file.DirInterface;
import org.waarp.common.file.FileUtils;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.common.xml.XmlUtil;
import org.waarp.common.xml.XmlValue;
import org.waarp.openr66.configuration.RuleFileBasedConfiguration;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.dao.AbstractDAO;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.RuleDAO;
import org.waarp.openr66.dao.database.DBRuleDAO;
import org.waarp.openr66.dao.database.StatementExecutor;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.database.data.DbTaskRunner.TASKSTEP;
import org.waarp.openr66.pojo.Rule;
import org.waarp.openr66.pojo.RuleTask;
import org.waarp.openr66.pojo.Transfer;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.localhandler.packet.RequestPacket;

import java.io.File;
import java.io.StringReader;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Rule Table object
 */
public class DbRule extends AbstractDbDataDao<Rule> {
  private static final String RULE_NOT_FOUND = "Rule not found";
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(DbRule.class);
  private static final String[] STRING_0_LENGTH = new String[0];
  private static final String[][] STRINGS_0_0_LENGTH = new String[0][0];

  public enum Columns {
    HOSTIDS, MODETRANS, RECVPATH, SENDPATH, ARCHIVEPATH, WORKPATH, RPRETASKS,
    RPOSTTASKS, RERRORTASKS, SPRETASKS, SPOSTTASKS, SERRORTASKS, UPDATEDINFO,
    IDRULE
  }

  public static final int[] dbTypes = {
      Types.LONGVARCHAR, Types.INTEGER, Types.VARCHAR, Types.VARCHAR,
      Types.VARCHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.LONGVARCHAR,
      Types.LONGVARCHAR, Types.LONGVARCHAR, Types.LONGVARCHAR,
      Types.LONGVARCHAR, Types.INTEGER, Types.NVARCHAR
  };

  public static final String table = " RULES ";

  public static final Columns[] indexes = {
      Columns.UPDATEDINFO
  };

  /**
   * Internal context XML fields
   */
  public static final String TASK_TYPE = "type";

  /**
   * Internal context XML fields
   */
  public static final String TASK_PATH = "path";

  /**
   * Internal context XML fields
   */
  public static final String TASK_DELAY = "delay";
  /**
   * Internal context XML fields
   */
  public static final String TASK_RANK = "rank";
  /**
   * Internal context XML fields
   */
  public static final String TASK_COMMENT = "comment";

  protected static final String selectAllFields =
      Columns.HOSTIDS.name() + ',' + Columns.MODETRANS.name() + ',' +
      Columns.RECVPATH.name() + ',' + Columns.SENDPATH.name() + ',' +
      Columns.ARCHIVEPATH.name() + ',' + Columns.WORKPATH.name() + ',' +
      Columns.RPRETASKS.name() + ',' + Columns.RPOSTTASKS.name() + ',' +
      Columns.RERRORTASKS.name() + ',' + Columns.SPRETASKS.name() + ',' +
      Columns.SPOSTTASKS.name() + ',' + Columns.SERRORTASKS.name() + ',' +
      Columns.UPDATEDINFO.name() + ',' + Columns.IDRULE.name();

  @Override
  protected final void initObject() {
    // Nothing
  }

  @Override
  protected final String getTable() {
    return table;
  }

  @Override
  protected final AbstractDAO<Rule> getDao(final boolean isCacheable)
      throws DAOConnectionException {
    return DAOFactory.getInstance().getRuleDAO(isCacheable);
  }

  @Override
  protected final String getPrimaryKey() {
    if (pojo != null) {
      return pojo.getName();
    }
    throw new IllegalArgumentException("pojo is null");
  }

  @Override
  protected final String getPrimaryField() {
    return Columns.IDRULE.name();
  }

  protected final void checkPath() {
    if (ParametersChecker.isNotEmpty(getRecvPath()) &&
        getRecvPath().charAt(0) != DirInterface.SEPARATORCHAR) {
      pojo.setRecvPath(DirInterface.SEPARATOR + getRecvPath());
    }
    if (ParametersChecker.isNotEmpty(getSendPath()) &&
        getSendPath().charAt(0) != DirInterface.SEPARATORCHAR) {
      pojo.setSendPath(DirInterface.SEPARATOR + getSendPath());
    }
    if (ParametersChecker.isNotEmpty(getArchivePath()) &&
        getArchivePath().charAt(0) != DirInterface.SEPARATORCHAR) {
      pojo.setArchivePath(DirInterface.SEPARATOR + getArchivePath());
    }
    if (ParametersChecker.isNotEmpty(getWorkPath()) &&
        getWorkPath().charAt(0) != DirInterface.SEPARATORCHAR) {
      pojo.setWorkPath(DirInterface.SEPARATOR + getWorkPath());
    }
  }

  /**
   * @param idRule
   * @param ids
   * @param mode
   * @param recvPath
   * @param sendPath
   * @param archivePath
   * @param workPath
   * @param rpreTasks
   * @param rpostTasks
   * @param rerrorTasks
   * @param spreTasks
   * @param spostTasks
   * @param serrorTasks
   */
  public DbRule(final String idRule, final String ids, final int mode,
                final String recvPath, final String sendPath,
                final String archivePath, final String workPath,
                final String rpreTasks, final String rpostTasks,
                final String rerrorTasks, final String spreTasks,
                final String spostTasks, final String serrorTasks)
      throws WaarpDatabaseSqlException {
    pojo = new Rule(idRule, mode, Arrays.asList(getIdsRule(ids)), recvPath,
                    sendPath, archivePath, workPath,
                    fromLegacyTasks(getTasksRule(rpreTasks)),
                    fromLegacyTasks(getTasksRule(rpostTasks)),
                    fromLegacyTasks(getTasksRule(rerrorTasks)),
                    fromLegacyTasks(getTasksRule(spreTasks)),
                    fromLegacyTasks(getTasksRule(spostTasks)),
                    fromLegacyTasks(getTasksRule(serrorTasks)));
    checkPath();
  }

  /**
   * @param idRule
   *
   * @throws WaarpDatabaseException
   */
  public DbRule(final String idRule) throws WaarpDatabaseException {
    RuleDAO ruleAccess = null;
    try {
      ruleAccess = DAOFactory.getInstance().getRuleDAO(true);
      pojo = ruleAccess.select(idRule);
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } catch (final DAONoDataException e) {
      throw new WaarpDatabaseNoDataException(RULE_NOT_FOUND + " " + idRule, e);
    } finally {
      DAOFactory.closeDAO(ruleAccess);
    }
    checkPath();
  }

  public DbRule(final Rule rule) {
    if (rule == null) {
      throw new IllegalArgumentException(
          "Argument in constructor cannot be null");
    }
    this.pojo = rule;
    checkPath();
  }

  /**
   * Constructor used from XML file
   *
   * @param idrule
   * @param idsArrayRef
   * @param recvpath
   * @param sendpath
   * @param archivepath
   * @param workpath
   * @param rpretasksArray
   * @param rposttasksArray
   * @param rerrortasksArray
   * @param spretasksArray
   * @param sposttasksArray
   * @param serrortasksArray
   */
  public DbRule(final String idrule, String[] idsArrayRef, final int mode,
                final String recvpath, final String sendpath,
                final String archivepath, final String workpath,
                final String[][] rpretasksArray,
                final String[][] rposttasksArray,
                final String[][] rerrortasksArray,
                final String[][] spretasksArray,
                final String[][] sposttasksArray,
                final String[][] serrortasksArray)
      throws WaarpDatabaseSqlException {
    if (idsArrayRef == null) {
      idsArrayRef = STRING_0_LENGTH;
    }
    pojo =
        new Rule(idrule, mode, Arrays.asList(idsArrayRef), recvpath, sendpath,
                 archivepath, workpath, fromLegacyTasks(rpretasksArray),
                 fromLegacyTasks(rposttasksArray),
                 fromLegacyTasks(rerrortasksArray),
                 fromLegacyTasks(spretasksArray),
                 fromLegacyTasks(sposttasksArray),
                 fromLegacyTasks(serrortasksArray));
    checkPath();
  }

  /**
   * Constructor from Json
   *
   * @param source
   *
   * @throws WaarpDatabaseSqlException
   */
  public DbRule(final ObjectNode source) throws WaarpDatabaseSqlException {
    pojo = new Rule();
    setFromJson(source, false);
    if (ParametersChecker.isEmpty(getIdRule())) {
      throw new WaarpDatabaseSqlException(
          "Not enough argument to create the object");
    }
    checkPath();
  }

  @Override
  protected final void checkValues() throws WaarpDatabaseSqlException {
    pojo.checkValues();
  }

  @Override
  public final void setFromJson(final ObjectNode node,
                                final boolean ignorePrimaryKey)
      throws WaarpDatabaseSqlException {
    super.setFromJson(node, ignorePrimaryKey);
    checkPath();
  }

  @Override
  protected final void setFromJson(final String field, final JsonNode value) {
    if (value == null) {
      return;
    }
    for (final Columns column : Columns.values()) {
      if (column.name().equalsIgnoreCase(field)) {
        switch (column) {
          case ARCHIVEPATH:
            pojo.setArchivePath(value.asText());
            break;
          case HOSTIDS:
            pojo.setHostids(Arrays.asList(getIdsRule(value.asText())));
            break;
          case IDRULE:
            pojo.setName(value.asText());
            break;
          case MODETRANS:
            pojo.setMode(value.asInt());
            break;
          case RECVPATH:
            pojo.setRecvPath(value.asText());
            break;
          case RERRORTASKS:
            pojo.setRErrorTasks(fromLegacyTasks(getTasksRule(value.asText())));
            break;
          case RPOSTTASKS:
            pojo.setRPostTasks(fromLegacyTasks(getTasksRule(value.asText())));
            break;
          case RPRETASKS:
            pojo.setRPreTasks(fromLegacyTasks(getTasksRule(value.asText())));
            break;
          case SENDPATH:
            pojo.setSendPath(value.asText());
            break;
          case SERRORTASKS:
            pojo.setSErrorTasks(fromLegacyTasks(getTasksRule(value.asText())));
            break;
          case SPOSTTASKS:
            pojo.setSPostTasks(fromLegacyTasks(getTasksRule(value.asText())));
            break;
          case SPRETASKS:
            pojo.setSPreTasks(fromLegacyTasks(getTasksRule(value.asText())));
            break;
          case WORKPATH:
            pojo.setWorkPath(value.asText());
            break;
          case UPDATEDINFO:
            pojo.setUpdatedInfo(
                org.waarp.openr66.pojo.UpdatedInfo.valueOf(value.asInt()));
            break;
        }
      }
    }
    checkPath();
  }

  /**
   * Private constructor for Commander only
   */
  private DbRule() {
    pojo = new Rule();
  }

  /**
   * Delete object from table DbRule only if no DbTaskRunner is using it.
   *
   * @throws WaarpDatabaseException
   */
  @Override
  public final void delete() throws WaarpDatabaseException {
    AbstractDAO<Transfer> transferDAO = null;
    try {
      transferDAO = DAOFactory.getInstance().getTransferDAO();
      final List<Filter> filters = new ArrayList<Filter>();
      final Filter filter =
          new Filter(DbTaskRunner.Columns.IDRULE.name(), "=", this.getIdRule());
      filters.add(filter);
      final long nb = transferDAO.count(filters);
      if (nb > 0) {
        throw new WaarpDatabaseNoDataException("Rule " + this.getIdRule() +
                                               " is still used by TaskRunner therefore it cannot be deleted.");
      }
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseNoConnectionException(e);
    } finally {
      DAOFactory.closeDAO(transferDAO);
    }
    super.delete();
  }

  /**
   * Delete all Rules but returns original one, therefore not checking if
   * any TaskRunner are using it (used by reloading rules)
   *
   * @return the previously existing DbRule
   *
   * @throws WaarpDatabaseException
   */
  public static DbRule[] deleteAll() throws WaarpDatabaseException {
    RuleDAO ruleAccess = null;
    List<Rule> rules;
    try {
      ruleAccess = DAOFactory.getInstance().getRuleDAO(false);
      rules = ruleAccess.getAll();
      ruleAccess.deleteAll();
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } finally {
      DAOFactory.closeDAO(ruleAccess);
    }
    final DbRule[] res = new DbRule[rules.size()];
    int i = 0;
    for (final Rule rule : rules) {
      res[i] = new DbRule(rule);
      i++;
    }
    return res;
  }

  /**
   * Get All DbRule from database or from internal hashMap in case of no
   * database support
   *
   * @return the array of DbRule
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbRule[] getAllRules()
      throws WaarpDatabaseNoConnectionException {
    RuleDAO ruleAccess = null;
    List<Rule> rules;
    try {
      ruleAccess = DAOFactory.getInstance().getRuleDAO(false);
      rules = ruleAccess.getAll();
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseNoConnectionException(e);
    } finally {
      DAOFactory.closeDAO(ruleAccess);
    }
    final DbRule[] res = new DbRule[rules.size()];
    int i = 0;
    for (final Rule rule : rules) {
      res[i] = new DbRule(rule);
      i++;
    }
    return res;
  }

  /**
   * For instance from Commander when getting updated information
   *
   * @param preparedStatement
   *
   * @return the next updated DbRule
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbRule getFromStatement(
      final DbPreparedStatement preparedStatement)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final DbRule dbRule = new DbRule();
    AbstractDAO<Rule> ruleDAO = null;
    try {
      ruleDAO = dbRule.getDao(false);
      dbRule.pojo = ((StatementExecutor<Rule>) ruleDAO).getFromResultSet(
          preparedStatement.getResultSet());
      return dbRule;
    } catch (final SQLException e) {
      DbSession.error(e);
      throw new WaarpDatabaseSqlException("Getting values in error", e);
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseSqlException("Getting values in error", e);
    } finally {
      DAOFactory.closeDAO(ruleDAO);
    }
  }

  /**
   * @return the DbPreparedStatement for getting Updated Object
   *
   * @throws WaarpDatabaseNoConnectionException
   */
  public static DbRule[] getUpdatedPrepareStament()
      throws WaarpDatabaseNoConnectionException {
    final List<Filter> filters = new ArrayList<Filter>(1);
    filters.add(new Filter(DBRuleDAO.UPDATED_INFO_FIELD, "=",
                           org.waarp.openr66.pojo.UpdatedInfo.fromLegacy(
                               UpdatedInfo.TOSUBMIT).ordinal()));
    RuleDAO ruleAccess = null;
    List<Rule> rules;
    try {
      ruleAccess = DAOFactory.getInstance().getRuleDAO(false);
      rules = ruleAccess.find(filters);
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseNoConnectionException(e);
    } finally {
      DAOFactory.closeDAO(ruleAccess);
    }
    final DbRule[] res = new DbRule[rules.size()];
    int i = 0;
    for (final Rule rule : rules) {
      res[i] = new DbRule(rule);
      i++;
    }
    return res;
  }

  @Override
  public final void changeUpdatedInfo(final UpdatedInfo info) {
    isSaved = false;
    pojo.setUpdatedInfo(org.waarp.openr66.pojo.UpdatedInfo.fromLegacy(info));
  }

  /**
   * Get Ids from String. If it is not ok, then it sets the default values and
   * return False, else returns True.
   *
   * @param idsref
   *
   * @return True if ok, else False (default values).
   */
  private String[] getIdsRule(final String idsref) {
    if (idsref == null) {
      // No ids so setting to the default!
      return STRING_0_LENGTH;
    }
    final StringReader reader = new StringReader(idsref);
    final Document document;
    try {
      document = XmlUtil.getNewSaxReader().read(reader);
      final XmlValue[] values =
          XmlUtil.read(document, RuleFileBasedConfiguration.hostsDecls);
      return RuleFileBasedConfiguration.getHostIds(values[0]);
    } catch (final DocumentException e) {
      logger.warn("Unable to read the ids for Rule: " + idsref + " : {}",
                  e.getMessage());
      // No ids so setting to the default!
      return STRING_0_LENGTH;
    } finally {
      FileUtils.close(reader);
    }
  }

  /**
   * Get Tasks from String. If it is not ok, then it sets the default values
   * and
   * return new array of Tasks or
   * null if in error.
   *
   * @param tasks
   *
   * @return Array of tasks or empty array if in error.
   */
  private String[][] getTasksRule(final String tasks) {
    if (ParametersChecker.isEmpty(tasks)) {
      // No tasks so setting to the default!
      return STRINGS_0_0_LENGTH;
    }
    final StringReader reader = new StringReader(tasks);
    final Document document;
    try {
      document = XmlUtil.getNewSaxReader().read(reader);
    } catch (final DocumentException e) {
      // No tasks so setting to the default!
      FileUtils.close(reader);
      return STRINGS_0_0_LENGTH;
    }
    final XmlValue[] values =
        XmlUtil.read(document, RuleFileBasedConfiguration.tasksDecl);
    final String[][] result =
        RuleFileBasedConfiguration.getTasksRule(values[0]);
    FileUtils.close(reader);
    return result;
  }

  /**
   * Get the full path from RecvPath (used only in copy MODETRANS)
   *
   * @param filename
   *
   * @return the full String path
   */
  public final String setRecvPath(final String filename) {
    if (ParametersChecker.isNotEmpty(pojo.getRecvPath())) {
      return pojo.getRecvPath() + DirInterface.SEPARATOR + filename;
    }
    return Configuration.configuration.getInPath() + DirInterface.SEPARATOR +
           filename;
  }

  /**
   * Get the full path from sendPath
   *
   * @param filename
   *
   * @return the full String path
   */
  public final String setSendPath(final String filename) {
    if (pojo.getSendPath() != null) {
      final File file = new File(filename);
      final String basename = file.getName();
      return pojo.getSendPath() + DirInterface.SEPARATOR + basename;
    }
    return Configuration.configuration.getOutPath() + DirInterface.SEPARATOR +
           filename;
  }

  /**
   * Get the full path from archivePath
   *
   * @param filename
   *
   * @return the full String path
   */
  public final String setArchivePath(final String filename) {
    if (pojo.getArchivePath() != null) {
      return pojo.getArchivePath() + DirInterface.SEPARATOR + filename;
    }
    return Configuration.configuration.getArchivePath() +
           DirInterface.SEPARATOR + filename;
  }

  /**
   * Get the full path from workPath
   *
   * @param filename
   *
   * @return the full String path
   */
  public final String setWorkingPath(final String filename) {
    if (pojo.getWorkPath() != null) {
      return pojo.getWorkPath() + DirInterface.SEPARATOR + filename +
             Configuration.EXT_R66;
    }
    return Configuration.configuration.getWorkingPath() +
           DirInterface.SEPARATOR + filename;
  }

  /**
   * Check if the given hostTo is in the allowed list
   *
   * @param hostId
   *
   * @return True if allow, else False
   */
  public final boolean checkHostAllow(final String hostId) {
    if (getIdsArray() == null || getIdsArray().length == 0) {
      return true; // always true in this case
    }
    for (final String element : getIdsArray()) {
      if (element.equalsIgnoreCase(hostId)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return True if this rule is adapted for SENDMODE
   */
  public final boolean isSendMode() {
    return !RequestPacket.isRecvMode(getMode());
  }

  /**
   * @return True if this rule is adapted for RECVMODE
   */
  public final boolean isRecvMode() {
    return RequestPacket.isRecvMode(getMode());
  }

  /**
   * Object to String
   *
   * @return the string that displays this object
   *
   * @see Object#toString()
   */
  @Override
  public final String toString() {
    return "Rule Name:" + getIdRule() + " IDS:" + pojo.getXMLHostids() +
           " MODETRANS: " + RequestPacket.TRANSFERMODE.values()[getMode()] +
           " RECV:" + getRecvPath() + " SEND:" + getSendPath() + " ARCHIVE:" +
           getArchivePath() + " WORK:" + getWorkPath() + " RPRET:{" +
           pojo.getXMLRPreTasks().replace('\n', ' ') + "} RPOST:{" +
           pojo.getXMLRPostTasks().replace('\n', ' ') + "} RERROR:{" +
           pojo.getXMLRErrorTasks().replace('\n', ' ') + "} SPRET:{" +
           pojo.getXMLSPreTasks().replace('\n', ' ') + "} SPOST:{" +
           pojo.getXMLSPostTasks().replace('\n', ' ') + "} SERROR:{" +
           pojo.getXMLSErrorTasks().replace('\n', ' ') + '}';
  }

  /**
   * @param isSender
   * @param step
   *
   * @return a string that prints (debug) the tasks to execute
   */
  public final String printTasks(final boolean isSender, final TASKSTEP step) {
    if (isSender) {
      switch (step) {
        case PRETASK:
          return "S:{" + pojo.getXMLSPreTasks().replace('\n', ' ') + '}';
        case POSTTASK:
          return "S:{" + pojo.getXMLSPostTasks().replace('\n', ' ') + '}';
        case ERRORTASK:
          return "S:{" + pojo.getXMLSErrorTasks().replace('\n', ' ') + '}';
        default:
          return "S:{no task}";
      }
    } else {
      switch (step) {
        case PRETASK:
          return "R:{" + pojo.getXMLRPreTasks().replace('\n', ' ') + '}';
        case POSTTASK:
          return "R:{" + pojo.getXMLRPostTasks().replace('\n', ' ') + '}';
        case ERRORTASK:
          return "R:{" + pojo.getXMLRErrorTasks().replace('\n', ' ') + '}';
        default:
          return "R:{no task}";
      }
    }
  }

  /**
   * Object to String
   *
   * @return the string that displays this object
   *
   * @see Object#toString()
   */
  public final String toShortString() {
    return "Rule Name:" + getIdRule() + " MODETRANS: " +
           RequestPacket.TRANSFERMODE.values()[getMode()];
  }

  /**
   * @param session
   * @param rule
   * @param mode
   *
   * @return the DbPreparedStatement according to the filter
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbPreparedStatement getFilterPrepareStament(
      final DbSession session, final String rule, final int mode)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final DbPreparedStatement preparedStatement =
        new DbPreparedStatement(session);
    final String request = "SELECT " + selectAllFields + " FROM " + table;
    String condition = null;
    if (ParametersChecker.isNotEmpty(rule)) {
      condition = " WHERE " + Columns.IDRULE.name() + " = '" + rule + "' ";
    }
    if (mode >= 0) {
      if (condition != null) {
        condition += " AND ";
      } else {
        condition = " WHERE ";
      }
      condition += Columns.MODETRANS.name() + " = ?";
    } else {
      condition = "";
    }
    preparedStatement.createPrepareStatement(
        request + condition + " ORDER BY " + Columns.IDRULE.name());
    if (mode >= 0) {
      try {
        preparedStatement.getPreparedStatement().setInt(1, mode);
      } catch (final SQLException e) {
        preparedStatement.realClose();
        throw new WaarpDatabaseSqlException(e);
      }
    }
    return preparedStatement;
  }

  /**
   * Write selected DbRule to a Json String
   *
   * @param preparedStatement
   *
   * @return the associated Json String
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static String getJson(final DbPreparedStatement preparedStatement,
                               final int limit)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final ArrayNode arrayNode = JsonHandler.createArrayNode();
    try {
      preparedStatement.executeQuery();
      int nb = 0;
      while (preparedStatement.getNext()) {
        final DbRule rule = getFromStatement(preparedStatement);
        final ObjectNode node = rule.getInternalJson();
        arrayNode.add(node);
        nb++;
        if (nb >= limit) {
          break;
        }
      }
    } finally {
      preparedStatement.realClose();
    }
    // \n is not correctly parsed within HTML so put double \\n in fine
    return WaarpStringUtils.cleanJsonForHtml(
        JsonHandler.writeAsString(arrayNode));
  }

  private ObjectNode getInternalJson() {
    final ObjectNode node = getJson();
    if (pojo.getHostids().isEmpty()) {
      node.put(Columns.HOSTIDS.name(), "");
    }
    if (pojo.getRecvPath() == null) {
      node.put(Columns.RECVPATH.name(), "");
    }
    if (pojo.getSendPath() == null) {
      node.put(Columns.SENDPATH.name(), "");
    }
    if (pojo.getArchivePath() == null) {
      node.put(Columns.ARCHIVEPATH.name(), "");
    }
    if (pojo.getWorkPath() == null) {
      node.put(Columns.WORKPATH.name(), "");
    }
    if (pojo.getRPreTasks().isEmpty()) {
      node.put(Columns.RPRETASKS.name(), "");
    }
    if (pojo.getRPostTasks().isEmpty()) {
      node.put(Columns.RPOSTTASKS.name(), "");
    }
    if (pojo.getRErrorTasks().isEmpty()) {
      node.put(Columns.RERRORTASKS.name(), "");
    }
    if (pojo.getSPreTasks().isEmpty()) {
      node.put(Columns.SPRETASKS.name(), "");
    }
    if (pojo.getSPostTasks().isEmpty()) {
      node.put(Columns.SPOSTTASKS.name(), "");
    }
    if (pojo.getSErrorTasks().isEmpty()) {
      node.put(Columns.SERRORTASKS.name(), "");
    }
    return node;
  }

  /**
   * @return the Json string for this
   */
  public final String getJsonAsString() {
    final ObjectNode node = getInternalJson();
    return WaarpStringUtils.cleanJsonForHtml(JsonHandler.writeAsString(node));
  }

  /**
   * @param session
   * @param body
   *
   * @return the runner in Html format specified by body by replacing all
   *     instance of fields
   */
  public final String toSpecializedHtml(final R66Session session,
                                        final String body) {
    final StringBuilder builder = new StringBuilder(body);
    WaarpStringUtils.replace(builder, "XXXRULEXXX", getIdRule());
    WaarpStringUtils.replace(builder, "XXXIDSXXX",
                             pojo.getXMLHostids() == null? "" :
                                 pojo.getXMLHostids());
    if (getMode() == RequestPacket.TRANSFERMODE.RECVMODE.ordinal()) {
      WaarpStringUtils.replace(builder, "XXXRECVXXX", "checked");
    } else if (getMode() == RequestPacket.TRANSFERMODE.SENDMODE.ordinal()) {
      WaarpStringUtils.replace(builder, "XXXSENDXXX", "checked");
    } else if (getMode() == RequestPacket.TRANSFERMODE.RECVMD5MODE.ordinal()) {
      WaarpStringUtils.replace(builder, "XXXRECVMXXX", "checked");
    } else if (getMode() == RequestPacket.TRANSFERMODE.SENDMD5MODE.ordinal()) {
      WaarpStringUtils.replace(builder, "XXXSENDMXXX", "checked");
    } else if (getMode() ==
               RequestPacket.TRANSFERMODE.RECVTHROUGHMODE.ordinal()) {
      WaarpStringUtils.replace(builder, "XXXRECVTXXX", "checked");
    } else if (getMode() ==
               RequestPacket.TRANSFERMODE.SENDTHROUGHMODE.ordinal()) {
      WaarpStringUtils.replace(builder, "XXXSENDTXXX", "checked");
    } else if (getMode() ==
               RequestPacket.TRANSFERMODE.RECVMD5THROUGHMODE.ordinal()) {
      WaarpStringUtils.replace(builder, "XXXRECVMTXXX", "checked");
    } else if (getMode() ==
               RequestPacket.TRANSFERMODE.SENDMD5THROUGHMODE.ordinal()) {
      WaarpStringUtils.replace(builder, "XXXSENDMTXXX", "checked");
    }
    WaarpStringUtils.replace(builder, "XXXRPXXX",
                             pojo.getRecvPath() == null? "" :
                                 pojo.getRecvPath());
    WaarpStringUtils.replace(builder, "XXXSPXXX",
                             pojo.getSendPath() == null? "" :
                                 pojo.getSendPath());
    WaarpStringUtils.replace(builder, "XXXAPXXX",
                             pojo.getArchivePath() == null? "" :
                                 pojo.getArchivePath());
    WaarpStringUtils.replace(builder, "XXXWPXXX",
                             pojo.getWorkPath() == null? "" :
                                 pojo.getWorkPath());
    WaarpStringUtils.replace(builder, "XXXRPTXXX",
                             pojo.getXMLRPreTasks() == null? "" :
                                 pojo.getXMLRPreTasks());
    WaarpStringUtils.replace(builder, "XXXRSTXXX",
                             pojo.getXMLRPostTasks() == null? "" :
                                 pojo.getXMLRPostTasks());
    WaarpStringUtils.replace(builder, "XXXRETXXX",
                             pojo.getXMLRErrorTasks() == null? "" :
                                 pojo.getXMLRErrorTasks());
    WaarpStringUtils.replace(builder, "XXXSPTXXX",
                             pojo.getXMLSPreTasks() == null? "" :
                                 pojo.getXMLSPreTasks());
    WaarpStringUtils.replace(builder, "XXXSSTXXX",
                             pojo.getXMLSPostTasks() == null? "" :
                                 pojo.getXMLSPostTasks());
    WaarpStringUtils.replace(builder, "XXXSETXXX",
                             pojo.getXMLSErrorTasks() == null? "" :
                                 pojo.getXMLSErrorTasks());
    return builder.toString();
  }

  /**
   * @return the recvPath
   */
  public final String getRecvPath() {
    if (ParametersChecker.isEmpty(getRuleRecvPath())) {
      return Configuration.configuration.getInPath();
    }
    return getRuleRecvPath();
  }

  /**
   * @return the sendPath
   */
  public final String getSendPath() {
    if (ParametersChecker.isEmpty(getRuleSendPath())) {
      return Configuration.configuration.getOutPath();
    }
    return getRuleSendPath();
  }

  /**
   * @return the archivePath
   */
  public final String getArchivePath() {
    if (ParametersChecker.isEmpty(getRuleArchivePath())) {
      return Configuration.configuration.getArchivePath();
    }
    return getRuleArchivePath();
  }

  /**
   * @return the workPath
   */
  public final String getWorkPath() {
    if (ParametersChecker.isEmpty(getRuleWorkPath())) {
      return Configuration.configuration.getWorkingPath();
    }
    return getRuleWorkPath();
  }

  /**
   * @return the Rule recvPath
   */
  public final String getRuleRecvPath() {
    return pojo.getRecvPath();
  }

  /**
   * @return the Rule sendPath
   */
  public final String getRuleSendPath() {
    return pojo.getSendPath();
  }

  /**
   * @return the Rule archivePath
   */
  public final String getRuleArchivePath() {
    return pojo.getArchivePath();
  }

  /**
   * @return the Rule workPath
   */
  public final String getRuleWorkPath() {
    return pojo.getWorkPath();
  }

  /**
   * @return the idRule
   */
  public final String getIdRule() {
    return pojo.getName();
  }

  /**
   * @return the mode
   */
  public final int getMode() {
    return pojo.getMode();
  }

  /**
   * @return the idsArray
   */
  public final String[] getIdsArray() {
    return pojo.getHostids().toArray(STRING_0_LENGTH);
  }

  /**
   * @return the rpreTasksArray
   */
  public final String[][] getRpreTasksArray() {
    return toLegacyTasks(pojo.getRPreTasks());
  }

  /**
   * @return the rpostTasksArray
   */
  public final String[][] getRpostTasksArray() {
    return toLegacyTasks(pojo.getRPostTasks());
  }

  /**
   * @return the rerrorTasksArray
   */
  public final String[][] getRerrorTasksArray() {
    return toLegacyTasks(pojo.getRErrorTasks());
  }

  /**
   * @return the spreTasksArray
   */
  public final String[][] getSpreTasksArray() {
    return toLegacyTasks(pojo.getSPreTasks());
  }

  /**
   * @return the spostTasksArray
   */
  public final String[][] getSpostTasksArray() {
    return toLegacyTasks(pojo.getSPostTasks());
  }

  /**
   * @return the serrorTasksArray
   */
  public final String[][] getSerrorTasksArray() {
    return toLegacyTasks(pojo.getSErrorTasks());
  }

  private List<RuleTask> fromLegacyTasks(final String[][] tasks) {
    final int size = tasks.length;
    final List<RuleTask> res = new ArrayList<RuleTask>(size);
    for (final String[] task : tasks) {
      if (task.length >= 3) {
        res.add(new RuleTask(task[0], task[1], Integer.parseInt(task[2])));
      }
    }
    return res;
  }

  private String[][] toLegacyTasks(final List<RuleTask> tasks) {
    final int size = tasks.size();
    final String[][] res = new String[size][];
    int i = 0;
    for (final RuleTask task : tasks) {
      res[i] = new String[3];
      res[i][0] = task.getType();
      res[i][1] = task.getPath();
      res[i][2] = String.valueOf(task.getDelay());
      i++;
    }
    return res;
  }
}
