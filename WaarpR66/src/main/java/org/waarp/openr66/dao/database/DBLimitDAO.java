package org.waarp.openr66.dao.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.dao.LimitDAO;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.Limit;
import org.waarp.openr66.pojo.UpdatedInfo;

/**
 * Implementation of LimitDAO for standard SQL databases
 */
public class DBLimitDAO extends StatementExecutor implements LimitDAO {

    private static final WaarpLogger logger = WaarpLoggerFactory.getLogger(LimitDAO.class);

    protected static final String TABLE = "configuration";

    public static final String HOSTID_FIELD = "hostid";
    public static final String READ_GLOBAL_LIMIT_FIELD = "readgloballimit";
    public static final String WRITE_GLOBAL_LIMIT_FIELD = "writegloballimit";
    public static final String READ_SESSION_LIMIT_FIELD = "readsessionlimit";
    public static final String WRITE_SESSION_LIMIT_FIELD = "writesessionlimit";
    public static final String DELAY_LIMIT_FIELD = "delaylimit";
    public static final String UPDATED_INFO_FIELD = "updatedinfo";

    protected static final String SQL_DELETE_ALL = "DELETE FROM " + TABLE;
    protected static final String SQL_DELETE = "DELETE FROM " + TABLE
        + " WHERE " + HOSTID_FIELD + " = ?";
    protected static final String SQL_GET_ALL = "SELECT * FROM " + TABLE;
    protected static final String SQL_EXIST = "SELECT 1 FROM " + TABLE
        + " WHERE " + HOSTID_FIELD + " = ?";
    protected static final String SQL_SELECT = "SELECT * FROM " + TABLE
        + " WHERE " + HOSTID_FIELD + " = ?";
    protected static final String SQL_INSERT = "INSERT INTO " + TABLE
        + " (" + HOSTID_FIELD + ", "
        + READ_GLOBAL_LIMIT_FIELD + ", "
        + WRITE_GLOBAL_LIMIT_FIELD + ", "
        + READ_SESSION_LIMIT_FIELD + ", "
        + WRITE_SESSION_LIMIT_FIELD + ", "
        + DELAY_LIMIT_FIELD + ", "
        + UPDATED_INFO_FIELD + ") VALUES (?,?,?,?,?,?,?)";

    protected static final String SQL_UPDATE = "UPDATE " + TABLE
        +  " SET " + HOSTID_FIELD + " = ?, "
        + READ_GLOBAL_LIMIT_FIELD + " = ?, "
        + WRITE_GLOBAL_LIMIT_FIELD + " = ?, "
        + READ_SESSION_LIMIT_FIELD + " = ?, "
        + WRITE_SESSION_LIMIT_FIELD + " = ?, "
        + DELAY_LIMIT_FIELD + " = ?, "
        + UPDATED_INFO_FIELD + " = ? WHERE " + HOSTID_FIELD + " = ?";


    protected Connection connection;

    public DBLimitDAO(Connection con) {
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
    public void delete(Limit limit)
        throws DAOConnectionException, DAONoDataException {
        PreparedStatement stm = null;
        try {
            stm = connection.prepareStatement(SQL_DELETE);
            setParameters(stm, limit.getHostid());
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
    public List<Limit> getAll() throws DAOConnectionException {
        ArrayList<Limit> limits = new ArrayList<Limit>();
        ResultSet res = null;
        PreparedStatement stm = null;
        try {
            stm = connection.prepareStatement(SQL_GET_ALL);
            res = executeQuery(stm);
            while (res.next()) {
                limits.add(getFromResultSet(res));
            }
        } catch (SQLException e) {
            throw new DAOConnectionException(e);
        } finally {
            closeStatement(stm);
            closeResultSet(res);
        }
        return limits;
    }

    @Override
    public List<Limit> find(List<Filter> filters) throws
                                                  DAOConnectionException {
        ArrayList<Limit> limits = new ArrayList<Limit>();
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
        ResultSet res = null;
        PreparedStatement stm = null;
        try {
            stm = connection.prepareStatement(query.toString());
            setParameters(stm, params);
            res = executeQuery(stm);
            while (res.next()) {
                limits.add(getFromResultSet(res));
            }
        } catch (SQLException e) {
            throw new DAOConnectionException(e);
        } finally {
            closeStatement(stm);
            closeResultSet(res);
        }
        return limits;
    }

    @Override
    public boolean exist(String hostid) throws DAOConnectionException {
        PreparedStatement stm = null;
        ResultSet res = null;
        try {
            stm = connection.prepareStatement(SQL_EXIST);
            setParameters(stm, hostid);
            res = executeQuery(stm);
            return res.next();
        } catch (SQLException e) {
            throw new DAOConnectionException(e);
        } finally {
            closeStatement(stm);
            closeResultSet(res);
        }
    }

    @Override
    public Limit select(String hostid)
        throws DAOConnectionException, DAONoDataException {
        PreparedStatement stm = null;
        ResultSet res = null;
        try {
            stm = connection.prepareStatement(SQL_SELECT);
            setParameters(stm, hostid);
            res = executeQuery(stm);
            if (res.next()) {
                return getFromResultSet(res);
            } else {
                throw new DAONoDataException(("No " + getClass().getName() + " found"));
            }
        } catch (SQLException e) {
            throw new DAOConnectionException(e);
        } finally {
            closeStatement(stm);
            closeResultSet(res);
        }
    }

    @Override
    public void insert(Limit limit) throws DAOConnectionException {
        Object[] params = {
            limit.getHostid(),
            limit.getReadGlobalLimit(),
            limit.getWriteGlobalLimit(),
            limit.getReadSessionLimit(),
            limit.getWriteSessionLimit(),
            limit.getDelayLimit(),
            limit.getUpdatedInfo().ordinal()
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
    public void update(Limit limit)
        throws DAOConnectionException, DAONoDataException {
        Object[] params = {
            limit.getHostid(),
            limit.getReadGlobalLimit(),
            limit.getWriteGlobalLimit(),
            limit.getReadSessionLimit(),
            limit.getWriteSessionLimit(),
            limit.getDelayLimit(),
            limit.getUpdatedInfo().ordinal(),
            limit.getHostid()
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

    private Limit getFromResultSet(ResultSet set) throws SQLException {
        return new Limit(
                set.getString(HOSTID_FIELD),
                set.getLong(DELAY_LIMIT_FIELD),
                set.getLong(READ_GLOBAL_LIMIT_FIELD),
                set.getLong(WRITE_GLOBAL_LIMIT_FIELD),
                set.getLong(READ_SESSION_LIMIT_FIELD),
                set.getLong(WRITE_SESSION_LIMIT_FIELD),
                UpdatedInfo.valueOf((set.getInt(UPDATED_INFO_FIELD))));
    }
}
