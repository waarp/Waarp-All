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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.data.AbstractDbData;
import org.waarp.common.database.data.DbValue;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseNoDataException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.file.DirInterface;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.common.xml.XmlUtil;
import org.waarp.common.xml.XmlValue;
import org.waarp.openr66.configuration.RuleFileBasedConfiguration;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.RuleDAO;
import org.waarp.openr66.dao.database.DBRuleDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.database.data.DbTaskRunner.TASKSTEP;
import org.waarp.openr66.pojo.Rule;
import org.waarp.openr66.pojo.RuleTask;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;
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
 *
 *
 */
public class DbRule extends AbstractDbData {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(DbRule.class);

  public static enum Columns {
    HOSTIDS, MODETRANS, RECVPATH, SENDPATH, ARCHIVEPATH, WORKPATH, RPRETASKS,
    RPOSTTASKS, RERRORTASKS, SPRETASKS, SPOSTTASKS, SERRORTASKS, UPDATEDINFO,
    IDRULE
  }

  public static final int[] dbTypes = {
      Types.LONGVARCHAR, Types.INTEGER, Types.NVARCHAR, Types.NVARCHAR,
      Types.NVARCHAR, Types.NVARCHAR, Types.LONGVARCHAR, Types.LONGVARCHAR,
      Types.LONGVARCHAR, Types.LONGVARCHAR, Types.LONGVARCHAR,
      Types.LONGVARCHAR, Types.INTEGER, Types.NVARCHAR
  };

  public static final String table = " RULES ";

  /**
   * Internal context XML fields
   */
  private static final String XMLHOSTIDS =
      "<" + RuleFileBasedConfiguration.XHOSTIDS + ">";

  /**
   * Internal context XML fields
   */
  private static final String XMLENDHOSTIDS =
      "</" + RuleFileBasedConfiguration.XHOSTIDS + ">";

  /**
   * Internal context XML fields
   */
  private static final String XMLHOSTID =
      "<" + RuleFileBasedConfiguration.XHOSTID + ">";

  /**
   * Internal context XML fields
   */
  private static final String XMLENDHOSTID =
      "</" + RuleFileBasedConfiguration.XHOSTID + ">";

  /**
   * Internal context XML fields
   */
  private static final String XMLTASKS =
      "<" + RuleFileBasedConfiguration.XTASKS + ">";

  /**
   * Internal context XML fields
   */
  private static final String XMLENDTASKS =
      "</" + RuleFileBasedConfiguration.XTASKS + ">";

  /**
   * Internal context XML fields
   */
  private static final String XMLTASK =
      "<" + RuleFileBasedConfiguration.XTASK + ">";

  /**
   * Internal context XML fields
   */
  private static final String XMLENDTASK =
      "</" + RuleFileBasedConfiguration.XTASK + ">";

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
  public static final String TASK_COMMENT = "comment";

  /**
   * Global Id
   */
  private Rule rule;

  // ALL TABLE SHOULD IMPLEMENT THIS
  public static final int NBPRKEY = 1;

  protected static final String selectAllFields =
      Columns.HOSTIDS.name() + "," + Columns.MODETRANS.name() + "," +
      Columns.RECVPATH.name() + "," + Columns.SENDPATH.name() + "," +
      Columns.ARCHIVEPATH.name() + "," + Columns.WORKPATH.name() + "," +
      Columns.RPRETASKS.name() + "," + Columns.RPOSTTASKS.name() + "," +
      Columns.RERRORTASKS.name() + "," + Columns.SPRETASKS.name() + "," +
      Columns.SPOSTTASKS.name() + "," + Columns.SERRORTASKS.name() + "," +
      Columns.UPDATEDINFO.name() + "," + Columns.IDRULE.name();

  protected static final String updateAllFields =
      Columns.HOSTIDS.name() + "=?," + Columns.MODETRANS.name() + "=?," +
      Columns.RECVPATH.name() + "=?," + Columns.SENDPATH.name() + "=?," +
      Columns.ARCHIVEPATH.name() + "=?," + Columns.WORKPATH.name() + "=?," +
      Columns.RPRETASKS.name() + "=?," + Columns.RPOSTTASKS.name() + "=?," +
      Columns.RERRORTASKS.name() + "=?," + Columns.SPRETASKS.name() + "=?," +
      Columns.SPOSTTASKS.name() + "=?," + Columns.SERRORTASKS.name() + "=?," +
      Columns.UPDATEDINFO.name() + "=?";

