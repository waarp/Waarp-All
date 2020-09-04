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
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.openr66.dao.RuleDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.pojo.Rule;
import org.waarp.openr66.pojo.RuleTask;
import org.waarp.openr66.pojo.UpdatedInfo;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.waarp.openr66.dao.DAOFactory.*;

/**
 * Implementation of RuleDAO for standard SQL databases
 */
public class DBRuleDAO extends StatementExecutor<Rule> implements RuleDAO {

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

  public DBRuleDAO(final Connection con) {
    super(con);
  }

  @Override
  protected String getId(final Rule e1) {
    return e1.getName();
  }

  @Override
  protected String getSelectRequest() {
    return SQL_SELECT;
  }

  @Override
  protected String getGetAllRequest() {
    return SQL_GET_ALL;
  }

  @Override
  protected String getExistRequest() {
    return SQL_EXIST;
  }

  @Override
  protected Object[] getInsertValues(final Rule rule) {
    return new Object[] {
        rule.getName(), rule.getXMLHostids(), rule.getMode(),
        rule.getRecvPath(), rule.getSendPath(), rule.getArchivePath(),
        rule.getWorkPath(), rule.getXMLRPreTasks(), rule.getXMLRPostTasks(),
        rule.getXMLRErrorTasks(), rule.getXMLSPreTasks(),
        rule.getXMLSPostTasks(), rule.getXMLSErrorTasks(),
        rule.getUpdatedInfo().ordinal()
    };
  }

  @Override
  protected String getInsertRequest() {
    return SQL_INSERT;
  }

  @Override
  protected Object[] getUpdateValues(final Rule rule) {
    return new Object[] {
        rule.getName(), rule.getXMLHostids(), rule.getMode(),
        rule.getRecvPath(), rule.getSendPath(), rule.getArchivePath(),
        rule.getWorkPath(), rule.getXMLRPreTasks(), rule.getXMLRPostTasks(),
        rule.getXMLRErrorTasks(), rule.getXMLSPreTasks(),
        rule.getXMLSPostTasks(), rule.getXMLSErrorTasks(),
        rule.getUpdatedInfo().ordinal(), rule.getName()
    };
  }

  @Override
  protected String getUpdateRequest() {
    return SQL_UPDATE;
  }

  @Override
  protected String getDeleteRequest() {
    return SQL_DELETE;
  }

  @Override
  protected String getDeleteAllRequest() {
    return SQL_DELETE_ALL;
  }

  @Override
  public Rule getFromResultSet(final ResultSet set)
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

  private List<String> retrieveHostids(final String xml)
      throws DAOConnectionException {
    final ArrayList<String> res = new ArrayList<String>();
    if (xml == null || xml.isEmpty()) {
      return res;
    }
    final Document document;
    try {
      final InputStream stream =
          new ByteArrayInputStream(xml.getBytes(WaarpStringUtils.UTF8));
      final DocumentBuilderFactory factory = getDocumentBuilderFactory();
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

  private List<RuleTask> retrieveTasks(final String xml)
      throws DAOConnectionException {
    final ArrayList<RuleTask> res = new ArrayList<RuleTask>();
    if (xml == null || xml.isEmpty()) {
      return res;
    }
    final Document document;
    try {
      final InputStream stream =
          new ByteArrayInputStream(xml.getBytes(WaarpStringUtils.UTF8));
      final DocumentBuilderFactory factory = getDocumentBuilderFactory();
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
