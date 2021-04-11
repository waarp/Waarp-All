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
package org.waarp.openr66.configuration;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.tree.DefaultElement;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseNoDataException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.file.DirInterface;
import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.xml.XmlDecl;
import org.waarp.common.xml.XmlHash;
import org.waarp.common.xml.XmlType;
import org.waarp.common.xml.XmlUtil;
import org.waarp.common.xml.XmlValue;
import org.waarp.openr66.context.task.TaskType;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.dao.RuleDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.xml.XMLDAOFactory;
import org.waarp.openr66.dao.xml.XMLRuleDAO;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoDataException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.waarp.common.database.DbConstant.*;

/**
 * Rule File Based Configuration
 */
public final class RuleFileBasedConfiguration {
  private static final String UNABLE_TO_READ_THE_XML_RULE_FILE =
      "Unable to read the XML Rule file: ";

  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(RuleFileBasedConfiguration.class);

  public static final String MULTIPLEROOT = "rules";
  public static final String ROOT = "rule";
  public static final String XIDRULE = "idrule";
  public static final String XHOSTIDS = "hostids";
  public static final String XHOSTID = "hostid";
  public static final String XMODE = "mode";
  public static final String XRECVPATH = "recvpath";
  public static final String XSENDPATH = "sendpath";
  public static final String XARCHIVEPATH = "archivepath";
  public static final String XWORKPATH = "workpath";
  public static final String XRPRETASKS = "rpretasks";
  public static final String XRPOSTTASKS = "rposttasks";
  public static final String XRERRORTASKS = "rerrortasks";
  public static final String XSPRETASKS = "spretasks";
  public static final String XSPOSTTASKS = "sposttasks";
  public static final String XSERRORTASKS = "serrortasks";
  public static final String XTASKS = "tasks";
  public static final String XTASK = "task";

  private static final String HOSTIDS_HOSTID = '/' + XHOSTIDS + '/' + XHOSTID;

  private static final String TASK = "/tasks/task";

  private static final XmlDecl[] taskDecl = {
      new XmlDecl(XmlType.STRING, DbRule.TASK_TYPE),
      new XmlDecl(XmlType.STRING, DbRule.TASK_PATH),
      new XmlDecl(XmlType.LONG, DbRule.TASK_DELAY),
      new XmlDecl(XmlType.INTEGER, DbRule.TASK_RANK),
      new XmlDecl(XmlType.STRING, DbRule.TASK_COMMENT)
  };
  public static final XmlDecl[] tasksDecl =
      { new XmlDecl(XTASK, XmlType.XVAL, TASK, taskDecl, true) };
  private static final XmlDecl[] subruleDecls = {
      new XmlDecl(XmlType.STRING, XIDRULE),
      new XmlDecl(XHOSTIDS, XmlType.STRING, HOSTIDS_HOSTID, true),
      new XmlDecl(XmlType.INTEGER, XMODE),
      new XmlDecl(XmlType.STRING, XRECVPATH),
      new XmlDecl(XmlType.STRING, XSENDPATH),
      new XmlDecl(XmlType.STRING, XARCHIVEPATH),
      new XmlDecl(XmlType.STRING, XWORKPATH),
      new XmlDecl(XRPRETASKS, XmlType.XVAL, XRPRETASKS, tasksDecl, false),
      new XmlDecl(XRPOSTTASKS, XmlType.XVAL, XRPOSTTASKS, tasksDecl, false),
      new XmlDecl(XRERRORTASKS, XmlType.XVAL, XRERRORTASKS, tasksDecl, false),
      new XmlDecl(XSPRETASKS, XmlType.XVAL, XSPRETASKS, tasksDecl, false),
      new XmlDecl(XSPOSTTASKS, XmlType.XVAL, XSPOSTTASKS, tasksDecl, false),
      new XmlDecl(XSERRORTASKS, XmlType.XVAL, XSERRORTASKS, tasksDecl, false)
  };
  private static final XmlDecl[] ruleDecls =
      { new XmlDecl(ROOT, XmlType.XVAL, ROOT, subruleDecls, false) };
  private static final XmlDecl[] multipleruleDecls = {
      new XmlDecl(MULTIPLEROOT, XmlType.XVAL, '/' + MULTIPLEROOT + '/' + ROOT,
                  subruleDecls, true)
  };
  public static final XmlDecl[] hostsDecls =
      { new XmlDecl(XHOSTIDS, XmlType.STRING, HOSTIDS_HOSTID, true), };