  protected static final String insertAllValues =
      " (?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";

  @Override
  protected void initObject() {
    primaryKey = new DbValue[] { new DbValue("", Columns.IDRULE.name()) };
    otherFields = new DbValue[] {
        // HOSTIDS, MODETRANS, RECVPATH, SENDPATH, ARCHIVEPATH, WORKPATH,
        // PRETASKS, POSTTASKS, ERRORTASKS
        new DbValue("", Columns.HOSTIDS.name(), true),
        new DbValue(0, Columns.MODETRANS.name()),
        new DbValue("", Columns.RECVPATH.name()),
        new DbValue("", Columns.SENDPATH.name()),
        new DbValue("", Columns.ARCHIVEPATH.name()),
        new DbValue("", Columns.WORKPATH.name()),
        new DbValue("", Columns.RPRETASKS.name(), true),
        new DbValue("", Columns.RPOSTTASKS.name(), true),
        new DbValue("", Columns.RERRORTASKS.name(), true),
        new DbValue("", Columns.SPRETASKS.name(), true),
        new DbValue("", Columns.SPOSTTASKS.name(), true),
        new DbValue("", Columns.SERRORTASKS.name(), true),
        new DbValue(0, Columns.UPDATEDINFO.name())
    };
    allFields = new DbValue[] {
        otherFields[0], otherFields[1], otherFields[2], otherFields[3],
        otherFields[4], otherFields[5], otherFields[6], otherFields[7],
        otherFields[8], otherFields[9], otherFields[10], otherFields[11],
        otherFields[12], primaryKey[0]
    };
  }

  @Override
  protected String getSelectAllFields() {
    return selectAllFields;
  }

  @Override
  protected String getTable() {
    return table;
  }

  @Override
  protected String getInsertAllValues() {
    return insertAllValues;
  }

  @Override
  protected String getUpdateAllFields() {
    return updateAllFields;
  }

  protected final void checkPath() {
    if (getRecvPath() != null && !getRecvPath().isEmpty() &&
        getRecvPath().charAt(0) != DirInterface.SEPARATORCHAR) {
      rule.setRecvPath(DirInterface.SEPARATOR + getRecvPath());
    }
    if (getSendPath() != null && !getSendPath().isEmpty() &&
        getSendPath().charAt(0) != DirInterface.SEPARATORCHAR) {
      rule.setSendPath(DirInterface.SEPARATOR + getSendPath());
    }
    if (getArchivePath() != null && !getArchivePath().isEmpty() &&
        getArchivePath().charAt(0) != DirInterface.SEPARATORCHAR) {
      rule.setArchivePath(DirInterface.SEPARATOR + getArchivePath());
    }
    if (getWorkPath() != null && !getWorkPath().isEmpty() &&
        getWorkPath().charAt(0) != DirInterface.SEPARATORCHAR) {
      rule.setWorkPath(DirInterface.SEPARATOR + getWorkPath());
    }
  }

  @Override
  protected void setToArray() {
    checkPath();
    allFields[Columns.HOSTIDS.ordinal()].setValue(rule.getXMLHostids());
    allFields[Columns.MODETRANS.ordinal()].setValue(getMode());
    allFields[Columns.RECVPATH.ordinal()].setValue(getRecvPath());
    allFields[Columns.SENDPATH.ordinal()].setValue(getSendPath());
    allFields[Columns.ARCHIVEPATH.ordinal()].setValue(getArchivePath());
    allFields[Columns.WORKPATH.ordinal()].setValue(getWorkPath());
    allFields[Columns.RPRETASKS.ordinal()].setValue(rule.getXMLRPreTasks());
    allFields[Columns.RPOSTTASKS.ordinal()].setValue(rule.getXMLRPostTasks());
    allFields[Columns.RERRORTASKS.ordinal()].setValue(rule.getXMLRErrorTasks());
    allFields[Columns.SPRETASKS.ordinal()].setValue(rule.getXMLSPreTasks());
    allFields[Columns.SPOSTTASKS.ordinal()].setValue(rule.getXMLSPostTasks());
    allFields[Columns.SERRORTASKS.ordinal()].setValue(rule.getXMLSErrorTasks());
    allFields[Columns.UPDATEDINFO.ordinal()]
        .setValue(rule.getUpdatedInfo().ordinal());
    allFields[Columns.IDRULE.ordinal()].setValue(getIdRule());
  }

