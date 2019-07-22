package org.waarp.openr66.dao.database;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.UpdatedInfo;

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.dao.RuleDAO;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.pojo.Rule;
import org.waarp.openr66.pojo.RuleTask;

/**
 * Implementation of RuleDAO for standard SQL databases
 */
public class DBRuleDAO extends StatementExecutor implements RuleDAO {

    private static final WaarpLogger logger = WaarpLoggerFactory.getLogger(DBRuleDAO.class);

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
    protected static final String SQL_DELETE = "DELETE FROM " + TABLE
        + " WHERE " + ID_FIELD + " = ?";
    protected static final String SQL_GET_ALL = "SELECT * FROM " + TABLE;
    protected static final String SQL_EXIST = "SELECT 1 FROM " + TABLE
        + " WHERE " + ID_FIELD + " = ?";
    protected static final String SQL_SELECT = "SELECT * FROM " + TABLE
        + " WHERE " + ID_FIELD + " = ?";
    protected static final String SQL_INSERT = "INSERT INTO " + TABLE
        + " (" + ID_FIELD + ", "
        + HOSTIDS_FIELD + ", "
        + MODE_TRANS_FIELD + ", "
        + RECV_PATH_FIELD + ", "
        + SEND_PATH_FIELD + ", "
        + ARCHIVE_PATH_FIELD + ", "
        + WORK_PATH_FIELD + ", "
        + R_PRE_TASKS_FIELD + ", "
        + R_POST_TASKS_FIELD + ", "
        + R_ERROR_TASKS_FIELD + ", "
        + S_PRE_TASKS_FIELD + ", "
        + S_POST_TASKS_FIELD + ", "
        + S_ERROR_TASKS_FIELD + ", "
        + UPDATED_INFO_FIELD + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    protected static final String SQL_UPDATE = "UPDATE " + TABLE
        + " SET " + ID_FIELD + " = ?, "
        + HOSTIDS_FIELD + " = ?, "
        + MODE_TRANS_FIELD + " = ? ,"
        + RECV_PATH_FIELD + " = ?, "
        + SEND_PATH_FIELD + " = ?, "
        + ARCHIVE_PATH_FIELD + " = ? ,"
        + WORK_PATH_FIELD + " = ? ,"
        + R_PRE_TASKS_FIELD + " = ? ,"
        + R_POST_TASKS_FIELD + " = ? ,"
        + R_ERROR_TASKS_FIELD + " = ? ,"
        + S_PRE_TASKS_FIELD + " = ? ,"
        + S_POST_TASKS_FIELD + " = ? ,"
        + S_ERROR_TASKS_FIELD + " = ? ,"
        + UPDATED_INFO_FIELD + " = ? WHERE " + ID_FIELD + " = ?";

    protected Connection connection;

    public DBRuleDAO(Connection con) {
        this.connection = con;
    }

