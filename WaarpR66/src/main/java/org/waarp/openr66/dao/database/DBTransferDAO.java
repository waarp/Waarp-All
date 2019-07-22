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
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.database.DbConstant;
import org.waarp.openr66.dao.TransferDAO;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.pojo.Transfer;
import org.waarp.openr66.pojo.UpdatedInfo;

/**
 * Implementation of TransferDAO for a standard SQL database
 */
public abstract class DBTransferDAO extends StatementExecutor implements TransferDAO {

    private static final WaarpLogger logger = WaarpLoggerFactory.getLogger(DBTransferDAO.class);

    // Table  name
    protected static final String TABLE = "runner";

    // Field name
    public static final String ID_FIELD = "specialid";
    public static final String GLOBAL_STEP_FIELD = "globalstep";
    public static final String GLOBAL_LAST_STEP_FIELD = "globallaststep";
    public static final String STEP_FIELD = "step";
    public static final String RANK_FIELD = "rank";
    public static final String STEP_STATUS_FIELD = "stepstatus";
    public static final String RETRIEVE_MODE_FIELD = "retrievemode";
    public static final String FILENAME_FIELD = "filename";
    public static final String IS_MOVED_FIELD = "ismoved";
    public static final String ID_RULE_FIELD = "idrule";
    public static final String BLOCK_SIZE_FIELD = "blocksz";
    public static final String ORIGINAL_NAME_FIELD = "originalname";
    public static final String FILE_INFO_FIELD = "fileinfo";
    public static final String TRANSFER_INFO_FIELD = "transferinfo";
    public static final String TRANSFER_MODE_FIELD = "modetrans";
    public static final String TRANSFER_START_FIELD = "starttrans";
    public static final String TRANSFER_STOP_FIELD = "stoptrans";
    public static final String INFO_STATUS_FIELD = "infostatus";
    public static final String OWNER_REQUEST_FIELD = "ownerreq";
    public static final String REQUESTED_FIELD = "requested";
    public static final String REQUESTER_FIELD = "requester";
    public static final String UPDATED_INFO_FIELD = "updatedInfo";

    // CRUD requests
    protected static String SQL_DELETE = "DELETE FROM " + TABLE
            + " WHERE " + ID_FIELD + " = ? AND "
            + REQUESTER_FIELD + " = ? AND "
            + REQUESTED_FIELD + " = ? AND "
            + OWNER_REQUEST_FIELD  + " = ?";
    protected static String SQL_DELETE_ALL = "DELETE FROM " + TABLE;
    protected static String SQL_EXIST = "SELECT 1 FROM " + TABLE + " WHERE "
            + ID_FIELD + " = ? AND "
            + REQUESTER_FIELD + " = ? AND "
            + REQUESTED_FIELD + " = ? AND "
            + OWNER_REQUEST_FIELD + " = ?";
    protected static String SQL_GET_ALL = "SELECT * FROM " + TABLE;
    protected static String SQL_INSERT = "INSERT INTO " + TABLE
            + " (" + GLOBAL_STEP_FIELD + ", "
            + GLOBAL_LAST_STEP_FIELD + ", "
            + STEP_FIELD + ", "
            + RANK_FIELD + ", "
            + STEP_STATUS_FIELD + ", "
            + RETRIEVE_MODE_FIELD + ", "
            + FILENAME_FIELD + ", "
            + IS_MOVED_FIELD + ", "
            + ID_RULE_FIELD + ", "
            + BLOCK_SIZE_FIELD + ", "
            + ORIGINAL_NAME_FIELD + ", "
            + FILE_INFO_FIELD + ", "
            + TRANSFER_INFO_FIELD + ", "
            + TRANSFER_MODE_FIELD + ", "
            + TRANSFER_START_FIELD + ", "
            + TRANSFER_STOP_FIELD + ", "
            + INFO_STATUS_FIELD + ", "
            + OWNER_REQUEST_FIELD + ", "
            + REQUESTED_FIELD + ", "
            + REQUESTER_FIELD + ", "
            + ID_FIELD + ", "
            + UPDATED_INFO_FIELD
            + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    protected static String SQL_SELECT = "SELECT * FROM " + TABLE
            + " WHERE " + ID_FIELD + " = ? AND "
            + REQUESTER_FIELD + " = ? AND "
            + REQUESTED_FIELD + " = ? AND "
            + OWNER_REQUEST_FIELD + " = ?";
    protected static String SQL_UPDATE = "UPDATE " + TABLE
            + " SET " + ID_FIELD + " = ?, "
            + GLOBAL_STEP_FIELD + " = ?, "
            + GLOBAL_LAST_STEP_FIELD + " = ?, "
            + STEP_FIELD + " = ?, "
            + RANK_FIELD + " = ?, "
            + STEP_STATUS_FIELD + " = ?, "
            + RETRIEVE_MODE_FIELD + " = ?, "
            + FILENAME_FIELD + " = ?, "
            + IS_MOVED_FIELD + " = ?, "
            + ID_RULE_FIELD + " = ?, "
            + BLOCK_SIZE_FIELD + " = ?, "
            + ORIGINAL_NAME_FIELD + " = ?, "
            + FILE_INFO_FIELD + " = ?, "
            + TRANSFER_INFO_FIELD + " = ?, "
            + TRANSFER_MODE_FIELD + " = ?, "
            + TRANSFER_START_FIELD + " = ?, "
            + TRANSFER_STOP_FIELD + " = ?, "
            + INFO_STATUS_FIELD + " = ?, "
            + OWNER_REQUEST_FIELD + " = ?, "
            + REQUESTED_FIELD + " = ?, "
            + REQUESTER_FIELD + " = ?, "
            + UPDATED_INFO_FIELD + " = ?  WHERE "
            + OWNER_REQUEST_FIELD + " = ? AND "
            + REQUESTER_FIELD + " = ? AND "
            + REQUESTED_FIELD + " = ? AND "
            + ID_FIELD + " = ?";