  /**
   * Extension of rule files
   */
  public static final String EXT_RULE = ".rule.xml";
  /**
   * Extension of multiple rules in one file
   */
  public static final String EXT_RULES = ".rules.xml";
  private static final String[][] STRINGS_0_0_LENGTH = new String[0][0];
  private static final String[] STRING_0_LENGTH = new String[0];

  private RuleFileBasedConfiguration() {
  }

  /**
   * Import all Rule files into the HashTable of Rules
   *
   * @param configDirectory
   *
   * @throws OpenR66ProtocolSystemException
   * @throws WaarpDatabaseException
   */
  public static void importRules(final File configDirectory)
      throws OpenR66ProtocolSystemException, WaarpDatabaseException {
    final DAOFactory daoFactory = DAOFactory.getInstance();
    if (daoFactory instanceof XMLDAOFactory) {
      RuleDAO ruleDAO = null;
      try {
        ruleDAO = daoFactory.getRuleDAO();
        if (ruleDAO instanceof XMLRuleDAO) {
          final XMLRuleDAO xmlRuleDAO = (XMLRuleDAO) ruleDAO;
          // To allow loading into cache all Rules
          xmlRuleDAO.getAll();
        }
      } catch (final DAOConnectionException e) {
        SysErrLogger.FAKE_LOGGER.syserr(e);
      } finally {
        if (ruleDAO != null) {
          ruleDAO.close();
        }
      }
    }
    File[] files =
        FileUtils.getFiles(configDirectory, new ExtensionFilter(EXT_RULE));
    for (final File file : files) {
      logger.info("Load rule from {}", file.getAbsolutePath());
      final DbRule rule = getFromFile(file);
      logger.debug("{}", rule);
    }
    files = FileUtils.getFiles(configDirectory, new ExtensionFilter(EXT_RULES));
    for (final File file : files) {
      getMultipleFromFile(file);
    }
  }

  /**
   * Utility function
   *
   * @param value
   *
   * @return the array of tasks or empty array if in error.
   */
  @SuppressWarnings("unchecked")
  public static String[][] getTasksRule(final XmlValue value) {
    final List<XmlValue[]> list = (List<XmlValue[]>) value.getList();
    if (list == null || list.isEmpty()) {
      // Unable to find the tasks for Rule, setting to the default
      return STRINGS_0_0_LENGTH;
    }
    final String[][] taskArray = new String[list.size()][5];
    for (int i = 0; i < list.size(); i++) {
      taskArray[i][0] = null;
      taskArray[i][1] = null;
      taskArray[i][2] = null;
      taskArray[i][3] = null;
      taskArray[i][4] = null;
    }
    int rank = 0;
    for (final XmlValue[] subvals : list) {
      final XmlHash hash = new XmlHash(subvals);
      final XmlValue valtype = hash.get(DbRule.TASK_TYPE);
      if (valtype == null || valtype.isEmpty() ||
          valtype.getString().isEmpty()) {
        continue;
      }
      final XmlValue valpath = hash.get(DbRule.TASK_PATH);
      if (valpath == null || valpath.isEmpty() ||
          valtype.getString().isEmpty()) {
        continue;
      }
      final XmlValue valdelay = hash.get(DbRule.TASK_DELAY);
      final String delay;
      if (valdelay == null || valdelay.isEmpty()) {
        delay = Long.toString(Configuration.configuration.getTimeoutCon());
      } else {
        delay = valdelay.getIntoString();
      }
      final XmlValue valcomment = hash.get(DbRule.TASK_COMMENT);
      final String comment;
      if (valcomment == null || valcomment.isEmpty() ||
          valcomment.getString().isEmpty()) {
        comment = "";
      } else {
        comment = valcomment.getString();
      }
      final XmlValue valrank = hash.get(DbRule.TASK_RANK);
      final String srank;
      if (valrank == null || valrank.isEmpty()) {
        srank = Integer.toString(rank);
      } else {
        srank = valrank.getIntoString();
      }
      taskArray[rank][0] = valtype.getString().toUpperCase();
      // CHECK TASK_TYPE
      if (!TaskType.isValidTask(taskArray[rank][0])) {
        // Bad Type
        logger.warn("Bad Type of Task: " + taskArray[rank][0]);
        continue;
      }
      taskArray[rank][1] = valpath.getString();
      taskArray[rank][2] = delay;
      taskArray[rank][3] = comment;
      taskArray[rank][4] = srank;
      rank++;
      hash.clear();
    }
    list.clear();
    return taskArray;
  }