    @Override
    public void close() {
        try {
            this.connection.close();
        } catch (SQLException e) {
            logger.warn("Cannot properly close the database connection", e);
        }
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
            } catch (SQLException e2) {
                throw new DAONoDataException(e2);
            }
        } catch (SQLException e) {
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
        } catch (SQLException e) {
            throw new DAOConnectionException(e);
        } finally {
            closeStatement(stm);
        }
    }

    @Override
    public List<Rule> getAll() throws DAOConnectionException {
        ArrayList<Rule> rules = new ArrayList<Rule>();
        PreparedStatement stm = null;
        ResultSet res = null;
        try {
            stm = connection.prepareStatement(SQL_GET_ALL);
            res = executeQuery(stm);
            while (res.next()) {
                rules.add(getFromResultSet(res));
            }
        } catch (SQLException e) {
            throw new DAOConnectionException(e);
        } finally {
            closeResultSet(res);
            closeStatement(stm);
        }
        return rules;
    }

    @Override
    public List<Rule> find(List<Filter> filters) throws DAOConnectionException {
        ArrayList<Rule> rules = new ArrayList<Rule>();
        // Create the SQL query
        StringBuilder query = new StringBuilder(SQL_GET_ALL);
        Object[] params = new Object[filters.size()];
        Iterator<Filter> it = filters.listIterator();
        if (it.hasNext()) {
            query.append(" WHERE ");
        }
        String prefix = "";
        int i = 0;
        while (it.hasNext()) {
            query.append(prefix);
            Filter filter = it.next();
            query.append(filter.key + " " + filter.operand + " ?");
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
        } catch (SQLException e) {
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
        } catch (SQLException e) {
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
                throw new DAONoDataException(("No " + getClass().getName() + " found"));
            }
        } catch (SQLException e) {
            throw new DAOConnectionException(e);
        } finally {
            closeResultSet(res);
            closeStatement(stm);
        }
    }

    @Override
    public void insert(Rule rule) throws DAOConnectionException {
        Object[] params = {
            rule.getName(),
            rule.getXMLHostids(),
            rule.getMode(),
            rule.getRecvPath(),
            rule.getSendPath(),
            rule.getArchivePath(),
            rule.getWorkPath(),
            rule.getXMLRPreTasks(),
            rule.getXMLRPostTasks(),
            rule.getXMLRErrorTasks(),
            rule.getXMLSPreTasks(),
            rule.getXMLSPostTasks(),
            rule.getXMLSErrorTasks(),
            rule.getUpdatedInfo().ordinal()
        };

        PreparedStatement stm = null;
        try {
            stm = connection.prepareStatement(SQL_INSERT);
            setParameters(stm, params);
            executeUpdate(stm);
        } catch (SQLException e) {
            throw new DAOConnectionException(e);
        } finally {
            closeStatement(stm);
        }
    }

    @Override
    public void update(Rule rule)
        throws DAOConnectionException, DAONoDataException {
        Object[] params = {
            rule.getName(),
            rule.getXMLHostids(),
            rule.getMode(),
            rule.getRecvPath(),
            rule.getSendPath(),
            rule.getArchivePath(),
            rule.getWorkPath(),
            rule.getXMLRPreTasks(),
            rule.getXMLRPostTasks(),
            rule.getXMLRErrorTasks(),
            rule.getXMLSPreTasks(),
            rule.getXMLSPostTasks(),
            rule.getXMLSErrorTasks(),
            rule.getUpdatedInfo().ordinal(),
            rule.getName()
        };

        PreparedStatement stm = null;
        try {
            stm = connection.prepareStatement(SQL_UPDATE);
            setParameters(stm, params);
            try {
                executeUpdate(stm);
            } catch (SQLException e2) {
                throw new DAONoDataException(e2);
            }
        } catch (SQLException e) {
            throw new DAOConnectionException(e);
        } finally {
            closeStatement(stm);
        }
    }

    protected Rule getFromResultSet(ResultSet set) throws SQLException,
                                                          DAOConnectionException {
        return new Rule(
                set.getString(ID_FIELD),
                set.getInt(MODE_TRANS_FIELD),
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

    private List<String> retrieveHostids(String xml) throws
                                                     DAOConnectionException {
        ArrayList<String> res = new ArrayList<String>();
        if ((xml == null) || xml.equals("")) {
            return res;
        }
        Document document = null;
        try {
            InputStream stream = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            document = DocumentBuilderFactory.newInstance().
            newDocumentBuilder().parse(stream);
        } catch (Exception e) {
            throw new DAOConnectionException(e);
        }
        document.getDocumentElement().normalize();

        NodeList hostsList = document.getElementsByTagName("hostid");
        for (int i = 0; i < hostsList.getLength(); i++) {
            res.add(hostsList.item(i).getTextContent());
        }
        return res;
    }

    private List<RuleTask> retrieveTasks(String xml) throws
                                                     DAOConnectionException {
        ArrayList<RuleTask> res = new ArrayList<RuleTask>();
        if ((xml == null) || xml.equals("")) {
            return res;
        }
        Document document = null;
        try {
            InputStream stream = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(stream);
        } catch (Exception e) {
            throw new DAOConnectionException(e);
        }
        document.getDocumentElement().normalize();

        NodeList tasksList = document.getElementsByTagName("task");
        for (int i = 0; i < tasksList.getLength(); i++) {
            Node node = tasksList.item(i);
            if(node.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) node;
                String type = e.getElementsByTagName("type").item(0).getTextContent();
                String path = e.getElementsByTagName("path").item(0).getTextContent();
                int delay = Integer.parseInt(e.getElementsByTagName("delay")
                        .item(0).getTextContent());
                res.add(new RuleTask(type, path, delay));
            }
        }
        return res;
    }
}