  @Override
  protected void setFromArray() throws WaarpDatabaseSqlException {
    rule.setHostids(Arrays.asList(
        getIdsRule((String) allFields[Columns.HOSTIDS.ordinal()].getValue())));
    rule.setMode((Integer) allFields[Columns.MODETRANS.ordinal()].getValue());
    rule.setRecvPath((String) allFields[Columns.RECVPATH.ordinal()].getValue());
    rule.setSendPath((String) allFields[Columns.SENDPATH.ordinal()].getValue());
    rule.setArchivePath(
        (String) allFields[Columns.ARCHIVEPATH.ordinal()].getValue());
    rule.setWorkPath((String) allFields[Columns.WORKPATH.ordinal()].getValue());
    rule.setRPreTasks(fromLegacyTasks(getTasksRule(
        (String) allFields[Columns.RPRETASKS.ordinal()].getValue())));
    rule.setRPostTasks(fromLegacyTasks(getTasksRule(
        (String) allFields[Columns.RPOSTTASKS.ordinal()].getValue())));
    rule.setRErrorTasks(fromLegacyTasks(getTasksRule(
        (String) allFields[Columns.RERRORTASKS.ordinal()].getValue())));
    rule.setSPreTasks(fromLegacyTasks(getTasksRule(
        (String) allFields[Columns.SPRETASKS.ordinal()].getValue())));
    rule.setSPostTasks(fromLegacyTasks(getTasksRule(
        (String) allFields[Columns.SPOSTTASKS.ordinal()].getValue())));
    rule.setSErrorTasks(fromLegacyTasks(getTasksRule(
        (String) allFields[Columns.SERRORTASKS.ordinal()].getValue())));
    rule.setUpdatedInfo(org.waarp.openr66.pojo.UpdatedInfo.valueOf(
        (Integer) allFields[Columns.UPDATEDINFO.ordinal()].getValue()));
    rule.setName((String) allFields[Columns.IDRULE.ordinal()].getValue());
    checkPath();
  }

  protected void setFromArrayClone(DbRule source)
      throws WaarpDatabaseSqlException {
    rule.setMode((Integer) allFields[Columns.MODETRANS.ordinal()].getValue());
    rule.setRecvPath((String) allFields[Columns.RECVPATH.ordinal()].getValue());
    rule.setSendPath((String) allFields[Columns.SENDPATH.ordinal()].getValue());
    rule.setArchivePath(
        (String) allFields[Columns.ARCHIVEPATH.ordinal()].getValue());
    rule.setWorkPath((String) allFields[Columns.WORKPATH.ordinal()].getValue());
    rule.setUpdatedInfo(org.waarp.openr66.pojo.UpdatedInfo.valueOf(
        (Integer) allFields[Columns.UPDATEDINFO.ordinal()].getValue()));
    rule.setName((String) allFields[Columns.IDRULE.ordinal()].getValue());

    rule.setHostids(source.rule.getHostids());

    rule.setRPreTasks(source.rule.getRPreTasks());
    rule.setRPostTasks(source.rule.getRPostTasks());
    rule.setRErrorTasks(source.rule.getRErrorTasks());
    rule.setSPreTasks(source.rule.getSPreTasks());
    rule.setSPostTasks(source.rule.getSPostTasks());
    rule.setSErrorTasks(source.rule.getSErrorTasks());
    checkPath();
  }

  @Override
  protected String getWherePrimaryKey() {
    return primaryKey[0].getColumn() + " = ? ";
  }