  /**
   * @param value the XmlValue hosting hostids/hostid
   *
   * @return the array of HostIds allowed for the current rule
   */
  public static String[] getHostIds(final XmlValue value) {
    String[] idsArray = STRING_0_LENGTH;
    if (value == null || value.getList() == null || value.getList().isEmpty()) {
      logger
          .debug("Unable to find the Hostid for Rule, setting to the default");
    } else {
      @SuppressWarnings("unchecked")
      final List<String> ids = (List<String>) value.getList();
      idsArray = new String[ids.size()];
      int i = 0;
      for (final String sval : ids) {
        if (sval.isEmpty()) {
          continue;
        }
        idsArray[i] = sval;
        i++;
      }
      ids.clear();
    }
    return idsArray;
  }

  /**
   * Load and update a Rule from a file
   *
   * @param file
   *
   * @return the newly created R66Rule from XML File
   *
   * @throws OpenR66ProtocolSystemException
   * @throws WaarpDatabaseException
   * @throws WaarpDatabaseNoDataException
   * @throws WaarpDatabaseSqlException
   * @throws WaarpDatabaseNoConnectionException
   * @throws OpenR66ProtocolNoDataException
   */
  public static DbRule getFromFile(final File file)
      throws OpenR66ProtocolSystemException, WaarpDatabaseNoConnectionException,
             WaarpDatabaseSqlException, WaarpDatabaseNoDataException,
             WaarpDatabaseException {
    final DbRule newRule;
    final Document document;
    // Open config file
    try {
      document = XmlUtil.getNewSaxReader().read(file);
    } catch (final DocumentException e) {
      logger.error(UNABLE_TO_READ_THE_XML_RULE_FILE + file.getName(), e);
      throw new OpenR66ProtocolSystemException(UNABLE_TO_READ_THE_XML_RULE_FILE,
                                               e);
    }
    if (document == null) {
      logger.error(UNABLE_TO_READ_THE_XML_RULE_FILE + file.getName());
      throw new OpenR66ProtocolSystemException(
          UNABLE_TO_READ_THE_XML_RULE_FILE);
    }
    final XmlValue[] values = XmlUtil.read(document, ruleDecls);
    newRule = getFromXmlValue(values);
    return newRule;
  }

