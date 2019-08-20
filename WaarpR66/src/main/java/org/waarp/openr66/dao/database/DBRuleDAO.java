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

package org.waarp.openr66.dao.database;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.RuleDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.Rule;
import org.waarp.openr66.pojo.RuleTask;
import org.waarp.openr66.pojo.UpdatedInfo;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.waarp.openr66.dao.DAOFactory.*;

/**
 * Implementation of RuleDAO for standard SQL databases
 */
public class DBRuleDAO extends StatementExecutor implements RuleDAO {

  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(DBRuleDAO.class);

  protected static final String TABLE = "rules";

  public static final String ID_FIELD = "idrule";
  public static final String HOSTIDS_FIELD = "hostids";
  public static final String MODE_TRANS_FIELD = "modetrans";
  public static final String RECV_PATH_FIELD = "recvpath";
  public static final String SEND_PATH_FIELD = "sendpath";
  public static final String ARCHIVE_PATH_FIELD = "archivepath";
  public static final String WORK_PATH_FIELD = "workpath";
  public static final String R_PRE_TASKS_FIELD = "rpretasks";
  public static final String R_POST_TASKS_FIELD = "rposttasks";
  public static final String R_ERROR_TASKS_FIELD = "rerrortasks";
  public static final String S_PRE_TASKS_FIELD = "spretasks";
  public static final String S_POST_TASKS_FIELD = "sposttasks";
  public static final String S_ERROR_TASKS_FIELD = "serrortasks";
  public static final String UPDATED_INFO_FIELD = "updatedinfo";

  protected static final String SQL_DELETE_ALL = "DELETE FROM " + TABLE;
  protected static final String SQL_DELETE =
      "DELETE FROM " + TABLE + " WHERE " + ID_FIELD + " = ?";
  protected static final String SQL_GET_ALL = "SELECT * FROM " + TABLE;
  protected static final String SQL_EXIST =
      "SELECT 1 FROM " + TABLE + " WHERE " + ID_FIELD + " = ?";
  protected static final String SQL_SELECT =
      "SELECT * FROM " + TABLE + " WHERE " + ID_FIELD + " = ?";
  protected static final String SQL_INSERT =
      "INSERT INTO " + TABLE + " (" + ID_FIELD + ", " + HOSTIDS_FIELD + ", " +
      MODE_TRANS_FIELD + ", " + RECV_PATH_FIELD + ", " + SEND_PATH_FIELD +
      ", " + ARCHIVE_PATH_FIELD + ", " + WORK_PATH_FIELD + ", " +
      R_PRE_TASKS_FIELD + ", " + R_POST_TASKS_FIELD + ", " +
      R_ERROR_TASKS_FIELD + ", " + S_PRE_TASKS_FIELD + ", " +
      S_POST_TASKS_FIELD + ", " + S_ERROR_TASKS_FIELD + ", " +
      UPDATED_INFO_FIELD + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

  protected static final String SQL_UPDATE =
      "UPDATE " + TABLE + " SET " + ID_FIELD + " = ?, " + HOSTIDS_FIELD +
      " = ?, " + MODE_TRANS_FIELD + " = ? ," + RECV_PATH_FIELD + " = ?, " +
      SEND_PATH_FIELD + " = ?, " + ARCHIVE_PATH_FIELD + " = ? ," +
      WORK_PATH_FIELD + " = ? ," + R_PRE_TASKS_FIELD + " = ? ," +
      R_POST_TASKS_FIELD + " = ? ," + R_ERROR_TASKS_FIELD + " = ? ," +
      S_PRE_TASKS_FIELD + " = ? ," + S_POST_TASKS_FIELD + " = ? ," +
      S_ERROR_TASKS_FIELD + " = ? ," + UPDATED_INFO_FIELD + " = ? WHERE " +
      ID_FIELD + " = ?";

  public DBRuleDAO(Connection con) {
    super(con);
  }