  @Override
  protected void setPrimaryKey() {
    primaryKey[0].setValue(getIdRule());
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
  public DbRule(String idRule, String ids, int mode, String recvPath,
                String sendPath, String archivePath, String workPath,
                String rpreTasks, String rpostTasks, String rerrorTasks,
                String spreTasks, String spostTasks, String serrorTasks) {
    super();
    rule = new Rule(idRule, mode, Arrays.asList(getIdsRule(ids)), recvPath,
                    sendPath, archivePath, workPath,
                    fromLegacyTasks(getTasksRule(rpreTasks)),
                    fromLegacyTasks(getTasksRule(rpostTasks)),
                    fromLegacyTasks(getTasksRule(rerrorTasks)),
                    fromLegacyTasks(getTasksRule(spreTasks)),
                    fromLegacyTasks(getTasksRule(spostTasks)),
                    fromLegacyTasks(getTasksRule(serrorTasks)));
    setToArray();
  }

  /**
   * @param idRule
   *
   * @throws WaarpDatabaseException
   */
  public DbRule(String idRule) throws WaarpDatabaseException {
    super();
    RuleDAO ruleAccess = null;
    try {
      ruleAccess = DAOFactory.getInstance().getRuleDAO();
      rule = ruleAccess.select(idRule);
      setToArray();
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } catch (final DAONoDataException e) {
      throw new WaarpDatabaseNoDataException("Rule not found", e);
    } finally {
      if (ruleAccess != null) {
        ruleAccess.close();
      }
    }
  }

  public DbRule(Rule rule) {
    super();
    if (rule == null) {
      throw new IllegalArgumentException(
          "Argument in constructor cannot be null");
    }
    this.rule = rule;
    setToArray();
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
  public DbRule(String idrule, String[] idsArrayRef, int mode, String recvpath,
                String sendpath, String archivepath, String workpath,
                String[][] rpretasksArray, String[][] rposttasksArray,
                String[][] rerrortasksArray, String[][] spretasksArray,
                String[][] sposttasksArray, String[][] serrortasksArray) {
    super();
    if (idsArrayRef == null) {
      idsArrayRef = new String[0];
    }
    rule =
        new Rule(idrule, mode, Arrays.asList(idsArrayRef), recvpath, sendpath,
                 archivepath, workpath, fromLegacyTasks(rpretasksArray),
                 fromLegacyTasks(rposttasksArray),
                 fromLegacyTasks(rerrortasksArray),
                 fromLegacyTasks(spretasksArray),
                 fromLegacyTasks(sposttasksArray),
                 fromLegacyTasks(serrortasksArray));
    setToArray();
  }

  /**
   * Constructor from Json
   *
   * @param source
   *
   * @throws WaarpDatabaseSqlException
   */
  public DbRule(ObjectNode source) throws WaarpDatabaseSqlException {
    super();
    rule = new Rule();
    setFromJson(source, false);
    if (getIdRule() == null || getIdRule().isEmpty()) {
      throw new WaarpDatabaseSqlException(
          "Not enough argument to create the object");
    }
    setToArray();
  }

  @Override
  public void setFromJson(ObjectNode node, boolean ignorePrimaryKey)
      throws WaarpDatabaseSqlException {
    super.setFromJson(node, ignorePrimaryKey);
  }

  /**
   * Delete all entries (used when purge and reload)
   *
   * @return the previous existing array of DbRule
   *
   * @throws WaarpDatabaseException
   */
  public static DbRule[] deleteAll() throws WaarpDatabaseException {
    RuleDAO ruleAccess = null;
    List<Rule> rules = null;
    try {
      ruleAccess = DAOFactory.getInstance().getRuleDAO();
      rules = ruleAccess.getAll();
      ruleAccess.deleteAll();
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } finally {
      if (ruleAccess != null) {
        ruleAccess.close();
      }
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
  public void delete() throws WaarpDatabaseException {
    RuleDAO ruleAccess = null;
    try {
      ruleAccess = DAOFactory.getInstance().getRuleDAO();
      ruleAccess.delete(rule);
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } catch (final DAONoDataException e) {
      throw new WaarpDatabaseNoDataException("Rule not found", e);
    } finally {
      if (ruleAccess != null) {
        ruleAccess.close();
      }
    }
  }

  @Override
  public void insert() throws WaarpDatabaseException {
    RuleDAO ruleAccess = null;
    try {
      ruleAccess = DAOFactory.getInstance().getRuleDAO();
      ruleAccess.insert(rule);
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } finally {
      if (ruleAccess != null) {
        ruleAccess.close();
      }
    }
  }

  @Override
  public boolean exist() throws WaarpDatabaseException {
    RuleDAO ruleAccess = null;
    try {
      ruleAccess = DAOFactory.getInstance().getRuleDAO();
      return ruleAccess.exist(rule.getName());
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } finally {
      if (ruleAccess != null) {
        ruleAccess.close();
      }
    }
  }

  @Override
  public void select() throws WaarpDatabaseException {
    RuleDAO ruleAccess = null;
    try {
      ruleAccess = DAOFactory.getInstance().getRuleDAO();
      rule = ruleAccess.select(rule.getName());
      setToArray();
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } catch (final DAONoDataException e) {
      throw new WaarpDatabaseNoDataException("Rule not found", e);
    } finally {
      if (ruleAccess != null) {
        ruleAccess.close();
      }
    }
  }

  @Override
  public void update() throws WaarpDatabaseException {
    RuleDAO ruleAccess = null;
    try {
      ruleAccess = DAOFactory.getInstance().getRuleDAO();
      ruleAccess.update(rule);
      setToArray();
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } catch (final DAONoDataException e) {
      throw new WaarpDatabaseNoDataException("Rule not found", e);
    } finally {
      if (ruleAccess != null) {
        ruleAccess.close();
      }
    }
  }

  /**
   * Private constructor for Commander only
   */
  private DbRule() {
    super();
    rule = new Rule();
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
    List<Rule> rules = null;
    try {
      ruleAccess = DAOFactory.getInstance().getRuleDAO();
      rules = ruleAccess.getAll();
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseNoConnectionException(e);
    } finally {
      if (ruleAccess != null) {
        ruleAccess.close();
      }
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
  public static DbRule getFromStatement(DbPreparedStatement preparedStatement)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final DbRule dbRule = new DbRule();
    dbRule.getValues(preparedStatement, dbRule.allFields);
    dbRule.setToArray();
    dbRule.isSaved = true;
    logger.debug("Get one Rule from Db: " + dbRule.getIdRule());
    return dbRule;
  }

  /**
   * @return the DbPreparedStatement for getting Updated Object
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbRule[] getUpdatedPrepareStament()
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final List<Filter> filters = new ArrayList<Filter>(1);
    filters.add(new Filter(DBRuleDAO.UPDATED_INFO_FIELD, "=",
                           org.waarp.openr66.pojo.UpdatedInfo
                               .fromLegacy(UpdatedInfo.TOSUBMIT).ordinal()));
    RuleDAO ruleAccess = null;
    List<Rule> rules;
    try {
      ruleAccess = DAOFactory.getInstance().getRuleDAO();
      rules = ruleAccess.find(filters);
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseNoConnectionException(e);
    } finally {
      if (ruleAccess != null) {
        ruleAccess.close();
      }
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
  public void changeUpdatedInfo(UpdatedInfo info) {
    rule.setUpdatedInfo(org.waarp.openr66.pojo.UpdatedInfo.fromLegacy(info));
  }

  /**
   * Get Ids from String. If it is not ok, then it sets the default values and
   * return False, else returns True.
   *
   * @param idsref
   *
   * @return True if ok, else False (default values).
   */
  private String[] getIdsRule(String idsref) {
    if (idsref == null) {
      // No ids so setting to the default!
      return new String[0];
    }
    final StringReader reader = new StringReader(idsref);
    Document document = null;
    try {
      document = new SAXReader().read(reader);
      final XmlValue[] values =
          XmlUtil.read(document, RuleFileBasedConfiguration.hostsDecls);
      return RuleFileBasedConfiguration.getHostIds(values[0]);
    } catch (final DocumentException e) {
      logger.warn("Unable to read the ids for Rule: " + idsref, e);
      // No ids so setting to the default!
      return new String[0];
    } finally {
      reader.close();
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
  private String[][] getTasksRule(String tasks) {
    if (tasks == null) {
      // No tasks so setting to the default!
      return new String[0][0];
    }
    final StringReader reader = new StringReader(tasks);
    Document document = null;
    try {
      document = new SAXReader().read(reader);
    } catch (final DocumentException e) {
      logger.info("Unable to read the tasks for Rule: " + tasks, e);
      // No tasks so setting to the default!
      reader.close();
      return new String[0][0];
    }
    final XmlValue[] values =
        XmlUtil.read(document, RuleFileBasedConfiguration.tasksDecl);
    final String[][] result =
        RuleFileBasedConfiguration.getTasksRule(values[0]);
    reader.close();
    return result;
  }

  /**
   * Get the full path from RecvPath (used only in copy MODETRANS)
   *
   * @param filename
   *
   * @return the full String path
   *
   * @throws OpenR66ProtocolSystemException
   */
  public String setRecvPath(String filename)
      throws OpenR66ProtocolSystemException {
    if (rule.getRecvPath() != null && !rule.getRecvPath().isEmpty()) {
      return rule.getRecvPath() + DirInterface.SEPARATOR + filename;
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
   *
   * @throws OpenR66ProtocolSystemException
   */
  public String setSendPath(String filename)
      throws OpenR66ProtocolSystemException {
    if (rule.getSendPath() != null) {
      final File file = new File(filename);
      final String basename = file.getName();
      return rule.getSendPath() + DirInterface.SEPARATOR + basename;
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
   *
   * @throws OpenR66ProtocolSystemException
   */
  public String setArchivePath(String filename)
      throws OpenR66ProtocolSystemException {
    if (rule.getArchivePath() != null) {
      return rule.getArchivePath() + DirInterface.SEPARATOR + filename;
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
   *
   * @throws OpenR66ProtocolSystemException
   */
  public String setWorkingPath(String filename)
      throws OpenR66ProtocolSystemException {
    if (rule.getWorkPath() != null) {
      return rule.getWorkPath() + DirInterface.SEPARATOR + filename +
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
  public boolean checkHostAllow(String hostId) {
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
  public boolean isSendMode() {
    return (!RequestPacket.isRecvMode(getMode()));
  }

  /**
   * @return True if this rule is adapted for RECVMODE
   */
  public boolean isRecvMode() {
    return RequestPacket.isRecvMode(getMode());
  }

  /**
   * Object to String
   *
   * @return the string that displays this object
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "Rule Name:" + getIdRule() + " IDS:" + rule.getXMLHostids() +
           " MODETRANS: " +
           RequestPacket.TRANSFERMODE.values()[getMode()].toString() +
           " RECV:" + getRecvPath() + " SEND:" + getSendPath() + " ARCHIVE:" +
           getArchivePath() + " WORK:" + getWorkPath() + " RPRET:{" +
           rule.getXMLRPreTasks().replace('\n', ' ') + "} RPOST:{" +
           rule.getXMLRPostTasks().replace('\n', ' ') + "} RERROR:{" +
           rule.getXMLRErrorTasks().replace('\n', ' ') + "} SPRET:{" +
           rule.getXMLSPreTasks().replace('\n', ' ') + "} SPOST:{" +
           rule.getXMLSPostTasks().replace('\n', ' ') + "} SERROR:{" +
           rule.getXMLSErrorTasks().replace('\n', ' ') + "}";
  }

  /**
   * @param isSender
   * @param step
   *
   * @return a string that prints (debug) the tasks to execute
   */
  public String printTasks(boolean isSender, TASKSTEP step) {
    if (isSender) {
      switch (step) {
        case PRETASK:
          return "S:{" + rule.getXMLRPreTasks().replace('\n', ' ') + "}";
        case POSTTASK:
          return "S:{" + rule.getXMLRPostTasks().replace('\n', ' ') + "}";
        case ERRORTASK:
          return "S:{" + rule.getXMLRErrorTasks().replace('\n', ' ') + "}";
        default:
          return "S:{no task}";
      }
    } else {
      switch (step) {
        case PRETASK:
          return "R:{" + rule.getXMLSPreTasks().replace('\n', ' ') + "}";
        case POSTTASK:
          return "R:{" + rule.getXMLSPostTasks().replace('\n', ' ') + "}";
        case ERRORTASK:
          return "R:{" + rule.getXMLSErrorTasks().replace('\n', ' ') + "}";
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
   * @see java.lang.Object#toString()
   */
  public String toShortString() {
    return "Rule Name:" + getIdRule() + " MODETRANS: " +
           RequestPacket.TRANSFERMODE.values()[getMode()].toString();
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
  public static DbPreparedStatement getFilterPrepareStament(DbSession session,
                                                            String rule,
                                                            int mode)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final DbPreparedStatement preparedStatement =
        new DbPreparedStatement(session);
    final String request = "SELECT " + selectAllFields + " FROM " + table;
    String condition = null;
    if (rule != null) {
      condition = " WHERE " + Columns.IDRULE.name() + " LIKE '%" + rule + "%' ";
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
   * @throws OpenR66ProtocolBusinessException
   */
  public static String getJson(DbPreparedStatement preparedStatement, int limit)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException,
             OpenR66ProtocolBusinessException {
    final ArrayNode arrayNode = JsonHandler.createArrayNode();
    try {
      preparedStatement.executeQuery();
      int nb = 0;
      while (preparedStatement.getNext()) {
        final DbRule rule = DbRule.getFromStatement(preparedStatement);
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
    return WaarpStringUtils.cleanJsonForHtml(arrayNode.toString());
  }

  private ObjectNode getInternalJson() {
    final ObjectNode node = getJson();
    if (rule.getHostids().size() == 0) {
      node.put(Columns.HOSTIDS.name(), "");
    }
    if (rule.getRecvPath() == null) {
      node.put(Columns.RECVPATH.name(), "");
    }
    if (rule.getSendPath() == null) {
      node.put(Columns.SENDPATH.name(), "");
    }
    if (rule.getArchivePath() == null) {
      node.put(Columns.ARCHIVEPATH.name(), "");
    }
    if (rule.getWorkPath() == null) {
      node.put(Columns.WORKPATH.name(), "");
    }
    if (rule.getRPreTasks().size() == 0) {
      node.put(Columns.RPRETASKS.name(), "");
    }
    if (rule.getRPostTasks().size() == 0) {
      node.put(Columns.RPOSTTASKS.name(), "");
    }
    if (rule.getRErrorTasks().size() == 0) {
      node.put(Columns.RERRORTASKS.name(), "");
    }
    if (rule.getSPreTasks().size() == 0) {
      node.put(Columns.SPRETASKS.name(), "");
    }
    if (rule.getSPostTasks().size() == 0) {
      node.put(Columns.SPOSTTASKS.name(), "");
    }
    if (rule.getSErrorTasks().size() == 0) {
      node.put(Columns.SERRORTASKS.name(), "");
    }
    return node;
  }

  /**
   * @return the Json string for this
   */
  public String getJsonAsString() {
    final ObjectNode node = getInternalJson();
    return JsonHandler.writeAsString(node).replaceAll("([^\\\\])\\\\n", "$1")
                      .replaceAll("([^\\\\])\\\\r", "$1")
                      .replace("\\\\", "\\\\\\\\");
  }

  /**
   * @param session
   * @param body
   *
   * @return the runner in Html format specified by body by replacing all
   *     instance of fields
   */
  public String toSpecializedHtml(R66Session session, String body) {
    final StringBuilder builder = new StringBuilder(body);
    WaarpStringUtils.replace(builder, "XXXRULEXXX", getIdRule());
    WaarpStringUtils.replace(builder, "XXXIDSXXX",
                             rule.getXMLHostids() == null? "" :
                                 rule.getXMLHostids());
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
                             rule.getRecvPath() == null? "" :
                                 rule.getRecvPath());
    WaarpStringUtils.replace(builder, "XXXSPXXX",
                             rule.getSendPath() == null? "" :
                                 rule.getSendPath());
    WaarpStringUtils.replace(builder, "XXXAPXXX",
                             rule.getArchivePath() == null? "" :
                                 rule.getArchivePath());
    WaarpStringUtils.replace(builder, "XXXWPXXX",
                             rule.getWorkPath() == null? "" :
                                 rule.getWorkPath());
    WaarpStringUtils.replace(builder, "XXXRPTXXX",
                             rule.getXMLRPreTasks() == null? "" :
                                 rule.getXMLRPreTasks());
    WaarpStringUtils.replace(builder, "XXXRSTXXX",
                             rule.getXMLRPostTasks() == null? "" :
                                 rule.getXMLRPostTasks());
    WaarpStringUtils.replace(builder, "XXXRETXXX",
                             rule.getXMLRErrorTasks() == null? "" :
                                 rule.getXMLRErrorTasks());
    WaarpStringUtils.replace(builder, "XXXSPTXXX",
                             rule.getXMLSPreTasks() == null? "" :
                                 rule.getXMLSPreTasks());
    WaarpStringUtils.replace(builder, "XXXSSTXXX",
                             rule.getXMLSPostTasks() == null? "" :
                                 rule.getXMLSPostTasks());
    WaarpStringUtils.replace(builder, "XXXSETXXX",
                             rule.getXMLSErrorTasks() == null? "" :
                                 rule.getXMLSErrorTasks());
    return builder.toString();
  }

  /**
   * @return the recvPath
   */
  public String getRecvPath() {
    if (getRuleRecvPath() == null || getRuleRecvPath().trim().isEmpty()) {
      return Configuration.configuration.getInPath();
    }
    return getRuleRecvPath();
  }

  /**
   * @return the sendPath
   */
  public String getSendPath() {
    if (getRuleSendPath() == null || getRuleSendPath().trim().isEmpty()) {
      return Configuration.configuration.getOutPath();
    }
    return getRuleSendPath();
  }

  /**
   * @return the archivePath
   */
  public String getArchivePath() {
    if (getRuleArchivePath() == null || getRuleArchivePath().trim().isEmpty()) {
      return Configuration.configuration.getArchivePath();
    }
    return getRuleArchivePath();
  }

  /**
   * @return the workPath
   */
  public String getWorkPath() {
    if (getRuleWorkPath() == null || getRuleWorkPath().trim().isEmpty()) {
      return Configuration.configuration.getWorkingPath();
    }
    return getRuleWorkPath();
  }

  /**
   * @return the Rule recvPath
   */
  public String getRuleRecvPath() {
    return rule.getRecvPath();
  }

  /**
   * @return the Rule sendPath
   */
  public String getRuleSendPath() {
    return rule.getSendPath();
  }

  /**
   * @return the Rule archivePath
   */
  public String getRuleArchivePath() {
    return rule.getArchivePath();
  }

  /**
   * @return the Rule workPath
   */
  public String getRuleWorkPath() {
    return rule.getWorkPath();
  }

  /**
   * @return the DbValue associated with this table
   */
  public static DbValue[] getAllType() {
    final DbRule item = new DbRule();
    return item.allFields;
  }

  /**
   * @return the idRule
   */
  public String getIdRule() {
    return rule.getName();
  }

  /**
   * @return the mode
   */
  public int getMode() {
    return rule.getMode();
  }

  /**
   * @return the idsArray
   */
  public String[] getIdsArray() {
    return rule.getHostids().toArray(new String[0]);
  }

  /**
   * @return the rpreTasksArray
   */
  public String[][] getRpreTasksArray() {
    return toLegacyTasks(rule.getRPreTasks());
  }

  /**
   * @return the rpostTasksArray
   */
  public String[][] getRpostTasksArray() {
    return toLegacyTasks(rule.getRPostTasks());
  }

  /**
   * @return the rerrorTasksArray
   */
  public String[][] getRerrorTasksArray() {
    return toLegacyTasks(rule.getRErrorTasks());
  }

  /**
   * @return the spreTasksArray
   */
  public String[][] getSpreTasksArray() {
    return toLegacyTasks(rule.getSPreTasks());
  }

  /**
   * @return the spostTasksArray
   */
  public String[][] getSpostTasksArray() {
    return toLegacyTasks(rule.getSPostTasks());
  }

  /**
   * @return the serrorTasksArray
   */
  public String[][] getSerrorTasksArray() {
    return toLegacyTasks(rule.getSErrorTasks());
  }

  private List<RuleTask> fromLegacyTasks(String[][] tasks) {
    final int size = tasks.length;
    final List<RuleTask> res = new ArrayList<RuleTask>(size);
    for (int i = 0; i < size; i++) {
      res.add(new RuleTask(tasks[i][0], tasks[i][1],
                           Integer.parseInt(tasks[i][2])));
    }
    return res;
  }

  private String[][] toLegacyTasks(List<RuleTask> tasks) {
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