  /**
   * Load and update multiple Rules from one file
   *
   * @param file
   *
   * @return a list of newly created R66Rule from XML File
   *
   * @throws OpenR66ProtocolSystemException
   * @throws WaarpDatabaseException
   * @throws WaarpDatabaseNoDataException
   * @throws WaarpDatabaseSqlException
   * @throws WaarpDatabaseNoConnectionException
   * @throws OpenR66ProtocolNoDataException
   */
  public static List<DbRule> getMultipleFromFile(final File file)
      throws OpenR66ProtocolSystemException, WaarpDatabaseNoConnectionException,
             WaarpDatabaseSqlException, WaarpDatabaseNoDataException,
             WaarpDatabaseException {
    final Document document;
    // Open config file
    try {
      document = XmlUtil.getNewSaxReader().read(file);
    } catch (final DocumentException e) {
      logger.error(UNABLE_TO_READ_THE_XML_RULE_FILE + file.getName(), e);
      throw new OpenR66ProtocolSystemException(UNABLE_TO_READ_THE_XML_RULE_FILE,
                                               e);
    }
    if (document == null) {
      logger.error(UNABLE_TO_READ_THE_XML_RULE_FILE + file.getName());
      throw new OpenR66ProtocolSystemException(
          UNABLE_TO_READ_THE_XML_RULE_FILE);
    }
    final XmlValue[] values = XmlUtil.read(document, multipleruleDecls);
    if (values.length <= 0) {
      return new ArrayList<DbRule>(0);
    }
    final XmlValue value = values[0];
    @SuppressWarnings("unchecked")
    final List<XmlValue[]> list = (List<XmlValue[]>) value.getList();
    final List<DbRule> result = new ArrayList<DbRule>(list.size());
    for (final XmlValue[] xmlValue : list) {
      result.add(getFromXmlValue(xmlValue));
    }
    return result;
  }

  /**
   * Load and update one Rule from a XmlValue
   *
   * @param root
   *
   * @return the newly created R66Rule from XML File
   *
   * @throws OpenR66ProtocolSystemException
   * @throws WaarpDatabaseException
   * @throws WaarpDatabaseNoDataException
   * @throws WaarpDatabaseSqlException
   * @throws WaarpDatabaseNoConnectionException
   * @throws OpenR66ProtocolNoDataException
   */
  private static DbRule getFromXmlValue(final XmlValue[] root)
      throws OpenR66ProtocolSystemException, WaarpDatabaseNoConnectionException,
             WaarpDatabaseSqlException, WaarpDatabaseNoDataException,
             WaarpDatabaseException {
    final DbRule newRule;
    final XmlHash hash = new XmlHash(root);
    XmlValue value = hash.get(XIDRULE);
    if (value == null || value.isEmpty() || value.getString().isEmpty()) {
      logger.error("Unable to find in Rule field: " + XIDRULE);
      throw new OpenR66ProtocolSystemException();
    }
    final String idrule = value.getString();
    value = hash.get(XMODE);
    if (value == null || value.isEmpty()) {
      logger.error("Unable to find in Rule field: " + XMODE);
      throw new OpenR66ProtocolSystemException();
    }
    final int mode = value.getInteger();
    final String recvpath;
    value = hash.get(XRECVPATH);
    if (value == null || value.isEmpty() || value.getString().isEmpty()) {
      recvpath = Configuration.configuration.getInPath();
    } else {
      recvpath = DirInterface.SEPARATOR + value.getString();
    }
    final String sendpath;
    value = hash.get(XSENDPATH);
    if (value == null || value.isEmpty() || value.getString().isEmpty()) {
      sendpath = Configuration.configuration.getOutPath();
    } else {
      sendpath = DirInterface.SEPARATOR + value.getString();
    }
    final String archivepath;
    value = hash.get(XARCHIVEPATH);
    if (value == null || value.isEmpty() || value.getString().isEmpty()) {
      archivepath = Configuration.configuration.getArchivePath();
    } else {
      archivepath = DirInterface.SEPARATOR + value.getString();
    }
    final String workpath;
    value = hash.get(XWORKPATH);
    if (value == null || value.isEmpty() || value.getString().isEmpty()) {
      workpath = Configuration.configuration.getWorkingPath();
    } else {
      workpath = DirInterface.SEPARATOR + value.getString();
    }
    final String[] idsArray;
    value = hash.get(XHOSTIDS);
    idsArray = getHostIds(value);
    String[][] rpretasks = STRINGS_0_0_LENGTH;
    value = hash.get(XRPRETASKS);
    if (value != null && !value.isEmpty()) {
      final XmlValue[] subvalues = value.getSubXml();
      if (subvalues.length > 0) {
        rpretasks = getTasksRule(subvalues[0]);
      }
    }
    String[][] rposttasks = STRINGS_0_0_LENGTH;
    value = hash.get(XRPOSTTASKS);
    if (value != null && !value.isEmpty()) {
      final XmlValue[] subvalues = value.getSubXml();
      if (subvalues.length > 0) {
        rposttasks = getTasksRule(subvalues[0]);
      }
    }
    String[][] rerrortasks = STRINGS_0_0_LENGTH;
    value = hash.get(XRERRORTASKS);
    if (value != null && !value.isEmpty()) {
      final XmlValue[] subvalues = value.getSubXml();
      if (subvalues.length > 0) {
        rerrortasks = getTasksRule(subvalues[0]);
      }
    }
    String[][] spretasks = STRINGS_0_0_LENGTH;
    value = hash.get(XSPRETASKS);
    if (value != null && !value.isEmpty()) {
      final XmlValue[] subvalues = value.getSubXml();
      if (subvalues.length > 0) {
        spretasks = getTasksRule(subvalues[0]);
      }
    }
    String[][] sposttasks = STRINGS_0_0_LENGTH;
    value = hash.get(XSPOSTTASKS);
    if (value != null && !value.isEmpty()) {
      final XmlValue[] subvalues = value.getSubXml();
      if (subvalues.length > 0) {
        sposttasks = getTasksRule(subvalues[0]);
      }
    }
    String[][] serrortasks = STRINGS_0_0_LENGTH;
    value = hash.get(XSERRORTASKS);
    if (value != null && !value.isEmpty()) {
      final XmlValue[] subvalues = value.getSubXml();
      if (subvalues.length > 0) {
        serrortasks = getTasksRule(subvalues[0]);
      }
    }

    newRule =
        new DbRule(idrule, idsArray, mode, recvpath, sendpath, archivepath,
                   workpath, rpretasks, rposttasks, rerrortasks, spretasks,
                   sposttasks, serrortasks);
    if (admin != null && admin.getSession() != null) {
      if (newRule.exist()) {
        newRule.update();
      } else {
        newRule.insert();
      }
    } else {
      // put in hashtable
      newRule.insert();
    }
    hash.clear();
    return newRule;
  }

