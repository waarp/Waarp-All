package org.waarp.openr66.dao.database.oracle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.waarp.openr66.dao.database.DBTransferDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;

public class OracleTransferDAO extends DBTransferDAO {

    protected static String SQL_GET_ID = "SELECT runseq.nextval FROM DUAL";

    public OracleTransferDAO(Connection con) throws DAOConnectionException {
        super(con);
    }

    @Override
    protected long getNextId() throws DAOConnectionException {
        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(SQL_GET_ID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            } else {
                throw new DAOConnectionException(
                        "Error no id available, you should purge the database.");
            }
        } catch (SQLException e) {
            throw new DAOConnectionException(e);
        } finally {
            closeStatement(ps);
        }
    }
}