  @Override
  public void delete(Rule rule)
      throws DAOConnectionException, DAONoDataException {
    PreparedStatement stm = null;
    try {
      stm = connection.prepareStatement(SQL_DELETE);
      setParameters(stm, rule.getName());
      try {
        executeUpdate(stm);
      } catch (final SQLException e2) {
        throw new DAONoDataException(e2);
      }
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeStatement(stm);
    }
  }

  @Override
  public void deleteAll() throws DAOConnectionException {
    PreparedStatement stm = null;
    try {
      stm = connection.prepareStatement(SQL_DELETE_ALL);
      executeUpdate(stm);
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeStatement(stm);
    }
  }

  @Override
  public List<Rule> getAll() throws DAOConnectionException {
    final ArrayList<Rule> rules = new ArrayList<Rule>();
    PreparedStatement stm = null;
    ResultSet res = null;
    try {
      stm = connection.prepareStatement(SQL_GET_ALL);
      res = executeQuery(stm);
      while (res.next()) {
        rules.add(getFromResultSet(res));
      }
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeResultSet(res);
      closeStatement(stm);
    }
    return rules;
  }

  @Override
  public List<Rule> find(List<Filter> filters) throws DAOConnectionException {
    final ArrayList<Rule> rules = new ArrayList<Rule>();
    // Create the SQL query
    final StringBuilder query = new StringBuilder(SQL_GET_ALL);
    final Object[] params = new Object[filters.size()];
    final Iterator<Filter> it = filters.listIterator();
    if (it.hasNext()) {
      query.append(" WHERE ");
    }
    String prefix = "";
    int i = 0;
    while (it.hasNext()) {
      query.append(prefix);
      final Filter filter = it.next();
      query.append(filter.key + ' ' + filter.operand + " ?");
      params[i] = filter.value;
      i++;
      prefix = " AND ";
    }
    // Execute query
    PreparedStatement stm = null;
    ResultSet res = null;
    try {
      stm = connection.prepareStatement(query.toString());
      setParameters(stm, params);
      res = executeQuery(stm);
      while (res.next()) {
        rules.add(getFromResultSet(res));
      }
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeResultSet(res);
      closeStatement(stm);
    }
    return rules;
  }

  @Override
  public boolean exist(String ruleName) throws DAOConnectionException {
    PreparedStatement stm = null;
    ResultSet res = null;
    try {
      stm = connection.prepareStatement(SQL_EXIST);
      setParameters(stm, ruleName);
      res = executeQuery(stm);
      return res.next();
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeResultSet(res);
      closeStatement(stm);
    }
  }

  @Override
  public Rule select(String ruleName)
      throws DAOConnectionException, DAONoDataException {
    PreparedStatement stm = null;
    ResultSet res = null;
    try {
      stm = connection.prepareStatement(SQL_SELECT);
      setParameters(stm, ruleName);
      res = executeQuery(stm);
      if (res.next()) {
        return getFromResultSet(res);
      } else {
        throw new DAONoDataException("No " + getClass().getName() + " found");
      }
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeResultSet(res);
      closeStatement(stm);
    }
  }

  @Override
  public void insert(Rule rule) throws DAOConnectionException {
    final Object[] params = {
        rule.getName(), rule.getXMLHostids(), rule.getMode(),
        rule.getRecvPath(), rule.getSendPath(), rule.getArchivePath(),
        rule.getWorkPath(), rule.getXMLRPreTasks(), rule.getXMLRPostTasks(),
        rule.getXMLRErrorTasks(), rule.getXMLSPreTasks(),
        rule.getXMLSPostTasks(), rule.getXMLSErrorTasks(),
        rule.getUpdatedInfo().ordinal()
    };

    PreparedStatement stm = null;
    try {
      stm = connection.prepareStatement(SQL_INSERT);
      setParameters(stm, params);
      executeUpdate(stm);
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeStatement(stm);
    }
  }