  /**
   * Construct a new Element with value
   *
   * @param name
   * @param value
   *
   * @return the new Element
   */
  private static Element newElement(final String name, final String value) {
    final Element node = new DefaultElement(name);
    node.addText(value);
    return node;
  }

  /**
   * Add a rule from root element (ROOT = rule)
   *
   * @param element
   * @param rule
   */
  private static void addToElement(final Element element, final DbRule rule) {
    element.add(newElement(XIDRULE, rule.getIdRule()));
    final Element hosts = new DefaultElement(XHOSTIDS);
    if (rule.getIdsArray() != null) {
      for (final String host : rule.getIdsArray()) {
        hosts.add(newElement(XHOSTID, host));
      }
    }
    element.add(hosts);
    element.add(newElement(XMODE, Integer.toString(rule.getMode())));
    String dir = rule.getRuleRecvPath();
    if (dir != null) {
      if (dir.startsWith(File.separator) ||
          dir.startsWith(DirInterface.SEPARATOR)) {
        element.add(newElement(XRECVPATH, dir.substring(1)));
      } else {
        element.add(newElement(XRECVPATH, dir));
      }
    }
    dir = rule.getRuleSendPath();
    if (dir != null) {
      if (dir.startsWith(File.separator) ||
          dir.startsWith(DirInterface.SEPARATOR)) {
        element.add(newElement(XSENDPATH, dir.substring(1)));
      } else {
        element.add(newElement(XSENDPATH, dir));
      }
    }
    dir = rule.getRuleArchivePath();
    if (dir != null) {
      if (dir.startsWith(File.separator) ||
          dir.startsWith(DirInterface.SEPARATOR)) {
        element.add(newElement(XARCHIVEPATH, dir.substring(1)));
      } else {
        element.add(newElement(XARCHIVEPATH, dir));
      }
    }
    dir = rule.getRuleWorkPath();
    if (dir != null) {
      if (dir.startsWith(File.separator) ||
          dir.startsWith(DirInterface.SEPARATOR)) {
        element.add(newElement(XWORKPATH, dir.substring(1)));
      } else {
        element.add(newElement(XWORKPATH, dir));
      }
    }
    Element tasks = new DefaultElement(XRPRETASKS);
    Element roottasks = new DefaultElement(XTASKS);
    int rank;
    String[][] array = rule.getRpreTasksArray();
    if (array != null) {
      for (rank = 0; rank < array.length; rank++) {
        final Element task = new DefaultElement(XTASK);
        task.add(newElement(DbRule.TASK_TYPE, array[rank][0]));
        task.add(newElement(DbRule.TASK_PATH, array[rank][1]));
        task.add(newElement(DbRule.TASK_DELAY, array[rank][2]));
        if (array[rank].length > 3) {
          task.add(newElement(DbRule.TASK_COMMENT, array[rank][3]));
        }
        if (array[rank].length > 4) {
          task.add(newElement(DbRule.TASK_RANK, array[rank][4]));
        }
        roottasks.add(task);
      }
    }
    tasks.add(roottasks);
    element.add(tasks);
    tasks = new DefaultElement(XRPOSTTASKS);
    roottasks = new DefaultElement(XTASKS);
    array = rule.getRpostTasksArray();
    if (array != null) {
      for (rank = 0; rank < array.length; rank++) {
        final Element task = new DefaultElement(XTASK);
        task.add(newElement(DbRule.TASK_TYPE, array[rank][0]));
        task.add(newElement(DbRule.TASK_PATH, array[rank][1]));
        task.add(newElement(DbRule.TASK_DELAY, array[rank][2]));
        if (array[rank].length > 3) {
          task.add(newElement(DbRule.TASK_COMMENT, array[rank][3]));
        }
        if (array[rank].length > 4) {
          task.add(newElement(DbRule.TASK_RANK, array[rank][4]));
        }
        roottasks.add(task);
      }
    }
    tasks.add(roottasks);
    element.add(tasks);
    tasks = new DefaultElement(XRERRORTASKS);
    roottasks = new DefaultElement(XTASKS);
    array = rule.getRerrorTasksArray();
    if (array != null) {
      for (rank = 0; rank < array.length; rank++) {
        final Element task = new DefaultElement(XTASK);
        task.add(newElement(DbRule.TASK_TYPE, array[rank][0]));
        task.add(newElement(DbRule.TASK_PATH, array[rank][1]));
        task.add(newElement(DbRule.TASK_DELAY, array[rank][2]));
        if (array[rank].length > 3) {
          task.add(newElement(DbRule.TASK_COMMENT, array[rank][3]));
        }
        if (array[rank].length > 4) {
          task.add(newElement(DbRule.TASK_RANK, array[rank][4]));
        }
        roottasks.add(task);
      }
    }
    tasks.add(roottasks);
    element.add(tasks);
    tasks = new DefaultElement(XSPRETASKS);
    roottasks = new DefaultElement(XTASKS);
    array = rule.getSpreTasksArray();
    if (array != null) {
      for (rank = 0; rank < array.length; rank++) {
        final Element task = new DefaultElement(XTASK);
        task.add(newElement(DbRule.TASK_TYPE, array[rank][0]));
        task.add(newElement(DbRule.TASK_PATH, array[rank][1]));
        task.add(newElement(DbRule.TASK_DELAY, array[rank][2]));
        if (array[rank].length > 3) {
          task.add(newElement(DbRule.TASK_COMMENT, array[rank][3]));
        }
        if (array[rank].length > 4) {
          task.add(newElement(DbRule.TASK_RANK, array[rank][4]));
        }
        roottasks.add(task);
      }
    }
    tasks.add(roottasks);
    element.add(tasks);
    tasks = new DefaultElement(XSPOSTTASKS);
    roottasks = new DefaultElement(XTASKS);
    array = rule.getSpostTasksArray();
    if (array != null) {
      for (rank = 0; rank < array.length; rank++) {
        final Element task = new DefaultElement(XTASK);
        task.add(newElement(DbRule.TASK_TYPE, array[rank][0]));
        task.add(newElement(DbRule.TASK_PATH, array[rank][1]));
        task.add(newElement(DbRule.TASK_DELAY, array[rank][2]));
        if (array[rank].length > 3) {
          task.add(newElement(DbRule.TASK_COMMENT, array[rank][3]));
        }
        if (array[rank].length > 4) {
          task.add(newElement(DbRule.TASK_RANK, array[rank][4]));
        }
        roottasks.add(task);
      }
    }
    tasks.add(roottasks);
    element.add(tasks);
    tasks = new DefaultElement(XSERRORTASKS);
    roottasks = new DefaultElement(XTASKS);
    array = rule.getSerrorTasksArray();
    if (array != null) {
      for (rank = 0; rank < array.length; rank++) {
        final Element task = new DefaultElement(XTASK);
        task.add(newElement(DbRule.TASK_TYPE, array[rank][0]));
        task.add(newElement(DbRule.TASK_PATH, array[rank][1]));
        task.add(newElement(DbRule.TASK_DELAY, array[rank][2]));
        if (array[rank].length > 3) {
          task.add(newElement(DbRule.TASK_COMMENT, array[rank][3]));
        }
        if (array[rank].length > 4) {
          task.add(newElement(DbRule.TASK_RANK, array[rank][4]));
        }
        roottasks.add(task);
      }
    }
    tasks.add(roottasks);
    element.add(tasks);
  }