    protected Connection connection;

    protected String getDeleteRequest() {
        return SQL_DELETE;
    }

    protected String getDeleteAllRequest() {
        return SQL_DELETE_ALL;
    }

    protected String getExistRequest() {
        return SQL_EXIST;
    }

    protected String getGetAllRequest() {
        return SQL_GET_ALL;
    }

    protected String getInsertRequest() {
        return SQL_INSERT;
    }

    protected String getSelectRequest() {
        return SQL_SELECT;
    }

    protected String getUpdateRequest() {
        return SQL_UPDATE;
    }

    public DBTransferDAO(Connection con) {
        this.connection = con;
    }

    @Override
    public void delete(Transfer transfer)
        throws DAOConnectionException, DAONoDataException {
        PreparedStatement stm = null;
        Object[] params = {
                transfer.getId(),
                transfer.getRequester(),
                transfer.getRequested(),
                transfer.getOwnerRequest()
        };
        try {
            stm = connection.prepareStatement(getDeleteRequest());
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

    @Override
    public void deleteAll() throws DAOConnectionException {
        PreparedStatement stm = null;
        try {
            stm = connection.prepareStatement(getDeleteAllRequest());
            executeUpdate(stm);
        } catch (SQLException e) {
            throw new DAOConnectionException(e);
        } finally {
            closeStatement(stm);
        }
    }

    @Override
    public List<Transfer> getAll() throws DAOConnectionException {
        ArrayList<Transfer> transfers = new ArrayList<Transfer>();
        PreparedStatement stm = null;
        ResultSet res = null;
        try {
            stm = connection.prepareStatement(getGetAllRequest());
            res = executeQuery(stm);
            while (res.next()) {
                transfers.add(getFromResultSet(res));
            }
        } catch (SQLException e) {
            throw new DAOConnectionException(e);
        } finally {
            closeResultSet(res);
            closeStatement(stm);
        }
        return transfers;
    }

    private String prepareFindQuery(List<Filter> filters, Object[] params) {
        StringBuilder query = new StringBuilder(getGetAllRequest());
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
        return query.toString();
    }

    @Override
    public List<Transfer> find(List<Filter> filters) throws
                                                     DAOConnectionException {
        ArrayList<Transfer> transfers = new ArrayList<Transfer>();
        // Create the SQL query
        Object[] params = new Object[filters.size()];
        String query = prepareFindQuery(filters, params);
        // Execute query
        PreparedStatement stm = null;
        ResultSet res = null;
        try {
            stm = connection.prepareStatement(query);
            setParameters(stm, params);
            res = executeQuery(stm);
            while (res.next()) {
                transfers.add(getFromResultSet(res));
            }
        } catch (SQLException e) {
            throw new DAOConnectionException(e);
        } finally {
            closeResultSet(res);
            closeStatement(stm);
        }
        return transfers;
    }

    @Override
    public List<Transfer> find(List<Filter> filters, int limit) throws
                                                                DAOConnectionException {
        ArrayList<Transfer> transfers = new ArrayList<Transfer>();
        // Create the SQL query
        Object[] params = new Object[filters.size()];
        StringBuilder query =  new StringBuilder(
                prepareFindQuery(filters, params));
        // Set LIMIT
        if (limit > 0) {
            query.append(" LIMIT " + limit);
        }
        // Execute query
        PreparedStatement stm = null;
        ResultSet res = null;
        try {
            stm = connection.prepareStatement(query.toString());
            setParameters(stm, params);
            res = executeQuery(stm);
            while (res.next()) {
                transfers.add(getFromResultSet(res));
            }
        } catch (SQLException e) {
            throw new DAOConnectionException(e);
        } finally {
            closeResultSet(res);
            closeStatement(stm);
        }
        return transfers;
    }

    @Override
    public List<Transfer> find(List<Filter> filters, int limit, int offset)
        throws DAOConnectionException {
        ArrayList<Transfer> transfers = new ArrayList<Transfer>();
        // Create the SQL query
        Object[] params = new Object[filters.size()];
        StringBuilder query =  new StringBuilder(
                prepareFindQuery(filters, params));
        // Set LIMIT
        if (limit > 0) {
            query.append(" LIMIT " + limit);
        }
        // Set OFFSET
        if (limit > 0) {
            query.append(" OFFSET " + offset);
        }
        // Execute query
        PreparedStatement stm = null;
        ResultSet res = null;
        try {
            stm = connection.prepareStatement(query.toString());
            setParameters(stm, params);
            res = executeQuery(stm);
            while (res.next()) {
                transfers.add(getFromResultSet(res));
            }
        } catch (SQLException e) {
            throw new DAOConnectionException(e);
        } finally {
            closeResultSet(res);
            closeStatement(stm);
        }
        return transfers;
    }

    @Override
    public List<Transfer> find(List<Filter> filters, String column,
                               boolean ascend) throws DAOConnectionException {
        ArrayList<Transfer> transfers = new ArrayList<Transfer>();
        // Create the SQL query
        Object[] params = new Object[filters.size()];
        StringBuilder query =  new StringBuilder(
                prepareFindQuery(filters, params));
        // Set ORDER BY
        if ((column != null) && (!column.equals(""))) {
            query.append(" ORDER BY " + column);
            if (!ascend) {
                query.append(" DESC");
            }
        }
        // Execute query
        PreparedStatement stm = null;
        ResultSet res = null;
        try {
            stm = connection.prepareStatement(query.toString());
            setParameters(stm, params);
            logger.trace(stm.toString());
            res = executeQuery(stm);
            while (res.next()) {
                transfers.add(getFromResultSet(res));
            }
        } catch (SQLException e) {
            throw new DAOConnectionException(e);
        } finally {
            closeResultSet(res);
            closeStatement(stm);
        }
        return transfers;
    }

    @Override
    public List<Transfer> find(List<Filter> filters, String column,
                               boolean ascend, int limit) throws
                                                          DAOConnectionException {
        ArrayList<Transfer> transfers = new ArrayList<Transfer>();
        // Create the SQL query
        Object[] params = new Object[filters.size()];
        StringBuilder query =  new StringBuilder(
                prepareFindQuery(filters, params));
        // Set ORDER BY
        if ((column != null) && (!column.equals(""))) {
            query.append(" ORDER BY " + column);
            if (!ascend) {
                query.append(" DESC");
            }
        }
        // Set LIMIT
        if (limit > 0) {
            query.append(" LIMIT " + limit);
        }
        // Execute query
        PreparedStatement stm = null;
        ResultSet res = null;
        try {
            stm = connection.prepareStatement(query.toString());
            setParameters(stm, params);
            res = executeQuery(stm);
            while (res.next()) {
                transfers.add(getFromResultSet(res));
            }
        } catch (SQLException e) {
            throw new DAOConnectionException(e);
        } finally {
            closeResultSet(res);
            closeStatement(stm);
        }
        return transfers;
    }

    @Override
    public List<Transfer> find(List<Filter> filters, String column,
                               boolean ascend, int limit, int offset)
        throws DAOConnectionException {
        ArrayList<Transfer> transfers = new ArrayList<Transfer>();
        // Create the SQL query
        Object[] params = new Object[filters.size()];
        StringBuilder query =  new StringBuilder(
                prepareFindQuery(filters, params));
        // Set ORDER BY
        query.append(" ORDER BY " + column);
        if (!ascend) {
            query.append(" DESC");
        }
        // Set LIMIT
        if (limit > 0) {
            query.append(" LIMIT " + limit);
        }
        // Set OFFSET
        if (limit > 0) {
            query.append(" OFFSET " + offset);
        }
        // Execute query
        PreparedStatement stm = null;
        ResultSet res = null;
        try {
            stm = connection.prepareStatement(query.toString());
            setParameters(stm, params);
            res = executeQuery(stm);
            while (res.next()) {
                transfers.add(getFromResultSet(res));
            }
        } catch (SQLException e) {
            throw new DAOConnectionException(e);
        } finally {
            closeResultSet(res);
            closeStatement(stm);
        }
        return transfers;
    }

    @Override
    public boolean exist(long id, String requester, String requested,
                         String owner) throws DAOConnectionException {
        PreparedStatement stm = null;
        ResultSet res = null;
        Object[] params = {
                id,
                requester,
                requested,
                owner
        };
        try {
            stm = connection.prepareStatement(getExistRequest());
            setParameters(stm, params);
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
    public Transfer select(long id, String requester, String requested,
                           String owner)
        throws DAOConnectionException, DAONoDataException {
        PreparedStatement stm = null;
        ResultSet res = null;
        Object[] params = {
                id,
                requester,
                requested,
                owner
        };
        try {
            stm = connection.prepareStatement(getSelectRequest());
            setParameters(stm, params);
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

    abstract protected long getNextId() throws DAOConnectionException;

    @Override
    public void insert(Transfer transfer) throws DAOConnectionException {
        if (transfer.getId() == DbConstant.ILLEGALVALUE) {
            transfer.setId(getNextId());
        }

        Object[] params = {
                transfer.getGlobalStep().ordinal(),
                transfer.getLastGlobalStep().ordinal(),
                transfer.getStep(),
                transfer.getRank(),
                transfer.getStepStatus().getCode(),
                transfer.getRetrieveMode(),
                transfer.getFilename(),
                transfer.getIsMoved(),
                transfer.getRule(),
                transfer.getBlockSize(),
                transfer.getOriginalName(),
                transfer.getFileInfo(),
                transfer.getTransferInfo(),
                transfer.getTransferMode(),
                transfer.getStart(),
                transfer.getStop(),
                transfer.getInfoStatus().getCode(),
                transfer.getOwnerRequest(),
                transfer.getRequested(),
                transfer.getRequester(),
                transfer.getId(),
                transfer.getUpdatedInfo().ordinal()
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
    public void update(Transfer transfer)
        throws DAOConnectionException, DAONoDataException {
        Object[] params = {
                transfer.getId(),
                transfer.getGlobalStep().ordinal(),
                transfer.getLastGlobalStep().ordinal(),
                transfer.getStep(),
                transfer.getRank(),
                transfer.getStepStatus().getCode(),
                transfer.getRetrieveMode(),
                transfer.getFilename(),
                transfer.getIsMoved(),
                transfer.getRule(),
                transfer.getBlockSize(),
                transfer.getOriginalName(),
                transfer.getFileInfo(),
                transfer.getTransferInfo(),
                transfer.getTransferMode(),
                transfer.getStart(),
                transfer.getStop(),
                transfer.getInfoStatus().getCode(),
                transfer.getOwnerRequest(),
                transfer.getRequested(),
                transfer.getRequester(),
                transfer.getUpdatedInfo().ordinal(),
                transfer.getOwnerRequest(),
                transfer.getRequester(),
                transfer.getRequested(),
                transfer.getId()
        };

        PreparedStatement stm = null;
        try {
            stm = connection.prepareStatement(getUpdateRequest());
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

    private Transfer getFromResultSet(ResultSet set) throws SQLException {
        return new Transfer(
                set.getLong(ID_FIELD),
                set.getString(ID_RULE_FIELD),
                set.getInt(TRANSFER_MODE_FIELD),
                set.getString(FILENAME_FIELD),
                set.getString(ORIGINAL_NAME_FIELD),
                set.getString(FILE_INFO_FIELD),
                set.getBoolean(IS_MOVED_FIELD),
                set.getInt(BLOCK_SIZE_FIELD),
                set.getBoolean(RETRIEVE_MODE_FIELD),
                set.getString(OWNER_REQUEST_FIELD),
                set.getString(REQUESTER_FIELD),
                set.getString(REQUESTED_FIELD),
                set.getString(TRANSFER_INFO_FIELD),
                Transfer.TASKSTEP.valueOf(set.getInt(GLOBAL_STEP_FIELD)),
                Transfer.TASKSTEP.valueOf(set.getInt(GLOBAL_LAST_STEP_FIELD)),
                set.getInt(STEP_FIELD),
                ErrorCode.getFromCode(set.getString(STEP_STATUS_FIELD)),
                ErrorCode.getFromCode(set.getString(INFO_STATUS_FIELD)),
                set.getInt(RANK_FIELD),
                set.getTimestamp(TRANSFER_START_FIELD),
                set.getTimestamp(TRANSFER_STOP_FIELD),
                UpdatedInfo.valueOf(set.getInt(UPDATED_INFO_FIELD)));
    }

    @Override
    public void close() {
        try {
            this.connection.close();
        } catch (SQLException e) {
            logger.warn("Cannot properly close the database connection", e);
        }
    }
}