  @Override
  public void update(Rule rule)
      throws DAOConnectionException, DAONoDataException {
    final Object[] params = {
        rule.getName(), rule.getXMLHostids(), rule.getMode(),
        rule.getRecvPath(), rule.getSendPath(), rule.getArchivePath(),
        rule.getWorkPath(), rule.getXMLRPreTasks(), rule.getXMLRPostTasks(),
        rule.getXMLRErrorTasks(), rule.getXMLSPreTasks(),
        rule.getXMLSPostTasks(), rule.getXMLSErrorTasks(),
        rule.getUpdatedInfo().ordinal(), rule.getName()
    };

    PreparedStatement stm = null;
    try {
      stm = connection.prepareStatement(SQL_UPDATE);
      setParameters(stm, params);
      try {
        executeUpdate(stm);
      } catch (final SQLException e2) {
        throw new DAONoDataException(e2);
      }
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeStatement(stm);
    }
  }

  protected Rule getFromResultSet(ResultSet set)
      throws SQLException, DAOConnectionException {
    return new Rule(set.getString(ID_FIELD), set.getInt(MODE_TRANS_FIELD),
                    retrieveHostids(set.getString(HOSTIDS_FIELD)),
                    set.getString(RECV_PATH_FIELD),
                    set.getString(SEND_PATH_FIELD),
                    set.getString(ARCHIVE_PATH_FIELD),
                    set.getString(WORK_PATH_FIELD),
                    retrieveTasks(set.getString(R_PRE_TASKS_FIELD)),
                    retrieveTasks(set.getString(R_POST_TASKS_FIELD)),
                    retrieveTasks(set.getString(R_ERROR_TASKS_FIELD)),
                    retrieveTasks(set.getString(S_PRE_TASKS_FIELD)),
                    retrieveTasks(set.getString(S_POST_TASKS_FIELD)),
                    retrieveTasks(set.getString(S_ERROR_TASKS_FIELD)),
                    UpdatedInfo.valueOf(set.getInt(UPDATED_INFO_FIELD)));
  }

  private List<String> retrieveHostids(String xml)
      throws DAOConnectionException {
    final ArrayList<String> res = new ArrayList<String>();
    if (xml == null || xml.isEmpty()) {
      return res;
    }
    Document document;
    try {
      final InputStream stream =
          new ByteArrayInputStream(xml.getBytes("UTF-8"));
      DocumentBuilderFactory factory = getDocumentBuilderFactory();
      document = factory.newDocumentBuilder().parse(stream);
    } catch (final Exception e) {
      throw new DAOConnectionException(e);
    }
    document.getDocumentElement().normalize();

    final NodeList hostsList = document.getElementsByTagName("hostid");
    for (int i = 0; i < hostsList.getLength(); i++) {
      res.add(hostsList.item(i).getTextContent());
    }
    return res;
  }

  private List<RuleTask> retrieveTasks(String xml)
      throws DAOConnectionException {
    final ArrayList<RuleTask> res = new ArrayList<RuleTask>();
    if (xml == null || xml.isEmpty()) {
      return res;
    }
    Document document;
    try {
      final InputStream stream =
          new ByteArrayInputStream(xml.getBytes("UTF-8"));
      DocumentBuilderFactory factory = getDocumentBuilderFactory();
      document = factory.newDocumentBuilder().parse(stream);
    } catch (final Exception e) {
      throw new DAOConnectionException(e);
    }
    document.getDocumentElement().normalize();

    final NodeList tasksList = document.getElementsByTagName("task");
    for (int i = 0; i < tasksList.getLength(); i++) {
      final Node node = tasksList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        final Element e = (Element) node;
        final String type =
            e.getElementsByTagName("type").item(0).getTextContent();
        final String path =
            e.getElementsByTagName("path").item(0).getTextContent();
        final int delay = Integer
            .parseInt(e.getElementsByTagName("delay").item(0).getTextContent());
        res.add(new RuleTask(type, path, delay));
      }
    }
    return res;
  }
}