  /**
   * Write the rule to a file from filename
   *
   * @param filename
   * @param rule
   *
   * @throws OpenR66ProtocolSystemException
   */
  private static void writeXMLInternal(final String filename, final DbRule rule)
      throws OpenR66ProtocolSystemException {
    final Document document = DocumentHelper.createDocument();
    final Element root = document.addElement(ROOT);
    addToElement(root, rule);
    try {
      XmlUtil.writeXML(filename, null, document);
    } catch (final IOException e) {
      throw new OpenR66ProtocolSystemException("Cannot write file: " + filename,
                                               e);
    }
  }

  /**
   * Write to directory files prefixed by hostname all Rules from database
   *
   * @param directory
   * @param hostname
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws OpenR66ProtocolSystemException
   */
  public static void writeXml(final String directory, final String hostname)
      throws WaarpDatabaseNoConnectionException,
             OpenR66ProtocolSystemException {
    final File dir = new File(directory);
    if (!dir.isDirectory()) {
      dir.mkdirs();//NOSONAR
    }
    final DbRule[] rules = DbRule.getAllRules();
    for (final DbRule rule : rules) {
      final String filename =
          dir.getAbsolutePath() + File.separator + hostname + '_' +
          rule.getIdRule() + EXT_RULE;
      logger.debug("Will write Rule: {} in {}", rule.getIdRule(), filename);
      writeXMLInternal(filename, rule);
    }
  }

  /**
   * Write to directory 1 file prefixed by hostname all Rules from database
   *
   * @param directory
   * @param hostname
   *
   * @return the filename
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws OpenR66ProtocolSystemException
   */
  public static String writeOneXml(final String directory,
                                   final String hostname)
      throws WaarpDatabaseNoConnectionException,
             OpenR66ProtocolSystemException {
    final File dir = new File(directory);
    if (!dir.isDirectory()) {
      dir.mkdirs();//NOSONAR
    }
    final DbRule[] rules = DbRule.getAllRules();
    final String filename =
        dir.getAbsolutePath() + File.separator + hostname + EXT_RULES;
    final Document document = DocumentHelper.createDocument();
    final Element root = document.addElement(MULTIPLEROOT);
    for (final DbRule rule : rules) {
      final Element element = root.addElement(ROOT);
      addToElement(element, rule);
    }
    try {
      XmlUtil.writeXML(filename, null, document);
    } catch (final IOException e) {
      throw new OpenR66ProtocolSystemException("Cannot write file: " + filename,
                                               e);
    }
    return filename;
  }
}
